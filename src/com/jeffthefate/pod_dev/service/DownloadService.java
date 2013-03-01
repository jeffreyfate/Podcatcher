package com.jeffthefate.pod_dev.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;

import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.Constants;
import com.jeffthefate.pod_dev.DatabaseHelper;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.Util;
import com.jeffthefate.pod_dev.VersionedNotificationBuilder;
import com.jeffthefate.pod_dev.activity.ActivityDownloads;
import com.jeffthefate.pod_dev.activity.ActivitySplash;
import com.jeffthefate.pod_dev.fragment.FragmentDownloads;

/**
 * An "interface" to restart the service if it has been killed.  This is run 
 * each time a shortcut is tapped to launch, so each time the user wants to 
 * launch something from the app, the service is sure to be running again if it
 * wasn't before.
 * 
 * @author Jeff Fate
 */
public class DownloadService extends Service {

    private ArrayList<String> urlList;
    private ArrayList<String> newList;
    private DownloadManager dm;
    private VersionedNotificationBuilder nBuilder;
    private Notification notification;
    private NotificationManager nManager;
    private DownloadReceiver downloadReceiver;
    private RemoveReceiver removeReceiver;
    
    private ArrayList<Messenger> clients;
    
    PowerManager.WakeLock wakeLock;
    
    private ArrayList<String> currList = new ArrayList<String>();
    private ArrayList<String> failedList = new ArrayList<String>();
    private int currId;
    
    private Context context;
    
