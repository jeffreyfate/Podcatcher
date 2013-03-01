package com.jeffthefate.pod_dev;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Button;

import com.jeffthefate.pod_dev.service.UpdateService;

public class Util {
    
    public static class UpdateFeedTask extends AsyncTask<Void, Void, Void> {
        
        Button subscribeButton;
        String url;

        public UpdateFeedTask(Button subscribeButton, String url) {
            this.subscribeButton = subscribeButton;
            this.url = url;
        }
        
        @Override
        protected Void doInBackground(Void... nothing) {
            publishProgress();
            ArrayList<String> result = updateFeed(url);
            boolean sync = ApplicationEx.isSyncing() &&
                    Util.readBooleanFromFile(Constants.GOOGLE_FILENAME);
            if (result != null && sync) {
                Intent intent = new Intent(ApplicationEx.getApp(), 
                        UpdateService.class);
                intent.putExtra("sync", sync);
                if (url != null)
                    intent.putExtra("feed", url);
                ApplicationEx.getApp().startService(intent);
            }
            return null;
        }
        
        protected void onProgressUpdate(Void... nothing) {
            subscribeButton.setText("Adding feed");
            subscribeButton.setEnabled(false);
        }

    }
    
    protected static ArrayList<String> updateFeed(String feed) {
        if (!ApplicationEx.hasConnection())
            return null;
        if (!ApplicationEx.dbHelper.feedExists(feed))
            return addNewFeed(feed);
        else if (ApplicationEx.dbHelper.getInt(feed, 
                DatabaseHelper.COL_FEED_PODCAST, DatabaseHelper.FEED_TABLE, 
                DatabaseHelper.COL_FEED_ADDRESS)
                    != 1)
            return null;
        else
            return getXml(feed);
    }
    
    public static ArrayList<String> addNewFeed(String feed) {
        ArrayList<String> result = getXml(feed);
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
    
    public static ArrayList<String> getXml(String jsonSub) {
        return new XmlDomParser(jsonSub).insertFeed();
    }
    
    public static void downloadImage(int feedId, String address) {
        String currDir = "";
        String currFile = "";
        String smallFile = "";
        String extension = "";
        currDir = ApplicationEx.cacheLocation + Constants.FEEDS_LOCATION +
                feedId + File.separator;
        File path = new File(currDir);
        path.setExecutable(true, false);
        path.setReadable(true, false);
        path.setWritable(true, false);
        path.mkdirs();
        if (address.endsWith(".png"))
            extension = ".png";
        else if (address.endsWith(".jpg") || address.endsWith(".jpeg"))
            extension = ".jpg";
        else if (address.endsWith(".gif"))
            extension = ".gif";
        else
            return;
        currFile = currDir + feedId + extension;
        smallFile = currDir + feedId + "_small" + ".png";
        if (Util.findFile(currDir, feedId + extension) && 
                Util.findFile(currDir, feedId + "_small" + ".png") &&
                ApplicationEx.dbHelper.getLong(address, 
                DatabaseHelper.COL_FEED_LAST_IMAGE_UPDATE, 
                DatabaseHelper.FEED_TABLE, DatabaseHelper.COL_FEED_IMAGE) >
                    System.currentTimeMillis(
                            )-Constants.IMAGE_UPDATE_INTERVAL) {
            return;
        }
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
                DatabaseHelper.COL_FEED_ID + "=" + feedId);
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
            ArrayList<HashMap<String, String>> feedList, long sortOrder) {
        /* If somehow the list is null, there was an error */
        if (feedList == null) {
            return new ArrayList<String>();
        }
        /* Setup content values with the feed information */
        ContentValues cv = new ContentValues();
        /* First map in the list is the feed map */
        HashMap<String, String> feedMap = feedList.get(0);
        cv.put(DatabaseHelper.COL_FEED_ADDRESS, feedId);
        cv.put(DatabaseHelper.COL_FEED_TITLE, feedMap.get("title"));
        cv.put(DatabaseHelper.COL_FEED_LINK, feedMap.get("link"));
        cv.put(DatabaseHelper.COL_FEED_DESCRIPTION, 
                feedMap.get("description"));
        cv.put(DatabaseHelper.COL_FEED_IMAGE, 
                feedMap.get("itunes|image") != null ? 
                    feedMap.get("itunes|image") : feedMap.get("image > url"));
        int isPodcast = -1;
        /* Determine if it is a podcast, based on that last map in the list */
        if (!ApplicationEx.dbHelper.feedExists(feedId)) {
            isPodcast = feedList.get(feedList.size()-1).isEmpty() ? 1 : 0;
            cv.put(DatabaseHelper.COL_FEED_PODCAST, isPodcast);
        }
        /* Or if it is already in the database, just get the value */
        else
            isPodcast = ApplicationEx.dbHelper.feedIsPodcast(feedId) ? 1 : 0;
        /* Put the feed info in the database - adds it if it doesn't exist,
         * otherwise it updates.  Want to keep the feed data current */
        ApplicationEx.dbHelper.addFeed(cv, sortOrder);
        if (isPodcast == 1) {
            /* Save the XML to the feed folder and get the feed image if it is
             * a podcast */
            int id = ApplicationEx.dbHelper.getFeedId(feedId);
            String epLoc = ApplicationEx.cacheLocation +
                    Constants.FEEDS_LOCATION + id + File.separator;
            File path = new File(epLoc);
            path.setExecutable(true, false);
            path.setReadable(true, false);
            path.setWritable(true, false);
            path.mkdirs();
            path = new File(epLoc + id + ".xml");
            path.setExecutable(true, false);
            path.setReadable(true, false);
            path.setWritable(true, false);
            new File(ApplicationEx.cacheLocation + Constants.TEMP_LOCATION +
                    "temp.xml").renameTo(path);
            Util.getImage(ApplicationEx.dbHelper.getFeedId(feedId));
        }
        /* Not a podcast or there are no episodes in the list, nothing to do */
        if (isPodcast == 0 || feedList.size() <= 2) {
            return new ArrayList<String>();
        }
        return insertEpisodes(feedId, feedList, isPodcast);
    }
    /*
     * Takes a date string with the format
     *      EEE, dd MMM yyyy kk:mm:ss Z
     * and converts it to epoch time long value.
     */
    public static long dateStringToEpoch(String dateString) {
        long epochTime = -1;
        try {
            epochTime = new SimpleDateFormat(
                    "EEE, dd MMM yyyy kk:mm:ss Z")
                .parse(dateString).getTime();
        } catch (ParseException e) {
            Log.e(Constants.LOG_TAG, "Unable to parse date " + dateString, e);
        }
        return epochTime;
    }
    
