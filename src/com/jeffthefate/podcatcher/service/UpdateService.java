package com.jeffthefate.podcatcher.service;

import java.io.IOException;
import java.util.ArrayList;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DatabaseUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.TextUtils;
import android.util.Log;

import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.DatabaseHelper;
import com.jeffthefate.podcatcher.DatabaseHelper.MagicType;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.Util;
import com.jeffthefate.podcatcher.Util.Magics;
import com.jeffthefate.podcatcher.VersionedGetAuthToken;
import com.jeffthefate.podcatcher.activity.ActivityMain;

/**
 * Service that runs in the background.  Registers receivers for actions that
 * the app will respond to.  Also, handles starting the widget updates.
 * 
 * @author Jeff
 */
public class UpdateService extends IntentService {

    String[] mAccountInfo;

    private boolean mGoodToken = false;
    private String apiKey;

    private Builder nBuilder;
    private NotificationManager nManager;
    private Notification notification;
    
    private NotificationReceiver nReceiver;
    private CancelReceiver nCancel;
    
    private PowerManager pm;
    PowerManager.WakeLock wakeLock;
    
    ArrayList<String> feeds;
    private StringBuilder currUrl = new StringBuilder();
    String title;
    
    boolean isSyncing = false;
    boolean remove = false;
    private boolean force = false;
    
    private StringBuilder nTitleBuilder = new StringBuilder();
    private StringBuilder dbBuilder = new StringBuilder();
    private StringBuilder checkTokenBuilder = new StringBuilder();
    private StringBuilder removeFeedBuilder = new StringBuilder();
    private StringBuilder addFeedsBuilder = new StringBuilder();
    private StringBuilder retrieveBuilder = new StringBuilder();
    
    /*
     * Required Service Methods
     */
    public UpdateService() {
        super(Constants.UPDATE_SERVICE_NAME);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        /* Create wake lock so the device wakes up for an update */
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                Constants.UPDATE_WAKE_LOCK);
        /* Start building the notification to display while updating */
        /* When user selects the notification, app opens to main view */
        Intent notificationIntent = new Intent(this, ActivityMain.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, 
                notificationIntent, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        nBuilder = new NotificationCompat.Builder(ApplicationEx.getApp());
        nBuilder.setTicker(Constants.STARTING_UPDATE).
            setWhen(System.currentTimeMillis()).
            setContentTitle(Constants.UPDATING).
            setContentText(Constants.EMPTY).
            addAction(R.drawable.ic_notification_cancel, Constants.CANCEL,
                    PendingIntent.getBroadcast(this, 0,
                            new Intent(Constants.ACTION_CANCEL_UPDATE), 0)).
            setContentIntent(pendingIntent);
        nManager = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        /* Cancel any previous notifications created by the update service */
        nManager.cancel(5555);
        /* Register receiver to update the notification when a new feed is
         * found */
        nReceiver = new NotificationReceiver();
        nCancel = new CancelReceiver();
        registerReceiver(nReceiver, 
                new IntentFilter(Constants.ACTION_UPDATE_NOTIFICATION));
        registerReceiver(nCancel, 
                new IntentFilter(Constants.ACTION_CANCEL_UPDATE));
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        /* If the update manager isn't currently running (doesn't exist)
         * then start the wake lock and intialize the update manager */
        wakeLock.acquire();
        feeds = new ArrayList<String>();
        if (intent.getExtras() != null) {
            /* If syncing with Google Reader is enabled */
            if (intent.hasExtra(Constants.SYNC))
                isSyncing = intent.getBooleanExtra(Constants.SYNC, false);
            /* If a list of feeds is passed - more than one feed is to
             * be updated */
            if (intent.hasExtra(Constants.FEED_LIST))
                feeds = intent.getStringArrayListExtra(Constants.FEED_LIST);
            else {
                /* If updating just one feed */
                if (intent.hasExtra(Constants.FEED)) {
                    feeds.add(intent.getStringExtra(Constants.FEED));
                }
                /* If we're unsubscribing to a feed */
                remove = intent.getBooleanExtra(Constants.UNSUBSCRIBE, false);
                /* Getting the passed title for the feed to unsubscribe */
                if (remove && intent.hasExtra(Constants.TITLE))
                    title = intent.getStringExtra(Constants.TITLE);
                force = intent.getBooleanExtra(Constants.FORCE, false);
            }
        }
        /* Creating and starting update manager to do the update(s) */
        if (!feeds.isEmpty())
            startUpdating();
    }
    