    @Override
    public void onCreate() {
        super.onCreate();
        android.os.Process.setThreadPriority(19);
        PowerManager pm = 
                (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(pm.PARTIAL_WAKE_LOCK, "DOWNLOAD WAKE LOCK");
        clients = new ArrayList<Messenger>();
        downloadReceiver = new DownloadReceiver();
        removeReceiver = new RemoveReceiver();
        registerReceiver(downloadReceiver, new IntentFilter(
                Constants.ACTION_START_DOWNLOAD));
        registerReceiver(removeReceiver, new IntentFilter(
                Constants.ACTION_REMOVE_DOWNLOAD));
        nManager = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        urlList = new ArrayList<String>();
        newList = new ArrayList<String>();
        context = this;
    }
    
    public void cancelDownload() {
        dm.cancel();
    }

    class DownloadManager extends AsyncTask<Void, Integer, Void> {
        
        private String progressText;
        private String currUrl;
        
        private int total;
        private int fileLength;
        private int reason = -1;
        private int percent = 0;
        
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

        @Override
        protected Void doInBackground(Void... nothing) {
            Intent notificationIntent = new Intent(ApplicationEx.getApp(), 
                    ActivityDownloads.class);
            notificationIntent.putStringArrayListExtra("epList", 
                    urlList);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            nBuilder.setSmallIcon(R.drawable.ic_launcher).
                    setTicker("Starting download").
                    setContentTitle("Downloading").
                    setContentText("");
            currList = new ArrayList<String>(urlList);
            Bundle bundle;
            while (!urlList.isEmpty()) {
                currUrl = urlList.remove(0);
                currId = ApplicationEx.dbHelper.getEpisodeId(currUrl);
                notificationIntent.putExtra("currEpId", currId);
                pendingIntent = PendingIntent.getActivity(
                        ApplicationEx.getApp(), 0, notificationIntent, 0);
                nBuilder.setContentIntent(pendingIntent);
                notification = nBuilder.getNotification();
                for (Messenger client : clients) {
                    Message mMessage = Message.obtain(null, 
                            FragmentDownloads.SET_LIST);
                    try {
                        client.send(mMessage);
                    } catch (RemoteException e) {
                        Log.e(Constants.LOG_TAG, "Can't connect to " +
                                "PlaybackActivity", e);
                    }
                }
                newDownload = true;
                if (ApplicationEx.dbHelper.getEpisodeDownloaded(currUrl)) {
                    currList.remove(currUrl);
                    for (Messenger client : clients) {
                        Message mMessage = Message.obtain(null, 
                                FragmentDownloads.SET_LIST);
                        try {
                            client.send(mMessage);
                        } catch (RemoteException e) {
                            Log.e(Constants.LOG_TAG, "Can't connect to " +
                                    "PlaybackActivity", e);
                        }
                    }
                    continue;
                }
                if (ApplicationEx.dbHelper.typeFromEpisodeUrl(currUrl)
                                .contains("video") &&
                        !ApplicationEx.hasWifi()) {
                    currList.remove(currUrl);
                    for (Messenger client : clients) {
                        Message mMessage = Message.obtain(null, 
                                FragmentDownloads.SET_LIST);
                        try {
                            client.send(mMessage);
                        } catch (RemoteException e) {
                            Log.e(Constants.LOG_TAG, "Can't connect to " +
                                    "PlaybackActivity", e);
                        }
                    }
                    continue;
                }
                if (!Util.isMyServiceForeground(
                        DownloadService.class.getName()))
                    startForeground(1338, notification);
                nBuilder.setTicker(null);
                total = 0;
                fileLength = 0;
                failed = false;
                retry = false;
                String fileLoc = ApplicationEx.dbHelper
                        .getEpisodeLocation(currUrl);
                File file = new File(fileLoc);
                do {
                    try {
                        connection = (HttpURLConnection) new URL(currUrl)
                                .openConnection();
                    } catch (MalformedURLException e) {
                        Log.e(Constants.LOG_TAG, "Bad URL from " + currUrl, 
                                e);
                        failed = true;
                        continue;
                    } catch (IOException e) {
                        Log.e(Constants.LOG_TAG, "Can't open connection to " + 
                                currUrl);
                        failed = true;
                        retry = retryFailure();
                        continue;
                    }
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    downloaded = 0;
                    if (file.exists()) {
                        downloaded = (int) file.length();
                        ApplicationEx.dbHelper.setEpisodeBytes(currUrl, 
                                downloaded);
                        connection.setRequestProperty("Range", 
                                "bytes=" + downloaded + "-");
                    }
                    fileLength = downloaded;
                    try {
                        connection.connect();
                    } catch (IOException e) {
                        Log.e(Constants.LOG_TAG, "Can't connect to " + 
                                currUrl);
                        failed = true;
                        retry = retryFailure();
                        continue;
                    }
                    try {
                        if (connection.getResponseCode() == 416) {
                            total = downloaded;
                            ApplicationEx.dbHelper.setEpisodeBytes(currUrl, 
                                    total);
                            break;
                        }
                        ApplicationEx.dbHelper.setCurrentDownload(
                                ApplicationEx.dbHelper.getEpisodeId(currUrl));
                        cLength = connection.getHeaderField("Content-Length");
                        if (cLength != null) {
                            int temp = Integer.parseInt(cLength);
                            if (fileLength == 0)
                                fileLength = temp;
                            else
                                fileLength += temp;
                        }
                        else
                            throw new IOException();
                        data = new byte[1024000];
                        progressText = "% of " + fileLength / 1000000 + 
                                "MB";
                        messageThread = new Thread() {
                            public void run() {
                                Bundle bundle;
                                percent = (int) ((
                                        (double)total/(double)fileLength
                                            ) * 100);
                                while (percent < 100 && !this.isInterrupted()) {
                                    publishProgress(percent);
                                    for (Messenger client : clients) {
                                        Message mMessage = Message.obtain(null, 
                                                FragmentDownloads.SET_PROGRESS);
                                        bundle = new Bundle();
                                        bundle.putInt("progress", percent);
                                        mMessage.setData(bundle);
                                        try {
                                            client.send(mMessage);
                                        } catch (RemoteException e) {
                                            Log.e(Constants.LOG_TAG, 
                                                    "Can't connect to " +
                                                    "PlaybackActivity", e);
                                        }
                                    }
                                    try {
                                        Thread.sleep(3000);
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                    percent = (int) ((
                                            (double)total/(double)fileLength
                                                ) * 100);
                                }
                            }
                        };
                        messageThread.setPriority(Thread.MIN_PRIORITY);
                        messageThread.start();
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
                        InputStream input = new BufferedInputStream(
                                connection.getInputStream());
                        while ((count = input.read(data)) != -1 && !cancel) {
                            total += count;
                            output.write(data, 0, count);
                            ApplicationEx.dbHelper.setEpisodeBytes(currUrl, 
                                    total);
                        }
                        killThread();
                        publishProgress((int) (((double)total/
                                (double)fileLength) * 100));
                        if (cancel) {
                            nBuilder.setContentText("");
                            publishProgress(-1);
                            failed = true;
                        }
                        output.flush();
                        output.close();
                        input.close();
                        if (fileLength == total) {
                            failed = false;
                        }
                    } catch (FileNotFoundException e) {
                        Log.e(Constants.LOG_TAG, "File not opened at " + 
                                fileLoc, e);
                        if ((!ApplicationEx.isExternalStorageWriteable() ||
                                !ApplicationEx.canAccessFeedsDir()) && 
                                    retries < 5) {
                            failed = true;
                            retry = true;
                            retries++;
                        }
                        else
                            break;
                    } catch (IOException e) {
                        Log.e(Constants.LOG_TAG, "Download failed for " + 
                                currUrl);
                        killThread();
                        failed = true;
                        retry = retryFailure();
                    }
                } while (failed && retry);
                if (fileLength > 0 && total == fileLength) {
                    file.setExecutable(true, false);
                    file.setReadable(true, false);
                    file.setWritable(true, false);
                    ApplicationEx.dbHelper.episodeDownloaded(
                            ApplicationEx.dbHelper.getInt(currUrl, 
                                    DatabaseHelper.COL_EPISODE_ID,
                                    DatabaseHelper.EPISODE_TABLE, 
                                    DatabaseHelper.COL_EPISODE_URL), 
                            true);
                    ApplicationEx.dbHelper.resetFail(currUrl);
                    Intent intent = new Intent(Constants.ACTION_NEW_DOWNLOAD);
                    if (ApplicationEx.dbHelper.getCurrentPlaylistType() == 1) {
                        intent.putIntegerArrayListExtra("idList", 
                                Util.getEpisodeIdList(true));
                    }
                    else {
                        intent.putIntegerArrayListExtra("idList", 
                                Util.getFeedEpisodeIdList(true));
                    }
                    ApplicationEx.getApp().sendBroadcast(intent);
                    ApplicationEx.getApp().sendBroadcast(
                            new Intent(Constants.ACTION_REFRESH_EPISODES));
                    ApplicationEx.getApp().sendBroadcast(
                            new Intent(Constants.ACTION_REFRESH_FEEDS));
                    ApplicationEx.getApp().sendBroadcast(
                            new Intent(Constants.ACTION_REFRESH_DURATION));
                    failedList.remove(currUrl);
                    currList.remove(currUrl);
                    for (Messenger client : clients) {
                        Message mMessage = Message.obtain(null, 
                                FragmentDownloads.SET_LIST);
                        try {
                            client.send(mMessage);
                        } catch (RemoteException e) {
                            Log.e(Constants.LOG_TAG, "Can't connect to " +
                                    "PlaybackActivity", e);
                        }
                    }
                    MediaMetadataRetriever metaRetriever = 
                            new MediaMetadataRetriever();
                    try {
                        metaRetriever.setDataSource(fileLoc);
                    } catch (IllegalArgumentException e) {
                        Log.e(Constants.LOG_TAG, "Bad location: " + fileLoc, e);
                    }
                    String duration = metaRetriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION);
                    int currDuration = Integer.parseInt(duration);
                    ContentValues cv = new ContentValues();
                    cv.put(DatabaseHelper.COL_EPISODE_DURATION, 
                            currDuration);
                    ApplicationEx.dbHelper.updateRecord(cv, 
                            DatabaseHelper.EPISODE_TABLE, 
                            DatabaseHelper.COL_EPISODE_ID + "=" + 
                                ApplicationEx.dbHelper.getEpisodeId(
                                        currUrl));
                }
                else {
                    if (failed) {
                        if (cancel) {
                            reason = Constants.REASON_USER;
                        }
                        else {
                            reason = Constants.REASON_CONNECTION;
                            ApplicationEx.dbHelper.addFailedTry(currUrl,
                                    reason);
                            failedList.add(currUrl);
                        }
                    }
                    for (Messenger client : clients) {
                        Message mMessage = Message.obtain(null, 
                                FragmentDownloads.SET_LIST);
                        try {
                            client.send(mMessage);
                        } catch (RemoteException e) {
                            Log.e(Constants.LOG_TAG, "Can't connect to " +
                                    "PlaybackActivity", e);
                        }
                    }
                }
                cancel = false;
            }
            killThread();
            ApplicationEx.dbHelper.setCurrentDownload(-1);
            return null;
        }
        
        public void cancel() {
            cancel = true;
            retry = false;
        }
        
        private void killThread() {
            if (messageThread != null)
                messageThread.interrupt();
            messageThread = null;
        }
        
        private boolean retryFailure() {
            publishProgress(-1);
            boolean cont = false;
            boolean timeout = false;
            long time = System.currentTimeMillis();
            NetworkInfo networkInfo;
            ConnectivityManager connMan = 
                (ConnectivityManager) ApplicationEx
                    .getApp().getSystemService(
                        Context.CONNECTIVITY_SERVICE);
            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                networkInfo = connMan
                        .getActiveNetworkInfo();
                timeout = System.currentTimeMillis()-time >= 30000;
                cont = !timeout && (networkInfo == null || 
                        networkInfo.getState() !=
                            NetworkInfo.State
                                .CONNECTED);
            } while (cont);
            return !timeout;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            if (progress[0] == -1) {
                nBuilder.setContentTitle("Download pending...").
                    setContentInfo("");
            }
            else {
                nBuilder.setContentTitle("Downloading");
                if (newDownload) {
                    nBuilder.setContentText(
                            ApplicationEx.dbHelper.getString(
                        currUrl, DatabaseHelper.COL_EP_FEED_TITLE, 
                        DatabaseHelper.EPISODE_TABLE, 
                        DatabaseHelper.COL_EPISODE_URL));
                    newDownload = false;
                }
                Intent notificationIntent = new Intent(ApplicationEx.getApp(), 
                        ActivityDownloads.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        ApplicationEx.getApp(), 0, notificationIntent, 0);
                nBuilder.setContentIntent(pendingIntent).
                    setContentInfo(progress[0] + progressText);
            }
            notification = nBuilder.getNotification();
            nManager.notify(null, 1338, notification);
        }
        
        protected void onPostExecute(Void nothing) {
            if (!failedList.isEmpty()) {
                Intent notificationIntent = new Intent(ApplicationEx.getApp(), 
                        ActivityDownloads.class);
                // Needs action or doesn't work
                notificationIntent.setAction("nothing");
                notificationIntent.putStringArrayListExtra("epList", 
                        failedList);
                notificationIntent.putExtra("failed", true);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        ApplicationEx.getApp(), 0, notificationIntent, 0);
                nBuilder.setContentIntent(pendingIntent).
                    setContentTitle("Download Failed").
                    setContentText("Touch this notification to view " +
                        "failed episodes").
                    setContentInfo("");
                notification = nBuilder.getNotification();
                nManager.notify(4444, notification);
            }
            stopSelf();
        }
        
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        wakeLock.acquire();
        nBuilder = VersionedNotificationBuilder.newInstance();
        nBuilder.create(ApplicationEx.getApp()).
            setWhen(System.currentTimeMillis());
        if (!ApplicationEx.isExternalStorageWriteable() ||
                !ApplicationEx.canAccessFeedsDir()) {
            Intent notificationIntent = new Intent(this, ActivitySplash.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, 
                    notificationIntent, 0);
            nBuilder.setSmallIcon(R.drawable.ic_launcher).
                setContentTitle("Download Failed").
                setContentText("Unable to access external storage").
                setContentIntent(pendingIntent);
            notification = nBuilder.getNotification();
            nManager.notify(1340, notification);
        }
        else {
            if (intent.getExtras() != null && intent.hasExtra("urls")) {
                String[] urlArray = intent.getExtras().getStringArray("urls");
                for (int i = 0; i < urlArray.length; i++) {
                    if (!urlList.contains(urlArray[i])) {
                        ApplicationEx.dbHelper.resetFail(urlArray[i]);
                        urlList.add(urlArray[i]);
                    }
                }
                ArrayList<String> failedEps = 
                        ApplicationEx.dbHelper.getFailedEpisodes();
                for (String url : failedEps) {
                    if (!urlList.contains(url)) {
                        ApplicationEx.dbHelper.resetFail(url);
                        urlList.add(url);
                    }
                }
                dm = new DownloadManager();
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    dm.execute();
                else
                    dm.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
        return START_STICKY_COMPATIBILITY;
    }
    
    @Override
    public void onDestroy() {
        wakeLock.release();
        unregisterReceiver(downloadReceiver);
        unregisterReceiver(removeReceiver);
        stopForeground(true);
        super.onDestroy();
    }
    
    public void addClient(Messenger messenger) {
        clients.add(messenger);
    }
    
    public void removeClient(Messenger messenger) {
        clients.remove(messenger);
    }
    
    public ArrayList<String> getCurrentList() {
        return currList;
    }
    
    public int getCurrentId() {
        return currId;
    }
    
    private final IBinder mBinder = new DownloadBinder();
    
    public class DownloadBinder extends Binder {
        public DownloadService getService() {
            // Return this instance of LocalService so clients can call public methods
            return DownloadService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String[] epUrls = intent.getStringArrayExtra("urls");
            for (int i = 0; i < epUrls.length; i++) {
                if (!urlList.contains(epUrls[i])) {
                    ApplicationEx.dbHelper.resetFail(epUrls[i]);
                    urlList.add(epUrls[i]);
                }
                if (!currList.contains(epUrls[i])) {
                    ApplicationEx.dbHelper.resetFail(epUrls[i]);
                    currList.add(epUrls[i]);
                }
            }
            for (Messenger client : clients) {
                Message mMessage = Message.obtain(null, 
                        FragmentDownloads.SET_LIST);
                try {
                    client.send(mMessage);
                } catch (RemoteException e) {
                    Log.e(Constants.LOG_TAG, "Can't connect to " +
                            "PlaybackActivity", e);
                }
            }
        }
    }
    
    class RemoveReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String[] epUrls = intent.getStringArrayExtra("urls");
            for (int i = 0; i < epUrls.length; i++) {
                if (urlList.contains(epUrls[i])) {
                    urlList.remove(epUrls[i]);
                }
                if (currList.contains(epUrls[i])) {
                    currList.remove(epUrls[i]);
                }
            }
            for (Messenger client : clients) {
                Message mMessage = Message.obtain(null, 
                        FragmentDownloads.SET_LIST);
                try {
                    client.send(mMessage);
                } catch (RemoteException e) {
                    Log.e(Constants.LOG_TAG, "Can't connect to " +
                            "PlaybackActivity", e);
                }
            }
        }
    }
    
}