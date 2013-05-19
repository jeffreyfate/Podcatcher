package com.jeffthefate.podcatcher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.Button;

import com.jeffthefate.podcatcher.service.UpdateService;

public class Util {
    
    public static class UpdateFeedTask extends AsyncTask<Void, Void, Void> {
        
        Button subscribeButton;
        String url;

        public UpdateFeedTask(Button subscribeButton, String url) {
            this.subscribeButton = subscribeButton;
            this.url = url;
        }
        
        @Override
        protected void onPreExecute() {
            if (subscribeButton != null) {
                subscribeButton.setText(Constants.ADDING_FEED);
                subscribeButton.setEnabled(false);
            }
        }
        
        @Override
        protected Void doInBackground(Void... nothing) {
            boolean sync = ApplicationEx.isSyncing() &&
                    Util.readBooleanFromFile(Constants.GOOGLE_FILENAME);
            Intent intent = new Intent(ApplicationEx.getApp(), 
                    UpdateService.class);
            intent.putExtra(Constants.SYNC, sync);
            if (url != null)
                intent.putExtra(Constants.FEED, url);
            ApplicationEx.getApp().startService(intent);
            return null;
        }

    }
    
    public static ArrayList<String> addNewFeed(String feed) {
        ArrayList<String> result = getXml(feed, false);
        if (result != null && result.size() > 0)
            getImage(ApplicationEx.dbHelper.getFeedId(feed));
        return result;
    }
    
    public static void getImage(int feedId) {
        if (ApplicationEx.hasConnection() && checkConnectionNoTask() && 
                ApplicationEx.canAccessFeedsDir() &&
                ApplicationEx.isExternalStorageWriteable()) {
            String feedImageUrl = ApplicationEx.dbHelper.getString(
                    Integer.toString(feedId), 
                    DatabaseHelper.COL_FEED_IMAGE, 
                    DatabaseHelper.FEED_TABLE, 
                    DatabaseHelper.COL_FEED_ID);
            if (feedImageUrl != null)
                downloadImage(feedId, feedImageUrl);
        }
    }
    
    public static ArrayList<String> getXml(String jsonSub, boolean force) {
        return new XmlDomParser(jsonSub).insertFeed(force);
    }
    