    private static ArrayList<String> insertEpisodes(String url, 
            ArrayList<HashMap<String, String>> feedList, int isPodcast) {
        /* Get the list of episodes for the current feed - the episode URLs */
        int feedId = ApplicationEx.dbHelper.getInt(url, 
                DatabaseHelper.COL_FEED_ID, DatabaseHelper.FEED_TABLE, 
                DatabaseHelper.COL_FEED_ADDRESS);
        ArrayList<String> episodesList = 
            ApplicationEx.dbHelper.getEpisodesList(feedId);
        boolean newEp = false;
        ContentValues cv = new ContentValues();
        long currEpTime = -1;
        int result = -1;
        String tempUrl = null;
        /* Keep track of the episode URLs as we go */
        ArrayList<String> idsList = new ArrayList<String>();
        ArrayList<String> newList = new ArrayList<String>();
        for (HashMap<String, String> episodeHash : feedList) {
            /* Skip if it is the feed map, the empty map or there is no URL */
            if (!feedList.get(0).equals(episodeHash) && !episodeHash.isEmpty() 
                    && episodeHash.get("url") != null) {
                currEpTime = dateStringToEpoch(episodeHash.get("pubDate"));
                /* Used to later get the Google IDs from Reader for each */
                idsList.add(episodeHash.get("url"));
                /* If the episode URL is in the database and both the episode
                 * title and feed title match */
                if (ApplicationEx.dbHelper.inDb(
                        new String[] {episodeHash.get("url")}, 
                        DatabaseHelper.EPISODE_TABLE, new String[] {
                            DatabaseHelper.COL_EPISODE_URL}) && 
                    ApplicationEx.dbHelper.inDb(
                        new String[] {feedList.get(0).get("title"), 
                                episodeHash.get("title")}, 
                        DatabaseHelper.EPISODE_TABLE, new String[] {
                            DatabaseHelper.COL_EP_FEED_TITLE,
                            DatabaseHelper.COL_EPISODE_TITLE})) {
                    /* Setup the result to 0 because this one exists */
                    result = 0;
                    tempUrl = episodeHash.get("url");
                    /* Remove from list - the remaining episodes are archive */
                    episodesList.remove(tempUrl);
                    cv.clear();
                    /* Not an archive episode */
                    cv.put(DatabaseHelper.COL_EPISODE_ARCHIVE, 0);
                    /* Only put the archive value to make sure it is set to 0 */
                    ApplicationEx.dbHelper.updateRecord(cv, 
                        DatabaseHelper.EPISODE_TABLE, 
                        DatabaseHelper.COL_EPISODE_URL + "='" + tempUrl + "'");
                }
                /* If the episode URL and title are NOT in the database */
                else if (!ApplicationEx.dbHelper.inDb(
                        new String[] {episodeHash.get("url")}, 
                        DatabaseHelper.EPISODE_TABLE, new String[] {
                            DatabaseHelper.COL_EPISODE_URL}) &&
                    !ApplicationEx.dbHelper.inDb(
                            new String[] {feedList.get(0).get("title"),
                                    episodeHash.get("title")}, 
                            DatabaseHelper.EPISODE_TABLE, new String[] {
                                    DatabaseHelper.COL_EP_FEED_TITLE,
                                    DatabaseHelper.COL_EPISODE_TITLE})) {
                    /* Enter all episode info to the content values */
                    cv.clear();
                    cv.put(DatabaseHelper.COL_EPISODE_URL, 
                            episodeHash.get("url"));
                    cv.put(DatabaseHelper.COL_EPISODE_PUB, currEpTime);
                    cv.put(DatabaseHelper.COL_EPISODE_SUMMARY, Jsoup.parse(
                            StringEscapeUtils.unescapeHtml4(
                                    episodeHash.get("itunes|summary") != null ?
                                            episodeHash.get("itunes|summary") :
                                                episodeHash.get("description")))
                            .text().replace("\"", "\"\""));
                    cv.put(DatabaseHelper.COL_EPISODE_TYPE, 
                            episodeHash.get("type"));
                    cv.put(DatabaseHelper.COL_EPISODE_LENGTH, 
                            episodeHash.get("length"));
                    cv.put(DatabaseHelper.COL_EPISODE_TITLE, Jsoup.parse(
                            StringEscapeUtils.unescapeHtml4(episodeHash.get(
                                    "title"))).text().replace("\"", "\"\""));
                    cv.put(DatabaseHelper.COL_EPISODE_UPDATED, 
                            System.currentTimeMillis());
                    cv.put(DatabaseHelper.COL_EPISODE_READ, Constants.UNREAD);
                    cv.put(DatabaseHelper.COL_EPISODE_DOWNLOADED, 
                            Constants.TO_DOWNLOAD);
                    cv.put(DatabaseHelper.COL_EPISODE_FAILED_TRIES, 0);
                    /* Insert the episode */
                    ApplicationEx.dbHelper.addEpisode(cv, url, 
                            extFromType(cv.getAsString(
                                    DatabaseHelper.COL_EPISODE_TYPE)));
                    /* Indicate there is a new one */
                    newEp = true;
                    newList.add(episodeHash.get("url"));
                }
                /* If the episode URL isn't in database and an entry for the
                 * episode does exist, update the record */
                else if (!ApplicationEx.dbHelper.inDb(
                            new String[] {episodeHash.get("url")}, 
                            DatabaseHelper.EPISODE_TABLE, new String[] {
                                DatabaseHelper.COL_EPISODE_URL}) && 
                        ApplicationEx.dbHelper.inDb(
                            new String[] {feedList.get(0).get("title"), 
                                    episodeHash.get("title")}, 
                            DatabaseHelper.EPISODE_TABLE, new String[] {
                                DatabaseHelper.COL_EP_FEED_TITLE,
                                DatabaseHelper.COL_EPISODE_TITLE})) {
                    cv.clear();
                    cv.put(DatabaseHelper.COL_EPISODE_URL, 
                            episodeHash.get("url"));
                    cv.put(DatabaseHelper.COL_EPISODE_PUB, currEpTime);
                    cv.put(DatabaseHelper.COL_EPISODE_SUMMARY, Jsoup.parse(
                            StringEscapeUtils.unescapeHtml4(
                                    episodeHash.get("itunes|summary") != null ?
                                            episodeHash.get("itunes|summary") :
                                                episodeHash.get("description")))
                            .text().replace("\"", "\"\""));
                    cv.put(DatabaseHelper.COL_EPISODE_TYPE, 
                            episodeHash.get("type"));
                    cv.put(DatabaseHelper.COL_EPISODE_LENGTH, 
                            episodeHash.get("length"));
                    cv.put(DatabaseHelper.COL_EPISODE_UPDATED, 
                            System.currentTimeMillis());
                    /* The location of this episode could have changed, so we
                     * need to keep it up to date */
                    ApplicationEx.dbHelper.updateRecord(cv, 
                            DatabaseHelper.EPISODE_TABLE, 
                            DatabaseHelper.COL_EP_FEED_TITLE + "=\"" + 
                                feedList.get(0).get("title") + "\" AND " +
                                DatabaseHelper.COL_EPISODE_TITLE + "=\"" +
                                episodeHash.get("title").replace("\"", "\"\"") +
                                "\"");
                }
            }            
        }
        /* Indicators that we need to get the Google IDs from Reader */
        if (ApplicationEx.isSyncing() && !idsList.isEmpty()) {
            String[] account = ApplicationEx.dbHelper.getAccount();
            if (account != null) {
                String authToken = account[1];
                /* Ask for the items from Reader to get the read status and the
                 * IDs */
                getFeedItemIds(authToken, idsList, url,
                        ApplicationEx.dbHelper.getNumEpisodes(url));
            }
        }
        /* Each of the remaining episodes in the list are archive because they
         * are no longer in the feed XML */
        cv.clear();
        cv.put(DatabaseHelper.COL_EPISODE_ARCHIVE, 1);
        for (String epUrl : episodesList) {
            ApplicationEx.dbHelper.updateRecord(cv, 
                    DatabaseHelper.EPISODE_TABLE, DatabaseHelper.COL_EPISODE_URL 
                    + "='" + epUrl + "'");
        }
        /* Refreshing each list when there is a new episode and setting the
         * return value to indicate there was a new episode */
        if (newEp) {
            ApplicationEx.getApp().sendBroadcast(
                    new Intent(Constants.ACTION_REFRESH_EPISODES));
            ApplicationEx.getApp().sendBroadcast(
                    new Intent(Constants.ACTION_REFRESH_FEEDS));
            result = 1;
        }
        return newList;
    }
    
