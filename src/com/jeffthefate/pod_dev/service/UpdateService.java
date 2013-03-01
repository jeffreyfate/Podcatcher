package com.jeffthefate.pod_dev.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.Constants;
import com.jeffthefate.pod_dev.DatabaseHelper;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.Util;
import com.jeffthefate.pod_dev.VersionedGetAuthToken;
import com.jeffthefate.pod_dev.Util.ListWriteTask;
import com.jeffthefate.pod_dev.Util.Magics;
import com.jeffthefate.pod_dev.VersionedNotificationBuilder;
import com.jeffthefate.pod_dev.activity.ActivitySplash;
import com.jeffthefate.pod_dev.activity.ActivitySplash.GetAuthTokenCallback;

/**
 * Service that runs in the background.  Registers receivers for actions that
 * the app will respond to.  Also, handles starting the widget updates.
 * 
 * @author Jeff
 */
public class UpdateService extends Service {

    String[] mAccountInfo;

    private boolean mGoodToken = false;
    private String apiKey;

    private VersionedNotificationBuilder nBuilder;
    private NotificationManager nManager;
    private Notification notification;
    
    private NotificationReceiver nReceiver;
    
    PowerManager.WakeLock wakeLock;
    
    ArrayList<String> feeds;
    String currUrl;
    String title;
    
    boolean isSyncing = false;
    boolean remove = false;
    
