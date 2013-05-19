package com.jeffthefate.podcatcher.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

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
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationCompat.InboxStyle;
import android.text.TextUtils;
import android.util.Log;

import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.DatabaseHelper;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.Util;
import com.jeffthefate.podcatcher.activity.ActivityDownloads;
import com.jeffthefate.podcatcher.activity.ActivityMain;

/**
 * An "interface" to restart the service if it has been killed.  This is run 
 * each time a shortcut is tapped to launch, so each time the user wants to 
 * launch something from the app, the service is sure to be running again if it
 * wasn't before.
 * 
 * @author Jeff Fate
 */
public class DownloadService extends IntentService {
    
    private ArrayList<String> urlList;
    private Builder nBuilderProgress;
    private Builder nBuilderFailed;
    private InboxStyle inboxStyle;
    private Notification notification;
    private NotificationManager nManager;
    private DownloadReceiver downloadReceiver;
    private RemoveReceiver removeReceiver;
    private CancelReceiver cancelReceiver;
    private CancelAllReceiver cancelAllReceiver;
    
    PowerManager.WakeLock wakeLock;
    
    private ArrayList<String> failedList = new ArrayList<String>();
    
    private final IBinder mBinder = new DownloadBinder();

    public DownloadService() {
        super(Constants.DOWNLOAD_SERVICE_NAME);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager pm = 
                (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                Constants.DOWNLOAD_WAKE_LOCK);
        downloadReceiver = new DownloadReceiver();
        removeReceiver = new RemoveReceiver();
        cancelReceiver = new CancelReceiver();
        cancelAllReceiver = new CancelAllReceiver();
        registerReceiver(downloadReceiver, new IntentFilter(
                Constants.ACTION_START_DOWNLOAD));
        registerReceiver(removeReceiver, new IntentFilter(
                Constants.ACTION_REMOVE_DOWNLOAD));
        registerReceiver(cancelReceiver, new IntentFilter(
                Constants.ACTION_CANCEL_DOWNLOAD));
        registerReceiver(cancelAllReceiver, new IntentFilter(
                Constants.ACTION_CANCEL_ALL_DOWNLOADS));
        nManager = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        urlList = new ArrayList<String>();
    }
    