    public static void downloadImage(int feedId, String address) {
        String currDir = Constants.EMPTY;
        String currFile = Constants.EMPTY;
        String smallFile = Constants.EMPTY;
        String extension = Constants.EMPTY;
        currDir = TextUtils.concat(ApplicationEx.cacheLocation,
                Constants.FEEDS_LOCATION, Integer.toString(feedId),
                File.separator).toString();
        File path = new File(currDir);
        path.setExecutable(true, false);
        path.setReadable(true, false);
        path.setWritable(true, false);
        path.mkdirs();
        if (address.endsWith(Constants.PNG))
            extension = Constants.PNG;
        else if (address.endsWith(Constants.JPG) ||
                address.endsWith(Constants.JPEG))
            extension = Constants.JPG;
        else if (address.endsWith(Constants.GIF))
            extension = Constants.GIF;
        else
            return;
        currFile = TextUtils.concat(currDir, Integer.toString(feedId),
                extension).toString();
        smallFile = TextUtils.concat(currDir, Integer.toString(feedId),
                Constants.SMALL, Constants.PNG).toString();
        if (Util.findFile(currDir, TextUtils.concat(Integer.toString(feedId),
                    extension).toString()) &&
                Util.findFile(currDir, TextUtils.concat(
                        Integer.toString(feedId), Constants.SMALL,
                        Constants.PNG).toString()) &&
                ApplicationEx.dbHelper.getLong(address,
                        DatabaseHelper.COL_FEED_LAST_IMAGE_UPDATE, 
                        DatabaseHelper.FEED_TABLE,
                        DatabaseHelper.COL_FEED_IMAGE) >
                    System.currentTimeMillis()-Constants.IMAGE_UPDATE_INTERVAL)
            { return; }
        URL urlObject;
        HttpURLConnection hConn;
        InputStream input;
        Bitmap bitmap;
        OutputStream output;
        byte data[];
        int count;
        try {
            urlObject = new URL(address);
            hConn = (HttpURLConnection)urlObject.openConnection();
            hConn.setReadTimeout(15000);
            hConn.setConnectTimeout(15000);
            input = hConn.getInputStream();
            output = new FileOutputStream(currFile);
            data = new byte[1024];
            while ((count = input.read(data)) > 0) {
                output.write(data, 0, count);
            }
            output.close();
        } catch (IOException e) {
            if (MalformedURLException.class.isInstance(e)) {
                Log.e(Constants.LOG_TAG, "Unable to create URL from " +
                        address, e);
            }
            else if (FileNotFoundException.class.isInstance(e)) {
                Log.e(Constants.LOG_TAG, "Unable to create file at " +
                        currFile, e);
            }
            else {
                Log.e(Constants.LOG_TAG, "Unable to open connection " +
                        address, e);
            }
            return;
        }
        path = new File(currFile);
        path.setExecutable(true, false);
        path.setReadable(true, false);
        path.setWritable(true, false);
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_FEED_LAST_IMAGE_UPDATE, 
                System.currentTimeMillis());
        ApplicationEx.dbHelper.updateRecord(cv, DatabaseHelper.FEED_TABLE, 
                TextUtils.concat(DatabaseHelper.COL_FEED_ID, Constants.EQUAL,
                        Integer.toString(feedId)).toString());
        bitmap = BitmapFactory.decodeFile(currFile);
        double ratio = (double) ((double)bitmap.getHeight() / 
                (double)bitmap.getWidth());
        Resources res = ApplicationEx.getApp().getResources();
        Bitmap smallBitmap = Bitmap.createScaledBitmap(bitmap,
                res.getDimensionPixelSize(
                        android.R.dimen.notification_large_icon_width), 
                (int) (ratio < 1 ? res.getDimensionPixelSize(
                    android.R.dimen.notification_large_icon_height)*ratio :
                        res.getDimensionPixelSize(
                            android.R.dimen.notification_large_icon_height)),
                true);
        OutputStream outStream = null;
        try {
            outStream = new FileOutputStream(smallFile);
            smallBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.close();
        } catch (FileNotFoundException e) {
            Log.e(Constants.LOG_TAG, "Unable to create file at " +
                    smallFile, e);
            return;
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, "Unable to close stream to " +
                    smallFile, e);
            return;
        }
        path = new File(smallFile);
        path.setExecutable(true, false);
        path.setReadable(true, false);
        path.setWritable(true, false);
        ApplicationEx.getApp().sendBroadcast(
                new Intent(Constants.ACTION_IMAGE_CHANGE));
    }
    
    protected static ArrayList<String> insertFeed(String feedId, 
            ArrayList<HashMap<String, String>> feedList, int sortOrder) {
        /* If somehow the list is null, there was an error */
        if (feedList == null)
            return new ArrayList<String>();
        int isPodcast = -1;
        /* Determine if it is a podcast, based on that last map in the list */
        if (!ApplicationEx.dbHelper.feedExists(feedId))
            isPodcast = feedList.get(feedList.size()-1).isEmpty() ? 1 : 0;
        /* Or if it is already in the database, just get the value */
        else
            isPodcast = ApplicationEx.dbHelper.feedIsPodcast(feedId);
        /* Not a podcast or there are no episodes in the list, nothing to do */
        if (isPodcast == 0 || feedList.size() <= 2) {
            return new ArrayList<String>();
        }
        boolean isNew = false;
        if (!ApplicationEx.dbHelper.feedExists(feedId))
            isNew = true;
        /* Setup content values with the feed information */
        ContentValues cv = new ContentValues();
        /* First map in the list is the feed map */
        HashMap<String, String> feedMap = feedList.get(0);
        cv.put(DatabaseHelper.COL_FEED_ADDRESS, feedId);
        cv.put(DatabaseHelper.COL_FEED_TITLE, feedMap.get(Constants.TITLE));
        cv.put(DatabaseHelper.COL_FEED_LINK, feedMap.get(Constants.LINK));
        cv.put(DatabaseHelper.COL_FEED_DESCRIPTION, 
                feedMap.get(Constants.DESCRIPTION));
        cv.put(DatabaseHelper.COL_FEED_IMAGE, 
                feedMap.get(Constants.ITUNES_IMAGE) != null ? 
                    feedMap.get(Constants.ITUNES_IMAGE) :
                        feedMap.get(Constants.IMAGE_URL_XML));
        cv.put(DatabaseHelper.COL_FEED_PODCAST, isPodcast);
        /* Put the feed info in the database - adds it if it doesn't exist,
         * otherwise it updates.  Want to keep the feed data current */
        ApplicationEx.dbHelper.addFeed(cv, sortOrder);
        if (isPodcast == 1) {
            /* Save the XML to the feed folder and get the feed image if it is
             * a podcast */
            int id = ApplicationEx.dbHelper.getFeedId(feedId);
            String xmlLoc = TextUtils.concat(ApplicationEx.cacheLocation,
                    Constants.FEEDS_LOCATION, Integer.toString(id),
                    File.separator).toString();
            File path = new File(xmlLoc);
            path.setExecutable(true, false);
            path.setReadable(true, false);
            path.setWritable(true, false);
            path.mkdirs();
            path = new File(TextUtils.concat(xmlLoc, Integer.toString(id),
                    Constants.XML).toString());
            path.setExecutable(true, false);
            path.setReadable(true, false);
            path.setWritable(true, false);
            new File(TextUtils.concat(ApplicationEx.cacheLocation,
                    Constants.TEMP_LOCATION, Constants.TEMP_NAME).toString())
                .renameTo(path);
            getImage(ApplicationEx.dbHelper.getFeedId(feedId));
        }
        /* Grab and insert all episodes from this feed */
        return insertEpisodes(feedId, feedList, isPodcast, isNew);
    }
    /*
     * Takes a date string with the format
     *      EEE, dd MMM yyyy kk:mm:ss Z
     * and converts it to epoch time long value.
     */
    public static long dateStringToEpoch(String dateString) {
        long epochTime = -1;
        try {
            epochTime = new SimpleDateFormat(Constants.SIMPLE_DATE,
                    Locale.getDefault()).parse(dateString).getTime();
        } catch (ParseException e) {
            //Log.e(Constants.LOG_TAG, "Unable to parse date " + dateString);
        }
        if (epochTime < 0) {
            try {
                epochTime = new SimpleDateFormat(Constants.SIMPLE_DATE_UTC,
                        Locale.getDefault()).parse(dateString).getTime();
            } catch (ParseException e) {
                //Log.e(Constants.LOG_TAG, "Unable to parse date " + dateString);
            }
        }
        return epochTime;
    }
    
    private static ArrayList<String> insertEpisodes(String url, 
            ArrayList<HashMap<String, String>> feedList, int isPodcast,
            boolean isNew) {
        /* Get the list of episodes for the current feed - the episode URLs */
        int feedId = ApplicationEx.dbHelper.getInt(url, 
                DatabaseHelper.COL_FEED_ID, DatabaseHelper.FEED_TABLE, 
                DatabaseHelper.COL_FEED_ADDRESS);
        ArrayList<String> episodesList = 
            ApplicationEx.dbHelper.getEpisodesList(feedId);
        ContentValues cv = new ContentValues();
        long currEpTime = -1;
        String tempUrl = null;
        String tempGuid = null;
        /* Keep track of the episode URLs as we go */
        ArrayList<String> idsList = new ArrayList<String>();
        ArrayList<String> newList = new ArrayList<String>();
        for (HashMap<String, String> episodeHash : feedList) {
            /* Skip if it is the feed map, the empty map or there is no URL */
            if (!feedList.get(0).equals(episodeHash) && !episodeHash.isEmpty() 
                    && episodeHash.get(Constants.URL) != null) {
                tempUrl = episodeHash.get(Constants.URL).replaceAll(
                        Constants.SPACE, Constants.URL_SPACE);
                tempGuid = episodeHash.get(Constants.GUID);
                if (tempGuid == null)
                    tempGuid = tempUrl;
                currEpTime = dateStringToEpoch(episodeHash.get(
                        Constants.PUB_DATE));
                /* Used to later get the Google IDs from Reader for each */
                idsList.add(tempUrl);
                /* If the episode URL is in the database and both the episode
                 * title and feed title match */
                if (ApplicationEx.dbHelper.inDb(
                        new String[] {tempGuid}, 
                        DatabaseHelper.EPISODE_TABLE, new String[] {
                            DatabaseHelper.COL_EPISODE_GUID}) && 
                    ApplicationEx.dbHelper.inDb(
                        new String[] {
                                DatabaseUtils.sqlEscapeString(
                                        feedList.get(0).get(Constants.TITLE)), 
                                DatabaseUtils.sqlEscapeString(
                                        episodeHash.get(Constants.TITLE))}, 
                        DatabaseHelper.EPISODE_TABLE, new String[] {
                            DatabaseHelper.COL_EP_FEED_TITLE,
                            DatabaseHelper.COL_EPISODE_TITLE})) {
                    /* Setup the result to 0 because this one exists */
                    /* Remove from list - the remaining episodes are archive */
                    episodesList.remove(tempUrl);
                    cv.clear();
                    /* Not an archive episode */
                    cv.put(DatabaseHelper.COL_EPISODE_ARCHIVE, 0);
                    /* Only put the archive value to make sure it is set to 0 */
                    ApplicationEx.dbHelper.updateRecord(cv, 
                        DatabaseHelper.EPISODE_TABLE, TextUtils.concat(
                                DatabaseHelper.COL_EPISODE_GUID,
                                Constants.EQUAL,
                                DatabaseUtils.sqlEscapeString(tempGuid))
                            .toString());
                }
                /* If the episode URL and title are NOT in the database */
                else if (!ApplicationEx.dbHelper.inDb(
                        new String[] {tempGuid}, 
                        DatabaseHelper.EPISODE_TABLE, new String[] {
                            DatabaseHelper.COL_EPISODE_GUID}) &&
                    !ApplicationEx.dbHelper.inDb(
                            new String[] {
                                    DatabaseUtils.sqlEscapeString(
                                            feedList.get(0).get(
                                                    Constants.TITLE)),
                                    DatabaseUtils.sqlEscapeString(
                                            episodeHash.get(Constants.TITLE))}, 
                            DatabaseHelper.EPISODE_TABLE, new String[] {
                                    DatabaseHelper.COL_EP_FEED_TITLE,
                                    DatabaseHelper.COL_EPISODE_TITLE}) &&
                    !ApplicationEx.dbHelper.inDb(new String[] {tempUrl},
                            DatabaseHelper.EPISODE_TABLE,
                            new String[] {DatabaseHelper.COL_EPISODE_URL})) {
                    /* Enter all episode info to the content values */
                    cv.clear();
                    cv.put(DatabaseHelper.COL_EPISODE_URL, tempUrl);
                    cv.put(DatabaseHelper.COL_EPISODE_PUB, currEpTime);
                    cv.put(DatabaseHelper.COL_EPISODE_SUMMARY, Jsoup.parse(
                            StringEscapeUtils.unescapeHtml4(
                                    episodeHash.get(Constants.ITUNES_SUMMARY) !=
                                    null ? episodeHash.get(
                                            Constants.ITUNES_SUMMARY) :
                                        episodeHash.get(Constants.DESCRIPTION)))
                            .text());
                    cv.put(DatabaseHelper.COL_EPISODE_TYPE, 
                            episodeHash.get(Constants.TYPE));
                    cv.put(DatabaseHelper.COL_EPISODE_LENGTH, 
                            episodeHash.get(Constants.LENGTH));
                    cv.put(DatabaseHelper.COL_EPISODE_TITLE,
                            episodeHash.get(Constants.TITLE));
                    cv.put(DatabaseHelper.COL_EPISODE_UPDATED, 
                            System.currentTimeMillis());
                    cv.put(DatabaseHelper.COL_EPISODE_GUID, tempGuid);
                    if (isNew)
                        cv.put(DatabaseHelper.COL_EPISODE_READ, Constants.READ);
                    else
                        cv.put(DatabaseHelper.COL_EPISODE_READ,
                                Constants.UNREAD);
                    cv.put(DatabaseHelper.COL_EPISODE_DOWNLOADED, 
                            Constants.TO_DOWNLOAD);
                    cv.put(DatabaseHelper.COL_EPISODE_FAILED_TRIES, 0);
                    /* Insert the episode */
                    ApplicationEx.dbHelper.addEpisode(cv, url, 
                            extFromType(cv.getAsString(
                                    DatabaseHelper.COL_EPISODE_TYPE)));
                    /* Indicate there is a new one */
                    if (!isNew)
                        newList.add(tempUrl);
                }
                /* If the episode URL isn't in database and an entry for the
                 * episode does exist, update the record */
                else if (!ApplicationEx.dbHelper.inDb(
                            new String[] {tempUrl}, 
                            DatabaseHelper.EPISODE_TABLE, new String[] {
                                DatabaseHelper.COL_EPISODE_URL}) && 
                        ApplicationEx.dbHelper.inDb(
                                new String[] {tempGuid}, 
                                DatabaseHelper.EPISODE_TABLE, new String[] {
                                    DatabaseHelper.COL_EPISODE_GUID})) {
                    cv.clear();
                    cv.put(DatabaseHelper.COL_EPISODE_URL, tempUrl);
                    cv.put(DatabaseHelper.COL_EPISODE_PUB, currEpTime);
                    cv.put(DatabaseHelper.COL_EPISODE_SUMMARY, Jsoup.parse(
                            StringEscapeUtils.unescapeHtml4(episodeHash.get(
                                    Constants.ITUNES_SUMMARY) != null ?
                                        episodeHash.get(
                                                Constants.ITUNES_SUMMARY) :
                                        episodeHash.get(Constants.DESCRIPTION)))
                            .text());
                    cv.put(DatabaseHelper.COL_EPISODE_TYPE, 
                            episodeHash.get(Constants.TYPE));
                    cv.put(DatabaseHelper.COL_EPISODE_LENGTH, 
                            episodeHash.get(Constants.LENGTH));
                    cv.put(DatabaseHelper.COL_EPISODE_UPDATED, 
                            System.currentTimeMillis());
                    /* The location of this episode could have changed, so we
                     * need to keep it up to date */
                    ApplicationEx.dbHelper.updateRecord(cv, 
                            DatabaseHelper.EPISODE_TABLE, TextUtils.concat(
                                    DatabaseHelper.COL_EP_FEED_TITLE,
                                    Constants.EQUAL, 
                                    DatabaseUtils.sqlEscapeString(
                                        feedList.get(0).get(Constants.TITLE)),
                                    Constants.AND_SPACES,
                                    DatabaseHelper.COL_EPISODE_TITLE,
                                    Constants.EQUAL,
                                    DatabaseUtils.sqlEscapeString(
                                            episodeHash.get(Constants.TITLE)))
                                    .toString());
                }
            }    
        }
        /* Indicators that we need to get the Google IDs from Reader */
        if (ApplicationEx.isSyncing() && !idsList.isEmpty()) {
            // TODO
        }
        /* Each of the remaining episodes in the list are archive because they
         * are no longer in the feed XML */
        cv.clear();
        cv.put(DatabaseHelper.COL_EPISODE_ARCHIVE, 1);
        for (String epUrl : episodesList) {
            ApplicationEx.dbHelper.updateRecord(cv, 
                    DatabaseHelper.EPISODE_TABLE, TextUtils.concat(
                            DatabaseHelper.COL_EPISODE_URL, Constants.EQUAL,
                            DatabaseUtils.sqlEscapeString(epUrl)).toString());
        }
        return newList;
    }
    
    private static String extFromType(String type) {
        String ext = Constants.EMPTY;
        if (type.equalsIgnoreCase(Constants.MPEG_TYPE)
                || type.equalsIgnoreCase(Constants.MP3_TYPE_1)
                        || type.equalsIgnoreCase(Constants.MP3_TYPE_2))
            ext = Constants.MP3;
        else if (type.equalsIgnoreCase(Constants.M4A_TYPE))
            ext = Constants.M4A;
        else if (type.equalsIgnoreCase(Constants.MP4_TYPE))
            ext = Constants.MP4;
        return ext;
    }
    
    /**
     * Retrieve the application's saved location.
     * @return  the current location saved
     */
    public static Location getLastKnown() {
        LocationManager lm = (LocationManager) ApplicationEx.getApp()
                .getSystemService(Context.LOCATION_SERVICE);
        return lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }
    
    public static void setSunriseAlarm() {
        AlarmManager alarmMan = (AlarmManager) ApplicationEx.getApp()
                .getSystemService(Context.ALARM_SERVICE);
        alarmMan.set(AlarmManager.RTC, ApplicationEx.sunriseTime,
                PendingIntent.getBroadcast(ApplicationEx.getApp(), 0, 
                        new Intent(Constants.ACTION_DISABLE_NIGHT),
                        PendingIntent.FLAG_UPDATE_CURRENT));
    }
    
    public static void cancelSunriseAlarm() {
        AlarmManager alarmMan = (AlarmManager) ApplicationEx.getApp()
                .getSystemService(Context.ALARM_SERVICE);
        PendingIntent sunIntent = PendingIntent.getBroadcast(
                ApplicationEx.getApp(), 0,
                new Intent(Constants.ACTION_DISABLE_NIGHT),
                PendingIntent.FLAG_UPDATE_CURRENT);
        alarmMan.cancel(sunIntent);
    }
    
    public static void setSunsetAlarm() {
        AlarmManager alarmMan = (AlarmManager) ApplicationEx.getApp()
                .getSystemService(Context.ALARM_SERVICE);
        alarmMan.set(AlarmManager.RTC, ApplicationEx.sunsetTime,
                PendingIntent.getBroadcast(ApplicationEx.getApp(), 0, 
                        new Intent(Constants.ACTION_ENABLE_NIGHT),
                        PendingIntent.FLAG_UPDATE_CURRENT));
    }
    
    public static void cancelSunsetAlarm() {
        AlarmManager alarmMan = (AlarmManager) ApplicationEx.getApp()
                .getSystemService(Context.ALARM_SERVICE);
        PendingIntent sunIntent = PendingIntent.getBroadcast(
                ApplicationEx.getApp(), 0,
                new Intent(Constants.ACTION_ENABLE_NIGHT),
                PendingIntent.FLAG_UPDATE_CURRENT);
        alarmMan.cancel(sunIntent);
    }
    
    public static void setDayAlarm() {
        AlarmManager alarmMan = (AlarmManager) ApplicationEx.getApp()
                .getSystemService(Context.ALARM_SERVICE);
        alarmMan.setRepeating(AlarmManager.RTC,
                System.currentTimeMillis() - (
                        System.currentTimeMillis() % Constants.DAY_MILLI),
                Constants.DAY_MILLI, PendingIntent.getBroadcast(
                        ApplicationEx.getApp(), 0, 
                        new Intent(Constants.ACTION_UPDATE_SUN_TIMES),
                        PendingIntent.FLAG_UPDATE_CURRENT));
    }
    
    public static void setRepeatingAlarm(int interval, boolean immediate) {
        cancelRepeatingAlarm();
        Intent serviceIntent = new Intent(ApplicationEx.getApp(), 
                UpdateService.class);
        serviceIntent.putExtra(Constants.SYNC, ApplicationEx.isSyncing() &&
                Util.readBooleanFromFile(Constants.GOOGLE_FILENAME));
        AlarmManager alarmMan = (AlarmManager) ApplicationEx.getApp()
                .getSystemService(Context.ALARM_SERVICE);
        long currTime = System.currentTimeMillis();
        long newInterval = getRepeatingIntervalFromPref(interval);
        long startTime = (currTime - (currTime % newInterval) + newInterval);
        alarmMan.setRepeating(AlarmManager.RTC_WAKEUP, startTime, newInterval, 
                PendingIntent.getService(ApplicationEx.getApp(), 0, 
                        serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        if (immediate)
            ApplicationEx.getApp().startService(serviceIntent);
    }
    
    public static void setMagicAlarm(long triggerTime, 
            ArrayList<String> feeds) {
        // After this one is set, when the update occurs, the service will
        // schedule the next feed to update and the trigger time and so on
        // forever.
        // Problems: Will need to keep track of the last feed that was updated
        // and when.
        // Also, when an update happens and
        // there aren't any new episodes, the feed needs to stay the same and 
        // the trigger time is to be updated to be the same plus some time
        // interval.
        cancelRepeatingAlarm();
        Intent serviceIntent = new Intent(ApplicationEx.getApp(), 
                UpdateService.class);
        serviceIntent.putExtra(Constants.SYNC, ApplicationEx.isSyncing() &&
                Util.readBooleanFromFile(Constants.GOOGLE_FILENAME));
        serviceIntent.putExtra(Constants.FEED_LIST, feeds);
        AlarmManager alarmMan = (AlarmManager) ApplicationEx.getApp()
                .getSystemService(Context.ALARM_SERVICE);
        alarmMan.set(AlarmManager.RTC_WAKEUP, triggerTime, 
                PendingIntent.getService(ApplicationEx.getApp(), 0, 
                        serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        ApplicationEx.dbHelper.setNextMagicUpdate(triggerTime);
        Util.persistUpdatePlaylist(feeds);
    }
    
    public static void cancelRepeatingAlarm() {
        AlarmManager alarmMan = (AlarmManager) ApplicationEx.getApp()
                .getSystemService(Context.ALARM_SERVICE);
        Intent serviceIntent = new Intent(ApplicationEx.getApp(), 
                UpdateService.class);
        PendingIntent updateIntent = PendingIntent.getService(
                ApplicationEx.getApp(), 0, serviceIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT);
        alarmMan.cancel(updateIntent);
    }
    
    private static long getRepeatingIntervalFromPref(int interval) {
        long newInterval = 60;
        switch(interval) {
        case 0:
            newInterval = 30;
            break;
        case 1:
            newInterval = 60;
            break;
        case 2:
            newInterval = 120;
            break;
        case 3:
            newInterval = 180;
            break;
        case 4:
            newInterval = 360;
            break;
        case 5:
            newInterval = 720;
            break;
        case 6:
            newInterval = 1440;
            break;
        }
        return newInterval*Constants.MINUTE_MILLI;
    }
    
    public static boolean findFile(String path, String filename) {
        File file = new File(path, filename);
        return file.exists();
    }
    
    public static boolean isMyServiceRunning(String serviceName) {
        ActivityManager manager = (ActivityManager) ApplicationEx.getApp().
                getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (serviceName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isMyServiceForeground(String serviceName) {
        ActivityManager manager = (ActivityManager) ApplicationEx.getApp().
                getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (serviceName.equals(service.service.getClassName()) &&
                    service.foreground) {
                return true;
            }
        }
        return false;
    }
    
    public static ArrayList<String> parseJsonString(String jsonString,
            String tag) {
        JSONObject jsonObject = null;
        JSONArray jsonSubs = null;
        try {
            jsonObject = new JSONObject(jsonString);
        } catch (JSONException e) {
            Log.e(Constants.LOG_TAG, 
                    "Bad JSON array from string: " + jsonString, e);
        }
        try {
            jsonSubs = jsonObject.getJSONArray(tag);
        } catch (JSONException j) {
            Log.e(Constants.LOG_TAG, 
                    "Bad JSON element: " + tag, j);
        }
        /*
        if (tag.equalsIgnoreCase("subscriptions") && readStringFromFile(
                Constants.SUBS_FILENAME).equalsIgnoreCase(jsonString)) {
            return null;
        }
        */
        ArrayList<String> doneSubs = new ArrayList<String>();
        for (int i = 0; i < jsonSubs.length(); i++) {
            try {
                doneSubs.add(jsonSubs.getJSONObject(i).getString("id"));
            } catch (JSONException e) {
                Log.e(Constants.LOG_TAG, 
                        "Can't get json object", e);
            }
        }
        return doneSubs;
    }
    
    protected static String getContinuationToken(String jsonString) {
        JSONObject jsonObject = null;
        String continuation = null;
        try {
            jsonObject = new JSONObject(jsonString);
            continuation = jsonObject.getString("continuation");
        } catch (JSONException e) {
            Log.e(Constants.LOG_TAG, 
                    "Bad JSON object from string: " + jsonString, e);
        }
        return continuation;
    }
    
    /**
     * Read the entire string from a file.
     * @param filename  file in local storage to read
     * @return  the text read from the file
     */
    protected static String readStringFromFile(String filename) {
        File[] files = ApplicationEx.getApp().getFilesDir().listFiles();
        File myFile = null;
        String text = Constants.EMPTY;
        for (File file : files) {
            if (file.getName().equalsIgnoreCase(filename))
                myFile = file;
        }
        if (myFile != null) {
            byte[] buffer = new byte[(int)myFile.length()];
            BufferedInputStream bufStream = null;
            try {
                bufStream = new BufferedInputStream(
                        ApplicationEx.getApp().openFileInput(filename), 1024);
            } catch (FileNotFoundException e) {
                Log.e(Constants.LOG_TAG, "File not found: " + filename, e);
            }
            try {
                bufStream.read(buffer);
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, "Unable to read file: " + filename, e);
            }
            text = new String(buffer);
            try {
                bufStream.close();
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, 
                        "Unable to close input stream for: " + filename, e);
            }
        }
        return text;
    }
    
    public static boolean readBooleanFromFile(String filename) {
        String value = readStringFromFile(filename);
        return Boolean.parseBoolean(value);
    }
    
    public static float readFloatFromFile(String filename) {
        String value = readStringFromFile(filename);
        Float currValue = null;
        try {
            currValue = Float.parseFloat(value);
        } catch (NumberFormatException e) {
            Log.e(Constants.LOG_TAG, "Bad float value: " + value);
            return 1.0f;
        }
        return currValue;
    }
    
    private static BufferedInputStream bufStream;
    private static StringBuilder text = new StringBuilder();
    private static byte[] buffer;
    private static File file;
    
    protected static StringBuilder readStringFromExternalFile(
            StringBuilder filename) {
        file = new File(filename.toString());
        if (file.length() <= 0)
            return null;
        buffer = new byte[(int)file.length()];
        try {
            bufStream = new BufferedInputStream(new FileInputStream(file), 
                    buffer.length);
        } catch (FileNotFoundException e) {
            Log.e(Constants.LOG_TAG, "File not found: " + filename, e);
        }
        try {
            bufStream.read(buffer);
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, "Unable to read file: " + filename, e);
        }
        text.setLength(0);
        text.append(bytesToString(buffer));
        try {
            bufStream.close();
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, 
                    "Unable to close input stream for: " + filename, e);
        }
        return text;
    }
    
    private static Charset charset;
    private static CharsetDecoder decoder;
    private static ByteBuffer srcBuffer;
    private static CharBuffer charBuffer;
    
    private static CharBuffer bytesToString(byte[] input) {
        charset = Charset.forName(Constants.UTF8);
        decoder = charset.newDecoder();
        srcBuffer = ByteBuffer.wrap(input);
        charBuffer = null;
        try {
            charBuffer = decoder.decode(srcBuffer);
        } catch (CharacterCodingException e) {}
        return charBuffer;
    }

    /**
     * Write a byte array representing a string to a file.
     * @param buffer    byte array containing the byte representation of the 
     *                  string
     * @param filename  the file to write to
     */
    public static void writeBufferToFile(byte[] buffer, String filename) {
        BufferedOutputStream bufStream = null;
        try {
            bufStream = new BufferedOutputStream(
                    ApplicationEx.getApp().openFileOutput(filename, 
                            Context.MODE_MULTI_PROCESS), buffer.length);
            bufStream.write(buffer);
            bufStream.flush();
            bufStream.close();
        } catch (FileNotFoundException e) {
            Log.e(Constants.LOG_TAG, "File not found: " + filename, e);
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, 
                    "BufferedOutputStream failed for: " + filename, e);
        }
    }
    
    protected static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }
    
    public static void deleteFeedEpisodes(int id, ArrayList<Integer> epIdList) {
        File feedDir = new File(TextUtils.concat(ApplicationEx.cacheLocation,
                Constants.FEEDS_LOCATION, Integer.toString(id), File.separator)
                        .toString());
        File currFile;
        String ext;
        for (Integer epId : epIdList) {
            ext = ApplicationEx.dbHelper.extFromEpisodeId(epId);
            currFile = new File(TextUtils.concat(feedDir.getAbsolutePath(),
                    Integer.toString(epId), ext).toString());
            if (feedDir.isDirectory() && currFile.exists())
                currFile.delete();
        }
    }
    
    public static ArrayList<Feed> getAllFeeds(int read, int downloaded, 
            int sort, int sortType, int archive) {
        Cursor cur = ApplicationEx.dbHelper.getAllFeeds(read, downloaded, sort, 
                sortType);
        ArrayList<Feed> feeds = new ArrayList<Feed>();
        int id = -1;
        if (cur.moveToFirst()) {
            do {
                id = cur.getInt(cur.getColumnIndex(DatabaseHelper.COL_FEED_ID));
                feeds.add(new Feed(
                        id, 
                        cur.getString(cur.getColumnIndex(
                                DatabaseHelper.COL_FEED_TITLE)),
                        TextUtils.concat(Integer.toString(
                                ApplicationEx.dbHelper.getReadEpisodes(id, true,
                                archive).size()), Constants.NEW_SPACE)
                                        .toString(),
                        cur.getInt(cur.getColumnIndex(
                                DatabaseHelper.COL_FEED_UNREAD)) == 
                                        Constants.UNREAD ? true : false,
                        cur.getInt(cur.getColumnIndex(
                                DatabaseHelper.COL_FEED_DOWNLOADED)) == 
                                        Constants.DOWNLOADED ? true : false,
                        cur.getString(cur.getColumnIndex(
                                DatabaseHelper.COL_FEED_ADDRESS)),
                        getFeedEpisodes(id, read, downloaded, sort, 
                                sortType, archive)));
            } while (cur.moveToNext());
        }
        cur.close();
        return feeds;
    }
    
    private static String convertDate(long pub) {
        if (pub == -1)
            return Constants.EMPTY;
        SimpleDateFormat df = new SimpleDateFormat(Constants.SIMPLE_TODAY,
                Locale.getDefault());
        long offset = TimeZone.getDefault().getOffset(pub);
        long currTime = System.currentTimeMillis() + offset;
        if (pub + offset < (currTime-(Constants.DAY_MILLI*365)))
            df = new SimpleDateFormat(Constants.SIMPLE_YEAR,
                    Locale.getDefault());
        else if (pub + offset < (currTime-(Constants.DAY_MILLI*6)-
                (currTime % Constants.DAY_MILLI)) || pub + offset >=
                (currTime-(currTime%Constants.DAY_MILLI) + Constants.DAY_MILLI))
            df = new SimpleDateFormat(Constants.SIMPLE_MONTH,
                    Locale.getDefault());
        else if (pub + offset < currTime-(currTime%Constants.DAY_MILLI))
            df = new SimpleDateFormat(Constants.SIMPLE_DAY,
                    Locale.getDefault());
        df.setTimeZone(TimeZone.getDefault());
        return (String) DateFormat.format(df.toLocalizedPattern(), pub);
    }
    
    public static ArrayList<Episode> getAllEpisodes(int read, int downloaded, 
            int sort, int sortType, int archive, boolean limit, int offset,
            int total) {
        Cursor cur = ApplicationEx.dbHelper.getAllEpisodes(read, downloaded,
                sort, sortType, archive, limit, offset, total);
        ArrayList<Episode> episodes = new ArrayList<Episode>();
        if (cur.moveToFirst()) {
            do {
                episodes.add(new Episode(
                        cur.getInt(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_ID)), 
                        cur.getString(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_TITLE)), 
                        convertDate(cur.getLong(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_PUB))), 
                        cur.getLong(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_PUB)),
                        cur.getInt(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_READ)) == 
                                        Constants.UNREAD ? true : false,
                        cur.getInt(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_DOWNLOADED)) == 
                                        Constants.DOWNLOADED ? true : false,
                        cur.getString(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_URL)),
                        cur.getInt(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_DURATION)),
                        cur.getInt(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_LOCKED)) == 1 ? true
                                        : false,
                        cur.getInt(cur.getColumnIndex(
                                DatabaseHelper.COL_EP_FEED_ID))));
            } while (cur.moveToNext());
        }
        cur.close();
        return episodes;
    }
    
    public static ArrayList<Episode> getFeedEpisodes(int feedId, int read, 
            int downloaded, int sort, int sortType, int archive) {
        Cursor cur = ApplicationEx.dbHelper.getFilteredFeedEpisodes(feedId, 
                read, downloaded, sort, sortType, archive);
        ArrayList<Episode> episodes = new ArrayList<Episode>();
        if (cur.moveToFirst()) {
            do {
                episodes.add(new Episode(
                        cur.getInt(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_ID)), 
                        cur.getString(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_TITLE)), 
                        convertDate(cur.getLong(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_PUB))), 
                        cur.getLong(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_PUB)),
                        cur.getInt(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_READ)) == 
                                        Constants.UNREAD ? true : false,
                        cur.getInt(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_DOWNLOADED)) == 
                                        Constants.DOWNLOADED ? true : false,
                        cur.getString(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_URL)),
                        cur.getInt(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_DURATION)),
                        cur.getInt(cur.getColumnIndex(
                                DatabaseHelper.COL_EPISODE_LOCKED)) == 1 ? true
                                        : false,
                        cur.getInt(cur.getColumnIndex(
                                DatabaseHelper.COL_EP_FEED_ID))));
            } while (cur.moveToNext());
        }
        cur.close();
        return episodes;
    }
    
    public static ArrayList<Episode> getDownloading(ArrayList<String> eps) {
        ArrayList<String> epsCopy = new ArrayList<String>(eps);
        ArrayList<Episode> episodes = new ArrayList<Episode>();
        int epId;
        String[] episode;
        for (String ep : epsCopy) {
            epId = ApplicationEx.dbHelper.getEpisodeId(ep);
            episode = ApplicationEx.dbHelper.getEpisode(epId);
            if (episode == null) {
                episode = new String[5];
                episode[1] = Constants.ERROR;
                episode[2] = Constants.ERROR;
            }
            episodes.add(new Episode(epId, episode[1], episode[2], -1, false, 
                    false, null, 0, false, -1));
        }
        return episodes;
    }
    
    public static boolean checkConnectionNoTask() {
        Connection connection;
        Response response;
        try {
            connection = Jsoup.connect(Constants.CONNECTION_URL)
                    .followRedirects(false);
            connection.get();
            response = connection.response();
        } catch (IOException e) {
            return false;
        }
        if (response.statusCode() != 200)
            return false;
        else
            return true;
    }
    
    @SuppressWarnings("rawtypes")
    public static class ListWriteTask extends AsyncTask<Void, Void, Void> {
        
        private ArrayList list;
        private String filename;
        
        public ListWriteTask(ArrayList list, String filename) {
            this.list = list;
            this.filename = filename;
        }
        
        @Override
        protected Void doInBackground(Void... params) {
            JSONArray jsonArray = new JSONArray(list);
            Util.writeBufferToFile(jsonArray.toString().getBytes(), filename);
            return null;
        }
    }
    
    @SuppressLint("NewApi")
    public static void persistCurrentPlaylist(ArrayList<Integer> epIdList) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            new ListWriteTask(epIdList, Constants.PLAYLIST_FILENAME)
                    .execute();
        else
            new ListWriteTask(epIdList, Constants.PLAYLIST_FILENAME)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    @SuppressLint("NewApi")
    public static void persistUpdatePlaylist(ArrayList<String> feedList) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            new ListWriteTask(feedList, Constants.UPDATE_LIST_FILENAME)
                    .execute();
        else
            new ListWriteTask(feedList, Constants.UPDATE_LIST_FILENAME)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    @SuppressLint("NewApi")
    public static void persistSpeedList(ArrayList<Double> speedList) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            new ListWriteTask(speedList, Constants.SPEED_LIST_FILENAME)
                    .execute();
        else
            new ListWriteTask(speedList, Constants.SPEED_LIST_FILENAME)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    public static ArrayList<Double> readSpeedList() {
        String json = Util.readStringFromFile(Constants.SPEED_LIST_FILENAME);
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(json);
        } catch (JSONException e) {
            Log.e(Constants.LOG_TAG, "Can't create array from json: " + json, 
                    e);
            e.printStackTrace();
        }
        ArrayList<Double> speedList = new ArrayList<Double>();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    speedList.add(jsonArray.getDouble(i));
                } catch (JSONException e) {
                    Log.e(Constants.LOG_TAG, 
                        "Can't get double from json array at index " + i, e);
                    e.printStackTrace();
                }
            }
        }
        return speedList;
    }
    
    public static ArrayList<Integer> readCurrentPlaylist() {
        String json = Util.readStringFromFile(Constants.PLAYLIST_FILENAME);
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(json);
        } catch (JSONException e) {
            Log.e(Constants.LOG_TAG, "Can't create array from json: " + json, 
                    e);
            e.printStackTrace();
        }
        ArrayList<Integer> epIdList = new ArrayList<Integer>();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    epIdList.add(jsonArray.getInt(i));
                } catch (JSONException e) {
                    Log.e(Constants.LOG_TAG, 
                            "Can't get int from json array at index " + i, e);
                    e.printStackTrace();
                }
            }
        }
        return epIdList;
    }
    
    public static ArrayList<String> readUpdatePlaylist() {
        String json = Util.readStringFromFile(Constants.UPDATE_LIST_FILENAME);
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(json);
        } catch (JSONException e) {
            Log.e(Constants.LOG_TAG, "Can't create array from json: " + json, 
                    e);
            e.printStackTrace();
        }
        ArrayList<String> feedList = new ArrayList<String>();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    feedList.add(jsonArray.getString(i));
                } catch (JSONException e) {
                    Log.e(Constants.LOG_TAG, 
                            "Can't get int from json array at index " + i, e);
                    e.printStackTrace();
                }
            }
        }
        return feedList;
    }
    
    public static void persistPreference(int currEpId, int key) {
        PreferenceManager.getDefaultSharedPreferences(ApplicationEx.getApp())
                .edit()
                .putInt(ApplicationEx.getApp().getString(key), currEpId)
                .commit();
    }
    
    public static void persistPreference(String value, int key) {
        PreferenceManager.getDefaultSharedPreferences(ApplicationEx.getApp())
                .edit()
                .putString(ApplicationEx.getApp().getString(key), value)
                .commit();
    }
    
    public static void persistPreference(boolean value, int key) {
        PreferenceManager.getDefaultSharedPreferences(ApplicationEx.getApp())
                .edit()
                .putBoolean(ApplicationEx.getApp().getString(key), value)
                .commit();
    }
    
    public static boolean readBooleanPreference(int key, boolean defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(
                ApplicationEx.getApp())
            .getBoolean(ApplicationEx.getApp().getString(key), defaultValue);
    }
    
    public static String readStringPreference(int key, String defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(
                ApplicationEx.getApp())
            .getString(ApplicationEx.getApp().getString(key), defaultValue);
    }
    
    public static int readIntegerPreference(int key, int defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(
                ApplicationEx.getApp())
            .getInt(ApplicationEx.getApp().getString(key), defaultValue);
    }
    
    public static boolean containsPreference(int key) {
        return PreferenceManager.getDefaultSharedPreferences(
                ApplicationEx.getApp())
            .contains(ApplicationEx.getApp().getString(key));
    }
    
    public static ArrayList<Integer> getEpisodeIdList(boolean downloadOnly,
            int offset, int total) {
        int read = ApplicationEx.dbHelper.getCurrentEpisodeRead();
        int downloaded = downloadOnly ? Constants.DOWNLOADED :
                ApplicationEx.dbHelper.getCurrentEpisodeDownloaded();
        int sort = Constants.PLAYLIST_SORT_EPISODE|Constants.PLAYLIST_SORT_DATE;
        int sortType = ApplicationEx.dbHelper.getCurrentEpisodeSortType();
        int archive = Util.readBooleanPreference(R.string.archive_key, false) 
                ? 1 : 0;
        ArrayList<Episode> episodes = getAllEpisodes(read, downloaded, 
                sort, sortType, archive, false, offset, total);
        ArrayList<Integer> epIdList = new ArrayList<Integer>();
        for (int i = 0; i < episodes.size(); i++) {
            // TODO Add ability to stream episodes here
            epIdList.add(episodes.get(i).getId());
        }
        return epIdList;
    }
    
    public static SparseIntArray getEpisodeOrder() {
        int read = ApplicationEx.dbHelper.getCurrentEpisodeRead();
        int downloaded = ApplicationEx.dbHelper.getCurrentEpisodeDownloaded();
        int sort = Constants.PLAYLIST_SORT_EPISODE|Constants.PLAYLIST_SORT_DATE;
        int sortType = ApplicationEx.dbHelper.getCurrentEpisodeSortType();
        int archive = Util.readBooleanPreference(R.string.archive_key, false) 
                ? 1 : 0;
        ArrayList<Episode> episodes = getAllEpisodes(read, downloaded, sort,
                sortType, archive, false, -1, -1);
        SparseIntArray epIdMap = new SparseIntArray();
        for (int i = 0; i < episodes.size(); i++) {
            epIdMap.put(i, episodes.get(i).getId());
        }
        return epIdMap;
    }
    
    public static long getAllEpisodesDuration() {
        ArrayList<Episode> episodes = getAllEpisodes(
                Constants.READ|Constants.UNREAD, Constants.DOWNLOADED, 
                Constants.PLAYLIST_SORT_EPISODE|Constants.PLAYLIST_SORT_DATE,
                Constants.PLAYLIST_SORT_TYPE_ASC,
                Util.readBooleanPreference(R.string.archive_key, false) ? 1 : 0,
                false, -1, -1);
        long duration = 0;
        for (Episode episode : episodes) {
            duration += episode.getDuration() /
                    ApplicationEx.dbHelper.getFeedSpeed(episode.getId());
        }
        return duration;
    }
    
    public static ArrayList<Integer> getFeedEpisodeIdList(
            boolean downloadOnly) {
        int read = ApplicationEx.dbHelper.getCurrentFeedRead();
        int downloaded = downloadOnly ? Constants.DOWNLOADED :
                ApplicationEx.dbHelper.getCurrentFeedDownloaded();
        int sortType = ApplicationEx.dbHelper.getCurrentFeedSortType();
        int sort = ApplicationEx.dbHelper.getCurrentFeedSort();
        int archive = Util.readBooleanPreference(R.string.archive_key, false) 
                ? 1 : 0;
        ArrayList<Feed> feeds = Util.getAllFeeds(read, downloaded, sort, 
                sortType, archive);
        ArrayList<Integer> epIdList = new ArrayList<Integer>();
        ArrayList<Episode> episodes;
        for (int i = 0; i < feeds.size(); i++) {
            episodes = feeds.get(i).getEpisodes();
            for (int j = 0; j < episodes.size(); j++) {
                // TODO Add ability to stream episodes
                epIdList.add(episodes.get(j).getId());
            }
        }
        return epIdList;
    }
    
    public static String getTimeString(long timeMillis) {
        // 56789000
        int timeSecs = (int) (timeMillis / 1000);
        // 29
        int secs = timeSecs % 60;
        // 46
        int mins = ((timeSecs - secs) / 60) % 60;
        // 126
        int hours = (((timeSecs - secs) / 60) - mins) / 60;
        String timeString = TextUtils.concat(Integer.toString(hours),
                Constants.COLON).toString();
        timeString = TextUtils.concat(timeString,
                (mins < 10 ? Constants.ZERO : Constants.EMPTY),
                Integer.toString(mins), Constants.COLON).toString();
        timeString = TextUtils.concat(timeString,
                (secs < 10 ? Constants.ZERO : Constants.EMPTY),
                Integer.toString(secs)).toString();
        return timeString;
    }
    
    public static String getEndTimeString(ArrayList<Integer> playlist) {
        long timeMillis = System.currentTimeMillis();
        for (Integer id : playlist) {
            timeMillis += (ApplicationEx.dbHelper.getEpisodeDuration(id)/
                    ApplicationEx.dbHelper.getFeedSpeed(id));
        }
        Calendar cal = Calendar.getInstance(TimeZone.getDefault(),
                Locale.getDefault());
        cal.setTimeInMillis(timeMillis);
        SimpleDateFormat df = new SimpleDateFormat(Constants.SIMPLE_END_TIME,
                Locale.getDefault());
        if (timeMillis - System.currentTimeMillis() < Constants.DAY_MILLI)
            df = new SimpleDateFormat(Constants.SIMPLE_END_TIME_TODAY,
                    Locale.getDefault());
        df.setTimeZone(TimeZone.getDefault());
        return (String) DateFormat.format(df.toLocalizedPattern(), timeMillis);
    }
    
    public static String getDateString(long time) {
        SimpleDateFormat df = new SimpleDateFormat(Constants.SIMPLE_FEED_DATE,
                Locale.getDefault());
        df.setTimeZone(TimeZone.getDefault());
        return (String) DateFormat.format(df.toLocalizedPattern(), time);
    }
    
    public static void savePosition(int position, int epId) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_EPISODE_POSITION, position);
        ApplicationEx.dbHelper.updateRecord(cv, DatabaseHelper.EPISODE_TABLE,
                TextUtils.concat(DatabaseHelper.COL_EPISODE_ID, Constants.EQUAL,
                        Integer.toString(epId)).toString());
        ApplicationEx.dbHelper.setCurrentEpisodeProgress(position);
    }
    
    public static class Magics {
        
        private long time;
        private ArrayList<String> feeds;
        
        public Magics() {
            time = -1;
            feeds = new ArrayList<String>();
        }
        
        public long getTime() {
            return time;
        }
        
        public ArrayList<String> getFeeds() {
            return feeds;
        }
        
        public void setTime(long time) {
            this.time = time;
        }
        
        public void setFeeds(ArrayList<String> feeds) {
            this.feeds = feeds;
        }
        
    }
    
    public static void saveAccountInfo(Bundle bundle, String[] accountInfo) {
        accountInfo[1] = bundle.getString(AccountManager.KEY_AUTHTOKEN);
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_API_NAME, accountInfo[0]);
        cv.put(DatabaseHelper.COL_API_KEY, accountInfo[1]);
        cv.put(DatabaseHelper.COL_API_ACCOUNT_TYPE, accountInfo[2]);
        cv.put(DatabaseHelper.COL_API_UPDATED, System.currentTimeMillis());
        if (!ApplicationEx.dbHelper.inDb(new String[] {accountInfo[0]}, 
                DatabaseHelper.API_TABLE, 
                new String[] {DatabaseHelper.COL_API_NAME})) {
            ApplicationEx.dbHelper.insertRecord(cv, DatabaseHelper.API_TABLE, 
                    DatabaseHelper.COL_API_NAME);
        }
        else {
            ApplicationEx.dbHelper.updateRecord(cv, DatabaseHelper.API_TABLE, 
                    TextUtils.concat(DatabaseHelper.COL_API_NAME,
                            Constants.EQUAL, DatabaseUtils.sqlEscapeString(
                                cv.getAsString(DatabaseHelper.COL_API_NAME)))
                            .toString());
        }
        Util.writeBufferToFile(Boolean.toString(false).getBytes(),
                Constants.CHECKTOKEN_FILENAME);
    }
    
    public static boolean updateKey(String accountName) {
        long updateTime = ApplicationEx.dbHelper.getLong(accountName, 
                DatabaseHelper.COL_API_UPDATED, DatabaseHelper.API_TABLE, 
                DatabaseHelper.COL_API_NAME);
        return updateTime <= System.currentTimeMillis()-(Constants.WEEK_MILLI*2)
                ? true : false;
    }
    
    public static class UpdateWaitCounter extends CountDownTimer {
        
        public UpdateWaitCounter(long timeLeft, long updateInterval) {
            super(timeLeft, updateInterval);
        }

        @Override
        public void onFinish() {
            ApplicationEx.setUpdateWait(false);
        }

        @Override
        public void onTick(long position) {
            ApplicationEx.setUpdateWait(true);
        }
    }
    
    /**
     * @param context Application context.
     * @param message The toast message to be displayed by the plug-in. Cannot be null.
     * @return A blurb for the plug-in.
     */
    public static String generateBlurb(final Context context,
            final String message) {
        final int maxBlurbLength = context.getResources().getInteger(
                R.integer.twofortyfouram_locale_maximum_blurb_length);

        if (message.length() > maxBlurbLength)
            return message.substring(0, maxBlurbLength);

        return message;
    }
    
    public static String getStringPref(int resId, String defValue) {
        return ApplicationEx.sharedPrefs.getString(
                ApplicationEx.getApp().getString(resId), defValue);
    }
    
    public static int getConnectedState() {
        Intent intent = ApplicationEx.getApp().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
    }
    
    public static boolean hasWifi() {
        ConnectivityManager cm =
                (ConnectivityManager) ApplicationEx.getApp().getSystemService(
                        Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info.getType() == ConnectivityManager.TYPE_WIFI;
    }
    
    public static boolean has4G() {
        ConnectivityManager cm =
                (ConnectivityManager) ApplicationEx.getApp().getSystemService(
                        Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (hasWifi() || (info.getType() == ConnectivityManager.TYPE_MOBILE
                && (info.getSubtype() == TelephonyManager.NETWORK_TYPE_LTE ||
                info.getSubtype() == TelephonyManager.NETWORK_TYPE_HSPAP)));
    }
    
}