    private static String extFromType(String type) {
        String ext = "";
        if (type.equalsIgnoreCase(Constants.MPEG_TYPE)
                || type.equalsIgnoreCase(Constants.MP3_TYPE_1)
                        || type.equalsIgnoreCase(Constants.MP3_TYPE_2))
            ext = ".mp3";
        else if (type.equalsIgnoreCase(Constants.M4A_TYPE))
            ext = ".m4a";
        else if (type.equalsIgnoreCase(Constants.MP4_TYPE))
            ext = ".mp4";
        return ext;
    }
    
    public static void setRepeatingAlarm(int interval, boolean immediate) {
        cancelRepeatingAlarm();
        Intent serviceIntent = new Intent(ApplicationEx.getApp(), 
                UpdateService.class);
        serviceIntent.putExtra("sync", ApplicationEx.isSyncing() &&
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
        serviceIntent.putExtra("sync", ApplicationEx.isSyncing() &&
                Util.readBooleanFromFile(Constants.GOOGLE_FILENAME));
        serviceIntent.putExtra("feedList", feeds);
        AlarmManager alarmMan = (AlarmManager) ApplicationEx.getApp()
                .getSystemService(Context.ALARM_SERVICE);
        alarmMan.set(AlarmManager.RTC_WAKEUP, triggerTime, 
                PendingIntent.getService(ApplicationEx.getApp(), 0, 
                        serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        Util.writeBufferToFile(Long.toString(triggerTime).getBytes(),
                Constants.MAGICTIME_FILENAME);
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
    
    public static String getToken(String apiKey) {
        Connection mConnection = Jsoup.connect(Constants.TOKEN_URL)
                .header("Authorization", Constants.AUTH_PARAMS + apiKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .userAgent("219396487960.apps.googleusercontent.com")
                .timeout(0);
        try {
            mConnection.get();
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, Constants.TOKEN_URL + 
                    " failed to respond with code " + 
                    mConnection.response().statusCode(), e);
            return null;
        }
        return mConnection.response().body();
    }
    
    public static HashMap<String, Boolean> getItemsRead(ArrayList<String> ids,
            String apiKey) {
        Connection mConnection = null;
        Response res = null;
        mConnection = Jsoup.connect(Constants.STREAM_ITEMS_CONTENTS_URL)
                .method(Connection.Method.POST);
        for (String id : ids) {
            mConnection.data("i", Constants.ITEM_PREFIX + id);
        }
        String token = getToken(apiKey);
        if (token == null)
            return null;
        mConnection.data("T", token)
                .header("Authorization", Constants.AUTH_PARAMS + apiKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .userAgent("219396487960.apps.googleusercontent.com")
                .ignoreHttpErrors(true)
                .timeout(0);
        try {
            res = mConnection.execute();
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, "Getting items failed to respond with " +
                    "code " + mConnection.response().statusCode(), e);
        }
        String newToken = "";
        if (res != null) {
            String body = res.body();
            JSONObject jsonObject = null;
            JSONArray jsonSubs = null;
            try {
                jsonObject = new JSONObject(body);
            } catch (JSONException e) {
                Log.e(Constants.LOG_TAG, 
                        "Bad JSON array from string: " + body, e);
            }
            if (jsonObject != null) {
                try {
                    jsonSubs = jsonObject.getJSONArray("items");
                } catch (JSONException j) {
                    Log.e(Constants.LOG_TAG, 
                            "Bad JSON element: items", j);
                }
                JSONArray categories;
                boolean read = false;
                HashMap<String, Boolean> readMap = 
                    new HashMap<String, Boolean>();
                for (int i = 0; i < jsonSubs.length(); i++) {
                    read = false;
                    try {
                        categories = jsonSubs.getJSONObject(i).getJSONArray(
                                "categories");
                        for (int j = 0; j < categories.length(); j++) {
                            if (categories.optString(j).endsWith(
                                    "/state/com.google/read"))
                                read = true;
                        }
                        readMap.put(jsonSubs.getJSONObject(i).getString("id")
                                .replace(Constants.ITEM_PREFIX, ""), read);
                    } catch (JSONException e) {
                        Log.e(Constants.LOG_TAG, "Can't get json object", e);
                    }
                }
                return readMap;
            }
        }
        return null;
    }
    /*
     * Request all the items by URL in the list from Google Reader and update
     * the read values, taken from Google Reader
     */
    public static void getFeedItemIds(String apiKey, ArrayList<String> idsList, 
            String feedUrl, int epNum) {
        ArrayList<String> itemsList = new ArrayList<String>();
        Response res = null;
        Connection mConnection = null;
        boolean passed = false;
        /* Feed needs to have feed/ at the beginning */
        if (!feedUrl.startsWith("feed/"))
            feedUrl = "feed/" + feedUrl;
        String contentsUrl = "";
        /* Create the URL to request the items from Reader - double the number
         * of items to get just in case */
        try {
            contentsUrl = Constants.STREAM_CONTENTS_URL + 
                    URLEncoder.encode(feedUrl, "UTF-8") + "?n=" + epNum*2 + 
                    "&r=n&ot=0&nt=" + System.currentTimeMillis()/1000;
        } catch (UnsupportedEncodingException e) {
            Log.e(Constants.LOG_TAG, "Unsupported encoding", e);
        }
        mConnection = Jsoup.connect(contentsUrl)
                .method(Connection.Method.GET)
                .header("Authorization", Constants.AUTH_PARAMS + apiKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .userAgent("219396487960.apps.googleusercontent.com")
                .ignoreHttpErrors(true)
                .timeout(5000);
        try {
            res = mConnection.execute();
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, Constants.SUBSCRIPTION_EDIT_URL + 
                    "feed/" + feedUrl + " failed to respond with code " + 
                    mConnection.response().statusCode(), e);
        }
        if (res != null) {
            /* 200 is success HTTP code */
            if (res.statusCode() == 200)
                passed = true;
            String body = res.body();
            /* Parse the episode items from the body of the response */
            HashMap<String, EpisodeId> episodeHash = getItems(body);
            ContentValues cv = new ContentValues();
            if (!episodeHash.isEmpty()) {
                /* Update the values (read and Google ID) for each */
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    new UpdateEpisodesRead(idsList, episodeHash, feedUrl)
                            .execute();
                else
                    new UpdateEpisodesRead(idsList, episodeHash, feedUrl)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }
    /* 
     * 
     */
    static class UpdateEpisodesRead extends AsyncTask<Void, Void, Void> {
        
        ArrayList<String> idsList;
        HashMap<String, EpisodeId> episodesHash;
        String feedUrl;
        
        UpdateEpisodesRead(ArrayList<String> idsList, 
                HashMap<String, EpisodeId> episodesHash,
                String feedUrl) {
            this.idsList = idsList;
            this.episodesHash = episodesHash;
            this.feedUrl = feedUrl;
        }
        
        @Override
        protected Void doInBackground(Void... nothing) {
            ContentValues cv = new ContentValues();
            String googleId;
            EpisodeId episodeId;
            /* Step through the episode URLs */
            for (String epUrl : idsList) {
                /* Nothing to do if the episodes hash doesn't have the current
                 * URL */
                if (!episodesHash.containsKey(epUrl))
                    continue;
                /* Get the Google ID and read value */
                cv.clear();
                episodeId = episodesHash.remove(epUrl);
                googleId = episodeId.getId();
                cv.put(DatabaseHelper.COL_EPISODE_GOOGLE_ID, googleId);
                /* If sync of this episode has previously failed, we want
                 * to set Google Reader to the value we have */
                if (ApplicationEx.dbHelper.getFailedSync(epUrl)) {
                    String[] account = 
                        ApplicationEx.dbHelper.getAccount();
                    if (account != null) {
                        /* Grab the current read value and set Reader to
                         * that value */
                        boolean success = markItemRead(account[1], 
                                feedUrl, googleId, 
                                ApplicationEx.dbHelper.getEpisodeRead(
                                        epUrl) == Constants.READ ? 
                                                true : false);
                        /* Set failed sync based on the result */
                        if (!success)
                            ApplicationEx.dbHelper.setFailedSync(
                                    ApplicationEx.dbHelper
                                        .getEpisodeIdFromGId(googleId));
                        else
                            ApplicationEx.dbHelper.resetFailedSync(
                                    ApplicationEx.dbHelper
                                        .getEpisodeIdFromGId(googleId));
                    }
                }
                /* Otherwise, we just update the database with the value
                 * from Google Reader */
                else {
                    cv.put(DatabaseHelper.COL_EPISODE_READ, 
                            episodeId.getRead() ? Constants.READ : 
                                Constants.UNREAD);
                }
                /* Add the Google ID and read value to database */
                ApplicationEx.dbHelper.updateRecord(cv, 
                        DatabaseHelper.EPISODE_TABLE, 
                        DatabaseHelper.COL_EPISODE_URL + "='" + epUrl + 
                        "'");
            }
            /* Check the feed episodes to update the feed read value */
            if (!ApplicationEx.dbHelper.hasReadEpisodes(
                    ApplicationEx.dbHelper.getFeedId(feedUrl), false)) {
                cv.clear();
                cv.put(DatabaseHelper.COL_FEED_UNREAD, Constants.READ);
                ApplicationEx.dbHelper.updateRecord(cv, 
                        DatabaseHelper.FEED_TABLE, 
                        DatabaseHelper.COL_FEED_ADDRESS + "='" + feedUrl
                        + "'");
            }
            else {
                cv.clear();
                cv.put(DatabaseHelper.COL_FEED_UNREAD, Constants.UNREAD);
                ApplicationEx.dbHelper.updateRecord(cv, 
                        DatabaseHelper.FEED_TABLE, 
                        DatabaseHelper.COL_FEED_ADDRESS + "='" + feedUrl
                        + "'");
            }
            return null;
        }
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
    
    private static class EpisodeId {
        private String id;
        private boolean read;
        
        public EpisodeId(String id, boolean read) {
            this.id = id;
            this.read = read;
        }
        
        public String getId() {
            return id;
        }
        
        public boolean getRead() {
            return read;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public void setRead(boolean read) {
            this.read = read;
        }
    }
    
    /* 
     * Take the JSON string and get the read and id values for each
     */
    protected static HashMap<String, EpisodeId> getItems(
            String jsonString) {
        JSONObject jsonObject = null;
        JSONArray jsonSubs = null;
        try {
            jsonObject = new JSONObject(jsonString);
        } catch (JSONException e) {
            Log.e(Constants.LOG_TAG, 
                    "Bad JSON array from string: " + jsonString, e);
        }
        try {
            jsonSubs = jsonObject.getJSONArray("items");
        } catch (JSONException j) {
            Log.e(Constants.LOG_TAG, 
                    "Bad JSON element: items", j);
        }
        HashMap<String, EpisodeId> episodeHash =
                new HashMap<String, EpisodeId>();
        JSONArray categories;
        boolean read = false;
        /* Parse each of the items */
        for (int i = 0; i < jsonSubs.length(); i++) {
            try {
                /* Look for the category indicating it is read */
                categories = jsonSubs.getJSONObject(i).getJSONArray(
                        "categories");
                for (int j = 0; j < categories.length(); j++) {
                    if (categories.optString(j).endsWith(
                            "/state/com.google/read"))
                        read = true;
                }
                /* Map the Google ID with the read value */
                episodeHash.put(jsonSubs.getJSONObject(i)
                        .getJSONArray("enclosure")
                            .getJSONObject(0).getString("href"),
                        new EpisodeId(jsonSubs.getJSONObject(i).getString("id")
                                .replace(Constants.ITEM_PREFIX, ""),
                            read));
            } catch (JSONException e) {
                Log.e(Constants.LOG_TAG, 
                        "Can't get json object", e);
            }
        }
        return episodeHash;
    }
    
    /**
     * Read the entire string from a file.
     * @param filename  file in local storage to read
     * @return  the text read from the file
     */
    protected static String readStringFromFile(String filename) {
        File[] files = ApplicationEx.getApp().getFilesDir().listFiles();
        File myFile = null;
        String text = "";
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
    
    public static long readLongFromFile(String filename) {
        String value = readStringFromFile(filename);
        Long currValue = null;
        try {
            currValue = Long.parseLong(value);
        } catch (NumberFormatException e) {
            Log.e(Constants.LOG_TAG, "Bad long value: " + value);
            return System.currentTimeMillis();
        }
        return currValue;
    }
    
    protected static String readStringFromExternalFile(String filename) {
        File myFile = new File(filename);
        if (myFile.length() <= 0)
            return null;
        byte[] buffer = new byte[(int)myFile.length()];
        BufferedInputStream bufStream = null;
        String text;
        try {
            bufStream = new BufferedInputStream(new FileInputStream(myFile), 
                    buffer.length);
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
        return text;
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
    
    public static boolean markItemRead(String apiKey, String feedUrl, 
            String itemId, boolean read) {
        Response res = null;
        Connection mConnection = null;
        boolean passed = false;
        if (!feedUrl.startsWith("feed/"))
            feedUrl = "feed/" + feedUrl;
        mConnection = Jsoup.connect(Constants.EDIT_TAG_URL)
                .method(Connection.Method.POST)
                .data(read ? "a" : "r", Constants.READ_STATE_STREAMID);
        if (!read)
            mConnection.data("a", "user/-/state/com.google/kept-unread");
        else
            mConnection.data("r", "user/-/state/com.google/kept-unread");
        String token = getToken(apiKey);
        if (token == null) {
            return false;
        }
        mConnection.data("async", "true")
                .data("s", feedUrl)
                .data("i", Constants.ITEM_PREFIX + itemId)
                .data("T", token)
                .header("Authorization", Constants.AUTH_PARAMS + apiKey)
                .header("Content-Type", 
                        "application/x-www-form-urlencoded;charset=UTF-8")
                .userAgent("219396487960.apps.googleusercontent.com")
                .ignoreHttpErrors(true);
        try {
            res = mConnection.execute();
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, Constants.SUBSCRIPTION_EDIT_URL + 
                    "feed/" + feedUrl + " failed to respond with code " + 
                    mConnection.response().statusCode(), e);
        }
        if (res != null) {
            if (res.statusCode() == 200) {
                ArrayList<String> epsList = new ArrayList<String>();
                epsList.add(itemId);
                HashMap<String, Boolean> idMap = 
                        Util.getItemsRead(epsList, apiKey);
                if (idMap != null) {
                    for (Entry<String, Boolean> entry : idMap.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(itemId) && 
                                entry.getValue() == read) {
                            passed = true;
                            break;
                        }
                    }
                }
            }
        }
        return passed;
    }
    
    public static class MarkReadTask extends AsyncTask<Void, Void, Void> {
        
        private String apiKey;
        private String feedAddress;
        private String googleId;
        private boolean read;
        
        public MarkReadTask(String apiKey, String feedAddress, String googleId,
                boolean read) {
            this.apiKey = apiKey;
            this.feedAddress = feedAddress;
            this.googleId = googleId;
            this.read = read;
        }

        @Override
        protected Void doInBackground(Void... params) {
            boolean success = markItemRead(apiKey, feedAddress, googleId, read);
            if (!success)
                ApplicationEx.dbHelper.setFailedSync(
                        ApplicationEx.dbHelper.getEpisodeIdFromGId(googleId));
            else
                ApplicationEx.dbHelper.resetFailedSync(
                        ApplicationEx.dbHelper.getEpisodeIdFromGId(googleId));
            return null;
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
        File feedDir = new File(ApplicationEx.cacheLocation +
                Constants.FEEDS_LOCATION + id + File.separator);
        File currFile;
        String ext;
        for (Integer epId : epIdList) {
            ext = ApplicationEx.dbHelper.extFromEpisodeId(epId);
            currFile = new File(feedDir.getAbsolutePath() + epId + ext);
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
                        ApplicationEx.dbHelper.getReadEpisodes(id, true, 
                                archive).size()+ " new",
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
            return "";
        SimpleDateFormat df = new SimpleDateFormat("'Today' kk:mm");
        long offset = TimeZone.getDefault().getOffset(pub);
        long currTime = System.currentTimeMillis() + offset;
        if (pub+offset < (currTime-(Constants.DAY_MILLI*365)))
            df = new SimpleDateFormat("dd MMM yyyy");
        else if (pub+offset < (currTime-(Constants.DAY_MILLI*6)-
                (currTime%Constants.DAY_MILLI)) || pub+offset >=
                (currTime-(currTime%Constants.DAY_MILLI)+Constants.DAY_MILLI))
            df = new SimpleDateFormat("dd MMM");
        else if (pub+offset < currTime-(currTime%Constants.DAY_MILLI))
            df = new SimpleDateFormat("EEEE");
        df.setTimeZone(TimeZone.getDefault());
        return (String) DateFormat.format(df.toLocalizedPattern(), pub);
    }
    
    public static ArrayList<Episode> getAllEpisodes(int read, int downloaded, 
            int sort, int sortType, int archive) {
        Cursor cur = ApplicationEx.dbHelper.getAllEpisodes(read, downloaded,
                sort, sortType, archive);
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
                                DatabaseHelper.COL_EPISODE_DURATION))));
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
                                DatabaseHelper.COL_EPISODE_DURATION))));
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
            episodes.add(new Episode(epId, episode[1], episode[2], -1, false, 
                    false, null, 0));
        }
        return episodes;
    }
    
    public static boolean checkConnectionNoTask() {
        Connection connection;
        Response response;
        try {
            connection = Jsoup.connect("http://www.android.com")
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
    
    public static void persistCurrentPlaylist(ArrayList<Integer> epIdList) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            new ListWriteTask(epIdList, Constants.PLAYLIST_FILENAME)
                    .execute();
        else
            new ListWriteTask(epIdList, Constants.PLAYLIST_FILENAME)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    public static void persistUpdatePlaylist(ArrayList<String> feedList) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            new ListWriteTask(feedList, Constants.UPDATE_LIST_FILENAME)
                    .execute();
        else
            new ListWriteTask(feedList, Constants.UPDATE_LIST_FILENAME)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
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
    
    public static boolean containsPreference(int key) {
        return PreferenceManager.getDefaultSharedPreferences(
                ApplicationEx.getApp())
            .contains(ApplicationEx.getApp().getString(key));
    }
    
    public static ArrayList<Integer> getEpisodeIdList(boolean downloadOnly) {
        int read = ApplicationEx.dbHelper.getCurrentEpisodeRead();
        int downloaded = downloadOnly ? Constants.DOWNLOADED :
                ApplicationEx.dbHelper.getCurrentEpisodeDownloaded();
        int sort = Constants.PLAYLIST_SORT_EPISODE|Constants.PLAYLIST_SORT_DATE;
        int sortType = ApplicationEx.dbHelper.getCurrentEpisodeSortType();
        int archive = Util.readBooleanPreference(R.string.archive_key, false) 
                ? 1 : 0;
        ArrayList<Episode> episodes = Util.getAllEpisodes(read, downloaded, 
                sort, sortType, archive);
        ArrayList<Integer> epIdList = new ArrayList<Integer>();
        for (int i = 0; i < episodes.size(); i++) {
            // TODO Add ability to stream episodes here
            epIdList.add(episodes.get(i).getId());
        }
        return epIdList;
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
        String timeString = Integer.toString(hours) + ":";
        timeString = timeString + (mins < 10 ? "0" : "") +
                Integer.toString(mins) + ":";
        timeString = timeString + (secs < 10 ? "0" : "") + 
                Integer.toString(secs);
        return timeString;
    }
    
    public static class SavePositionTask extends AsyncTask<Void, Void, Void> {
        
        private int position;
        private int id;
        
        public SavePositionTask(int position, int id) {
            this.position = position;
            this.id = id;
        }
        
        @Override
        protected Void doInBackground(Void... nothing) {
            ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.COL_EPISODE_POSITION, position);
            ApplicationEx.dbHelper.updateRecord(cv, 
                    DatabaseHelper.EPISODE_TABLE, DatabaseHelper.COL_EPISODE_ID 
                    + "=" + id);
            ApplicationEx.dbHelper.setCurrentEpisodeProgress(position);
            return null;
        }
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
                    DatabaseHelper.COL_API_NAME + "='" + 
                    cv.getAsString(DatabaseHelper.COL_API_NAME) + "'");
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
    
}
