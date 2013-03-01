package com.jeffthefate.pod_dev;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;

import com.jeffthefate.pod_dev.Util.Magics;
import com.jeffthefate.pod_dev.Util.MarkReadTask;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

/**
 * Executes all the database actions, including many helper functions and
 * constants.
 * 
 * @author Jeff Fate
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    
    public static SQLiteDatabase db;
    
    private static final String DB_NAME = "podcatcherDb";
    
    public static final String API_TABLE = "Api";
    public static final String FEED_TABLE = "Feeds";
    public static final String EPISODE_TABLE = "Episodes";
    public static final String APP_TABLE = "App";
    public static final String COL_API_NAME = "ApiName";
    public static final String COL_API_KEY = "ApiKey";
    public static final String COL_API_ACCOUNT_TYPE = "ApiAccountType";
    public static final String COL_API_UPDATED = "ApiUpdated";
    public static final String COL_FEED_ID = "_id";
    public static final String COL_FEED_ADDRESS = "FeedAddress";
    public static final String COL_FEED_TITLE = "FeedTitle";
    public static final String COL_FEED_LINK = "FeedLink";
    public static final String COL_FEED_DESCRIPTION = "FeedDescription";
    public static final String COL_FEED_IMAGE = "FeedImage";
    public static final String COL_FEED_LAST_EP_TIME = "LastEpTime";
    public static final String COL_FEED_LAST_UPDATE = "LastUpdate";
    public static final String COL_FEED_PODCAST = "FeedPodcast";
    public static final String COL_FEED_LAST_IMAGE_UPDATE = "FeedImageUpdate";
    public static final String COL_FEED_MAGIC_INTERVAL = "FeedMagicUpdate";
    public static final String COL_FEED_FAILED_TRIES = "FeedFailedTries";
    public static final String COL_FEED_SORT = "FeedSort";
    public static final String COL_FEED_UNREAD = "FeedUnread";
    public static final String COL_FEED_DOWNLOADED = "FeedDownloaded";
    public static final String COL_FEED_SPEED = "FeedSpeed";
    public static final String COL_EPISODE_ID = "_id";
    public static final String COL_EP_FEED_ID = "FeedId";
    public static final String COL_EP_FEED_TITLE = "FeedTitle";
    public static final String COL_EP_FEED_LAST_EP_TIME = "LastEpTime";
    public static final String COL_EPISODE_URL = "EpisodeUrl";
    public static final String COL_EPISODE_PUB = "EpisodePub";
    public static final String COL_EPISODE_SUMMARY = "EpisodeSummary";
    public static final String COL_EPISODE_TYPE = "EpisodeType";
    public static final String COL_EPISODE_LOCATION = "EpisodeLocation";
    public static final String COL_EPISODE_TITLE = "EpisodeTitle";
    public static final String COL_EPISODE_LENGTH = "EpisodeLength";
    public static final String COL_EPISODE_UPDATED = "EpisodeUpdated";
    public static final String COL_EPISODE_READ = "EpisodeRead";
    public static final String COL_EPISODE_DOWNLOADED = "EpisodeDownloaded";
    public static final String COL_EPISODE_DOWNLOADED_BYTES = 
        "EpisodeDownloadedBytes";
    public static final String COL_EPISODE_BYTES_TOTAL = "EpisodeBytesTotal";
    public static final String COL_EPISODE_REASON = "EpisodeReason";
    public static final String COL_EPISODE_FAILED_TRIES = "EpisodeFailedTries";
    public static final String COL_EPISODE_POSITION = "EpisodePosition";
    public static final String COL_EPISODE_DURATION = "EpisodeDuration";
    public static final String COL_EPISODE_ARCHIVE = "EpisodeArchive";
    public static final String COL_EPISODE_GOOGLE_ID = "EpisodeGoogleId";
    public static final String COL_EPISODE_RETRY_SYNC = "EpisodeRetrySync";
    public static final String COL_APP_FEED_POSITION = "AppFeedPosition";
    public static final String COL_APP_EPISODE_POSITION = "AppEpisodePosition";
    public static final String COL_APP_DOWNLOAD_POSITION =
        "AppDownloadPosition";
    public static final String COL_APP_LAST_TAB = "AppLastTab";
    public static final String COL_APP_CURRENT_EPISODE = "AppCurrentEpisode";
    public static final String COL_APP_PREVIOUS_EPISODE = "AppPreviousEpisode";
    public static final String COL_APP_NEXT_EPISODE = "AppNextEpisode";
    public static final String COL_APP_CURRENT_DOWNLOAD = "AppCurrentDownload";
    public static final String COL_APP_CURRENT_PLAYLIST_TYPE =
        "AppCurrentPlaylistType";
    public static final String COL_APP_EPISODE_PROGRESS = "AppEpisodeProgress";
    public static final String COL_APP_EPISODE_DURATION = "AppEpisodeDuration";
    public static final String COL_APP_FEED_READ = "AppFeedRead";
    public static final String COL_APP_FEED_DOWNLOADED = "AppFeedDownloaded";
    public static final String COL_APP_EPISODE_READ = "AppEpisodeRead";
    public static final String COL_APP_EPISODE_DOWNLOADED =
        "AppEpisodeDownloaded";
    public static final String COL_APP_EPISODE_SORTTYPE = "AppEpisodeSorttype";
    public static final String COL_APP_FEED_SORTTYPE = "AppFeedSorttype";
    public static final String COL_APP_FEED_SORT = "AppFeedSort";
    /**
     * Create API table string
     */
    private static final String CREATE_API_TABLE = "CREATE TABLE " + API_TABLE
            + " (" + COL_API_NAME + " TEXT PRIMARY KEY, " + COL_API_KEY +
            " TEXT, " + COL_API_ACCOUNT_TYPE + " TEXT, " + COL_API_UPDATED +
            " INTEGER DEFAULT -1)";
    /**
     * Create feed table string
     */
    private static final String CREATE_FEED_TABLE = "CREATE TABLE "
            + FEED_TABLE + " (" + COL_FEED_ID + " INTEGER PRIMARY KEY, "
            + COL_FEED_ADDRESS + " TEXT, " + COL_FEED_TITLE + " TEXT, " 
            + COL_FEED_LINK + " TEXT, " + COL_FEED_DESCRIPTION + " TEXT, " 
            + COL_FEED_IMAGE + " TEXT, " + COL_FEED_LAST_EP_TIME 
            + " INTEGER DEFAULT -1, " + COL_FEED_LAST_UPDATE 
            + " INTEGER DEFAULT -1, " + COL_FEED_PODCAST 
            + " INTEGER DEFAULT -1, " + COL_FEED_LAST_IMAGE_UPDATE 
            + " INTEGER DEFAULT -1, " + COL_FEED_MAGIC_INTERVAL 
            + " INTEGER DEFAULT -1, " + COL_FEED_FAILED_TRIES + 
            " INTEGER DEFAULT 0, " + COL_FEED_SORT + " INTEGER DEFAULT -1, " +
            COL_FEED_UNREAD + " INTEGER DEFAULT 2, " + COL_FEED_DOWNLOADED + 
            " INTEGER DEFAULT 2, " + COL_FEED_SPEED + " REAL DEFAULT 1.0)";
    /**
     * Create episode table string
     */
    private static final String CREATE_EPISODE_TABLE = "CREATE TABLE "
            + EPISODE_TABLE + " (" + COL_EPISODE_ID + " INTEGER PRIMARY KEY, "
            + COL_EP_FEED_ID + " INTEGER DEFAULT -1, " + COL_EP_FEED_TITLE + 
            " TEXT, " + COL_EP_FEED_LAST_EP_TIME + " INTEGER DEFAULT -1, " + 
            COL_EPISODE_URL + " TEXT, " + COL_EPISODE_PUB + 
            " INTEGER DEFAULT -1, " + COL_EPISODE_LENGTH + 
            " INTEGER DEFAULT -1, "+ COL_EPISODE_SUMMARY + " TEXT, " + 
            COL_EPISODE_TYPE + " TEXT, " + COL_EPISODE_LOCATION + " TEXT, " + 
            COL_EPISODE_TITLE + " TEXT, " + COL_EPISODE_UPDATED + 
            " INTEGER DEFAULT -1, " + COL_EPISODE_READ + " INTEGER DEFAULT 1, "
            + COL_EPISODE_DOWNLOADED + " INTEGER DEFAULT 1, "
            + COL_EPISODE_DOWNLOADED_BYTES + " INTEGER DEFAULT -1, "
            + COL_EPISODE_BYTES_TOTAL + " INTEGER DEFAULT -1, " + 
            COL_EPISODE_REASON + " INTEGER DEFAULT -1, " +
            COL_EPISODE_FAILED_TRIES + " INTEGER DEFAULT 0, " +
            COL_EPISODE_POSITION + " INTEGER DEFAULT 0, " +
            COL_EPISODE_DURATION + " INTEGER DEFAULT 0, " +
            COL_EPISODE_ARCHIVE + " INTEGER DEFAULT 0, " +
            COL_EPISODE_GOOGLE_ID + " TEXT DEFAULT " + Constants.NO_GOOGLE +
            ", " + COL_EPISODE_RETRY_SYNC + " INTEGER DEFAULT 0)";
    /**
     * Create app table string
     */
    private static final String CREATE_APP_TABLE = "CREATE TABLE "
            + APP_TABLE + " (" + COL_APP_FEED_POSITION + " INTEGER DEFAULT 0, "
            + COL_APP_EPISODE_POSITION + " INTEGER DEFAULT 0, " +
            COL_APP_DOWNLOAD_POSITION + " INTEGER DEFAULT 0, " +
            COL_APP_LAST_TAB + " INTEGER DEFAULT 0, " +
            COL_APP_CURRENT_EPISODE + " INTEGER DEFAULT 0, " +
            COL_APP_PREVIOUS_EPISODE + " INTEGER DEFAULT 0, " +
            COL_APP_NEXT_EPISODE + " INTEGER DEFAULT 0, " +
            COL_APP_CURRENT_DOWNLOAD + " INTEGER DEFAULT 0, " +
            COL_APP_CURRENT_PLAYLIST_TYPE + " INTEGER DEFAULT 0, " +
            COL_APP_EPISODE_PROGRESS + " INTEGER DEFAULT 0, " +
            COL_APP_EPISODE_DURATION + " INTEGER DEFAULT 0, " +
            COL_APP_FEED_READ + " INTEGER DEFAULT " +
            Constants.UNREAD + ", " +
            COL_APP_FEED_DOWNLOADED + " INTEGER DEFAULT " +
            Constants.DOWNLOADED + ", " +
            COL_APP_EPISODE_READ + " INTEGER DEFAULT " +
            Constants.UNREAD + ", " +
            COL_APP_EPISODE_DOWNLOADED + " INTEGER DEFAULT " +
            Constants.DOWNLOADED + ", " +
            COL_APP_EPISODE_SORTTYPE + " INTEGER DEFAULT 0, " +
            COL_APP_FEED_SORTTYPE + " INTEGER DEFAULT 0, " +
            COL_APP_FEED_SORT + " INTEGER DEFAULT 0)";
    /**
     * Create the helper object that creates and manages the database.
     * 
     * @param context
     *            the context used to create this object
     */
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, 1);
        db = getWritableDatabase();
    }
    
    private static DatabaseHelper instance;
    
    public static DatabaseHelper getInstance() {
        if (instance == null)
            instance = new DatabaseHelper(ApplicationEx.getApp());
        return instance;
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_API_TABLE);
        db.execSQL(CREATE_FEED_TABLE);
        db.execSQL(CREATE_EPISODE_TABLE);
        db.execSQL(CREATE_APP_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + API_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + FEED_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + EPISODE_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + APP_TABLE);
        onCreate(db);
    }

    /**
     * Look for an item in a specific table.
     * 
     * @param name
     *            identifier for the item to lookup
     * @param table
     *            the table to look in
     * @param column
     *            the column to look under
     * @return if the item is found
     */
    public synchronized boolean inDb(String[] values, String table, 
            String[] columns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            if (i != 0) {
                sb.append(" AND " );
            }
            sb.append(columns[i]);
            sb.append("=?");
        }
        Cursor cur = db.query(
                table, columns, sb.toString(), values, null, null, null);
        boolean inDb = false;
        if (cur.moveToFirst())
            inDb = true;
        cur.close();
        return inDb;
    }
    
    public synchronized boolean feedExists(String feedAddress) {
        return inDb(new String[] {feedAddress}, FEED_TABLE, 
                new String[] {COL_FEED_ADDRESS});
    }

    public synchronized Cursor getAllFeeds(int read, int downloaded, int sort, 
            int sortType) {
        StringBuilder sb = new StringBuilder(COL_FEED_PODCAST);
        sb.append("=?");
        if (read != (Constants.READ|Constants.UNREAD)) {
            sb.append(" AND ");
            sb.append(COL_FEED_UNREAD);
            sb.append("=?");
        }
        if (downloaded != (Constants.DOWNLOADED|Constants.TO_DOWNLOAD)) {
            sb.append(" AND ");
            sb.append(COL_FEED_DOWNLOADED);
            sb.append("=?");
        }
        return db.query(FEED_TABLE, new String[] {COL_FEED_ID, 
                COL_FEED_TITLE, COL_FEED_UNREAD, 
                COL_FEED_DOWNLOADED, COL_FEED_LAST_EP_TIME, COL_FEED_ADDRESS}, 
                sb.toString(), getFeedFilter(read, downloaded), null, null, 
                getFeedSort(sort, sortType));
    }
    
    public synchronized void setFeedSpeed(double speed, int epId) {
        int feedId = getFeedId(epId);
        ContentValues cv = new ContentValues();
        cv.put(COL_FEED_SPEED, speed);
        updateRecord(cv, FEED_TABLE, COL_FEED_ID + "=" + feedId);
    }
    
    public synchronized float getFeedSpeed(int epId) {
        float speed = 0.0f;
        int feedId = getFeedId(epId);
        Cursor cur = db.query(FEED_TABLE, new String[] {COL_FEED_SPEED}, 
                COL_FEED_ID + "=?", new String[] {Integer.toString(feedId)}, 
                null, null, null);
        if (cur.moveToFirst()) {
            speed = cur.getFloat(cur.getColumnIndex(COL_FEED_SPEED));
        }
        cur.close();
        return speed;
    }
    
    public synchronized int getNumEpisodes(String feedUrl) {
        Cursor cur = getEpisodes(getFeedId(feedUrl));
        int size = cur.getCount();
        cur.close();
        return size;
    }
    
    public synchronized void setEpisodeBytes(String epUrl, long bytes) {
        ContentValues cv = new ContentValues();
        cv.put(COL_EPISODE_DOWNLOADED_BYTES, bytes);
        updateRecord(cv, EPISODE_TABLE, COL_EPISODE_URL + "='" + epUrl + "'");
    }
    
    private String[] getFeedFilter(int read, int downloaded) {
        ArrayList<String> list = new ArrayList<String>();
        list.add(Integer.toString(1));
        if (read != (Constants.READ|Constants.UNREAD))
            list.add(Integer.toString(read));
        if (downloaded != (Constants.DOWNLOADED|Constants.TO_DOWNLOAD))
            list.add(Integer.toString(downloaded));
        return list.toArray(new String[list.size()]);
    }
    
    private String[] getFilter(int read, int downloaded, int id, int archive) {
        ArrayList<String> list = new ArrayList<String>();
        if (archive == 0)
            list.add(Integer.toString(0));
        if (read != (Constants.READ|Constants.UNREAD))
            list.add(Integer.toString(read));
        if (downloaded != (Constants.DOWNLOADED|Constants.TO_DOWNLOAD))
            list.add(Integer.toString(downloaded));
        if (id != -1)
            list.add(Integer.toString(id));
        return list.toArray(new String[list.size()]);
    }
    
    private String getFeedSort(int sort, int sortType) {
        Log.i(Constants.LOG_TAG, "sort: " + sort);
        Log.i(Constants.LOG_TAG, "sortType: " + sortType);
        String[] sortTypeStrings = new String[] {" ASC", " DESC"};
        String sortString = "";
        switch(sort) {
        case 5:
            sortString = COL_FEED_TITLE;
            break;
        case 6:
            sortString = COL_FEED_LAST_EP_TIME;
            break;
        }
        switch(sortType) {
        case 1:
            sortString = sortString + sortTypeStrings[0];
            break;
        case 2:
            sortString = sortString + sortTypeStrings[1];
            break;
        default:
            sortString = sortString + sortTypeStrings[1];
            break;
        }
        return sortString;
    }
    
    private String getEpisodeSort(int sort, int sortType) {
        String[] sortTypeStrings = new String[] {" ASC", " DESC"};
        String sortTypeStringOne = "";
        String sortTypeStringTwo = "";
        String sortStringOne = "";
        String sortStringTwo = "";
        String sortStringThree = "";
        String sortString = "";
        switch(sort) {
        case 5:
            sortStringOne = COL_EP_FEED_TITLE; 
            sortStringTwo = COL_EPISODE_PUB;
            sortStringThree = COL_EPISODE_URL;
            break;
        case 6:
            sortStringOne = COL_EPISODE_PUB;
            sortStringThree = COL_EPISODE_URL;
            break;
        }
        switch(sortType) {
        case 1:
            sortString = sortStringOne + sortTypeStrings[0];
            if (!sortStringTwo.equals(""))
                sortString = sortStringOne + sortTypeStrings[0] + ", " + 
                    sortStringTwo + sortTypeStrings[0] + ", " + sortStringThree 
                    + sortTypeStrings[0];
            else
                sortString = sortString + ", " + sortStringThree + 
                    sortTypeStrings[0];
            break;
        case 2:
            sortString = sortStringOne + sortTypeStrings[1];
            if (!sortStringTwo.equals(""))
                sortString = sortStringOne + sortTypeStrings[0] + ", " + 
                    sortStringTwo + sortTypeStrings[1] + ", " + sortStringThree 
                    + sortTypeStrings[1];
            else
                sortString = sortString + ", " + sortStringThree + 
                    sortTypeStrings[1];
            break;
        case 3:
            if (!sortStringTwo.equals(""))
                sortString = sortStringOne + sortTypeStrings[1] + ", " + 
                    sortStringTwo + sortTypeStrings[0] + ", " + sortStringThree 
                    + sortTypeStrings[0];
            else
                sortString = sortStringOne + sortTypeStrings[0] + ", " + 
                    sortStringThree + sortTypeStrings[0];
            break;
        case 4:
            if (!sortStringTwo.equals(""))
                sortString = sortStringOne + sortTypeStrings[1] + ", " + 
                    sortStringTwo + sortTypeStrings[1] + ", " + sortStringThree 
                    + sortTypeStrings[1];
            else
                sortString = sortStringOne + sortTypeStrings[1] + ", " + 
                    sortStringThree + sortTypeStrings[1];
            break;
        default:
            sortStringOne = sortStringOne + sortTypeStrings[0];
            if (!sortStringTwo.equals(""))
                sortStringTwo = sortStringTwo + sortTypeStrings[0];
            sortString = sortStringOne + ", " + sortStringTwo  + 
                sortTypeStrings[0] + ", " + sortStringThree + 
                sortTypeStrings[0];
            break;
        }
        return sortString;
    }
    
    public String getEpisodeUrl(int id) {
        return getString(id, COL_EPISODE_URL, EPISODE_TABLE, COL_EPISODE_ID);
    }
    // Get data by day, per feed?
    // Sort by feed
    // Sort by date
    // Limit to each day - midnight to midnight
    // Join for all days?
    public synchronized Cursor getAllEpisodes(int read, int downloaded,
            int sort, int sortType, int archive) {
        String filterString = "";
        if (archive == 0)
            filterString = filterString + DatabaseHelper.COL_EPISODE_ARCHIVE + 
                    "=?";
        if (read != (Constants.READ|Constants.UNREAD)) {
            if (!filterString.equals(""))
                filterString = filterString + " AND ";
            filterString = filterString + DatabaseHelper.COL_EPISODE_READ + 
                    "=?";
        }
        if (downloaded != (Constants.DOWNLOADED|Constants.TO_DOWNLOAD)) {
            if (!filterString.equals(""))
                filterString = filterString + " AND ";
            filterString = filterString + DatabaseHelper.COL_EPISODE_DOWNLOADED
                    + "=?";
        }
        return db.query(EPISODE_TABLE, new String[] {COL_EPISODE_ID, 
                COL_EP_FEED_ID, COL_EP_FEED_TITLE, COL_EP_FEED_LAST_EP_TIME, 
                COL_EPISODE_TITLE, COL_EPISODE_PUB, COL_EPISODE_URL,
                COL_EPISODE_READ, COL_EPISODE_DOWNLOADED, COL_EPISODE_DURATION},
                filterString, getFilter(read, downloaded, -1, archive), null, 
                null, getEpisodeSort(sort, sortType));
    }
    
    public synchronized Cursor getFilteredFeedEpisodes(int feedId, int read,
            int downloaded, int sort, int sortType, int archive) {
        String filterString = "";
        if (archive == 0)
            filterString = filterString + DatabaseHelper.COL_EPISODE_ARCHIVE +
                    "=?";
        if (read != (Constants.READ|Constants.UNREAD)) {
            if (!filterString.equals(""))
                filterString = filterString + " AND ";
            filterString = filterString + DatabaseHelper.COL_EPISODE_READ + 
                    "=?";
        }
        if (downloaded != (Constants.DOWNLOADED|Constants.TO_DOWNLOAD)) {
            if (!filterString.equals(""))
                filterString = filterString + " AND ";
            filterString = filterString + DatabaseHelper.COL_EPISODE_DOWNLOADED 
                    + "=?";
        }
        if (!filterString.equals(""))
            filterString = filterString + " AND ";
        filterString = filterString + DatabaseHelper.COL_EP_FEED_ID + "=?";
        return db.query(EPISODE_TABLE, new String[] {COL_EPISODE_ID, 
                COL_EP_FEED_TITLE, COL_EPISODE_TITLE, COL_EPISODE_PUB, 
                COL_EPISODE_URL, COL_EPISODE_READ, COL_EPISODE_DOWNLOADED,
                COL_EPISODE_DURATION}, filterString, 
                getFilter(read, downloaded, feedId, archive), null, null, 
                getEpisodeSort(sort, sortType));
    }

    public synchronized Cursor getEpisodes(int feedId) {
        return db.query(EPISODE_TABLE, new String[] {COL_EPISODE_ID, 
                COL_EP_FEED_TITLE, COL_EP_FEED_LAST_EP_TIME, COL_EPISODE_TITLE, 
                COL_EPISODE_PUB, COL_EPISODE_URL, COL_EPISODE_READ, 
                COL_EPISODE_DOWNLOADED}, COL_EP_FEED_ID + "=?", 
                new String[] {Integer.toString(feedId)}, null, null, 
                COL_EPISODE_PUB + " DESC");
    }
    
    public ArrayList<Integer> getReadEpisodes(int feedId, 
            boolean read, int archive) {
        String filterString = COL_EP_FEED_ID + "=? AND " + COL_EPISODE_READ + 
                "=?";
        String[] argArray = new String[] {Integer.toString(feedId), 
                Integer.toString(read ? Constants.UNREAD : Constants.READ)};
        if (archive == 0) {
            filterString = filterString + " AND " + 
                    DatabaseHelper.COL_EPISODE_ARCHIVE + "=?";
            argArray = new String[] {Integer.toString(feedId), 
                    Integer.toString(read ? Constants.UNREAD : Constants.READ),
                    "0"};
        }
        Cursor cur = db.query(EPISODE_TABLE, new String[] {COL_EPISODE_ID}, 
                filterString, argArray, null, null, null);
        ArrayList<Integer> epList = new ArrayList<Integer>();
        if (cur.moveToFirst()) {
            do {
                epList.add(cur.getInt(cur.getColumnIndex(COL_EPISODE_ID)));
            } while (cur.moveToNext());
        }
        cur.close();
        return epList;
    }
    /*
     * Get the episode URLs, starting with the latest (most recent)
     */
    public ArrayList<String> getEpisodesList(int feedId) {
        Cursor cur = db.query(EPISODE_TABLE, new String[] {COL_EPISODE_URL}, 
                COL_EP_FEED_ID + "=?", 
                new String[] {Integer.toString(feedId)}, 
                null, null, COL_EPISODE_PUB + " DESC");
        ArrayList<String> episodesList = new ArrayList<String>();
        if (cur.moveToFirst()) {
            do {
                episodesList.add(
                        cur.getString(cur.getColumnIndex(COL_EPISODE_URL)));
            } while (cur.moveToNext());
        }
        cur.close();
        return episodesList;
    }
    
    public ArrayList<String> getFailedSyncEpisodes(int feedId) {
        Cursor cur = db.query(EPISODE_TABLE, new String[] {COL_EPISODE_URL}, 
                COL_EP_FEED_ID + "=? AND " + COL_EPISODE_RETRY_SYNC + "=?", 
                new String[] {Integer.toString(feedId), "1"}, null, null,
                COL_EPISODE_PUB + " DESC");
        ArrayList<String> episodesList = new ArrayList<String>();
        if (cur.moveToFirst()) {
            do {
                episodesList.add(
                        cur.getString(cur.getColumnIndex(COL_EPISODE_URL)));
            } while (cur.moveToNext());
        }
        cur.close();
        return episodesList;
    }
    
    public ArrayList<String> getEpisodeIds(String feedUrl) {
        String feedId = Integer.toString(getFeedId(feedUrl));
        ArrayList<String> episodesList = new ArrayList<String>();
        Cursor cur = db.query(EPISODE_TABLE, 
                new String[] {COL_EPISODE_GOOGLE_ID}, COL_EP_FEED_ID + "=?", 
                new String[] {feedId}, null, null, COL_EPISODE_PUB + " DESC");
        if (cur.moveToFirst()) {
            do {
                episodesList.add(cur.getString(cur.getColumnIndex(
                        COL_EPISODE_GOOGLE_ID)));
            } while (cur.moveToNext());
        }
        cur.close();
        return episodesList;
    }
    
    public synchronized boolean hasReadEpisodes(int feedId, boolean read) {
        Cursor cur = db.query(EPISODE_TABLE, new String[] {COL_EPISODE_PUB}, 
                COL_EP_FEED_ID + "=? AND " + COL_EPISODE_READ + "=?", 
                new String[] {Integer.toString(feedId), 
                    Integer.toString(read ? Constants.READ : Constants.UNREAD)}, 
                null, null, null);
        boolean hasRead = false;
        if (cur.moveToFirst())
            hasRead = true;
        cur.close();
        return hasRead;
    }
    
    public synchronized boolean hasDownloadedEpisode(int feedId) {
        Cursor cur = db.query(EPISODE_TABLE, new String[] {COL_EPISODE_PUB}, 
                COL_EP_FEED_ID + "=? AND " + COL_EPISODE_DOWNLOADED + "=?", 
                new String[] {Integer.toString(feedId), 
                    Integer.toString(Constants.DOWNLOADED)}, 
                null, null, null);
        boolean hasCloud = false;
        if (cur.moveToFirst())
            hasCloud = true;
        cur.close();
        return hasCloud;
    }
    
    public synchronized String[] getEpisode(int epId) {
        String[] episodeArray = new String[5];
        Cursor cur =  db.query(EPISODE_TABLE, new String[] {COL_EPISODE_TITLE, 
                COL_EP_FEED_TITLE, COL_EPISODE_SUMMARY, COL_EP_FEED_ID}, 
                COL_EPISODE_ID + "=?", new String[] {Integer.toString(epId)}, 
                null, null, null);
        if (cur.moveToFirst()) {
            episodeArray[0] = getFeedImageLocation(cur.getInt(
                    cur.getColumnIndex(DatabaseHelper.COL_EP_FEED_ID)));
            episodeArray[1] = cur.getString(cur.getColumnIndex(
                    DatabaseHelper.COL_EPISODE_TITLE));
            episodeArray[2] = cur.getString(cur.getColumnIndex(
                    DatabaseHelper.COL_EP_FEED_TITLE));
            episodeArray[3] = cur.getString(cur.getColumnIndex(
                    DatabaseHelper.COL_EPISODE_SUMMARY));
            episodeArray[4] = getSmallFeedImageLocation(cur.getInt(
                    cur.getColumnIndex(DatabaseHelper.COL_EP_FEED_ID)));
        }
        else
            episodeArray = null;
        cur.close();
        return episodeArray;
    }

    public synchronized long getFeedLastEpTime(int feedId) {
        Cursor cur = db.query(FEED_TABLE,
                new String[] { COL_FEED_LAST_EP_TIME }, COL_FEED_ID + "=?",
                new String[] { Integer.toString(feedId) }, null, null, null);
        try {
            if (cur.moveToFirst()) {
                return cur.getLong(cur.getColumnIndex(COL_FEED_LAST_EP_TIME));
            }
            return -1;
        } finally {
            cur.close();
        }
    }
    
    public synchronized int getEpisodeProgress(int epId) {
        Cursor cur = db.query(EPISODE_TABLE,
                new String[] { COL_EPISODE_POSITION }, COL_EPISODE_ID + "=?",
                new String[] { Integer.toString(epId) }, null, null, null);
        try {
            if (cur.moveToFirst()) {
                return cur.getInt(cur.getColumnIndex(COL_EPISODE_POSITION));
            }
            return 0;
        } finally {
            cur.close();
        }
    }
    
    public synchronized int getEpisodeDuration(int epId) {
        Cursor cur = db.query(EPISODE_TABLE,
                new String[] { COL_EPISODE_DURATION }, COL_EPISODE_ID + "=?",
                new String[] { Integer.toString(epId) }, null, null, null);
        try {
            if (cur.moveToFirst()) {
                return cur.getInt(cur.getColumnIndex(COL_EPISODE_DURATION));
            }
            return 0;
        } finally {
            cur.close();
        }
    }

    public synchronized String[] getAccount() {
        Cursor cur = db.rawQuery("SELECT * FROM " + API_TABLE, null);
        String[] accountInfo = new String[3];
        if (cur.moveToFirst()) {
            accountInfo[0] = cur.getString(cur.getColumnIndex(COL_API_NAME));
            accountInfo[1] = cur.getString(cur.getColumnIndex(COL_API_KEY));
            accountInfo[2] = cur.getString(cur.getColumnIndex(
                    COL_API_ACCOUNT_TYPE));
            cur.close();
            return accountInfo;
        }
        cur.close();
        return null;
    }
    
    public boolean feedIsPodcast(String feedUrl) {
        int podcastValue = getInt(feedUrl, COL_FEED_PODCAST, FEED_TABLE, 
                COL_FEED_ADDRESS);
        return podcastValue == 1 ? true : false;
    }
    
    public int getFeedId(String address) {
        return getInt(address, COL_FEED_ID, FEED_TABLE, COL_FEED_ADDRESS);
    }
    
    public int getEpisodeId(String address) {
        return getInt(address, COL_EPISODE_ID, EPISODE_TABLE, COL_EPISODE_URL);
    }
    
    int getFeedId(int epId) {
        return getInt(epId, COL_EP_FEED_ID, EPISODE_TABLE, COL_EPISODE_ID);
    }

    public synchronized ArrayList<String> getFeedAddresses(int isPodcast) {
        Cursor cur = db.rawQuery("SELECT " + COL_FEED_ADDRESS + " FROM "
                + FEED_TABLE + " WHERE " + COL_FEED_PODCAST + "=? ORDER BY " +
                COL_FEED_TITLE + " ASC",
                new String[] { Integer.toString(isPodcast) });
        ArrayList<String> feedAddresses = new ArrayList<String>();
        if (cur.moveToFirst()) {
            do {
                feedAddresses.add(cur.getString(cur
                        .getColumnIndex(COL_FEED_ADDRESS)));
            } while (cur.moveToNext());
        }
        cur.close();
        return feedAddresses;
    }

    /**
     * Get the id of an item in a table.
     * 
     * @param name
     *            identifier for the item to lookup
     * @param columnId
     *            the name of the id column
     * @param table
     *            the table to look in
     * @param columnName
     *            the name of the name column
     * @return the found id
     */
    public synchronized int getInt(String name, String columnId, String table,
            String columnName) {
        Cursor cur = db.rawQuery("SELECT " + columnId + " FROM " + table
                + " WHERE " + columnName + "=?", new String[] { name });
        int bookmarkId = -1;
        if (cur.moveToFirst())
            bookmarkId = cur.getInt(cur.getColumnIndex(columnId));
        cur.close();
        return bookmarkId;
    }
    
    public synchronized int getInt(int id, String columnId, String table,
            String columnName) {
        Cursor cur = db.query(table, new String[] {columnId}, columnName + "=?",
                new String[] {Integer.toString(id)}, null, null, null);
        int bookmarkId = -1;
        if (cur.moveToFirst())
            bookmarkId = cur.getInt(cur.getColumnIndex(columnId));
        cur.close();
        return bookmarkId;
    }

    public synchronized long getLong(String name, String columnId, String table,
            String columnName) {
        Cursor cur = db.rawQuery("SELECT " + columnId + " FROM " + table
                + " WHERE " + columnName + "=?", new String[] { name });
        long bookmarkId = -1;
        if (cur.moveToFirst())
            bookmarkId = cur.getLong(cur.getColumnIndex(columnId));
        cur.close();
        return bookmarkId;
    }
    
    public synchronized long getFeedMagicInterval(String feedAddress) {
        Cursor cur = db.query(FEED_TABLE, 
                new String[] {COL_FEED_MAGIC_INTERVAL}, COL_FEED_ADDRESS + 
                "=?", new String[] {feedAddress}, null, null, null);
        long interval = -1;
        if (cur.moveToFirst())
            interval = cur.getLong(
                    cur.getColumnIndex(COL_FEED_MAGIC_INTERVAL));
        cur.close();
        return interval;
    }
    
    public synchronized void setFeedMagicInterval(String feedAddress, 
            long interval) {
        ContentValues cv = new ContentValues();
        cv.put(COL_FEED_MAGIC_INTERVAL, interval);
        updateRecord(cv, FEED_TABLE, COL_FEED_ADDRESS + "='" + feedAddress + 
                "'");
    }

    /**
     * Get the value from a column that is a string.
     * 
     * @param name
     *            identifier for the item to lookup
     * @param columnString
     *            the name of the column that has the string
     * @param table
     *            the table to look in
     * @param columnName
     *            the name of the name column
     * @return the found string
     */
    public synchronized String getString(String name, String columnString,
            String table, String columnName) {
        Cursor cur = db.rawQuery("SELECT " + columnString + " FROM " + table
                + " WHERE " + columnName + "=?", new String[] { name });
        String appPackage = null;
        if (cur.moveToFirst())
            appPackage = cur.getString(cur.getColumnIndex(columnString));
        cur.close();
        return appPackage;
    }
    
    public synchronized String getString(int id, String columnString,
            String table, String columnName) {
        Cursor cur = db.rawQuery("SELECT " + columnString + " FROM " + table
                + " WHERE " + columnName + "=?", 
                new String[] { Integer.toString(id) });
        String appPackage = null;
        if (cur.moveToFirst())
            appPackage = cur.getString(cur.getColumnIndex(columnString));
        cur.close();
        return appPackage;
    }
    
    public synchronized void deleteFeed(int feedId) {
        Cursor cur = this.getEpisodes(feedId);
        if (cur.moveToFirst()) {
            do {
                db.delete(EPISODE_TABLE, COL_EPISODE_ID + "=?", 
                        new String[] {Integer.toString(cur.getInt(
                                cur.getColumnIndex(COL_EPISODE_ID)))});
            } while (cur.moveToNext());
        }
        cur.close();
        db.delete(FEED_TABLE, COL_FEED_ID + "=?", 
                new String[] {Integer.toString(feedId)});
    }

    /**
     * Insert a new record into a table in the database.
     * 
     * @param cv
     *            list of content values to be entered
     * @param tableName
     *            the table name to be inserted into
     * @param columnName
     *            the column that isn't null if the rest are null
     * @return the row id of the inserted row
     */
    public synchronized long insertRecord(ContentValues cv, String tableName,
            String columnName) {
        long result = db.insert(tableName, columnName, cv);
        return result;
    }

    /**
     * Update a record in a table in the database.
     * 
     * @param cv
     *            list of content values to be entered
     * @param tableName
     *            the table name to be inserted into
     * @param whereClause
     *            what to look for
     * @return the number of rows affected
     */
    public synchronized long updateRecord(ContentValues cv, String tableName,
            String whereClause) {
        return db.update(tableName, cv, whereClause, null);
    }

    public boolean addFeed(ContentValues cv, long sortOrder) {
        String feedAddress = cv.getAsString(COL_FEED_ADDRESS);
        if (inDb(new String[] {feedAddress}, FEED_TABLE, 
                new String[] {COL_FEED_ADDRESS})) {
            updateRecord(cv, FEED_TABLE, COL_FEED_ADDRESS + "='" + 
                    cv.getAsString(COL_FEED_ADDRESS) + "'");
            return true;
        } else {
            cv.put(COL_FEED_SORT, sortOrder);
            insertRecord(cv, FEED_TABLE, COL_FEED_ADDRESS);
            if (cv.getAsInteger(COL_FEED_PODCAST) == 0)
                return false;
            else
                return true;
        }
    }

    public void addEpisode(ContentValues cv, String feedAddress, String ext) {
        int feedId = getInt(feedAddress, COL_FEED_ID, FEED_TABLE,
                COL_FEED_ADDRESS);
        String feedTitle = getString(feedAddress, COL_FEED_TITLE, FEED_TABLE, 
                COL_FEED_ADDRESS);
        // Look through all eps in the feed to determine what the magic update
        // interval should be
        // All feed episodes
        Cursor cur = getEpisodes(feedId);
        long minDiff = 999999999000l;
        long currDiff = -1;
        long lastPub = -1;
        long currPub = -1;
        ArrayList<Long> diffs = new ArrayList<Long>();
        // Number of episodes
        long totalEps = cur.getCount();
        ContentValues feedCv = new ContentValues();
        // Look for time between published episode times to be greatest, if
        // more than one difference is larger than another one, keep track of
        // the last pub time before the large gap
        long total = 0;
        long avg = 0;
        int dOfWeek = -1;
        boolean multiDay = false;
        long earliest = 999999999000l;
        ArrayList<Long> endOfDays = new ArrayList<Long>();
        Calendar cal = null;
        if (cur.moveToFirst() && totalEps >= 10) {
            do {
                if (currPub > -1 && lastPub > -1) {
                    // Keep track of the average differences between episode
                    // publish times
                    cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    cal.setTimeInMillis(lastPub);
                    dOfWeek = dOfWeek == -1 ? cal.get(Calendar.DAY_OF_WEEK) : 
                            (dOfWeek + cal.get(Calendar.DAY_OF_WEEK)) / 2;
                    currDiff = lastPub - currPub;
                    diffs.add(currDiff);
                    total += currDiff;
                    avg = total / diffs.size();
                    if (currDiff > 0)
                        minDiff = currDiff < minDiff ? currDiff : minDiff;
                }
                // Set last pub time to the previous ep pub time
                lastPub = currPub;
                // Set current pub time to the time of current episode
                currPub = cur.getLong(cur.getColumnIndex(COL_EPISODE_PUB));
                earliest = currPub % Constants.DAY_MILLI < earliest ?
                        currPub % Constants.DAY_MILLI : earliest;
            } while (cur.moveToNext());
            // Step through each publish time to keep track of ones with long
            // differences after them
            cur.moveToFirst();
            long endDaysAvg = 0;
            long currDayTime = 0;
            for (long diff : diffs) {
                cur.moveToNext();
                if (diff >= avg*2) {
                    currDayTime = cur.getLong(
                            cur.getColumnIndex(COL_EPISODE_PUB)) % 
                                    Constants.DAY_MILLI;
                    if (endDaysAvg == 0)
                        endDaysAvg = currDayTime;
                    else
                        endDaysAvg = (endDaysAvg + currDayTime) / 2;
                }
            }
            if (endDaysAvg > 0)
                // Add padding to value to indicate this needs to be updated
                // each day on this value in the day
                minDiff = Constants.PLUS_MULTI + endDaysAvg;
            else {
                // Add day of the week padding
                if (minDiff > Constants.DAY_MILLI*2)
                    minDiff = ((dOfWeek + 1)*Constants.PLUS_DAY_OF_WEEK) + 
                            earliest;
            }
            //minDiff = minDiff + (avg / 6);
        }
        else
            minDiff = Constants.MINUTE_MILLI * Integer.parseInt(
                    Util.readStringPreference(R.string.interval_key, "60"));
        cur.close();
        String epLoc = ApplicationEx.cacheLocation + Constants.FEEDS_LOCATION +
                feedId + File.separator;
        //new File(epLoc).mkdirs();
        feedCv.clear();
        long feedLastEpTime = getFeedLastEpTime(feedId);
        long epPub = cv.getAsLong(COL_EPISODE_PUB); 
        feedCv.put(COL_FEED_LAST_EP_TIME, 
                epPub > feedLastEpTime ? epPub : feedLastEpTime);
        feedCv.put(COL_FEED_MAGIC_INTERVAL, minDiff);
        feedCv.put(COL_FEED_UNREAD, Constants.UNREAD);
        feedCv.put(COL_FEED_DOWNLOADED, Constants.TO_DOWNLOAD);
        updateRecord(feedCv, FEED_TABLE, COL_FEED_ID + "=" + feedId);
        cv.put(COL_EP_FEED_ID, feedId);
        cv.put(COL_EP_FEED_TITLE, Jsoup.parse(StringEscapeUtils.unescapeHtml4(
                feedTitle)).text());
        cv.put(COL_EP_FEED_LAST_EP_TIME, getFeedLastEpTime(feedId));
        insertRecord(cv, EPISODE_TABLE, COL_EPISODE_URL);
        int epId = getInt(cv.getAsString(COL_EPISODE_URL), COL_EPISODE_ID, 
                EPISODE_TABLE, COL_EPISODE_URL);
        String epFile = epLoc + epId + ext;
        cv.clear();
        cv.put(COL_EPISODE_LOCATION, epFile);
        updateRecord(cv, EPISODE_TABLE, COL_EPISODE_ID + "=" + epId);
    }

    public void addAccount(String accountName) {
        ContentValues cv = new ContentValues();
        cv.put(COL_API_NAME, accountName);
        cv.put(COL_API_KEY, (String) null);
        cv.put(COL_API_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
        if (getAccount() != null)
            updateRecord(cv, API_TABLE, null);
        else
            insertRecord(cv, API_TABLE, COL_API_NAME);
    }
    
    public synchronized String typeFromEpisodeUrl(String url) {
        Cursor cur = db.query(EPISODE_TABLE, new String[] { COL_EPISODE_TYPE },
                COL_EPISODE_URL + "=?", new String[] { url }, null, null, null);
        String type = "";
        if (cur.moveToFirst())
            type = cur.getString(cur.getColumnIndex(COL_EPISODE_TYPE));
        cur.close();
        return type;
    }

    public synchronized String extFromEpisodeUrl(String url) {
        Cursor cur = db.query(EPISODE_TABLE, new String[] { COL_EPISODE_TYPE },
                COL_EPISODE_URL + "=?", new String[] { url }, null, null, null);
        String type = "";
        if (cur.moveToFirst())
            type = cur.getString(cur.getColumnIndex(COL_EPISODE_TYPE));
        cur.close();
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
    
    public synchronized String extFromEpisodeId(int id) {
        Cursor cur = db.query(EPISODE_TABLE, new String[] { COL_EPISODE_TYPE },
                COL_EPISODE_ID + "=?", new String[] {Integer.toString(id)}, 
                null, null, null);
        String type = "";
        if (cur.moveToFirst())
            type = cur.getString(cur.getColumnIndex(COL_EPISODE_TYPE));
        cur.close();
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
    
    public boolean addFailedTry(String url, int reason) {
        ContentValues cv = new ContentValues();
        cv.put(COL_EPISODE_REASON, reason);
        updateRecord(cv, EPISODE_TABLE, COL_EPISODE_URL + "='" + url + "'");
        int currFailed = getInt(url, COL_FEED_FAILED_TRIES, FEED_TABLE, 
                COL_FEED_ADDRESS);
        if (currFailed == -1) {
            currFailed = getInt(url, COL_EPISODE_FAILED_TRIES, EPISODE_TABLE, 
                    COL_EPISODE_URL);
            if (currFailed == -1) {
                return false;
            }
            else {
                if (currFailed == 5)
                    return false;
                else {
                    cv.clear();
                    cv.put(COL_EPISODE_FAILED_TRIES, ++currFailed);
                    updateRecord(cv, EPISODE_TABLE, COL_EPISODE_URL + "='" + 
                            url + "'");
                    return true;
                }
            }
        }
        else {
            if (currFailed == 5)
                return false;
            else {
                cv.clear();
                cv.put(COL_FEED_FAILED_TRIES, ++currFailed);
                updateRecord(cv, FEED_TABLE, COL_FEED_ADDRESS + "='" + url + 
                        "'");
                return true;
            }
        }
    }
    
    public void setFailedSync(int epId) {
        ContentValues cv = new ContentValues();
        cv.put(COL_EPISODE_RETRY_SYNC, 1);
        updateRecord(cv, EPISODE_TABLE, COL_EPISODE_ID + "=" + epId);
    }
    
    public void resetFailedSync(int epId) {
        ContentValues cv = new ContentValues();
        cv.put(COL_EPISODE_RETRY_SYNC, 0);
        updateRecord(cv, EPISODE_TABLE, COL_EPISODE_ID + "=" + epId);
    }
    
    public boolean getFailedSync(String epUrl) {
        if (getInt(epUrl, COL_EPISODE_RETRY_SYNC, EPISODE_TABLE, 
                COL_EPISODE_URL) >= 1)
            return true;
        else
            return false;
    }
    
    public int getEpisodeRead(String epUrl) {
        return getInt(epUrl, COL_EPISODE_READ, EPISODE_TABLE, COL_EPISODE_URL);
    }
    
    public int getEpisodeReadFromGId(String epId) {
        return getInt(epId, COL_EPISODE_READ, EPISODE_TABLE, 
                COL_EPISODE_GOOGLE_ID);
    }
    
    public int getEpisodeIdFromGId(String epId) {
        return getInt(epId, COL_EPISODE_ID, EPISODE_TABLE, 
                COL_EPISODE_GOOGLE_ID);
    }
    
    public boolean getEpisodeDownloaded(String epUrl) {
        int downloaded = getInt(epUrl, COL_EPISODE_DOWNLOADED, EPISODE_TABLE, 
                COL_EPISODE_URL);
        return downloaded == Constants.DOWNLOADED ? true : false;
    }
    
    public int didFail(String url) {
        int currFailed = getInt(url, COL_FEED_FAILED_TRIES, FEED_TABLE, 
                COL_FEED_ADDRESS);
        if (currFailed == -1) {
            currFailed = getInt(url, COL_EPISODE_FAILED_TRIES, EPISODE_TABLE, 
                    COL_EPISODE_URL);
            return currFailed;
        }
        return currFailed;
    }
    
    public boolean resetFail(String url) {
        ContentValues cv = new ContentValues();
        cv.put(COL_FEED_FAILED_TRIES, 0);
        long result = updateRecord(cv, FEED_TABLE, COL_FEED_ADDRESS + "='" + 
                url + "'");
        if (result == 0) {
            cv.clear();
            cv.put(COL_EPISODE_FAILED_TRIES, 0);
            result = updateRecord(cv, EPISODE_TABLE, COL_EPISODE_URL + "='" + 
                    url + "'");
            if (result == 0)
                return false;
            else
                return true;
        }
        else
            return true;
    }
    
    public boolean markRead(int epId, boolean read) {
        ContentValues cv = new ContentValues();
        if (read) {
            cv.put(COL_EPISODE_READ, Constants.READ);
        }
        else
            cv.put(COL_EPISODE_READ, Constants.UNREAD);
        long result = updateRecord(cv, EPISODE_TABLE, COL_EPISODE_ID + "=" + 
                epId);
        int feedId = getFeedId(epId);
        if (read) {
            if (hasReadEpisodes(feedId, !read)) {
                cv.clear();
                cv.put(COL_FEED_UNREAD, Constants.UNREAD);
                updateRecord(cv, FEED_TABLE, COL_FEED_ID + "=" + feedId);
            }
            else {
                cv.clear();
                cv.put(COL_FEED_UNREAD, Constants.READ);
                updateRecord(cv, FEED_TABLE, COL_FEED_ID + "=" + feedId);
            }
        }
        else {
            cv.clear();
            cv.put(COL_FEED_UNREAD, Constants.UNREAD);
            updateRecord(cv, FEED_TABLE, COL_FEED_ID + "=" + feedId);
        }
        if (!hasDownloadedEpisode(feedId)) {
            cv.clear();
            cv.put(COL_FEED_DOWNLOADED, Constants.DOWNLOADED);
            updateRecord(cv, FEED_TABLE, COL_FEED_ID + "=" + feedId);
        }
        else {
            cv.clear();
            cv.put(COL_FEED_DOWNLOADED, Constants.TO_DOWNLOAD);
            updateRecord(cv, FEED_TABLE, COL_FEED_ID + "=" + feedId);
        }
        if (ApplicationEx.isSyncing() && Util.readBooleanFromFile(
                Constants.GOOGLE_FILENAME)) {
            String googleId = getString(epId, COL_EPISODE_GOOGLE_ID, 
                    EPISODE_TABLE, COL_EPISODE_ID);
            if (googleId != null && !googleId.equals(Constants.NO_GOOGLE)) {
                String[] account = getAccount();
                if (account != null) {
                    MarkReadTask task = new MarkReadTask(account[1], 
                            getString(feedId, COL_FEED_ADDRESS, FEED_TABLE, 
                                    COL_FEED_ID), googleId, read);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                        task.execute();
                    else
                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
            else {
                ApplicationEx.dbHelper.setFailedSync(epId);
            }
        }
        if (result == 0) {
            return false;
        }
        else {
            return true;
        }
    }
    
    public ArrayList<Integer> markFeedEpisodesRead(int feedId, boolean read, 
            int archive) {
        ArrayList<Integer> epList = getReadEpisodes(feedId, read, archive);
        for (Integer epId : epList) {
            markRead(epId, read);
        }
        return epList;
    }
    
    public void updateFeedRead(int feedId) {
        ContentValues cv = new ContentValues();
        if (hasReadEpisodes(feedId, false)) {
            cv.put(COL_FEED_UNREAD, Constants.UNREAD);
            
        }
        else {
            cv.put(COL_FEED_UNREAD, Constants.READ);
        }
        updateRecord(cv, FEED_TABLE, COL_FEED_ID + "=" + feedId);
    }
    
    public boolean isFeedRead(int feedId, boolean read) {
        int readState = getInt(feedId, COL_FEED_UNREAD, FEED_TABLE, 
                COL_FEED_ID);
        boolean feedState = (read && readState == Constants.UNREAD) ||
                (!read && readState == Constants.READ);
        return feedState;
    }
    
    public synchronized boolean getEpisodeFailed(int id) {
        Cursor cur = db.query(EPISODE_TABLE, new String[] {COL_EPISODE_URL}, 
                COL_EPISODE_ID + "=? AND " + COL_EPISODE_FAILED_TRIES + ">?", 
                new String[] {Integer.toString(id), "0"}, null, null, null);
        boolean failed = cur.getCount() > 0;
        cur.close();
        return failed;
    }
    
    public synchronized ArrayList<String> getFailedEpisodes() {
        Cursor cur = db.query(EPISODE_TABLE, new String[] {COL_EPISODE_URL}, 
                COL_EPISODE_FAILED_TRIES + ">?", new String[] {"0"}, null, null, 
                null);
        ArrayList<String> epUrls = new ArrayList<String>();
        if (cur.moveToFirst()) {
            do {
                epUrls.add(cur.getString(cur.getColumnIndex(COL_EPISODE_URL)));
            } while (cur.moveToNext());
        }
        cur.close();
        return epUrls;
    }
    
    public String getFeedTitle(String url) {
        return getString(url, COL_FEED_TITLE, FEED_TABLE, COL_FEED_ADDRESS);
    }
    
    public String getEpisodeLocation(String url) {
        String location = getString(url, COL_EPISODE_LOCATION, EPISODE_TABLE, 
                COL_EPISODE_URL);
        if (location == null) {
            int epId = getEpisodeId(url);
            int feedId = getEpisodeFeedId(epId);
            String ext = extFromEpisodeUrl(url);
            location = ApplicationEx.cacheLocation + Constants.FEEDS_LOCATION +
                    feedId + File.separator + epId + ext;
        }
        return location;
    }
    
    public String getEpisodeLocation(int id) {
        String location = getString(Integer.toString(id), COL_EPISODE_LOCATION, 
                EPISODE_TABLE, COL_EPISODE_ID);
        if (location == null) {
            int feedId = getEpisodeFeedId(id);
            String ext = extFromEpisodeId(id);
            location = ApplicationEx.cacheLocation + Constants.FEEDS_LOCATION +
                    feedId + File.separator + id + ext;
        }
        return location;
    }
    
    public boolean isDownloaded(int id) {
        int downloaded = getInt(id, COL_EPISODE_DOWNLOADED, EPISODE_TABLE, 
                COL_EPISODE_ID);
        if (downloaded == Constants.DOWNLOADED)
            return true;
        else
            return false;
    }
    
    public String getEpisodeFeedTitle(int id) {
        return getString(id, COL_EP_FEED_TITLE, EPISODE_TABLE, COL_EPISODE_ID);
    }
    
    public int getEpisodeFeedId(int id) {
        return getInt(id, COL_EP_FEED_ID, EPISODE_TABLE, COL_EPISODE_ID);
    }
    
    public String getEpisodeTitle(int id) {
        return getString(id, COL_EPISODE_TITLE, EPISODE_TABLE, COL_EPISODE_ID);
    }
    
    public String getFeedImageLocation(int id) {
        String path = ApplicationEx.cacheLocation + Constants.FEEDS_LOCATION +
                id + File.separator;
        if (Util.findFile(path, id + ".png"))
            return path + id + ".png";
        else if (Util.findFile(path, id + ".jpg"))
            return path + id + ".jpg";
        else if (Util.findFile(path, id + ".gif"))
            return path + id + ".gif";
        else
            return "";
    }
    
    public String getSmallFeedImageLocation(int id) {
        String path = ApplicationEx.cacheLocation + Constants.FEEDS_LOCATION +
                id + File.separator;
        if (Util.findFile(path, id + "_small.png"))
            return path + id + "_small.png";
        else
            return "";
    }
    
    public synchronized int getFeedCount() {
        Cursor cur = db.rawQuery("SELECT COUNT(*) FROM " + FEED_TABLE,
                new String[] {});
        int count = 0;
        if (cur.moveToFirst()) {
            count = cur.getInt(0);
        }
        cur.close();
        return count;
    }
    
    public void feedDownloaded(int feedId, boolean isDownloaded) {
        ContentValues cv = new ContentValues();
        if (isDownloaded)
            cv.put(COL_FEED_DOWNLOADED, Constants.DOWNLOADED);
        else
            cv.put(COL_FEED_DOWNLOADED, Constants.TO_DOWNLOAD);
        updateRecord(cv, FEED_TABLE, COL_FEED_ID + "=" + feedId);
    }
    
    public void episodeDownloaded(int episodeId, boolean isDownloaded) {
        ContentValues cv = new ContentValues();
        if (isDownloaded)
            cv.put(COL_EPISODE_DOWNLOADED, Constants.DOWNLOADED);
        else
            cv.put(COL_EPISODE_DOWNLOADED, Constants.TO_DOWNLOAD);
        updateRecord(cv, EPISODE_TABLE, COL_EPISODE_ID + "=" + episodeId);
        int feedId = getFeedId(episodeId);
        feedDownloaded(feedId, !isFeedCloud(feedId));
    }
    
    private boolean isFeedCloud(int feedId) {
        if (getFeedEpisodes(feedId, Constants.READ, Constants.DOWNLOADED) == 0
                && getFeedEpisodes(feedId, Constants.UNREAD, 
                        Constants.DOWNLOADED) == 0)
            return true;
        else
            return false;
    }
    
    private int getFeedEpisodes(int feedId, int read, int downloaded) {
        Cursor cur = db.query(EPISODE_TABLE, new String[] {COL_EPISODE_ID}, 
                COL_EP_FEED_ID + "=? AND " + COL_EPISODE_READ + "=? AND " +
                COL_EPISODE_DOWNLOADED + "=?", new String[] {
                    Integer.toString(feedId), Integer.toString(read), 
                    Integer.toString(downloaded)}, null, null, null);
        int count = cur.getCount();
        cur.close();
        return count;
    }
    
    public synchronized ArrayList<String> getNewFeeds() {
        ArrayList<String> newFeeds = new ArrayList<String>();
        Cursor cur = db.query(FEED_TABLE, new String[] {COL_FEED_ADDRESS}, null, 
                null, null, null, COL_FEED_ADDRESS + " DESC");
        if (cur.moveToFirst()) {
            do {
                if (!cur.getString(cur.getColumnIndex(COL_FEED_ADDRESS))
                        .startsWith("feed/")) {
                    newFeeds.add(cur.getString(
                            cur.getColumnIndex(COL_FEED_ADDRESS)));
                }
            } while (cur.moveToNext());
        }
        cur.close();
        return newFeeds;
    }
    
    public synchronized String getEpisodeGoogleId(String epUrl) {
        return getString(epUrl, COL_EPISODE_GOOGLE_ID, EPISODE_TABLE, 
                COL_EPISODE_URL);
    }
    
    public synchronized ArrayList<String> getNoGoogleEpisodes(int feedId) {
        ArrayList<String> noGoogle = new ArrayList<String>();
        Cursor cur = db.query(EPISODE_TABLE, new String[] {COL_EPISODE_URL}, 
                COL_EP_FEED_ID + "=? AND " + COL_EPISODE_GOOGLE_ID + "=?", 
                new String[] {Integer.toString(feedId), Constants.NO_GOOGLE}, 
                null, null, null);
        if (cur.moveToFirst()) {
            do {
                noGoogle.add(cur.getString(cur.getColumnIndex(
                        COL_EPISODE_URL)));
            } while (cur.moveToNext());
        }
        cur.close();
        return noGoogle;
    }
    
    public synchronized Magics getNextMagicInterval(boolean failed) {
        HashMap<String,Long> nextUpdate = new HashMap<String,Long>();
        long now = System.currentTimeMillis();
        long nowDay = now - (now % Constants.DAY_MILLI);
        Cursor cur = db.query(FEED_TABLE, new String[] {COL_FEED_MAGIC_INTERVAL, 
                    COL_FEED_ADDRESS, COL_FEED_LAST_UPDATE, 
                    COL_FEED_LAST_EP_TIME}, 
                COL_FEED_MAGIC_INTERVAL + ">?", new String[] {"-1"}, null, null, 
                COL_FEED_MAGIC_INTERVAL + " ASC");
        ArrayList<Long> intervals = new ArrayList<Long>();
        long lastEpTime = 0;
        long magicInterval = 0;
        long lastUpdateTime = 0;
        long nextFeedTime = Constants.PLUS_MULTI;
        String feedAddress = null;
        ArrayList<String> addresses = new ArrayList<String>();
        long intervalSetting = Constants.MINUTE_MILLI * Integer.parseInt(
                Util.readStringPreference(R.string.interval_key, "60"));
        Magics currMagics = new Util.Magics();
        HashMap<String, Long> feedMagics = new HashMap<String, Long>();
        if (cur.moveToFirst()) {
            do {
                feedAddress = cur.getString(cur.getColumnIndex(
                        COL_FEED_ADDRESS));
                lastUpdateTime = cur.getLong(cur.getColumnIndex(
                        COL_FEED_LAST_UPDATE));
                magicInterval = cur.getLong(cur.getColumnIndex(
                        COL_FEED_MAGIC_INTERVAL));
                /* If the feed has multiple episodes in a day */
                if (magicInterval > Constants.PLUS_MULTI)
                    /* Change the interval to today (midnight) plus the interval
                     * masked by the multi value */
                    magicInterval = nowDay + 
                            (magicInterval - Constants.PLUS_MULTI);
                /* If the feed is weekly */
                else if (magicInterval > Constants.PLUS_DAY_OF_WEEK) {
                    /* Get the magic interval day of week */
                    int dayOfWeek = (int) ((magicInterval - 
                            (magicInterval % Constants.PLUS_DAY_OF_WEEK)) / 
                                Constants.PLUS_DAY_OF_WEEK);
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(
                            "UTC"));
                    cal.setTimeInMillis(now);
                    magicInterval = magicInterval % Constants.PLUS_DAY_OF_WEEK;
                    int offset = Math.abs(dayOfWeek - 
                            cal.get(Calendar.DAY_OF_WEEK));
                    magicInterval = (nowDay + magicInterval + 
                            (offset * Constants.DAY_MILLI));
                }
                else if (magicInterval == 0)
                    magicInterval = lastUpdateTime + intervalSetting;
                else
                    magicInterval = lastUpdateTime + magicInterval;
                if (magicInterval <= nextFeedTime) {
                    nextFeedTime = magicInterval;
                    Log.w(Constants.LOG_TAG, "nextFeedTime: ");
                    Log.w(Constants.LOG_TAG, ApplicationEx.dbHelper.getFeedTitle(feedAddress));
                    Log.w(Constants.LOG_TAG, " -> " + nextFeedTime);
                }
                feedMagics.put(feedAddress, magicInterval);
            } while (cur.moveToNext());
        }
        cur.close();
        for (Entry<String, Long> feed : feedMagics.entrySet()) {
            feedAddress = feed.getKey();
            Log.d(Constants.LOG_TAG, ApplicationEx.dbHelper.getFeedTitle(feedAddress));
            magicInterval = feed.getValue();
            Log.d(Constants.LOG_TAG, " -> " + magicInterval);
            if (magicInterval <= nextFeedTime + intervalSetting)
                addresses.add(feedAddress);
        }
        if (nextFeedTime < System.currentTimeMillis())
            nextFeedTime = System.currentTimeMillis()+5000;
        if (failed)
            nextFeedTime += 300000;
        Log.i(Constants.LOG_TAG, "NEXT UPDATE: ");
        Log.i(Constants.LOG_TAG, Long.toString(nextFeedTime));
        for (String address : addresses) {
            Log.i(Constants.LOG_TAG, address);
        }
        currMagics.setFeeds(addresses);
        currMagics.setTime(nextFeedTime == Constants.PLUS_MULTI ?
                System.currentTimeMillis() : nextFeedTime);
        return currMagics;
    }
    
    public int getInt(String columnId, String table) {
        Cursor cur = db.query(table, new String[] {columnId}, null, null,
                null, null, null);
        int value = -1;
        if (cur.moveToFirst())
            value = cur.getInt(cur.getColumnIndex(columnId));
        cur.close();
        return value;
    }
    
    private boolean hasAppRecord() {
        Cursor cur = db.query(APP_TABLE, new String[] {this.COL_APP_LAST_TAB},
                null, null, null, null, null);
        boolean hasRecord = false;
        if (cur.moveToFirst())
            hasRecord = true;
        return hasRecord;
    }
    
    public int getAppFeedPosition() {
        int pos = getInt(COL_APP_FEED_POSITION, APP_TABLE);
        return pos == -1 ? 0 : pos;
    }
    
    public void setAppFeedPosition(int position) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_FEED_POSITION, position);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getAppEpisodePosition() {
        int pos = getInt(COL_APP_EPISODE_POSITION, APP_TABLE);
        return pos == -1 ? 0 : pos;
    }
    
    public void setAppEpisodePosition(int position) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_EPISODE_POSITION, position);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getAppDownloadPosition() {
        int pos = getInt(COL_APP_DOWNLOAD_POSITION, APP_TABLE);
        return pos == -1 ? 0 : pos;
    }
    
    public void setAppDownloadPosition(int position) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_DOWNLOAD_POSITION, position);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getAppLastTab() {
        int tab = getInt(COL_APP_LAST_TAB, APP_TABLE);
        return tab < 0 ? 0 : tab;
    }
    
    public void setAppLastTab(int position) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_LAST_TAB, position);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getCurrentEpisode() {
        return getInt(COL_APP_CURRENT_EPISODE, APP_TABLE);
    }
    
    public void setCurrentEpisode(int epId) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_CURRENT_EPISODE, epId);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getPrevEpisode() {
        return getInt(COL_APP_PREVIOUS_EPISODE, APP_TABLE);
    }
    
    public void setPrevEpisode(int epId) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_PREVIOUS_EPISODE, epId);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getNextEpisode() {
        return getInt(COL_APP_NEXT_EPISODE, APP_TABLE);
    }
    
    public void setNextEpisode(int epId) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_NEXT_EPISODE, epId);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getCurrentDownload() {
        return getInt(COL_APP_CURRENT_DOWNLOAD, APP_TABLE);
    }
    
    public void setCurrentDownload(int epId) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_CURRENT_DOWNLOAD, epId);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getCurrentPlaylistType() {
        int type = getInt(COL_APP_CURRENT_PLAYLIST_TYPE, APP_TABLE);
        return type < 0 ? 0 : type;
    }
    
    public void setCurrentPlaylistType(int type) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_CURRENT_PLAYLIST_TYPE, type);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getCurrentEpisodeProgress() {
        return getInt(COL_APP_EPISODE_PROGRESS, APP_TABLE);
    }
    
    public void setCurrentEpisodeProgress(int progress) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_EPISODE_PROGRESS, progress);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getCurrentEpisodeDuration() {
        return getInt(COL_APP_EPISODE_DURATION, APP_TABLE);
    }
    
    public void setCurrentEpisodeDuration(int duration) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_EPISODE_DURATION, duration);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getCurrentFeedRead() {
        int read = getInt(COL_APP_FEED_READ, APP_TABLE);
        return read < Constants.UNREAD ? Constants.UNREAD : read;
    }
    
    public void setCurrentFeedRead(int read) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_FEED_READ, read);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getCurrentFeedDownloaded() {
        int downloaded = getInt(COL_APP_FEED_DOWNLOADED, APP_TABLE);
        return downloaded < Constants.DOWNLOADED ? Constants.DOWNLOADED :
            downloaded;
    }
    
    public void setCurrentFeedDownloaded(int downloaded) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_FEED_DOWNLOADED, downloaded);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getCurrentEpisodeRead() {
        int read = getInt(COL_APP_EPISODE_READ, APP_TABLE);
        return read < Constants.UNREAD ? Constants.UNREAD : read;
    }
    
    public void setCurrentEpisodeRead(int read) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_EPISODE_READ, read);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getCurrentEpisodeDownloaded() {
        int downloaded = getInt(COL_APP_EPISODE_DOWNLOADED, APP_TABLE);
        return downloaded < Constants.DOWNLOADED ? Constants.DOWNLOADED :
            downloaded;
    }
    
    public void setCurrentEpisodeDownloaded(int downloaded) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_EPISODE_DOWNLOADED, downloaded);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getCurrentEpisodeSortType() {
        int sortType = getInt(COL_APP_EPISODE_SORTTYPE, APP_TABLE);
        return sortType < 1 ? Constants.PLAYLIST_SORT_TYPE_ASC : sortType;
    }
    
    public void setCurrentEpisodeSortType(int sortType) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_EPISODE_SORTTYPE, sortType);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getCurrentFeedSortType() {
        int sortType = getInt(COL_APP_FEED_SORTTYPE, APP_TABLE);
        return sortType < 1 ? Constants.PLAYLIST_SORT_TYPE_ASC : sortType;
    }
    
    public void setCurrentFeedSortType(int sortType) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_FEED_SORTTYPE, sortType);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getCurrentFeedSort() {
        int sort = getInt(COL_APP_FEED_SORT, APP_TABLE);
        return sort < 1 ?
                Constants.PLAYLIST_SORT_FEED|Constants.PLAYLIST_SORT_DATE :
                    sort;
    }
    
    public void setCurrentFeedSort(int sort) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_FEED_SORT, sort);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }

}