    @SuppressLint("NewApi")
    @Override
    protected void onHandleIntent(Intent intent) {
        // Make sure the device is awake so it can establish connection
        wakeLock.acquire();
        // Build downloading notification
        nBuilderProgress = new NotificationCompat.Builder(
                ApplicationEx.getApp());
        nBuilderProgress.setWhen(System.currentTimeMillis());
        nBuilderProgress.setSmallIcon(R.drawable.ic_download, 0);
        nBuilderProgress.setLargeIcon(
                BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_launcher));
        // Can't download - indicate failure with notification
        if (!ApplicationEx.isExternalStorageWriteable() ||
                !ApplicationEx.canAccessFeedsDir()) {
            Intent notificationIntent = new Intent(this, ActivityMain.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, 
                    notificationIntent, 0);
            level = 0;
            nBuilderFailed = new NotificationCompat.Builder(
                    ApplicationEx.getApp());
            nBuilderFailed.setSmallIcon(R.drawable.ic_download, level).
                setContentTitle(Constants.DOWNLOAD_FAILED).
                setContentText(Constants.UNABLE_EXTERNAL_STORAGE).
                setContentIntent(pendingIntent);
            notification = nBuilderFailed.build();
            nManager.notify(1340, notification);
        }
        else {
            // Get list of episodes to download from intent
            if (intent.getExtras() != null && intent.hasExtra(Constants.URLS)) {
                String[] urlArray = intent.getExtras().getStringArray(
                        Constants.URLS);
                for (int i = 0; i < urlArray.length; i++) {
                    if (!urlList.contains(urlArray[i])) {
                        // Starting downloading, so reset that it failed before
                        ApplicationEx.dbHelper.resetFail(urlArray[i]);
                        urlList.add(urlArray[i]);
                    }
                }
                /*
                // Get previously failed episodes and try those again
                ArrayList<String> failedEps = 
                        ApplicationEx.dbHelper.getFailedEpisodes();
                for (String url : failedEps) {
                    if (!urlList.contains(url)) {
                        // Starting downloading, so reset that it failed before
                        ApplicationEx.dbHelper.resetFail(url);
                        urlList.add(url);
                    }
                }
                */
                // Start the task to download
                startDownloading();
            }
        }
    }
    
    @Override
    public void onDestroy() {
        // Remove the lock
        wakeLock.release();
        unregisterReceiver(downloadReceiver);
        unregisterReceiver(removeReceiver);
        unregisterReceiver(cancelReceiver);
        unregisterReceiver(cancelAllReceiver);
        stopForeground(true);
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    /**
     * Specific binder for this service so clients can get an instance of this.
     * @author Jeff Fate
     */
    public class DownloadBinder extends Binder {
        public DownloadService getService() {
            // Return this instance of LocalService so clients can call public methods
            return DownloadService.this;
        }
    }
    /**
     * Receive new episodes to be added to the queue and be downloaded.
     * @author Jeff Fate
     */
    class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String[] epUrls = intent.getStringArrayExtra(Constants.URLS);
            for (int i = 0; i < epUrls.length; i++) {
                if (!urlList.contains(epUrls[i])) {
                    ApplicationEx.dbHelper.resetFail(epUrls[i]);
                    urlList.add(epUrls[i]);
                }
                if (!ApplicationEx.downloadList.contains(epUrls[i])) {
                    ApplicationEx.dbHelper.resetFail(epUrls[i]);
                    ApplicationEx.downloadList.add(epUrls[i]);
                }
            }
            updateNotificationList();
            ApplicationEx.getApp().sendBroadcast(
                    new Intent(Constants.ACTION_UPDATE_DOWNLOADS));
        }
    }
    /**
     * Remove episodes from the queue to be downloaded.
     * @author Jeff Fate
     */
    class RemoveReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String[] epUrls = intent.getStringArrayExtra(Constants.URLS);
            for (int i = 0; i < epUrls.length; i++) {
                if (epUrls[i].equals(currUrl))
                    cancel();
                if (urlList.contains(epUrls[i])) {
                    urlList.remove(epUrls[i]);
                }
                if (ApplicationEx.downloadList.contains(epUrls[i])) {
                    ApplicationEx.downloadList.remove(epUrls[i]);
                }
            }
            updateNotificationList();
            ApplicationEx.getApp().sendBroadcast(
                    new Intent(Constants.ACTION_UPDATE_DOWNLOADS));
        }
    }
    /**
     * Download canceled by user, from context menu in downloads fragment.
     */
    class CancelReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            cancel();
        }
    }
    /**
     * All downloads canceled by user, from menu in downloads fragment.
     */
    class CancelAllReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ApplicationEx.downloadList.clear();
            urlList.clear();
            cancel();
        }
    }
        
    private String progressText;
    private String currUrl;
    
    private int total;
    private int fileLength;
    
    private Thread messageThread = null;
    
    boolean newDownload = false;
    
    boolean cancel = false;
    boolean retry = false;
    boolean failed = false;
    
    HttpURLConnection connection;
    int downloaded = 0;
    
    String cLength;
    
    byte data[];
    
    FileOutputStream output;
    
    int count;
    int retries = 0;
    
    PendingIntent pendingIntent;
    
    private StringBuilder downloadBuilder = new StringBuilder(0);
    private StringBuilder databaseBuilder = new StringBuilder(0);
    
    private int level = 0;

    @SuppressLint("NewApi")
    private void startDownloading() {
        // Setup the downloading notification
        Intent notificationIntent = new Intent(ApplicationEx.getApp(), 
                ActivityDownloads.class);
        notificationIntent.putStringArrayListExtra(Constants.EP_LIST, 
                urlList);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        nBuilderProgress.setTicker(Constants.DOWNLOAD_STARTING).
                setContentTitle(Constants.DOWNLOADING).
                setContentText(Constants.EMPTY).
                addAction(R.drawable.ic_notification_cancel,
                    Constants.CANCEL_ALL, PendingIntent.getBroadcast(this, 0,
                        new Intent(Constants.ACTION_CANCEL_ALL_DOWNLOADS), 0));
        // Copy list of downloads for other uses
        ApplicationEx.downloadList = new ArrayList<String>(urlList);
        String fileLoc;
        File file;
        int temp;
        BufferedInputStream input = null;
        Intent intent = new Intent(Constants.ACTION_NEW_DOWNLOAD);
        MediaMetadataRetriever metaRetriever = 
                new MediaMetadataRetriever();
        String duration;
        int currDuration;
        ContentValues cv;
        // Only keep doing this while there are downloads
        while (!urlList.isEmpty()) {
            if ((Util.readBooleanPreference(R.string.wifi_key, false) ?
                    !Util.hasWifi() : false) ||
                (Util.readBooleanPreference(R.string.power_key, false) ?
                     Util.getConnectedState() <= 0 : false) ||
                (Util.readBooleanPreference(R.string.four_g_key, false) ?
                    !Util.has4G() : false)) {
                urlList.remove(0);
                continue;
            }
            // The current downloaded, off the top of the list
            currUrl = urlList.remove(0);
            // Reset progress for this one
            ApplicationEx.currEpProgress = -1;
            level = 0;
            nBuilderProgress.setSmallIcon(R.drawable.ic_download,
                    level);
            updateNotification(ApplicationEx.currEpProgress);
            // Grab the current ID for UI indicator
            ApplicationEx.currEpId =
                    ApplicationEx.dbHelper.getEpisodeId(currUrl);
            // Indicate which one is currently downloading in the UI
            notificationIntent.putExtra(Constants.CURR_EP_ID,
                    ApplicationEx.currEpId);
            // Update the downloads list UI when accessed from notification
            pendingIntent = PendingIntent.getActivity(
                    ApplicationEx.getApp(), 0, notificationIntent, 0);
            nBuilderProgress.setContentIntent(pendingIntent);
            // "Connect" to the downloads UI
            ApplicationEx.getApp().sendBroadcast(
                    new Intent(Constants.ACTION_UPDATE_DOWNLOADS));
            // Indicate the notification needs to update to reflect the
            // currently downloading episode
            newDownload = true;
            // Remove the current download if it is already downloaded and
            // update the connection with the UI, then move on
            if (ApplicationEx.dbHelper.getEpisodeDownloaded(currUrl)) {
                ApplicationEx.downloadList.remove(currUrl);
                ApplicationEx.getApp().sendBroadcast(
                        new Intent(Constants.ACTION_UPDATE_DOWNLOADS));
                continue;
            }
            // Remove the current download if it is video and you're not
            // on wifi then update the connection with the UI, then move on
            if (Util.readBooleanPreference(R.string.video_key, false) &&
                    ApplicationEx.dbHelper.typeFromEpisodeUrl(currUrl)
                            .contains(Constants.VIDEO) &&
                    !ApplicationEx.hasWifi()) {
                ApplicationEx.downloadList.remove(currUrl);
                ApplicationEx.getApp().sendBroadcast(
                        new Intent(Constants.ACTION_UPDATE_DOWNLOADS));
                continue;
            }
            // If the service isn't already running in the forground, start
            if (!Util.isMyServiceForeground(
                    DownloadService.class.getName()))
                startForeground(1338, notification);
            nBuilderProgress.setTicker(null);
            total = 0;
            fileLength = 0;
            failed = false;
            retry = false;
            // Find where to store it
            fileLoc = ApplicationEx.dbHelper.getEpisodeLocation(currUrl);
            // Create the local file
            file = new File(fileLoc);
            // Make sure there are no spaces in the URL
            if (currUrl.contains(Constants.SPACE)) {
                cv = new ContentValues();
                String oldUrl = currUrl;
                currUrl = currUrl.replaceAll(Constants.SPACE,
                        Constants.URL_SPACE);
                cv.put(DatabaseHelper.COL_EPISODE_URL, currUrl);
                ApplicationEx.dbHelper.updateRecord(cv,
                        DatabaseHelper.EPISODE_TABLE, TextUtils.concat(
                                DatabaseHelper.COL_EPISODE_URL, Constants.EQUAL,
                                DatabaseUtils.sqlEscapeString(oldUrl))
                                        .toString());
            }
            // Keep doing this while it has failed and should retry
            do {
                try {
                    // Create connection at the episode URL
                    connection = (HttpURLConnection) new URL(currUrl)
                            .openConnection();
                } catch (MalformedURLException e) {
                    Log.e(Constants.LOG_TAG, "Bad URL from " + currUrl, 
                            e);
                    // Mark failed and move on to the next
                    failed = true;
                    continue;
                } catch (IOException e) {
                    Log.e(Constants.LOG_TAG, "Can't open connection to " + 
                            currUrl);
                    // Mark failed and retry - it could be temporary
                    failed = true;
                    // Returns true if a connection was established before
                    // the 30 second timeout, so we should retry
                    retry = retryFailure();
                    continue;
                }
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                downloaded = 0;
                // If part already downloaded, start where it left off
                if (file.exists()) {
                    downloaded = (int) file.length();
                    downloadBuilder.setLength(0);
                    downloadBuilder.append(Constants.RANGE_BYTES)
                            .append(downloaded).append(Constants.RANGE_END);
                    connection.setRequestProperty(Constants.RANGE, 
                            downloadBuilder.toString());
                }
                fileLength = downloaded;
                try {
                    connection.connect();
                } catch (IOException e) {
                    Log.e(Constants.LOG_TAG, "Can't connect to " + 
                            currUrl);
                    // Mark failed and retry - it could be temporary
                    failed = true;
                    // Returns true if a connection was established before
                    // the 30 second timeout, so we should retry
                    retry = retryFailure();
                    continue;
                }
                try {
                    // Range is not correct - perhaps something changed on
                    // the server side.  We start over here.
                    if (connection.getResponseCode() == 416) {
                        if (file.exists())
                            file.delete();
                        connection.getInputStream().close();
                        connection.disconnect();
                        failed = true;
                        retry = true;
                        retries++;
                        continue;
                    }
                    // Set this episode as downloading, to be reflected in
                    // the UI if wanted
                    ApplicationEx.dbHelper.setCurrentDownload(
                            ApplicationEx.dbHelper.getEpisodeId(currUrl));
                    // Grab the length of what is to be downloaded.  This
                    // could be the full size of the file or the part of the
                    // file needed
                    cLength = connection.getHeaderField(
                            Constants.CONTENT_LENGTH);
                    if (cLength != null) {
                        temp = Integer.parseInt(cLength);
                        data = new byte[8192];
                        if (fileLength == 0)
                            fileLength = temp;
                        else
                            fileLength += temp;
                    }
                    else
                        // Cause failure for this episode
                        throw new IOException();
                    // Save total bytes to database
                    ApplicationEx.currEpTotal = fileLength;
                    // Create progress text base with the size of the file
                    downloadBuilder.setLength(0);
                    downloadBuilder.append(Constants.PERCENT_OF)
                            .append((fileLength/1000000))
                            .append(Constants.MB);
                    progressText = downloadBuilder.toString();
                    // Create thread to update the notification with
                    // progress
                    messageThread = new Thread() {
                        public void run() {
                            try {
                                // Calculate percent done
                                ApplicationEx.currEpProgress = (int) ((
                                        (double)total/(double)fileLength
                                            ) * 100);
                                // Keep doing this until download hits 100% or
                                // the thread gets interrupted
                                while (ApplicationEx.currEpProgress < 100) {
                                    if (Thread.interrupted())  // Clears interrupted status!
                                         throw new InterruptedException();
                                    // Update the notification with current
                                    // percent
                                    if (level > 5)
                                        level = 0;
                                    nBuilderProgress.setSmallIcon(
                                            R.drawable.ic_download, level);
                                    level++;
                                    updateNotification(
                                            ApplicationEx.currEpProgress);
                                    // Wait 3 seconds to update again
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                    // Calculate percent done
                                    ApplicationEx.currEpProgress = (int) ((
                                            (double)total/(double)fileLength
                                                ) * 100);
                                }
                                level = 0;
                                nBuilderProgress.setSmallIcon(
                                        R.drawable.ic_download, level);
                                updateNotification(
                                        ApplicationEx.currEpProgress);
                            } catch (InterruptedException e) {}
                        }
                    };
                    // Create output stream - continue if the episode is
                    // partial
                    switch (connection.getResponseCode()) {
                    case HttpURLConnection.HTTP_PARTIAL:
                        total = downloaded;
                        output = new FileOutputStream(fileLoc, true);
                        break;
                    case HttpURLConnection.HTTP_OK:
                        output = new FileOutputStream(fileLoc);
                        break;
                    default:
                        output = new FileOutputStream(fileLoc);
                        break;
                    }
                    // Start updating the notification
                    messageThread.setPriority(Thread.MIN_PRIORITY);
                    messageThread.start();
                    // Create input stream with current connection
                    try {
                        input = new BufferedInputStream(
                                connection.getInputStream());
                    } catch (FileNotFoundException e) {
                        failed = true;
                        retry = false;
                        killThread();
                        connection.disconnect();
                        output.flush();
                        output.close();
                        // Update the feed again because this likely means there
                        // is a replacement episode
                        Intent updateIntent = new Intent(ApplicationEx.getApp(), 
                                UpdateService.class);
                        updateIntent.putExtra(Constants.SYNC,
                                ApplicationEx.isSyncing() &&
                                Util.readBooleanFromFile(
                                        Constants.GOOGLE_FILENAME));
                        updateIntent.putExtra(Constants.FEED,
                                ApplicationEx.dbHelper.getFeedUrl(currUrl));
                        updateIntent.putExtra(Constants.FORCE, false);
                        ApplicationEx.getApp().startService(updateIntent);
                        break;
                    }
                    // Download and write to file while there is still more
                    // data and it hasn't been canceled
                    if (input != null) {
                        while ((count = input.read(data)) != -1 && !cancel) {
                            total += count;
                            output.write(data, 0, count);
                        }
                    }
                    // Stop updating UI of the download status
                    killThread();
                    // Canceled by the user
                    if (cancel) {
                        // Blank the episode in the notification
                        nBuilderProgress.setContentText(Constants.EMPTY);
                        ApplicationEx.currEpProgress = -1;
                        level = 0;
                        nBuilderProgress.setSmallIcon(R.drawable.ic_download,
                                level);
                        updateNotification(ApplicationEx.currEpProgress);
                        connection.disconnect();
                        output.flush();
                        output.close();
                        input.close();
                        break;
                    }
                    // Clean up the output and input streams
                    connection.disconnect();
                    output.flush();
                    output.close();
                    input.close();
                    // Mark as successful if all was downloaded
                    if (fileLength == total)
                        failed = false;
                } catch (FileNotFoundException e) {
                    // File couldn't be created at the given location
                    Log.e(Constants.LOG_TAG, "File not opened at " + 
                            fileLoc, e);
                    // If the file system couldn't be accessed, retry
                    // Otherwise, just fail it
                    if ((!ApplicationEx.isExternalStorageWriteable() ||
                            !ApplicationEx.canAccessFeedsDir()) && 
                                retries < 5) {
                        failed = true;
                        retry = true;
                        retries++;
                    }
                    else {
                        failed = true;
                        retry = false;
                        break;
                    }
                } catch (IOException e) {
                    Log.e(Constants.LOG_TAG, "Download failed for " + 
                            currUrl);
                    // Stop updating UI of the download status, if
                    // applicable and then retry
                    killThread();
                    failed = true;
                    if (!cancel && retries < 5) {
                        retry = retryFailure();
                        retries++;
                    }
                    else
                        retry = false;
                    try {
                        if (output != null) {
                            output.flush();
                            output.close();
                        }
                        if (input != null)
                            input.close();
                    } catch (IOException e1) {
                        Log.e(Constants.LOG_TAG, "Closing input/output streams"
                                + " failed for " + currUrl);
                    }
                }
            } while (failed && retry);
            retries = 0;
            // If done downloading - assume file length is known and what
            // was downloaded matches it
            if (fileLength > 0 && total == fileLength) {
                // Make sure file isn't locked
                if (Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.GINGERBREAD) {
                    file.setExecutable(true, false);
                    file.setReadable(true, false);
                    file.setWritable(true, false);
                }
                // Mark as downloaded
                ApplicationEx.dbHelper.episodeDownloaded(
                        ApplicationEx.dbHelper.getInt(currUrl, 
                                DatabaseHelper.COL_EPISODE_ID,
                                DatabaseHelper.EPISODE_TABLE, 
                                DatabaseHelper.COL_EPISODE_URL), 
                        true);
                // Mark as no longer failed
                ApplicationEx.dbHelper.resetFail(currUrl);
                // Update the current playlist (downloaded episodes)
                // Set the date sorted playlist, if currently set
                switch (ApplicationEx.dbHelper.getCurrentPlaylistType()) {
                case Constants.PLAYLIST_BY_DATE:
                    intent.putIntegerArrayListExtra(Constants.ID_LIST, 
                            Util.getEpisodeIdList(true, -1, -1));
                    ApplicationEx.getApp().sendBroadcast(intent);
                    break;
                case Constants.PLAYLIST_BY_NAME:
                    intent.putIntegerArrayListExtra(Constants.ID_LIST, 
                            Util.getFeedEpisodeIdList(true));
                    ApplicationEx.getApp().sendBroadcast(intent);
                    break;
                }
                // Send the playlist broadcast
                ApplicationEx.getApp().sendBroadcast(intent);
                // Refresh episodes list
                ApplicationEx.getApp().sendBroadcast(
                        new Intent(Constants.ACTION_REFRESH_EPISODES));
                // Refresh feeds list
                ApplicationEx.getApp().sendBroadcast(
                        new Intent(Constants.ACTION_REFRESH_FEEDS));
                // Update the total time in the episodes list
                ApplicationEx.getApp().sendBroadcast(
                        new Intent(Constants.ACTION_REFRESH_DURATION));
                // If it exists, remove this one from the failed list
                failedList.remove(currUrl);
                // Remove from current list so the UI is up to date
                ApplicationEx.downloadList.remove(currUrl);
                // Get the duration of this episode
                // This is present before SDK 10, but is not in the
                // documentation until 10
                try {
                    metaRetriever.setDataSource(fileLoc);
                    duration = metaRetriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION);
                    currDuration = Integer.parseInt(duration);
                    cv = new ContentValues();
                    cv.put(DatabaseHelper.COL_EPISODE_DURATION, 
                            currDuration);
                    databaseBuilder.setLength(0);
                    databaseBuilder.append(DatabaseHelper.COL_EPISODE_ID)
                            .append(Constants.EQUAL)
                            .append(ApplicationEx.dbHelper.getEpisodeId(
                                        currUrl));
                    ApplicationEx.dbHelper.updateRecord(cv, 
                            DatabaseHelper.EPISODE_TABLE, 
                            databaseBuilder.toString());
                } catch (IllegalArgumentException e) {
                    Log.e(Constants.LOG_TAG, "Bad location: " + fileLoc, e);
                }
            }
            // If file length is not known (0) or what is downloaded doesn't
            // match that length
            else {
                // Known failure
                if (failed && !cancel) {
                    // Keep track of failure for this episode and add
                    // to the list of failed episodes
                    ApplicationEx.dbHelper.addFailedTry(currUrl);
                    failedList.add(currUrl);
                    // Remove from current list so the UI is up to date
                    ApplicationEx.downloadList.remove(currUrl);
                }
            }
            // Update the list in the downloads UI
            ApplicationEx.getApp().sendBroadcast(
                    new Intent(Constants.ACTION_UPDATE_DOWNLOADS));
            // Reset cancel value
            cancel = false;
        }
        // Stop updating UI of the download status
        killThread();
        // No episode is currently downloading
        ApplicationEx.dbHelper.setCurrentDownload(-1);
        showFailed();
    }
    /**
     * Set current download to canceled and mark it as no retry
     */
    public void cancel() {
        cancel = true;
        retry = false;
    }
    /**
     * Stop the thread that updates the UI of the download status
     */
    private void killThread() {
        if (messageThread != null)
            messageThread.interrupt();
        messageThread = null;
    }
    /**
     * Wait for the network info to report a connection.  Timeout is set to
     * 30 seconds.        
     * @return true if we want to retry because we got a connection within
     * the timeout range
     */
    private boolean retryFailure() {
        // Display that the download is paused
        if (!cancel) {
            ApplicationEx.currEpProgress = -1;
            level = 0;
            nBuilderProgress.setSmallIcon(R.drawable.ic_download,
                    level);
            updateNotification(ApplicationEx.currEpProgress);
        }
        boolean cont = false;
        boolean timeout = false;
        long time = System.currentTimeMillis();
        NetworkInfo networkInfo;
        ConnectivityManager connMan = 
            (ConnectivityManager) ApplicationEx
                .getApp().getSystemService(
                    Context.CONNECTIVITY_SERVICE);
        if (!cancel) {
            do {
                // Wait a second
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                // Get the network info
                networkInfo = connMan.getActiveNetworkInfo();
                // Check if we have tried for 30 seconds
                timeout = System.currentTimeMillis()-time >= 30000;
                // If network state isn't connected or there is no network state
                // or we haven't yet reached the 30 second timeout, continue
                cont = !timeout && (networkInfo == null || 
                        networkInfo.getState() !=
                            NetworkInfo.State
                                .CONNECTED);
            } while (cont && !cancel);
        }
        return !timeout && !cancel;
    }
    
    private Intent progressIntent;
    /**
     * Update the notification, on the UI thread
     */
    private void updateNotification(int progress) {
        // Indicates that there is a connection issue, so update accordingly
        if (progress == -1)
            nBuilderProgress.setContentTitle(Constants.DOWNLOAD_PENDING).
                setContentInfo(Constants.EMPTY);
        // Currently downloading
        else {
            nBuilderProgress.setContentTitle(ApplicationEx.dbHelper.getString(
                    currUrl, DatabaseHelper.COL_EPISODE_TITLE, 
                    DatabaseHelper.EPISODE_TABLE,
                    DatabaseHelper.COL_EPISODE_URL));
            // Update the episode in the notification
            downloadBuilder.setLength(0);
            downloadBuilder.append(progress).append(progressText);
            nBuilderProgress.setContentInfo(downloadBuilder.toString());
            if (newDownload) {
                nBuilderProgress.setContentText(
                        ApplicationEx.dbHelper.getString(
                    currUrl, DatabaseHelper.COL_EP_FEED_TITLE, 
                    DatabaseHelper.EPISODE_TABLE, 
                    DatabaseHelper.COL_EPISODE_URL));
                updateNotificationList();
                newDownload = false;
            }
        }
        // Update the intent and the progress text
        Intent notificationIntent = new Intent(ApplicationEx.getApp(), 
                ActivityDownloads.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                ApplicationEx.getApp(), 0, notificationIntent, 0);
        nBuilderProgress.setContentIntent(pendingIntent);
        progressIntent = new Intent(Constants.ACTION_UPDATE_PROGRESS);
        progressIntent.putExtra(Constants.PROGRESS, progress);
        ApplicationEx.getApp().sendBroadcast(progressIntent);
        // Send off the update to the notification
        notification = nBuilderProgress.build();
        nManager.notify(null, 1338, notification);
    }
    
    private void updateNotificationList() {
        if (ApplicationEx.downloadList.size() > 1) {
            inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setSummaryText(ApplicationEx.dbHelper.getString(
                    currUrl, DatabaseHelper.COL_EP_FEED_TITLE, 
                    DatabaseHelper.EPISODE_TABLE, 
                    DatabaseHelper.COL_EPISODE_URL));
            inboxStyle.addLine(Constants.NEXT);
            for (int i = 1; i < (ApplicationEx.downloadList.size() <= 6 ?
                    ApplicationEx.downloadList.size() : 6); i++) {
                inboxStyle.addLine(
                    ApplicationEx.dbHelper.getEpisodeFeedTitle(
                        ApplicationEx.dbHelper.getEpisodeId(
                            ApplicationEx.downloadList.get(i))));
            }
            if (ApplicationEx.downloadList.size() > 6)
                inboxStyle.addLine(Constants.ELLIPSIS);
            nBuilderProgress.setStyle(inboxStyle);
        }
        else
            nBuilderProgress.setStyle(null);
    }
    
    private void showFailed() {
        // There were failed episodes
        if (!failedList.isEmpty()) {
            // Indicate to the user that some failed by showing notification
            Intent notificationIntent = new Intent(ApplicationEx.getApp(), 
                    ActivityDownloads.class);
            // Needs action or doesn't work
            notificationIntent.setAction(Constants.NOTHING);
            // Shows download list of failed episodes
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                    Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    ApplicationEx.getApp(), 0, notificationIntent, 0);
            nBuilderFailed = new NotificationCompat.Builder(
                    ApplicationEx.getApp());
            level = 0;
            nBuilderFailed.setSmallIcon(R.drawable.ic_download, level).
                setContentIntent(pendingIntent).
                setContentTitle(Constants.DOWNLOAD_FAILED).
                setContentText(Constants.VIEW_FAILED).
                setContentInfo(Constants.EMPTY);
            notification = nBuilderFailed.build();
            nManager.notify(4444, notification);
            ApplicationEx.downloadList.clear();
            ApplicationEx.downloadList.addAll(failedList);
        }
        // Done
        stopSelf();
    }

}