    UpdateManager um;
    
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
            String title = intent.getStringExtra("title");
            nBuilder.setContentText("New feed: " + title);
            notification = nBuilder.getNotification();
            nManager.notify(null, 1337, notification);
        }
    }
    /*
     * AsyncTasks
     */
    
    /*
     * Syncs with Google Reader: gets Google IDs for episodes and keeps the read
     * values for episodes in sync.
     */
    private class UpdateSync extends AsyncTask<Void, Void, Void> {
        int feedId;
        String url;
        
        UpdateSync(int feedId, String url) {
            this.feedId = feedId;
            this.url = url;
        }
        
        @Override
        protected Void doInBackground(Void... nothing) {
            /* Get the list of episodes that don't have a Google ID */
            ArrayList<String> noGoogle = 
                ApplicationEx.dbHelper.getNoGoogleEpisodes(feedId);
            /* If there are episodes missing Google IDs get the ids from Google
             * Reader */
            if (!noGoogle.isEmpty()) {
                String[] account = ApplicationEx.dbHelper.getAccount();
                if (account != null) {
                    String authToken = account[1];
                    Util.getFeedItemIds(authToken, noGoogle, currUrl,
                            ApplicationEx.dbHelper.getNumEpisodes(currUrl));
                }
            }
            /* Get the episode IDs for the current feed */
            ArrayList<String> epIdsList = ApplicationEx.dbHelper.getEpisodeIds(
                        currUrl);
            if (!epIdsList.isEmpty()) {
                /* Get the list of episodes that have previously failed sync */
                ArrayList<String> failedSyncList = ApplicationEx.dbHelper
                        .getFailedSyncEpisodes(feedId);
                /* Take out the ones that don't have a Google ID, as they won't
                 * sync anyway */
                failedSyncList.removeAll(noGoogle);
                /* Retry the sync of previously failed episodes */
                for (String epUrl : failedSyncList) {
                    if (apiKey != null) {
                        String googleId = ApplicationEx.dbHelper
                            .getEpisodeGoogleId(epUrl);
                        if (googleId != null && !googleId
                                .equals(Constants.NO_GOOGLE)) {
                            /* Mark it with the read status from the database.
                             * Not using the task because we want this
                             * operation to finish before we move on -
                             * not asynchronous. */
                            if (!Util.markItemRead(apiKey, url, googleId,
                                    ApplicationEx.dbHelper.getEpisodeRead(epUrl)
                                        == Constants.READ ? true : false))
                                ApplicationEx.dbHelper.setFailedSync(
                                    ApplicationEx.dbHelper.getEpisodeIdFromGId(
                                                googleId));
                            else
                                ApplicationEx.dbHelper.resetFailedSync(
                                    ApplicationEx.dbHelper.getEpisodeIdFromGId(
                                                googleId));
                        }
                    }
                }
                /* Get all the read values for the current episodes */
                HashMap<String, Boolean> idMap = Util.getItemsRead(epIdsList,
                        apiKey);
                if (idMap == null)
                    return null;
                ContentValues cv = new ContentValues();
                for (Entry<String, Boolean> entry : 
                        idMap.entrySet()) {
                    /* If the values match OR sync has failed previously, don't
                     * update the value in the database from Google */
                    if ((ApplicationEx.dbHelper.getEpisodeReadFromGId(
                            entry.getKey()) == Constants.READ ? true : false) == 
                                entry.getValue() || 
                            ApplicationEx.dbHelper.getFailedSync(
                                    entry.getKey())) {
                        continue;
                    }
                    /* Otherwise, put the new read value in the database */
                    cv.clear();
                    cv.put(DatabaseHelper.COL_EPISODE_READ, entry.getValue() ? 
                                Constants.READ : Constants.UNREAD);
                    ApplicationEx.dbHelper.updateRecord(cv,
                            DatabaseHelper.EPISODE_TABLE,
                            DatabaseHelper.COL_EPISODE_GOOGLE_ID + "='" +
                                entry.getKey() + "'");
                }
                /* Check and see if the feed is read or unread now and upate the
                 * value in the database */
                ApplicationEx.dbHelper.updateFeedRead(feedId);
            }
            return null;
        }
    }
    /*
     * Runs updating of any given feeds.  Can unsubscribe to feeds or add new
     * ones, if given.
     */
    private class UpdateManager extends AsyncTask<Void, Void, Void> {
        
        private ArrayList<String> failedList = new ArrayList<String>();
        private int reason = -1;
        private int retries = 0;

        @Override
        protected Void doInBackground(Void... nothing) {
            ArrayList<String> subsList = new ArrayList<String>();
            String token = null;
            /* Grab saved Google account info */
            mAccountInfo = ApplicationEx.dbHelper.getAccount();
            /* Check and get a new token (if necessary) if the token is old
             * enough */
            if (isSyncing && mAccountInfo != null) {
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
                apiKey = mAccountInfo[1];
                token = Util.getToken(apiKey);
                /* If there is a working token, add any new feeds that
                 * aren't added to Reader, then get all feeds from Reader */
                addFeeds(token, ApplicationEx.dbHelper.getNewFeeds());
                subsList = retrieveFeeds(apiKey);
                Log.i(Constants.LOG_TAG, "subsList size: " + subsList.size());
            }
            /* If updating just sync'd feeds, get all local feeds and any newly
             * added ones on Reader */
            if (feeds.isEmpty()) {
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
            /* Check for a data connection, if there isn't one display a
             * notification with ability to try again */
            if (ApplicationEx.hasConnection() && Util.checkConnectionNoTask()) {
                notification = nBuilder.getNotification();
                startForeground(1337, notification);
            }
            else {
                failedList.addAll(feeds);
                return null;
            }
            /* Update result
             * -1   failure
             * 0    no new episodes
             * 1+   new episode
             */
            ArrayList<String> result = null;
            ArrayList<String> newList = new ArrayList<String>();
            /* Step through all feeds */
            for (String url : feeds) {
                /* Update the notification and format the feed URL */
                nBuilder.setContentTitle("Updating");
                if (url.startsWith("http"))
                    currUrl = "feed/" + url;
                else if (url.startsWith("feed/"))
                    currUrl = url;
                boolean failed = false;
                boolean retry = false;
                do {
                    int feedId = ApplicationEx.dbHelper.getInt(currUrl, 
                            DatabaseHelper.COL_FEED_ID, 
                            DatabaseHelper.FEED_TABLE, 
                            DatabaseHelper.COL_FEED_ADDRESS);
                    /* Unsubscribe feed from Reader if there is vaild token */
                    if (remove && token != null) {
                        nBuilder.setContentText("Removing: " + title);
                        notification = nBuilder.getNotification();
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
                    if (!ApplicationEx.dbHelper.feedExists(currUrl)) {
                        result = Util.addNewFeed(currUrl);
                        if (result != null && result.size() > 0)
                            newList.addAll(result);
                        else if (result == null) {
                            failed = true;
                            retry = retryFailure();
                        }
                        break;
                    }
                    else if (ApplicationEx.dbHelper.getInt(currUrl, 
                            DatabaseHelper.COL_FEED_PODCAST, 
                            DatabaseHelper.FEED_TABLE, 
                            DatabaseHelper.COL_FEED_ADDRESS)
                                != 1)
                        break;
                    /* Set the feed title in the notification */
                    publishProgress();
                    /* This downloads the XML for the feed, then parses the feed
                     * and episode information.  Inserts new episodes and upates
                     * episode and feed information in the database */
                    result = Util.getXml(currUrl);
                    /* There was a new episode found */
                    if (result != null && result.size() > 0)
                        newList.addAll(result);
                    /* There was some error, so need to try again */
                    else if (result == null) {
                        failed = true;
                        retry = retryFailure();
                    }
                    /* No new episodes found */
                    else {
                        /* Update the time feed last updated */
                        ContentValues cv = new ContentValues();
                        cv.put(DatabaseHelper.COL_FEED_LAST_UPDATE, 
                                System.currentTimeMillis());
                        ApplicationEx.dbHelper.updateRecord(cv, 
                                DatabaseHelper.FEED_TABLE, 
                                DatabaseHelper.COL_FEED_ID + "=" + feedId);
                        failed = false;
                        /* Get the latest changes from Google Reader */
                        if (isSyncing) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                                new UpdateSync(feedId, currUrl).execute();
                            else
                                new UpdateSync(feedId, currUrl)
                                        .executeOnExecutor(
                                        AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                        /* If using magic feed update, add a little more time
                         * to the magic interval to try again soon */
                        if (Integer.parseInt(Util.readStringPreference(
                                R.string.update_key, "0")) == 2) {
                            if (result.size() < 1) {
                                long currMagic = ApplicationEx.dbHelper
                                        .getFeedMagicInterval(currUrl);
                                if (currMagic >= Constants.PLUS_DAY_OF_WEEK)
                                    currMagic += Constants.HOUR_MILLI;
                                else {
                                    if (currMagic >= Constants.HOUR_MILLI*12)
                                        currMagic += Constants.HOUR_MILLI;
                                    else
                                        currMagic += (currMagic / 10);
                                }
                                ApplicationEx.dbHelper.setFeedMagicInterval(
                                        currUrl, currMagic);
                            }
                        }
                    }
                /* Keep trying again if there is failure and we should retry */
                } while (failed && retry);
                /* Reset retries if we are done processing this feed */
                retries = 0;
                /* Reset failed value for feed if there was a success */
                if (!failed) {
                    ApplicationEx.dbHelper.resetFail(currUrl);
                }
                /* Otherwise add a failed try and add the current to failed
                 * list */
                else {
                    reason = Constants.REASON_CONNECTION;
                    ApplicationEx.dbHelper.addFailedTry(currUrl, reason);
                    failedList.add(currUrl);
                }
            }
            /* After all the updates, we have all the new episodes */
            if (newList.size() > 0 && Util.readBooleanPreference(
                    R.string.download_key, false)) {
                String[] urlArray = new String[newList.size()];
                newList.toArray(urlArray);
                /* Add them to a currently running download service or start the
                 * service with all the episode URLs */
                if (Util.isMyServiceRunning(DownloadService.class.getName())) {
                    Intent downloadIntent = new Intent(
                            Constants.ACTION_START_DOWNLOAD);
                    downloadIntent.putExtra("urls", urlArray);
                    ApplicationEx.getApp().sendBroadcast(downloadIntent);
                }
                else {
                    Intent downloadIntent = new Intent(ApplicationEx.getApp(), 
                            DownloadService.class);
                    downloadIntent.putExtra("urls", urlArray);
                    ApplicationEx.getApp().startService(downloadIntent);
                }
            }
            return null;
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
            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                networkInfo = connMan.getActiveNetworkInfo();
                timeout = System.currentTimeMillis()-time >= 10000;
                cont = !timeout && (networkInfo == null || 
                        networkInfo.getState() != NetworkInfo.State.CONNECTED);
            } while (cont);
            /* True if we didn't time out and it hasn't failed more than 4 times
             * and it hasn't retried more than 4 times */
            return !timeout && ApplicationEx.dbHelper.didFail(currUrl) < 5 &&
                    retries < 5;
        }

        @Override
        protected void onProgressUpdate(Void... nothing) {
            /* Display the current feed title in the notification */
            nBuilder.setContentText(ApplicationEx.dbHelper.getString(
                    currUrl, DatabaseHelper.COL_FEED_TITLE, 
                    DatabaseHelper.FEED_TABLE, 
                    DatabaseHelper.COL_FEED_ADDRESS));
            notification = nBuilder.getNotification();
            nManager.notify(null, 1337, notification);
        }
        
        protected void onPostExecute(Void nothing) {
            /* If any failed, setup a failed notification and display */
            if (!failedList.isEmpty())
                failByConnection(failedList);
            /* If magic update enabled, set the next alarm for update */
            if (Integer.parseInt(Util.readStringPreference(
                    R.string.update_key, "0")) == 2) {
                Magics currMagics = ApplicationEx.dbHelper.getNextMagicInterval(
                            !failedList.isEmpty());
                Util.setMagicAlarm(currMagics.getTime(), currMagics.getFeeds());
            }
            /* Kill the service */
            stopSelf();
        }
        
    }
    /*
     * Other
     */
    
    /*
     * Callback started after the Google token is returned.
     */
    private class GetAuthTokenCallback implements AccountManagerCallback {
        
        public void run(AccountManagerFuture result) {
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
    /*
     * Required Service Methods
     */
    @Override
    public void onCreate() {
        super.onCreate();
        /* Create wake lock so the device wakes up for an update */
        PowerManager pm = 
                (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(pm.PARTIAL_WAKE_LOCK, "UPDATE WAKE LOCK");
        /* Start building the notification to display while updating */
        /* When user selects the notification, app opens to main view */
        Intent notificationIntent = new Intent(this, ActivitySplash.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, 
                notificationIntent, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        nBuilder = VersionedNotificationBuilder.newInstance();
        nBuilder.create(ApplicationEx.getApp()).
            setSmallIcon(R.drawable.ic_launcher).
            setTicker("Starting update").
            setWhen(System.currentTimeMillis()).
            setContentTitle("Updating").
            setContentText("").
            setContentIntent(pendingIntent);
        nManager = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        /* Cancel any previous notifications created by the update service */
        nManager.cancel(5555);
        /* Register receiver to update the notification when a new feed is
         * found */
        nReceiver = new NotificationReceiver();
        registerReceiver(nReceiver, 
                new IntentFilter(Constants.ACTION_UPDATE_NOTIFICATION));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        /* If the update manager isn't currently running (doesn't exist)
         * then start the wake lock and intialize the update manager */
        if (um == null) {
            wakeLock.acquire();
            feeds = new ArrayList<String>();
            if (intent.getExtras() != null) {
                /* If syncing with Google Reader is enabled */
                if (intent.hasExtra("sync"))
                    isSyncing = intent.getBooleanExtra("sync", false);
                /* If a list of feeds is passed - more than one feed is to
                 * be updated */
                if (intent.hasExtra("feedList"))
                    feeds = intent.getStringArrayListExtra("feedList");
                else {
                    /* If updating just one feed */
                    if (intent.hasExtra("feed")) {
                        feeds.add(intent.getStringExtra("feed"));
                    }
                    /* If we're unsubscribing to a feed */
                    remove = intent.getBooleanExtra("unsubscribe", false);
                    /* Getting the passed title for the feed to unsubscribe */
                    if (remove && intent.hasExtra("title"))
                        title = intent.getStringExtra("title");
                }
            }
            /* Creating and starting update manager to do the update(s) */
            um = new UpdateManager();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                um.execute();
            else
                um.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        return START_REDELIVER_INTENT;
    }
    
    private void failByConnection(ArrayList<String> failedList) {
        Intent failIntent = new Intent(ApplicationEx.getApp(), 
                UpdateService.class);
        // Needs action or doesn't work
        failIntent.setAction("nothing");
        failIntent.putExtra("sync", ApplicationEx.isSyncing() && 
                Util.readBooleanFromFile(Constants.GOOGLE_FILENAME));
        failIntent.putExtra("feedList", failedList);
        nBuilder.setWhen(System.currentTimeMillis()).
            setContentTitle("Update failed").
            setContentText("Touch this notification to retry").
            setContentIntent(PendingIntent.getService(
                    ApplicationEx.getApp(), 0, failIntent, 0));
        notification = nBuilder.getNotification();
        nManager.notify(null, 5555, notification);
    }
    /*
     * Test the current token with the Google Reader system
     */
    private boolean checkToken() {
        /* Not sure if we have a good token, use it to access Reader */
        Document doc = null;
        Connection mConnection = Jsoup.connect(
                Constants.SUBSCRIPTION_LIST_URL)
                .header("Authorization", Constants.AUTH_PARAMS + 
                        mAccountInfo[1])
                .header("Content-Type", "application/x-www-form-urlencoded")
                .userAgent("219396487960.apps.googleusercontent.com")
                .timeout(5000);
        try {
            doc = mConnection.get();
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, Constants.SUBSCRIPTION_LIST_URL + 
                    " failed to respond with code " + 
                    mConnection.response().statusCode(), e);
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
        /* If for some reason the service is killed, cancel the update task */
        um.cancel(true);
        /* Let go of the wake lock */
        wakeLock.release();
        /* Unregister the notifcation receiver (*/
        unregisterReceiver(nReceiver);
        /* Stop the ongoing notification */
        stopForeground(true);
        super.onDestroy();
    }
    /*
     * Unsubscribe from a feed in Google Reader
     */
    private boolean removeFeed(String token, String apiKey, String feedUrl) {
        Response res = null;
        Connection mConnection = null;
        boolean passed = false;
        /* Format the feed URL correctly, just in case */
        if (!feedUrl.startsWith("feed/"))
            feedUrl = "feed/" + feedUrl;
        /* Setup the URL to unsubscribe from the feed in Reader */
        mConnection = Jsoup.connect(Constants.SUBSCRIPTION_EDIT_URL)
                .method(Connection.Method.POST)
                .data("s", feedUrl)
                .data("ac", "unsubscribe")
                .data("T", token)
                .header("Authorization", Constants.AUTH_PARAMS + apiKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .userAgent("219396487960.apps.googleusercontent.com")
                .ignoreHttpErrors(true)
                .timeout(0);
        try {
            res = mConnection.execute();
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, Constants.SUBSCRIPTION_EDIT_URL + 
                    "feed/" + feedUrl + " failed to respond with code " + 
                    mConnection.response().statusCode(), e);
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
            mConnection = Jsoup.connect(Constants.SUBSCRIPTION_EDIT_URL)
                    .method(Connection.Method.POST)
                    .data("s", "feed/" + feedUrl)
                    .data("ac", "subscribe")
                    .data("T", token)
                    .data("a", "user/-/label/Google Queue")
                    .header("Authorization", Constants.AUTH_PARAMS + apiKey)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .userAgent("219396487960.apps.googleusercontent.com")
                    .ignoreHttpErrors(true)
                    .timeout(0);
            try {
                res = mConnection.execute();
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, Constants.SUBSCRIPTION_EDIT_URL + 
                        "feed/" + feedUrl + " failed to respond with code " + 
                        mConnection.response().statusCode(), e);
            }
            /* Null response means we failed */
            if (res == null)
                passed = false;
            /* Otherwise, if we get a 200 response code, we update the feed with
             * the Google Reader version of the URL */
            else {
                if (mConnection.response().statusCode() == 200) {
                    ContentValues cv = new ContentValues();
                    cv.put(DatabaseHelper.COL_FEED_ADDRESS, "feed/" + feedUrl);
                    ApplicationEx.dbHelper.updateRecord(cv, 
                            DatabaseHelper.FEED_TABLE, 
                            DatabaseHelper.COL_FEED_ADDRESS + "='" + feedUrl + 
                            "'");
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
        Connection mConnection = Jsoup.connect(
                Constants.SUBSCRIPTION_LIST_URL)
                .header("Authorization", Constants.AUTH_PARAMS + apiKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .userAgent("219396487960.apps.googleusercontent.com")
                .timeout(0);
        try {
            doc = mConnection.get();
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, Constants.SUBSCRIPTION_LIST_URL + 
                    " failed to respond with code " + 
                    mConnection.response().statusCode(), e);
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
    /*
     * This gets called when we bind with a client, but we have no clients for
     * this service, so we return null.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}