    /*
     * Private Classes
     */
    
    /*
     * BroadcastReceivers
     */
    
    /*
     * Receive updates to the notification content text; displaying a new feed
     */
    private class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String title = intent.getStringExtra(Constants.TITLE);
            nTitleBuilder.setLength(0);
            nTitleBuilder.append(Constants.NEW_FEED).append(title);
            nBuilder.setContentText(nTitleBuilder);
            notification = nBuilder.build();
            nManager.notify(null, 1337, notification);
        }
    }
    
    /*
     * Cancel the updating
     */
    private class CancelReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopNotificationIcon();
            stopForeground(true);
            cancel = true;
        }
    }

    private ArrayList<String> failedList = new ArrayList<String>();
    private int retries = 0;
    private boolean cancel = false;
    
    private int level = 0;
    private Thread thread;
    private boolean notifyStopped = false;
    
    private void updateNotificationIcon() {
        if (thread != null)
            stopNotificationIcon();
        thread = new Thread() {
            public void run() {
                try {
                    while (true) {
                        if (Thread.interrupted() || notifyStopped)
                            throw new InterruptedException();
                        if (level < 0)
                            level = 3;
                        nBuilder.setSmallIcon(R.drawable.ic_notification_update, level);
                        notification = nBuilder.build();
                        nManager.notify(null, 1337, notification);
                        level--;
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {}
            }
        };
        thread.start();
    }
    
    private void stopNotificationIcon() {
        notifyStopped = true;
        if (thread != null)
            thread.interrupt();
        thread = null;
    }

    @SuppressLint("NewApi")
    protected void startUpdating() {
        ArrayList<String> subsList = new ArrayList<String>();
        String token = null;
        /* Grab saved Google account info */
        mAccountInfo = ApplicationEx.dbHelper.getAccount();
        /* Check for a data connection, if there isn't one display a
         * notification with ability to try again */
        if (ApplicationEx.hasConnection() && Util.checkConnectionNoTask()) {
            nBuilder.setSmallIcon(R.drawable.ic_notification_update, level);
            notification = nBuilder.build();
            startForeground(1337, notification);
            level++;
            updateNotificationIcon();
        }
        else {
            failedList.addAll(feeds);
            failByConnection(failedList);
            return;
        }
        /* Check and get a new token (if necessary) if the token is old
         * enough */
        if (!cancel && isSyncing && mAccountInfo != null) {
            if (Util.checkConnectionNoTask()) {
                checkToken();
                if (!mGoodToken) {
                    /* If no good token is available, get a new token */
                    Account[] mAccounts = AccountManager.get(
                            getApplicationContext()).getAccountsByType(
                                    mAccountInfo[2]);
                    Account mAccount = null;
                    for (Account account : mAccounts) {
                        if (account.name.equals(mAccountInfo[0])) {
                            mAccount = account;
                            break;
                        }
                    }
                    GetAuthTokenCallback callback =
                            new GetAuthTokenCallback();
                    VersionedGetAuthToken.newInstance().create(mAccount,
                            callback).getAuthToken();
                }
            }
            /* If there is a working token, add any new feeds that
             * aren't added to Reader, then get all feeds from Reader */
            addFeeds(token, ApplicationEx.dbHelper.getNewFeeds());
            subsList = retrieveFeeds(apiKey);
        }
        if (cancel)
            return;
        /* If updating just sync'd feeds, get all local feeds and any newly
         * added ones on Reader */
        if (!cancel && feeds.isEmpty()) {
            feeds = ApplicationEx.dbHelper.getFeedAddresses(1);
            if (subsList != null) {
                subsList.removeAll(
                        ApplicationEx.dbHelper.getFeedAddresses(0));
                for (String feed : feeds) {
                    if (subsList.contains(feed))
                        subsList.remove(feed);
                }
                feeds.addAll(subsList);
            }
        }
        if (cancel)
            return;
        /* Update result
         * -1   failure
         * 0    no new episodes
         * 1+   new episode
         */
        ArrayList<String> result = null;
        ArrayList<String> newList = new ArrayList<String>();
        boolean failed = false;
        boolean retry = false;
        int feedId;
        ContentValues cv;
        /* Step through all feeds */
        for (String url : feeds) {
            if (cancel)
                return;
            /* Update the notification and format the feed URL */
            nBuilder.setContentTitle(Constants.UPDATE_NOTIFY_TITLE);
            currUrl.setLength(0);
            currUrl.append(url);
            cv = new ContentValues();
            cv.put(DatabaseHelper.COL_FEED_ADDRESS, currUrl.toString());
            dbBuilder.setLength(0);
            dbBuilder.append(DatabaseHelper.COL_FEED_ADDRESS)
                    .append(Constants.EQUAL)
                    .append(DatabaseUtils.sqlEscapeString(url));
            ApplicationEx.dbHelper.updateRecord(cv,
                    DatabaseHelper.FEED_TABLE, dbBuilder.toString());
            failed = false;
            retry = false;
            do {
                if (cancel)
                    return;
                feedId = ApplicationEx.dbHelper.getInt(currUrl.toString(), 
                        DatabaseHelper.COL_FEED_ID, 
                        DatabaseHelper.FEED_TABLE, 
                        DatabaseHelper.COL_FEED_ADDRESS);
                /* Unsubscribe feed from Reader if there is vaild token */
                if (remove && token != null) {
                    nTitleBuilder.setLength(0);
                    nTitleBuilder.append(Constants.REMOVE_NOTIFY_TITLE)
                            .append(title);
                    nBuilder.setContentText(nTitleBuilder.toString());
                    notification = nBuilder.build();
                    nManager.notify(null, 1337, notification);
                    failed = !removeFeed(token, apiKey, currUrl);
                    /* Unable to connect and send request to Reader */
                    if (failed)
                        /* Keep retrying if you should retry */
                        retry = retryFailure();
                    else
                        /* Remove the database records for the feed if
                         * successful delete from Reader */
                        ApplicationEx.dbHelper.deleteFeed(feedId);
                    continue;
                }
                /* Add the feed to the database if it is a new one from
                 * Reader */
                if (!ApplicationEx.dbHelper.feedExists(
                        currUrl.toString()) && !cancel) {
                    result = Util.addNewFeed(currUrl.toString());
                    if (result != null && result.size() > 0)
                        newList.addAll(result);
                    else if (result == null) {
                        failed = true;
                        retry = retryFailure();
                    }
                    break;
                }
                else if (ApplicationEx.dbHelper.getInt(currUrl.toString(), 
                        DatabaseHelper.COL_FEED_PODCAST, 
                        DatabaseHelper.FEED_TABLE, 
                        DatabaseHelper.COL_FEED_ADDRESS)
                            != 1)
                    break;
                if (cancel)
                    return;
                /* Set the feed title in the notification */
                updateNotification();
                /* This downloads the XML for the feed, then parses the feed
                 * and episode information.  Inserts new episodes and upates
                 * episode and feed information in the database */
                result = Util.getXml(currUrl.toString(), force);
                if (cancel)
                    return;
                /* There was a new episode found */
                if (result != null && result.size() > 0) {
                    ApplicationEx.getApp().sendBroadcast(
                            new Intent(Constants.ACTION_REFRESH_EPISODES));
                    ApplicationEx.getApp().sendBroadcast(
                            new Intent(Constants.ACTION_REFRESH_FEEDS));
                    newList.addAll(result);
                }
                /* There was some error, so need to try again */
                else if (result == null) {
                    failed = true;
                    retry = retryFailure();
                }
                /* No new episodes found */
                else {
                    failed = false;
                    /* Get the latest changes from Google Reader */
                    if (isSyncing) {
                        // TODO
                    }
                    /* If using magic feed update, add a little more time
                     * to the magic interval to try again soon */
                    ContentValues feedCv = new ContentValues();
                    MagicType magicType =
                            ApplicationEx.dbHelper.calculateMagicIntervalNew(
                                    feedId);
                    Log.i(Constants.LOG_TAG, "current: " + currUrl);
                    Log.i(Constants.LOG_TAG, "calculated magic: " + magicType.getTime() + ":" + magicType.getType().toString());
                    feedCv.put(DatabaseHelper.COL_FEED_MAGIC_INTERVAL,
                            magicType.getTime());
                    feedCv.put(DatabaseHelper.COL_FEED_MAGIC_TYPE,
                            magicType.getType());
                    ApplicationEx.dbHelper.updateRecord(feedCv,
                            DatabaseHelper.FEED_TABLE,
                            TextUtils.concat(DatabaseHelper.COL_FEED_ID,
                            Constants.EQUAL,
                            Integer.toString(feedId)).toString());
                }
                /* Update the time feed last updated */
                cv = new ContentValues();
                cv.put(DatabaseHelper.COL_FEED_LAST_UPDATE, 
                        System.currentTimeMillis());
                dbBuilder.setLength(0);
                dbBuilder.append(DatabaseHelper.COL_FEED_ID)
                        .append(Constants.EQUAL)
                        .append(feedId);
                ApplicationEx.dbHelper.updateRecord(cv, 
                        DatabaseHelper.FEED_TABLE,
                        dbBuilder.toString());
            /* Keep trying again if there is failure and we should retry */
            } while (failed && retry && !cancel);
            if (cancel)
                return;
            /* Reset retries if we are done processing this feed */
            retries = 0;
            /* Reset failed value for feed if there was a success */
            if (!failed) {
                ApplicationEx.dbHelper.resetFail(currUrl.toString());
            }
            /* Otherwise add a failed try and add the current to failed
             * list */
            else {
                ApplicationEx.dbHelper.addFailedTry(currUrl.toString());
                failedList.add(currUrl.toString());
            }
        }
        /* After all the updates, we have all the new episodes */
        /* Check if any are currently downloading or have finished since the
         * list was created */
        ArrayList<String> newListTemp = new ArrayList<String>(newList);
        for (String episode : newListTemp) {
            if (ApplicationEx.downloadList.contains(episode) ||
                    ApplicationEx.dbHelper.getEpisodeDownloaded(episode))
                newList.remove(episode);
        }
        Log.e(Constants.LOG_TAG, "newList size: " + newList.size());
        Log.e(Constants.LOG_TAG, "download key: " + Util.readBooleanPreference(
                R.string.download_key, false));
        if (newList.size() > 0 && Util.readBooleanPreference(
                    R.string.download_key, false) &&
                (Util.readBooleanPreference(R.string.power_key, false) ?
                        Util.getConnectedState() > 0 : true) &&
                ((Util.readBooleanPreference(R.string.wifi_key, false) ?
                        Util.hasWifi() : true) ||
                (Util.readBooleanPreference(R.string.four_g_key, false) ?
                        Util.has4G() : true))) {
            String[] urlArray = new String[newList.size()];
            newList.toArray(urlArray);
            /* Add them to a currently running download service or start the
             * service with all the episode URLs */
            if (Util.isMyServiceRunning(DownloadService.class.getName())) {
                Intent downloadIntent = new Intent(
                        Constants.ACTION_START_DOWNLOAD);
                downloadIntent.putExtra(Constants.URLS, urlArray);
                ApplicationEx.getApp().sendBroadcast(downloadIntent);
            }
            else {
                Intent downloadIntent = new Intent(ApplicationEx.getApp(), 
                        DownloadService.class);
                downloadIntent.putExtra(Constants.URLS, urlArray);
                ApplicationEx.getApp().startService(downloadIntent);
            }
        }
        /* If any failed, setup a failed notification and display */
        if (!failedList.isEmpty())
            failByConnection(failedList);
        /* If magic update enabled, set the next alarm for update */
        if (Integer.parseInt(Util.readStringPreference(
                R.string.update_key, Constants.ZERO)) == 2) {
            if (failedList.isEmpty()) {
                Magics currMagics =
                        ApplicationEx.dbHelper.getNextMagicInterval();
                Util.setMagicAlarm(currMagics.getTime(), currMagics.getFeeds());
            }
            else
                Util.setMagicAlarm(System.currentTimeMillis() +
                        Integer.parseInt(Util.readStringPreference(
                                R.string.interval_key, Constants.SIXTY)),
                        failedList);
        }
    }
    /*
     * Check to see if you can retry a failure. The most common problem is
     * connectivity, so when we get connectivity we try again.
     */
    private boolean retryFailure() {
        /* Increase how many times we've retried */
        retries++;
        boolean cont = false;
        boolean timeout = false;
        long time = System.currentTimeMillis();
        NetworkInfo networkInfo;
        ConnectivityManager connMan = (ConnectivityManager) ApplicationEx
                .getApp().getSystemService(Context.CONNECTIVITY_SERVICE);
        /* Keep trying to get a connected network for 10 seconds */
        if (cancel)
            return false;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
            networkInfo = connMan.getActiveNetworkInfo();
            timeout = System.currentTimeMillis()-time >= 10000;
            cont = !timeout && (networkInfo == null || 
                    networkInfo.getState() != NetworkInfo.State.CONNECTED);
        } while (cont && !cancel);
        /* True if we didn't time out and it hasn't failed more than 4 times
         * and it hasn't retried more than 4 times */
        return !timeout && ApplicationEx.dbHelper.didFail(
                currUrl.toString()) < 5 && retries < 5;
    }

    private void updateNotification() {
        /* Display the current feed title in the notification */
        nBuilder.setContentText(ApplicationEx.dbHelper.getString(
                currUrl.toString(), DatabaseHelper.COL_FEED_TITLE, 
                DatabaseHelper.FEED_TABLE, 
                DatabaseHelper.COL_FEED_ADDRESS));
        notification = nBuilder.build();
        nManager.notify(null, 1337, notification);
    }
    
    /*
     * Other
     */
    
    /*
     * Callback started after the Google token is returned.
     */
    private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
        
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                /* Get the returned intent, if any */
                bundle = (Bundle) result.getResult();
                Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
                /* An intent is returned if it is required to authorize the
                 * token for the service */
                if(intent != null)
                    startActivity(intent);
                /* Otherwise we have a token and can process it */
                else
                    onGetAuthToken(bundle);
            } catch (OperationCanceledException e) {
                Log.e(Constants.LOG_TAG, "Authentication request canceled", e);
            } catch (AuthenticatorException e) {
                Log.e(Constants.LOG_TAG, "Invalid authentication", e);
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, "Authentication server failed to " + 
                        "respond", e);
            }
        }
        /* Save the token in the database and indicate we have a good token */
        private void onGetAuthToken(Bundle bundle) {
            Util.saveAccountInfo(bundle, mAccountInfo);
            mGoodToken = true;
        }
    };
    
    private void failByConnection(ArrayList<String> failedList) {
        Intent failIntent = new Intent(ApplicationEx.getApp(), 
                UpdateService.class);
        // Needs action or doesn't work
        failIntent.setAction(Constants.NOTHING);
        failIntent.putExtra(Constants.SYNC, ApplicationEx.isSyncing() && 
                Util.readBooleanFromFile(Constants.GOOGLE_FILENAME));
        failIntent.putExtra(Constants.FEED_LIST, failedList);
        nBuilder.setWhen(System.currentTimeMillis()).
            setContentTitle(Constants.UPDATE_FAILED).
            setContentText(Constants.RETRY_FAILED).
            setContentIntent(PendingIntent.getService(
                    ApplicationEx.getApp(), 0, failIntent, 0));
        notification = nBuilder.build();
        nManager.notify(null, 5555, notification);
    }
    /*
     * Test the current token with the Google Reader system
     */
    private boolean checkToken() {
        /* Not sure if we have a good token, use it to access Reader */
        Document doc = null;
        checkTokenBuilder.setLength(0);
        checkTokenBuilder.append(Constants.AUTH_PARAMS).append(mAccountInfo[1]);
        Connection mConnection = Jsoup.connect(
                Constants.SUBSCRIPTION_LIST_URL)
                .header(Constants.AUTH_HEADER, checkTokenBuilder.toString())
                .header(Constants.CONTENT_TYPE_HEADER,
                        Constants.CONTENT_TYPE_VALUE)
                .userAgent(Constants.GOOGLE_USER_AGENT)
                .timeout(5000);
        try {
            doc = mConnection.get();
        } catch (IOException e) {
            checkTokenBuilder.setLength(0);
            checkTokenBuilder.append(Constants.SUBSCRIPTION_LIST_URL)
                    .append(Constants.FAILED_RESPOND)
                    .append(mConnection.response().statusCode());
            Log.e(Constants.LOG_TAG, checkTokenBuilder.toString(), e);
        }
        /* If we think we have a good connection and still don't get a
         * document returned, then we have an invalid token, so we
         * invalidate it and indicate we don't have a good token */
        if (doc == null && ApplicationEx.hasConnection() && 
                Util.checkConnectionNoTask()) {
            AccountManager.get(getApplicationContext())
            .invalidateAuthToken(
                    "com.google", mAccountInfo[1]);
            mGoodToken = false;
        }
        /* Otherwise we have a good token */
        else
            mGoodToken = true;
        /* Save the state of the token */
        Util.writeBufferToFile(Boolean.toString(!mGoodToken).getBytes(),
                Constants.CHECKTOKEN_FILENAME);
        return mGoodToken;
    }

    @Override
    public void onDestroy() {
        /* Let go of the wake lock */
        wakeLock.release();
        /* Unregister the notifcation receiver (*/
        unregisterReceiver(nReceiver);
        unregisterReceiver(nCancel);
        /* Stop the ongoing notification */
        stopNotificationIcon();
        stopForeground(true);
        super.onDestroy();
    }
    /*
     * Unsubscribe from a feed in Google Reader
     */
    private boolean removeFeed(String token, String apiKey,
            StringBuilder feedUrl) {
        Response res = null;
        Connection mConnection = null;
        boolean passed = false;
        /* Setup the URL to unsubscribe from the feed in Reader */
        removeFeedBuilder.setLength(0);
        removeFeedBuilder.append(Constants.AUTH_PARAMS).append(apiKey);
        mConnection = Jsoup.connect(Constants.SUBSCRIPTION_EDIT_URL)
                .method(Connection.Method.POST)
                .data("s", feedUrl.toString())
                .data("ac", "unsubscribe")
                .data("T", token)
                .header(Constants.AUTH_HEADER, removeFeedBuilder.toString())
                .header(Constants.CONTENT_TYPE_HEADER,
                        Constants.CONTENT_TYPE_VALUE)
                .userAgent(Constants.GOOGLE_USER_AGENT)
                .ignoreHttpErrors(true)
                .timeout(0);
        try {
            res = mConnection.execute();
        } catch (IOException e) {
            removeFeedBuilder.setLength(0);
            removeFeedBuilder.append(Constants.SUBSCRIPTION_LIST_URL)
                    .append(feedUrl)
                    .append(Constants.FAILED_RESPOND)
                    .append(mConnection.response().statusCode());
            Log.e(Constants.LOG_TAG, removeFeedBuilder.toString(), e);
        }
        /* If we don't get a response, then we fail */
        if (res == null)
            passed = false;
        /* Otherwise, if we get a 200 code in the response, we succeed */
        else {
            if (res.statusCode() == 200)
                passed = true;
        }
        return passed;
    }
    /*
     * Add a list of feeds to Google Reader
     */
    private boolean addFeeds(String token, ArrayList<String> feedUrls) {
        Response res = null;
        Connection mConnection = null;
        boolean passed = false;
        /* Need a token */
        if (token == null)
            return passed;
        for (String feedUrl : feedUrls) {
            /* Create a URL that adds the feedUrl with the Google Queue label */
            addFeedsBuilder.setLength(0);
            addFeedsBuilder.append(feedUrl);
            mConnection = Jsoup.connect(Constants.SUBSCRIPTION_EDIT_URL)
                    .method(Connection.Method.POST)
                    .data("s", addFeedsBuilder.toString());
            addFeedsBuilder.setLength(0);
            addFeedsBuilder.append(Constants.AUTH_PARAMS).append(apiKey);
            mConnection.data("ac", "subscribe")
                    .data("T", token)
                    .data("a", "user/-/label/Google Queue")
                    .header(Constants.AUTH_HEADER, addFeedsBuilder.toString())
                    .header(Constants.CONTENT_TYPE_HEADER,
                            Constants.CONTENT_TYPE_VALUE)
                    .userAgent(Constants.GOOGLE_USER_AGENT)
                    .ignoreHttpErrors(true)
                    .timeout(0);
            try {
                res = mConnection.execute();
            } catch (IOException e) {
                addFeedsBuilder.setLength(0);
                addFeedsBuilder.append(Constants.SUBSCRIPTION_EDIT_URL)
                        .append(feedUrl)
                        .append(Constants.FAILED_RESPOND)
                        .append(mConnection.response().statusCode());
                Log.e(Constants.LOG_TAG, addFeedsBuilder.toString(), e);
            }
            /* Null response means we failed */
            if (res == null)
                passed = false;
            /* Otherwise, if we get a 200 response code, we update the feed with
             * the Google Reader version of the URL */
            else {
                if (mConnection.response().statusCode() == 200) {
                    addFeedsBuilder.setLength(0);
                    addFeedsBuilder.append(feedUrl);
                    ContentValues cv = new ContentValues();
                    cv.put(DatabaseHelper.COL_FEED_ADDRESS,
                            addFeedsBuilder.toString());
                    addFeedsBuilder.setLength(0);
                    addFeedsBuilder.append(DatabaseHelper.COL_FEED_ADDRESS)
                            .append(Constants.EQUAL)
                            .append(DatabaseUtils.sqlEscapeString(feedUrl));
                    ApplicationEx.dbHelper.updateRecord(cv, 
                            DatabaseHelper.FEED_TABLE,
                            addFeedsBuilder.toString());
                }
            }
        }
        return passed;
    }
    /*
     * Get the list of feeds from Google Reader
     */
    private ArrayList<String> retrieveFeeds(String apiKey) {
        Document doc = null;
        /* Create URL to connect to Google Reader */
        retrieveBuilder.setLength(0);
        retrieveBuilder.append(Constants.AUTH_PARAMS).append(apiKey);
        Connection mConnection = Jsoup.connect(
                Constants.SUBSCRIPTION_LIST_URL)
                .header(Constants.AUTH_HEADER, retrieveBuilder.toString())
                .header(Constants.CONTENT_TYPE_HEADER,
                        Constants.CONTENT_TYPE_VALUE)
                .userAgent(Constants.GOOGLE_USER_AGENT)
                .timeout(0);
        try {
            doc = mConnection.get();
        } catch (IOException e) {
            retrieveBuilder.setLength(0);
            retrieveBuilder.append(Constants.SUBSCRIPTION_LIST_URL)
                    .append(Constants.FAILED_RESPOND)
                    .append(mConnection.response().statusCode());
            Log.e(Constants.LOG_TAG, retrieveBuilder.toString(), e);
        }
        /* If we succeed, the response contains all the subscribed feeds */
        if (doc != null) {
            /* Response is in JSON */
            String jsonString = doc.getAllElements().text();
            /* Parse and return the result */
            return Util.parseJsonString(jsonString, "subscriptions");
        }
        /* Otherwise, it is an error and we return null */
        else
            Log.e(Constants.LOG_TAG, 
                    "Google Reader subscriptions returned null");
        return null;
    }

}