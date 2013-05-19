package com.jeffthefate.podcatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.jeffthefate.podcatcher.Util.Magics;

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
    public static final String COL_FEED_MAGIC_TYPE = "FeedMagicType";
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
    public static final String COL_EPISODE_FAILED_TRIES = "EpisodeFailedTries";
    public static final String COL_EPISODE_POSITION = "EpisodePosition";
    public static final String COL_EPISODE_DURATION = "EpisodeDuration";
    public static final String COL_EPISODE_ARCHIVE = "EpisodeArchive";
    public static final String COL_EPISODE_GOOGLE_ID = "EpisodeGoogleId";
    public static final String COL_EPISODE_RETRY_SYNC = "EpisodeRetrySync";
    public static final String COL_EPISODE_LOCKED = "EpisodeLocked";
    public static final String COL_EPISODE_GUID = "EpisodeGuid";
    public static final String COL_APP_FEED_POSITION = "AppFeedPosition";
    public static final String COL_APP_FEED_LIST_POSITION =
        "AppFeedListPosition";
    public static final String COL_APP_EPISODE_POSITION = "AppEpisodePosition";
    public static final String COL_APP_EPISODE_LIST_POSITION =
        "AppEpisodeListPosition";
    public static final String COL_APP_DOWNLOAD_POSITION =
        "AppDownloadPosition";
    public static final String COL_APP_LAST_TAB = "AppLastTab";
    public static final String COL_APP_CURRENT_EPISODE = "AppCurrentEpisode";
    public static final String COL_APP_PREVIOUS_EPISODE = "AppPreviousEpisode";
    public static final String COL_APP_NEXT_EPISODE = "AppNextEpisode";
    public static final String COL_APP_CURRENT_DOWNLOAD = "AppCurrentDownload";
    public static final String COL_APP_CURRENT_PLAYLIST_TYPE =
        "AppCurrentPlaylistType";
    public static final String COL_APP_CURRENT_SPEED = "AppCurrentSpeed";
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
    public static final String COL_APP_PREV_BRIGHTNESS_MODE = "BrightnessMode";
    public static final String COL_APP_PREV_BRIGHTNESS_LEVEL =
        "BrightnessLevel";
    public static final String COL_APP_NEXT_MAGIC_TIME = "AppNextMagicTime";
    /**
     * Create API table string
     */
    private static final String CREATE_API_TABLE = TextUtils.concat(
            "CREATE TABLE ", API_TABLE, " (", COL_API_NAME,
            " TEXT PRIMARY KEY, ", COL_API_KEY, " TEXT, ", COL_API_ACCOUNT_TYPE,
            " TEXT, ", COL_API_UPDATED, " INTEGER DEFAULT -1)").toString();
    /**
     * Create feed table string
     */
    private static final String CREATE_FEED_TABLE = TextUtils.concat(
            "CREATE TABLE ", FEED_TABLE, " (", COL_FEED_ID,
            " INTEGER PRIMARY KEY, ", COL_FEED_ADDRESS, " TEXT, ",
            COL_FEED_TITLE, " TEXT, ", COL_FEED_LINK, " TEXT, ",
            COL_FEED_DESCRIPTION, " TEXT, ", COL_FEED_IMAGE, " TEXT, ",
            COL_FEED_LAST_EP_TIME, " INTEGER DEFAULT -1, ",
            COL_FEED_LAST_UPDATE, " INTEGER DEFAULT -1, ", COL_FEED_PODCAST, 
            " INTEGER DEFAULT -1, ", COL_FEED_LAST_IMAGE_UPDATE,
            " INTEGER DEFAULT -1, ", COL_FEED_MAGIC_INTERVAL,
            " INTEGER DEFAULT -1, ", COL_FEED_MAGIC_TYPE, " TEXT DEFAULT ",
            FeedPublishType.Daily.toString(), ", ", COL_FEED_FAILED_TRIES,
            " INTEGER DEFAULT 0, ", COL_FEED_SORT, " INTEGER DEFAULT -1, ",
            COL_FEED_UNREAD, " INTEGER DEFAULT 1, ", COL_FEED_DOWNLOADED,
            " INTEGER DEFAULT 1, ", COL_FEED_SPEED, " REAL DEFAULT 1.0)")
            .toString();
    /**
     * Create episode table string
     */
    private static final String CREATE_EPISODE_TABLE = TextUtils.concat(
            "CREATE TABLE ", EPISODE_TABLE, " (", COL_EPISODE_ID,
            " INTEGER PRIMARY KEY, ", COL_EP_FEED_ID, " INTEGER DEFAULT -1, ",
            COL_EP_FEED_TITLE, " TEXT, ", COL_EP_FEED_LAST_EP_TIME,
            " INTEGER DEFAULT -1, ", COL_EPISODE_URL, " TEXT, ",
            COL_EPISODE_PUB, " INTEGER DEFAULT -1, ", COL_EPISODE_LENGTH,
            " INTEGER DEFAULT -1, ", COL_EPISODE_SUMMARY, " TEXT, ",
            COL_EPISODE_TYPE, " TEXT, ", COL_EPISODE_LOCATION, " TEXT, ",
            COL_EPISODE_TITLE, " TEXT, ", COL_EPISODE_UPDATED,
            " INTEGER DEFAULT -1, ", COL_EPISODE_READ, " INTEGER DEFAULT 1, ",
            COL_EPISODE_DOWNLOADED, " INTEGER DEFAULT 1, ",
            COL_EPISODE_FAILED_TRIES, " INTEGER DEFAULT 0, ",
            COL_EPISODE_POSITION, " INTEGER DEFAULT 0, ", COL_EPISODE_DURATION,
            " INTEGER DEFAULT 0, ", COL_EPISODE_ARCHIVE, " INTEGER DEFAULT 0, ",
            COL_EPISODE_GOOGLE_ID, " TEXT DEFAULT ", Constants.NO_GOOGLE,
            Constants.COMMA_SPACE, COL_EPISODE_RETRY_SYNC,
            " INTEGER DEFAULT 0, ", COL_EPISODE_LOCKED, " INTEGER DEFAULT 0, ",
            COL_EPISODE_GUID, " TEXT)")
            .toString();
    /**
     * Create app table string
     */
    private static final String CREATE_APP_TABLE = TextUtils.concat(
            "CREATE TABLE ", APP_TABLE, " (", COL_APP_FEED_POSITION,
            " INTEGER DEFAULT 0, ", COL_APP_FEED_LIST_POSITION,
            " INTEGER DEFAULT 0, ", COL_APP_EPISODE_POSITION,
            " INTEGER DEFAULT 0, ", COL_APP_EPISODE_LIST_POSITION,
            " INTEGER DEFAULT 0, ", COL_APP_DOWNLOAD_POSITION,
            " INTEGER DEFAULT 0, ", COL_APP_LAST_TAB, " INTEGER DEFAULT 0, ",
            COL_APP_CURRENT_EPISODE, " INTEGER DEFAULT 0, ",
            COL_APP_PREVIOUS_EPISODE, " INTEGER DEFAULT 0, ",
            COL_APP_NEXT_EPISODE, " INTEGER DEFAULT 0, ",
            COL_APP_CURRENT_DOWNLOAD, " INTEGER DEFAULT 0, ",
            COL_APP_CURRENT_SPEED, " REAL DEFAULT 0, ",
            COL_APP_CURRENT_PLAYLIST_TYPE, " INTEGER DEFAULT -1, ",
            COL_APP_EPISODE_PROGRESS, " INTEGER DEFAULT 0, ",
            COL_APP_EPISODE_DURATION, " INTEGER DEFAULT 0, ", COL_APP_FEED_READ,
            " INTEGER DEFAULT ", Integer.toString(Constants.UNREAD),
            Constants.COMMA_SPACE, COL_APP_FEED_DOWNLOADED, " INTEGER DEFAULT ",
            Integer.toString(Constants.DOWNLOADED), Constants.COMMA_SPACE,
            COL_APP_EPISODE_READ, " INTEGER DEFAULT ",
            Integer.toString(Constants.UNREAD), Constants.COMMA_SPACE,
            COL_APP_EPISODE_DOWNLOADED, " INTEGER DEFAULT ",
            Integer.toString(Constants.DOWNLOADED), Constants.COMMA_SPACE,
            COL_APP_EPISODE_SORTTYPE, " INTEGER DEFAULT 0, ",
            COL_APP_FEED_SORTTYPE, " INTEGER DEFAULT 0, ", COL_APP_FEED_SORT,
            " INTEGER DEFAULT 0, ", COL_APP_PREV_BRIGHTNESS_MODE,
            " INTEGER DEFAULT -1, ", COL_APP_PREV_BRIGHTNESS_LEVEL,
            " INTEGER DEFAULT -1, ", COL_APP_NEXT_MAGIC_TIME,
            " INTEGER DEFAULT -1)").toString();
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
        db.execSQL(TextUtils.concat("DROP TABLE IF EXISTS ", API_TABLE)
                .toString());
        db.execSQL(TextUtils.concat("DROP TABLE IF EXISTS ", FEED_TABLE)
                .toString());
        db.execSQL(TextUtils.concat("DROP TABLE IF EXISTS ", EPISODE_TABLE)
                .toString());
        db.execSQL(TextUtils.concat("DROP TABLE IF EXISTS ", APP_TABLE)
                .toString());
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
                sb.append(Constants.AND_SPACES);
            }
            sb.append(columns[i]);
            sb.append(Constants.EQUAL_QUESTION);
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
        sb.append(Constants.EQUAL_QUESTION);
        if (read != (Constants.READ|Constants.UNREAD)) {
            sb.append(Constants.AND_SPACES);
            sb.append(COL_FEED_UNREAD);
            sb.append(Constants.EQUAL_QUESTION);
        }
        if (downloaded != (Constants.DOWNLOADED|Constants.TO_DOWNLOAD)) {
            sb.append(Constants.AND_SPACES);
            sb.append(COL_FEED_DOWNLOADED);
            sb.append(Constants.EQUAL_QUESTION);
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
        updateRecord(cv, FEED_TABLE, TextUtils.concat(COL_FEED_ID,
                Constants.EQUAL, Integer.toString(feedId)).toString());
    }
    
    public synchronized float getFeedSpeed(int epId) {
        float speed = 0.0f;
        int feedId = getFeedId(epId);
        Cursor cur = db.query(FEED_TABLE, new String[] {COL_FEED_SPEED}, 
                TextUtils.concat(COL_FEED_ID, Constants.EQUAL_QUESTION)
                    .toString(),
                new String[] {Integer.toString(feedId)}, null, null, null);
        if (cur.moveToFirst()) {
            speed = cur.getFloat(cur.getColumnIndex(COL_FEED_SPEED));
        }
        cur.close();
        return speed;
    }
    
    public synchronized int getNumEpisodes(String feedUrl) {
        Cursor cur = getEpisodes(getFeedId(feedUrl), 0);
        int size = cur.getCount();
        cur.close();
        return size;
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
        String[] sortTypeStrings = new String[] {Constants.ASC, Constants.DESC};
        String sortString = Constants.EMPTY;
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
            sortString = TextUtils.concat(sortString, sortTypeStrings[0])
                    .toString();
            break;
        case 2:
            sortString = TextUtils.concat(sortString, sortTypeStrings[1])
                    .toString();
            break;
        default:
            sortString = TextUtils.concat(sortString, sortTypeStrings[1])
                    .toString();
            break;
        }
        return sortString;
    }
    
    private String getEpisodeSort(int sort, int sortType) {
        String[] sortTypeStrings = new String[] {Constants.ASC, Constants.DESC};
        String sortStringOne = Constants.EMPTY;
        String sortStringTwo = Constants.EMPTY;
        String sortStringThree = Constants.EMPTY;
        String sortString = Constants.EMPTY;
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
            sortString = TextUtils.concat(sortStringOne, sortTypeStrings[0])
                    .toString();
            if (!sortStringTwo.equals(Constants.EMPTY))
                sortString = TextUtils.concat(sortStringOne, sortTypeStrings[0],
                        Constants.COMMA_SPACE, sortStringTwo,
                        sortTypeStrings[0], Constants.COMMA_SPACE,
                        sortStringThree, sortTypeStrings[0]).toString();
            else
                sortString = TextUtils.concat(sortString, Constants.COMMA_SPACE,
                        sortStringThree, sortTypeStrings[0]).toString();
            break;
        case 2:
            sortString = TextUtils.concat(sortStringOne, sortTypeStrings[1])
                    .toString();
            if (!sortStringTwo.equals(Constants.EMPTY))
                sortString = TextUtils.concat(sortStringOne, sortTypeStrings[0],
                        Constants.COMMA_SPACE, sortStringTwo,
                        sortTypeStrings[1], Constants.COMMA_SPACE,
                        sortStringThree, sortTypeStrings[1]).toString();
            else
                sortString = TextUtils.concat(sortString, Constants.COMMA_SPACE,
                        sortStringThree, sortTypeStrings[1]).toString();
            break;
        case 3:
            if (!sortStringTwo.equals(Constants.EMPTY))
                sortString = TextUtils.concat(sortStringOne, sortTypeStrings[1],
                        Constants.COMMA_SPACE, sortStringTwo,
                        sortTypeStrings[0], Constants.COMMA_SPACE,
                        sortStringThree, sortTypeStrings[0]).toString();
            else
                sortString = TextUtils.concat(sortStringOne, sortTypeStrings[0],
                        Constants.COMMA_SPACE, sortStringThree,
                        sortTypeStrings[0]).toString();
            break;
        case 4:
            if (!sortStringTwo.equals(Constants.EMPTY))
                sortString = TextUtils.concat(sortStringOne, sortTypeStrings[1],
                        Constants.COMMA_SPACE, sortStringTwo,
                        sortTypeStrings[1], Constants.COMMA_SPACE,
                        sortStringThree, sortTypeStrings[1]).toString();
            else
                sortString = TextUtils.concat(sortStringOne, sortTypeStrings[1],
                        Constants.COMMA_SPACE, sortStringThree,
                        sortTypeStrings[1]).toString();
            break;
        default:
            sortStringOne = TextUtils.concat(sortStringOne, sortTypeStrings[0])
                    .toString();
            if (!sortStringTwo.equals(Constants.EMPTY))
                sortStringTwo = TextUtils.concat(sortStringTwo,
                        sortTypeStrings[0]).toString();
            sortString = TextUtils.concat(sortStringOne, Constants.COMMA_SPACE,
                    sortStringTwo, sortTypeStrings[0], Constants.COMMA_SPACE,
                    sortStringThree, sortTypeStrings[0]).toString();
            break;
        }
        return sortString;
    }
    
    public String getEpisodeUrl(int id) {
        return getString(id, COL_EPISODE_URL, EPISODE_TABLE, COL_EPISODE_ID);
    }
    
    public String getEpisodeGuid(int id) {
        return getString(id, COL_EPISODE_GUID, EPISODE_TABLE, COL_EPISODE_ID);
    }
    
    // Get data by day, per feed?
    // Sort by feed
    // Sort by date
    // Limit to each day - midnight to midnight
    // Join for all days?
    public synchronized Cursor getAllEpisodes(int read, int downloaded,
            int sort, int sortType, int archive, boolean limit, int offset,
            int total) {
        String filterString = Constants.EMPTY;
        if (archive == 0)
            filterString = TextUtils.concat(filterString,
                    DatabaseHelper.COL_EPISODE_ARCHIVE,
                    Constants.EQUAL_QUESTION).toString();
        if (read != (Constants.READ|Constants.UNREAD)) {
            if (!filterString.equals(Constants.EMPTY))
                filterString = TextUtils.concat(filterString,
                        Constants.AND_SPACES).toString();
            filterString = TextUtils.concat(filterString,
                    DatabaseHelper.COL_EPISODE_READ, Constants.EQUAL_QUESTION)
                            .toString();
        }
        if (downloaded != (Constants.DOWNLOADED|Constants.TO_DOWNLOAD)) {
            if (!filterString.equals(Constants.EMPTY))
                filterString = TextUtils.concat(filterString,
                        Constants.AND_SPACES).toString();
            filterString = TextUtils.concat(filterString,
                    DatabaseHelper.COL_EPISODE_DOWNLOADED,
                    Constants.EQUAL_QUESTION).toString();
        }
        return db.query(EPISODE_TABLE, new String[] {COL_EPISODE_ID, 
                COL_EP_FEED_ID, COL_EP_FEED_TITLE, COL_EP_FEED_LAST_EP_TIME, 
                COL_EPISODE_TITLE, COL_EPISODE_PUB, COL_EPISODE_URL,
                COL_EPISODE_READ, COL_EPISODE_DOWNLOADED, COL_EPISODE_DURATION,
                COL_EPISODE_LOCKED}, filterString, getFilter(read, downloaded,
                    -1, archive), null, null, getEpisodeSort(sort, sortType),
                limit ? TextUtils.concat(Integer.toString(offset),
                        Constants.COMMA_SPACE, Integer.toString(total))
                                .toString() : null);
    }
    
    public synchronized int getEpisodeCount(int read, int downloaded, int sort,
            int sortType, int archive) {
        String filterString = Constants.EMPTY;
        if (archive == 0)
            filterString = TextUtils.concat(filterString,
                    DatabaseHelper.COL_EPISODE_ARCHIVE,
                    Constants.EQUAL_QUESTION).toString();
        if (read != (Constants.READ|Constants.UNREAD)) {
            if (!filterString.equals(Constants.EMPTY))
                filterString = TextUtils.concat(filterString,
                        Constants.AND_SPACES).toString();
            filterString = TextUtils.concat(filterString,
                    DatabaseHelper.COL_EPISODE_READ, Constants.EQUAL_QUESTION)
                            .toString();
        }
        if (downloaded != (Constants.DOWNLOADED|Constants.TO_DOWNLOAD)) {
            if (!filterString.equals(Constants.EMPTY))
                filterString = TextUtils.concat(filterString,
                        Constants.AND_SPACES).toString();
            filterString = TextUtils.concat(filterString,
                    DatabaseHelper.COL_EPISODE_DOWNLOADED,
                    Constants.EQUAL_QUESTION).toString();
        }
        Cursor cur = db.query(EPISODE_TABLE, new String[] {COL_EPISODE_ID, 
                COL_EP_FEED_ID, COL_EP_FEED_TITLE, COL_EP_FEED_LAST_EP_TIME, 
                COL_EPISODE_TITLE, COL_EPISODE_PUB, COL_EPISODE_URL,
                COL_EPISODE_READ, COL_EPISODE_DOWNLOADED, COL_EPISODE_DURATION,
                COL_EPISODE_LOCKED}, filterString, getFilter(read, downloaded,
                    -1, archive), null, null, getEpisodeSort(sort, sortType));
        int count = cur.getCount();
        cur.close();
        return count;
    }
    
    public synchronized Cursor getFilteredFeedEpisodes(int feedId, int read,
            int downloaded, int sort, int sortType, int archive) {
        String filterString = Constants.EMPTY;
        if (archive == 0)
            filterString = TextUtils.concat(filterString,
                    DatabaseHelper.COL_EPISODE_ARCHIVE,
                    Constants.EQUAL_QUESTION).toString();
        if (read != (Constants.READ|Constants.UNREAD)) {
            if (!filterString.equals(Constants.EMPTY))
                filterString = TextUtils.concat(filterString,
                        Constants.AND_SPACES).toString();
            filterString = TextUtils.concat(filterString,
                    DatabaseHelper.COL_EPISODE_READ, Constants.EQUAL_QUESTION)
                            .toString();
        }
        if (downloaded != (Constants.DOWNLOADED|Constants.TO_DOWNLOAD)) {
            if (!filterString.equals(Constants.EMPTY))
                filterString = TextUtils.concat(filterString,
                        Constants.AND_SPACES).toString();
            filterString = TextUtils.concat(filterString,
                    DatabaseHelper.COL_EPISODE_DOWNLOADED,
                    Constants.EQUAL_QUESTION).toString();
        }
        if (!filterString.equals(Constants.EMPTY))
            filterString = TextUtils.concat(filterString, Constants.AND_SPACES)
                    .toString();
        filterString = TextUtils.concat(filterString,
                DatabaseHelper.COL_EP_FEED_ID, Constants.EQUAL_QUESTION)
                        .toString();
        return db.query(EPISODE_TABLE, new String[] {COL_EPISODE_ID,
                COL_EP_FEED_ID, COL_EP_FEED_TITLE, COL_EPISODE_TITLE,
                COL_EPISODE_PUB, COL_EPISODE_URL, COL_EPISODE_READ,
                COL_EPISODE_DOWNLOADED, COL_EPISODE_DURATION,
                COL_EPISODE_LOCKED}, filterString, getFilter(read, downloaded,
                        feedId, archive), null, null,
                getEpisodeSort(sort, sortType));
    }

    public synchronized Cursor getEpisodes(int feedId, int limit) {
        return db.query(EPISODE_TABLE, new String[] {COL_EPISODE_ID, 
                COL_EP_FEED_TITLE, COL_EP_FEED_LAST_EP_TIME, COL_EPISODE_TITLE, 
                COL_EPISODE_PUB, COL_EPISODE_URL, COL_EPISODE_READ, 
                COL_EPISODE_DOWNLOADED}, TextUtils.concat(COL_EP_FEED_ID,
                        Constants.EQUAL_QUESTION).toString(), 
                new String[] {Integer.toString(feedId)}, null, null, 
                TextUtils.concat(COL_EPISODE_PUB, Constants.DESC).toString(),
                (limit > 0 ? Integer.toString(limit) : null));
    }
    
    public ArrayList<Integer> getReadEpisodes(int feedId, 
            boolean read, int archive) {
        String filterString = TextUtils.concat(COL_EP_FEED_ID,
                Constants.EQUAL_QUESTION, Constants.AND_SPACES,
                COL_EPISODE_READ, Constants.EQUAL_QUESTION).toString();
        String[] argArray = new String[] {Integer.toString(feedId), 
                Integer.toString(read ? Constants.UNREAD : Constants.READ)};
        if (archive == 0) {
            filterString = TextUtils.concat(filterString, Constants.AND_SPACES,
                    DatabaseHelper.COL_EPISODE_ARCHIVE,
                    Constants.EQUAL_QUESTION).toString();
            argArray = new String[] {Integer.toString(feedId), 
                    Integer.toString(read ? Constants.UNREAD : Constants.READ),
                    Constants.ZERO};
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
                TextUtils.concat(COL_EP_FEED_ID, Constants.EQUAL_QUESTION)
                        .toString(), new String[] {Integer.toString(feedId)}, 
                null, null, TextUtils.concat(COL_EPISODE_PUB, Constants.DESC)
                        .toString());
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
                TextUtils.concat(COL_EP_FEED_ID, Constants.EQUAL_QUESTION,
                        Constants.AND_SPACES, COL_EPISODE_RETRY_SYNC,
                        Constants.EQUAL_QUESTION).toString(),
                new String[] {Integer.toString(feedId), Constants.ONE}, null,
                null, TextUtils.concat(COL_EPISODE_PUB, Constants.DESC)
                        .toString());
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
    
    public synchronized boolean hasReadEpisodes(int feedId, boolean read) {
        Cursor cur = db.query(EPISODE_TABLE, new String[] {COL_EPISODE_PUB}, 
                TextUtils.concat(COL_EP_FEED_ID, Constants.EQUAL_QUESTION,
                        Constants.AND_SPACES, COL_EPISODE_READ,
                        Constants.EQUAL_QUESTION).toString(), 
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
                TextUtils.concat(COL_EP_FEED_ID, Constants.EQUAL_QUESTION,
                        Constants.AND_SPACES, COL_EPISODE_DOWNLOADED,
                        Constants.EQUAL_QUESTION).toString(), 
                new String[] {Integer.toString(feedId), 
                    Integer.toString(Constants.DOWNLOADED)}, 
                null, null, null);
        boolean hasDownload = false;
        if (cur.moveToFirst())
            hasDownload = true;
        cur.close();
        return hasDownload;
    }
    
    public synchronized String[] getEpisode(int epId) {
        String[] episodeArray = new String[6];
        Cursor cur =  db.query(EPISODE_TABLE, new String[] {COL_EPISODE_TITLE, 
                COL_EP_FEED_TITLE, COL_EPISODE_SUMMARY, COL_EP_FEED_ID,
                COL_EPISODE_DURATION}, TextUtils.concat(COL_EPISODE_ID,
                        Constants.EQUAL_QUESTION).toString(),
                new String[] {Integer.toString(epId)}, null, null, null);
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
            episodeArray[5] = Long.toString(cur.getLong(cur.getColumnIndex(
                    DatabaseHelper.COL_EPISODE_DURATION)));
        }
        else
            episodeArray = null;
        cur.close();
        return episodeArray;
    }
    
    public synchronized String[] getFeed(int feedId) {
        String[] feedArray = new String[4];
        Cursor cur =  db.query(FEED_TABLE, new String[] {COL_FEED_TITLE, 
                COL_FEED_DESCRIPTION, COL_FEED_LAST_EP_TIME,
                COL_FEED_LAST_UPDATE}, TextUtils.concat(COL_FEED_ID,
                        Constants.EQUAL_QUESTION).toString(),
                new String[] {Integer.toString(feedId)}, null, null, null);
        if (cur.moveToFirst()) {
            feedArray[0] = cur.getString(cur.getColumnIndex(
                    DatabaseHelper.COL_FEED_TITLE));
            feedArray[1] = cur.getString(cur.getColumnIndex(
                    DatabaseHelper.COL_FEED_DESCRIPTION));
            feedArray[2] = TextUtils.concat("Latest episode: ",
                    Util.getDateString(cur.getLong(cur.getColumnIndex(
                            DatabaseHelper.COL_FEED_LAST_EP_TIME)))).toString();
            feedArray[3] = TextUtils.concat("Last updated: ",
                    Util.getDateString(cur.getLong(cur.getColumnIndex(
                            DatabaseHelper.COL_FEED_LAST_UPDATE)))).toString();
        }
        else
            feedArray = null;
        cur.close();
        return feedArray;
    }

    public synchronized long getFeedLastEpTime(int feedId) {
        Cursor cur = db.query(FEED_TABLE,
                new String[] { COL_FEED_LAST_EP_TIME }, TextUtils.concat(
                        COL_FEED_ID, Constants.EQUAL_QUESTION).toString(),
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
                new String[] { COL_EPISODE_POSITION }, TextUtils.concat(
                        COL_EPISODE_ID, Constants.EQUAL_QUESTION).toString(),
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
                new String[] { COL_EPISODE_DURATION }, TextUtils.concat(
                        COL_EPISODE_ID, Constants.EQUAL_QUESTION).toString(),
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
        Cursor cur = db.rawQuery(TextUtils.concat("SELECT * FROM ", API_TABLE)
                .toString(), null);
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
    
    public int feedIsPodcast(String feedUrl) {
        return getInt(feedUrl, COL_FEED_PODCAST, FEED_TABLE, 
                COL_FEED_ADDRESS);
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
        Cursor cur = db.rawQuery(TextUtils.concat(Constants.SELECT,
                COL_FEED_ADDRESS, Constants.FROM, FEED_TABLE, Constants.WHERE,
                COL_FEED_PODCAST, "=? ORDER BY ", COL_FEED_TITLE, Constants.ASC)
                        .toString(),
                new String[] {Integer.toString(isPodcast)});
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
        Cursor cur = db.rawQuery(TextUtils.concat(Constants.SELECT, columnId,
                Constants.FROM, table, Constants.WHERE, columnName,
                Constants.EQUAL_QUESTION).toString(), new String[] {name});
        int bookmarkId = -1;
        if (cur.moveToFirst())
            bookmarkId = cur.getInt(cur.getColumnIndex(columnId));
        cur.close();
        return bookmarkId;
    }
    
    public synchronized int getInt(int id, String columnId, String table,
            String columnName) {
        Cursor cur = db.query(table, new String[] {columnId}, TextUtils.concat(
                columnName, Constants.EQUAL_QUESTION).toString(),
                new String[] {Integer.toString(id)}, null, null, null);
        int bookmarkId = -1;
        if (cur.moveToFirst())
            bookmarkId = cur.getInt(cur.getColumnIndex(columnId));
        cur.close();
        return bookmarkId;
    }

    public synchronized long getLong(String name, String columnId, String table,
            String columnName) {
        Cursor cur = db.rawQuery(TextUtils.concat(Constants.SELECT, columnId,
                Constants.FROM, table, Constants.WHERE, columnName,
                Constants.EQUAL_QUESTION).toString(), new String[] {name});
        long bookmarkId = -1;
        if (cur.moveToFirst())
            bookmarkId = cur.getLong(cur.getColumnIndex(columnId));
        cur.close();
        return bookmarkId;
    }
    
    public synchronized long getFeedMagicInterval(String feedAddress) {
        Cursor cur = db.query(FEED_TABLE, 
                new String[] {COL_FEED_MAGIC_INTERVAL}, TextUtils.concat(
                        COL_FEED_ADDRESS, Constants.EQUAL_QUESTION).toString(),
                new String[] {feedAddress}, null, null, null);
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
        updateRecord(cv, FEED_TABLE, TextUtils.concat(COL_FEED_ADDRESS,
                Constants.EQUAL, DatabaseUtils.sqlEscapeString(feedAddress))
                        .toString());
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
        Cursor cur = db.rawQuery(TextUtils.concat(Constants.SELECT,
                columnString, Constants.FROM, table, Constants.WHERE,
                columnName, Constants.EQUAL_QUESTION).toString(),
                new String[] {name});
        String appPackage = null;
        if (cur.moveToFirst())
            appPackage = cur.getString(cur.getColumnIndex(columnString));
        cur.close();
        return appPackage;
    }
    
    public synchronized String getString(int id, String columnString,
            String table, String columnName) {
        Cursor cur = db.rawQuery(TextUtils.concat(Constants.SELECT,
                columnString, Constants.FROM, table, Constants.WHERE,
                columnName, Constants.EQUAL_QUESTION).toString(), 
                new String[] {Integer.toString(id)});
        String appPackage = null;
        if (cur.moveToFirst())
            appPackage = cur.getString(cur.getColumnIndex(columnString));
        cur.close();
        return appPackage;
    }
    
    public synchronized void deleteFeed(int feedId) {
        Cursor cur = this.getEpisodes(feedId, 0);
        if (cur.moveToFirst()) {
            do {
                db.delete(EPISODE_TABLE, TextUtils.concat(COL_EPISODE_ID,
                        Constants.EQUAL_QUESTION).toString(), 
                        new String[] {Integer.toString(cur.getInt(
                                cur.getColumnIndex(COL_EPISODE_ID)))});
            } while (cur.moveToNext());
        }
        cur.close();
        db.delete(FEED_TABLE, TextUtils.concat(COL_FEED_ID,
                Constants.EQUAL_QUESTION).toString(), 
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

    public boolean addFeed(ContentValues cv, int sortOrder) {
        String feedAddress = cv.getAsString(COL_FEED_ADDRESS);
        if (inDb(new String[] {feedAddress}, FEED_TABLE, 
                new String[] {COL_FEED_ADDRESS})) {
            updateRecord(cv, FEED_TABLE, TextUtils.concat(COL_FEED_ADDRESS,
                    Constants.EQUAL, DatabaseUtils.sqlEscapeString(
                            cv.getAsString(COL_FEED_ADDRESS))).toString());
            return true;
        } else {
            cv.put(COL_FEED_SORT, sortOrder);
         // TODO Make this a setting
            cv.put(COL_FEED_UNREAD, Constants.READ);
            cv.put(COL_FEED_DOWNLOADED, Constants.TO_DOWNLOAD);
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
        //new File(epLoc).mkdirs();
        ContentValues feedCv = new ContentValues();
        long feedLastEpTime = getFeedLastEpTime(feedId);
        long epPub = cv.getAsLong(COL_EPISODE_PUB); 
        feedCv.put(COL_FEED_LAST_EP_TIME, 
                epPub > feedLastEpTime ? epPub : feedLastEpTime);
        MagicType magicType = calculateMagicIntervalNew(feedId);
        feedCv.put(COL_FEED_MAGIC_INTERVAL, magicType.getTime());
        feedCv.put(COL_FEED_MAGIC_TYPE, magicType.getType());
        int episodeRead = cv.getAsInteger(DatabaseHelper.COL_EPISODE_READ);
        if (episodeRead == Constants.UNREAD)
            feedCv.put(COL_FEED_UNREAD, Constants.UNREAD);
        feedCv.put(COL_FEED_DOWNLOADED, hasDownloadedEpisode(feedId) ?
                Constants.DOWNLOADED : Constants.TO_DOWNLOAD);
        updateRecord(feedCv, FEED_TABLE, TextUtils.concat(COL_FEED_ID,
                Constants.EQUAL, Integer.toString(feedId)).toString());
        cv.put(COL_EP_FEED_ID, feedId);
        cv.put(COL_EP_FEED_TITLE, Jsoup.parse(StringEscapeUtils.unescapeHtml4(
                feedTitle)).text());
        cv.put(COL_EP_FEED_LAST_EP_TIME, getFeedLastEpTime(feedId));
        insertRecord(cv, EPISODE_TABLE, COL_EPISODE_URL);
        int epId = getInt(cv.getAsString(COL_EPISODE_URL), COL_EPISODE_ID, 
                EPISODE_TABLE, COL_EPISODE_URL);
        String epFile = TextUtils.concat(
                TextUtils.concat(ApplicationEx.cacheLocation,
                Constants.FEEDS_LOCATION, Integer.toString(feedId),
                File.separator).toString(), Integer.toString(epId), ext)
                .toString();
        cv.clear();
        cv.put(COL_EPISODE_LOCATION, epFile);
        updateRecord(cv, EPISODE_TABLE, TextUtils.concat(COL_EPISODE_ID,
                Constants.EQUAL, Integer.toString(epId)).toString());
    }
    
    public enum FeedPublishType {
        Semidaily,
        Daily,
        Weekday,
        Semiweekly,
        Weekly
    }
    
    private TreeMap<Integer, Long> getMedianDayTime(long nowDay,
            TreeMap<Long, Integer> timeMap) {
        TreeMap<Integer, Long> sortMap = new TreeMap<Integer, Long>();
        long newTime;
        for (Entry<Long, Integer> entry : timeMap.entrySet()) {
            newTime = entry.getKey();
            if (newTime < nowDay)
                newTime += Constants.DAY_MILLI;
            sortMap.put(entry.getValue(), newTime);
        }
        return sortMap;
    }
    
    private TreeMap<Integer, Long> getMedianDiff(
            TreeMap<Long, Integer> diffMap) {
        TreeMap<Integer, Long> sortMap = new TreeMap<Integer, Long>();
        for (Entry<Long, Integer> entry : diffMap.entrySet()) {
            sortMap.put(entry.getValue(), entry.getKey());
        }
        return sortMap;
    }
    
    private TreeMap<Integer, Integer> getMedianDay(int day,
            TreeMap<Integer, Integer> dayMap) {
        TreeMap<Integer, Integer> sortMap = new TreeMap<Integer, Integer>();
        Integer currDay;
        int oldDiff;
        int newDiff;
        for (Entry<Integer, Integer> entry : dayMap.entrySet()) {
            currDay = sortMap.get(entry.getValue());
            if (currDay == null)
                currDay = entry.getKey();
            oldDiff = currDay - day;
            if (oldDiff < 1)
                oldDiff += 7;
            newDiff = entry.getKey() - day;
            if (newDiff < 1)
                newDiff += 7;
            currDay = oldDiff < newDiff ? currDay : entry.getKey();
            sortMap.put(entry.getValue(), currDay);
        }
        return sortMap;
    }
    
    public MagicType calculateMagicIntervalNew(int feedId) {
        // When this feed is next due to update
        long magicUpdateTime = 0;
        // Keeps track of how many of each time happen
        TreeMap<Long, Integer> timeMap = new TreeMap<Long, Integer>();
        // Keeps track of how many of each day happen
        TreeMap<Integer, Integer> dayMap = new TreeMap<Integer, Integer>();
        // Keep track of the differences between episodes
        TreeMap<Long, Integer> diffMap = new TreeMap<Long, Integer>();
        // Default publish type is daily
        FeedPublishType publishType = FeedPublishType.Daily;
        // Get episodes of feed, ordered by published date, newest first
        Cursor cur = getEpisodes(feedId, 0);
        // Current episode published time
        long currPub = -1;
        long prevPub = -1;
        // Default interval from preferences
        long preference = Constants.MINUTE_MILLI * Integer.parseInt(
                Util.readStringPreference(R.string.interval_key,
                        Constants.SIXTY));
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // The newest episode time
        long latestPub = 0;
        // Keep current day of the week
        int dayOfWeek;
        // Keep track of how many of the current one
        Integer count;
        long dayTime;
        long prefTime;
        long diff;
        if (cur.moveToFirst()) {
            // Store newest one for later
            latestPub = cur.getLong(cur.getColumnIndex(COL_EPISODE_PUB));
            do {
                // Store the current one
                currPub = cur.getLong(cur.getColumnIndex(COL_EPISODE_PUB));
                // Put it in a calendar
                cal.setTimeInMillis(currPub);
                // Grab day of week for current one
                dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                // Get how many are on this day
                count = dayMap.get(dayOfWeek);
                if (count == null)
                    count = 0;
                // Increment and store how many are on this day
                dayMap.put(dayOfWeek, ++count);
                // Get the time of day for current one
                dayTime = cal.getTimeInMillis() % Constants.DAY_MILLI;
                // Use interval preference to find consistencies
                prefTime = (dayTime - (dayTime % preference));
                // Get how many are at this time
                count = timeMap.get(prefTime);
                if (count == null)
                    count = 0;
                // Increment and store
                timeMap.put(prefTime, ++count);
                if (prevPub > 0) {
                    diff = prevPub - currPub;
                    diff = diff - (diff % preference);
                    count = diffMap.get(diff);
                    if (count == null)
                        count = 0;
                    diffMap.put(diff, ++count);
                }
                prevPub = currPub;
            } while (cur.moveToNext());
        }
        // Find how many episodes published per week, on average
        int weeklyAvg = (int) (cur.getCount() /
                ((latestPub - currPub) / Constants.WEEK_MILLI));
        long avg = (latestPub - currPub) / cur.getCount();
        avg = avg - avg % preference;
        Log.v(Constants.LOG_TAG, "episodes per week: " + weeklyAvg);
        Log.v(Constants.LOG_TAG, "days per week: " + dayMap.size());
        // Find how many days have episodes and use whichever is fewer
        int interval = weeklyAvg < dayMap.size() ? weeklyAvg : dayMap.size();
        switch (interval) {
        case 0:
        case 1:
        case 2:
        case 3:
        case 4:
            publishType = FeedPublishType.Weekly;
            break;
        case 5:
        case 6:
            publishType = FeedPublishType.Weekday;
            break;
        }
        TreeMap<Integer, Long> sortedTimes;
        TreeMap<Integer, Integer> sortedDays;
        cal.setTimeInMillis(now);
        dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        Entry<Integer, Long> currEntry;
        long currValue = -1;
        switch (publishType) {
        case Semidaily:
            // Get the median time
            // Remove that time from the map
            // Get median time again
            // Use which ever is next
            break;
        case Daily:
            sortedTimes = getMedianDayTime(
                    now % Constants.DAY_MILLI, timeMap);
            // Get the median time
            cal.setTimeInMillis(now - (now % Constants.DAY_MILLI));
            // Find the actual time to set as the update time
            do {
                currEntry = sortedTimes.pollLastEntry();
                currValue = currEntry.getValue();
                magicUpdateTime = cal.getTimeInMillis() + currValue;
            } while ((magicUpdateTime - latestPub < avg ||
                    magicUpdateTime - now > avg) && !sortedTimes.isEmpty());
            if (magicUpdateTime < now)
                magicUpdateTime = now + preference;
            break;
        case Weekday:
            // Determine if there are multiple episodes in a day
            boolean multiple = weeklyAvg >= (dayMap.size()*1.5);
            // Find the most common time difference between episodes published
            TreeMap<Integer, Long> diffSorted = this.getMedianDiff(diffMap);
            do {
                avg = diffSorted.pollLastEntry().getValue();
            } while (avg == 0);
            // Find the date of the last published episode
            cal.setTimeInMillis(latestPub);
            int latestDate = cal.get(Calendar.DAY_OF_YEAR);
            // Get published times (within the day) sorted by number of instances
            sortedTimes = getMedianDayTime(
                    now % Constants.DAY_MILLI, timeMap);
            // Move the calendar to midnight on the next weekday
            cal.setTimeInMillis(now - (now % Constants.DAY_MILLI));
            while (cal.get(Calendar.DAY_OF_WEEK) < 2 ||
                    cal.get(Calendar.DAY_OF_WEEK) > 6) {
                cal.add(Calendar.DAY_OF_WEEK, 1);
            }
            // Use the next updated time (within the day) to update
            if (multiple) {
                Entry<Long, Integer> entry;
                do {
                    entry = timeMap.pollFirstEntry();
                    magicUpdateTime = cal.getTimeInMillis() + entry.getKey();
                } while (magicUpdateTime < now && !timeMap.isEmpty());
            }
            // Use the most common time (within the day) to find the next update
            else {
                do {
                    currEntry = sortedTimes.pollLastEntry();
                    currValue = currEntry.getValue();
                    magicUpdateTime = cal.getTimeInMillis() + currValue;
                } while ((magicUpdateTime - latestPub < avg ||
                        magicUpdateTime - now > avg) &&
                        !sortedTimes.isEmpty());
                if (magicUpdateTime - latestPub >= avg)
                    magicUpdateTime = latestPub + Constants.DAY_MILLI;
            }
            if (magicUpdateTime < now)
                magicUpdateTime = now + preference;
            break;
        case Semiweekly:
            // Get the median time
            // Get the 2 median days
            break;
        case Weekly:
            cal.setTimeInMillis(latestPub);
            latestDate = cal.get(Calendar.DAY_OF_YEAR);
            sortedTimes = getMedianDayTime(0, timeMap);
            cal.setTimeInMillis(now - (now % Constants.DAY_MILLI));
            sortedDays = getMedianDay(cal.get(Calendar.DAY_OF_WEEK), dayMap);
            int updateDay = sortedDays.pollLastEntry().getValue();
            while (cal.get(Calendar.DAY_OF_WEEK) != updateDay) {
                cal.add(Calendar.DAY_OF_WEEK, 1);
            }
            if (cal.get(Calendar.DAY_OF_YEAR) == latestDate)
                cal.add(Calendar.WEEK_OF_YEAR, 1);
            do {
                currEntry = sortedTimes.pollLastEntry();
                currValue = currEntry.getValue();
                magicUpdateTime = cal.getTimeInMillis() + currValue;
            } while ((magicUpdateTime - latestPub < avg ||
                    magicUpdateTime - now > avg) && !sortedTimes.isEmpty());
            if (magicUpdateTime - latestPub >= avg)
                magicUpdateTime = latestPub + Constants.DAY_MILLI;
            if (magicUpdateTime < now)
                magicUpdateTime = now + preference;
            break;
        }
        cur.close();
        return new MagicType(magicUpdateTime, publishType.toString());
    }
    
    public class MagicType {
        long time;
        String type;
        
        public MagicType(long time, String type) {
            this.time = time;
            this.type = type;
        }
        
        public long getTime() {
            return time;
        }
        
        public String getType() {
            return type;
        }
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
                TextUtils.concat(COL_EPISODE_URL, Constants.EQUAL_QUESTION)
                        .toString(), new String[] { url }, null, null, null);
        String type = Constants.EMPTY;
        if (cur.moveToFirst())
            type = cur.getString(cur.getColumnIndex(COL_EPISODE_TYPE));
        cur.close();
        return type;
    }

    public synchronized String extFromEpisodeUrl(String url) {
        Cursor cur = db.query(EPISODE_TABLE, new String[] { COL_EPISODE_TYPE },
                TextUtils.concat(COL_EPISODE_URL, Constants.EQUAL_QUESTION)
                        .toString(), new String[] { url }, null, null, null);
        String type = Constants.EMPTY;
        if (cur.moveToFirst())
            type = cur.getString(cur.getColumnIndex(COL_EPISODE_TYPE));
        cur.close();
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
    
    public synchronized String extFromEpisodeId(int id) {
        Cursor cur = db.query(EPISODE_TABLE, new String[] { COL_EPISODE_TYPE },
                TextUtils.concat(COL_EPISODE_ID, Constants.EQUAL_QUESTION)
                        .toString(), new String[] {Integer.toString(id)}, null,
                null, null);
        String type = Constants.EMPTY;
        if (cur.moveToFirst())
            type = cur.getString(cur.getColumnIndex(COL_EPISODE_TYPE));
        cur.close();
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
    
    public boolean addFailedTry(String url) {
        ContentValues cv = new ContentValues();
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
                    updateRecord(cv, EPISODE_TABLE, TextUtils.concat(
                            COL_EPISODE_URL, Constants.EQUAL,
                            DatabaseUtils.sqlEscapeString(url)).toString());
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
                updateRecord(cv, FEED_TABLE, TextUtils.concat(COL_FEED_ADDRESS,
                        Constants.EQUAL, DatabaseUtils.sqlEscapeString(url))
                                .toString());
                return true;
            }
        }
    }
    
    public void setFailedSync(int epId) {
        ContentValues cv = new ContentValues();
        cv.put(COL_EPISODE_RETRY_SYNC, 1);
        updateRecord(cv, EPISODE_TABLE, TextUtils.concat(COL_EPISODE_ID,
                Constants.EQUAL, Integer.toString(epId)).toString());
    }
    
    public void resetFailedSync(int epId) {
        ContentValues cv = new ContentValues();
        cv.put(COL_EPISODE_RETRY_SYNC, 0);
        updateRecord(cv, EPISODE_TABLE, TextUtils.concat(COL_EPISODE_ID,
                Constants.EQUAL, Integer.toString(epId)).toString());
    }
    
    public boolean getFailedSync(String epUrl) {
        if (getInt(epUrl, COL_EPISODE_RETRY_SYNC, EPISODE_TABLE, 
                COL_EPISODE_URL) >= 1)
            return true;
        else
            return false;
    }
    
    public boolean getEpisodeLocked(String epUrl) {
        if (getInt(epUrl, COL_EPISODE_LOCKED, EPISODE_TABLE, COL_EPISODE_URL)
                == 1)
            return true;
        else
            return false;
    }
    
    public void setEpisodeLocked(String epUrl, boolean locked) {
        ContentValues cv = new ContentValues();
        cv.put(COL_EPISODE_LOCKED, locked ? 1 : 0);
        updateRecord(cv, EPISODE_TABLE, TextUtils.concat(COL_EPISODE_URL,
                Constants.EQUAL, DatabaseUtils.sqlEscapeString(epUrl))
                        .toString());
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
        long result = updateRecord(cv, FEED_TABLE, TextUtils.concat(
                COL_FEED_ADDRESS, Constants.EQUAL,
                DatabaseUtils.sqlEscapeString(url)).toString());
        if (result == 0) {
            cv.clear();
            cv.put(COL_EPISODE_FAILED_TRIES, 0);
            result = updateRecord(cv, EPISODE_TABLE, TextUtils.concat(
                    COL_EPISODE_URL, Constants.EQUAL,
                    DatabaseUtils.sqlEscapeString(url)).toString());
            if (result == 0)
                return false;
            else
                return true;
        }
        else
            return true;
    }
    
    @SuppressLint("NewApi")
    public boolean markRead(int epId, boolean read) {
        ContentValues cv = new ContentValues();
        if (read) {
            cv.put(COL_EPISODE_READ, Constants.READ);
        }
        else
            cv.put(COL_EPISODE_READ, Constants.UNREAD);
        long result = updateRecord(cv, EPISODE_TABLE, TextUtils.concat(
                COL_EPISODE_ID, Constants.EQUAL, Integer.toString(epId))
                        .toString());
        int feedId = getFeedId(epId);
        if (read) {
            if (hasReadEpisodes(feedId, !read)) {
                cv.clear();
                cv.put(COL_FEED_UNREAD, Constants.UNREAD);
                updateRecord(cv, FEED_TABLE, TextUtils.concat(COL_FEED_ID,
                        Constants.EQUAL, Integer.toString(feedId)).toString());
            }
            else {
                cv.clear();
                cv.put(COL_FEED_UNREAD, Constants.READ);
                updateRecord(cv, FEED_TABLE, TextUtils.concat(COL_FEED_ID,
                        Constants.EQUAL, Integer.toString(feedId)).toString());
            }
        }
        else {
            cv.clear();
            cv.put(COL_FEED_UNREAD, Constants.UNREAD);
            updateRecord(cv, FEED_TABLE, TextUtils.concat(COL_FEED_ID,
                    Constants.EQUAL, Integer.toString(feedId)).toString());
        }
        if (hasDownloadedEpisode(feedId)) {
            cv.clear();
            cv.put(COL_FEED_DOWNLOADED, Constants.DOWNLOADED);
            updateRecord(cv, FEED_TABLE, TextUtils.concat(COL_FEED_ID,
                    Constants.EQUAL, Integer.toString(feedId)).toString());
        }
        else {
            cv.clear();
            cv.put(COL_FEED_DOWNLOADED, Constants.TO_DOWNLOAD);
            updateRecord(cv, FEED_TABLE, TextUtils.concat(COL_FEED_ID,
                    Constants.EQUAL, Integer.toString(feedId)).toString());
        }
        if (ApplicationEx.isSyncing() && Util.readBooleanFromFile(
                Constants.GOOGLE_FILENAME)) {
            // TODO
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
        ContentValues cv = new ContentValues();
        cv.put(COL_EPISODE_READ, read ? Constants.READ : Constants.UNREAD);
        updateRecord(cv, EPISODE_TABLE, TextUtils.concat(COL_EP_FEED_ID,
                Constants.EQUAL, Integer.toString(feedId)).toString());
        cv.clear();
        cv.put(COL_FEED_UNREAD, read ? Constants.READ : Constants.UNREAD);
        updateRecord(cv, FEED_TABLE, TextUtils.concat(COL_FEED_ID,
                Constants.EQUAL, Integer.toString(feedId)).toString());
        return epList;
    }
    
    public void updateFeedRead(int feedId) {
        ContentValues cv = new ContentValues();
        if (hasReadEpisodes(feedId, false))
            cv.put(COL_FEED_UNREAD, Constants.UNREAD);
        else
            cv.put(COL_FEED_UNREAD, Constants.READ);
        updateRecord(cv, FEED_TABLE, TextUtils.concat(COL_FEED_ID,
                Constants.EQUAL, Integer.toString(feedId)).toString());
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
                TextUtils.concat(COL_EPISODE_ID, Constants.EQUAL_QUESTION,
                        Constants.AND_SPACES, COL_EPISODE_FAILED_TRIES,
                        Constants.GREATER).toString(), 
                new String[] {Integer.toString(id), Constants.ZERO}, null, null,
                null);
        boolean failed = cur.getCount() > 0;
        cur.close();
        return failed;
    }
    
    public String getFeedTitle(String url) {
        return getString(url, COL_FEED_TITLE, FEED_TABLE, COL_FEED_ADDRESS);
    }
    
    public String getFeedUrl(String episodeUrl) {
        int feedId = getEpisodeFeedId(getEpisodeId(episodeUrl));
        return getString(Integer.toString(feedId), COL_FEED_ADDRESS, FEED_TABLE,
                COL_FEED_ID);
    }
    
    public String getEpisodeLocation(String url) {
        String location = getString(url, COL_EPISODE_LOCATION, EPISODE_TABLE, 
                COL_EPISODE_URL);
        if (location == null) {
            int epId = getEpisodeId(url);
            int feedId = getEpisodeFeedId(epId);
            String ext = extFromEpisodeUrl(url);
            location = TextUtils.concat(ApplicationEx.cacheLocation,
                    Constants.FEEDS_LOCATION, Integer.toString(feedId),
                    File.separator, Integer.toString(epId), ext).toString();
        }
        return location;
    }
    
    public String getEpisodeLocation(int id) {
        String location = getString(Integer.toString(id), COL_EPISODE_LOCATION, 
                EPISODE_TABLE, COL_EPISODE_ID);
        if (location == null) {
            int feedId = getEpisodeFeedId(id);
            String ext = extFromEpisodeId(id);
            location = TextUtils.concat(ApplicationEx.cacheLocation,
                    Constants.FEEDS_LOCATION, Integer.toString(feedId),
                    File.separator, Integer.toString(id), ext).toString();
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
        String path = TextUtils.concat(ApplicationEx.cacheLocation,
                Constants.FEEDS_LOCATION, Integer.toString(id), File.separator)
                        .toString();
        if (Util.findFile(path, TextUtils.concat(Integer.toString(id),
                Constants.PNG).toString()))
            return TextUtils.concat(path, Integer.toString(id), Constants.PNG)
                    .toString();
        else if (Util.findFile(path, TextUtils.concat(Integer.toString(id),
                Constants.JPG).toString()))
            return TextUtils.concat(path, Integer.toString(id), Constants.JPG)
                    .toString();
        else if (Util.findFile(path, TextUtils.concat(Integer.toString(id),
                Constants.GIF).toString()))
            return TextUtils.concat(path, Integer.toString(id), Constants.GIF)
                    .toString();
        else
            return Constants.EMPTY;
    }
    
    public String getSmallFeedImageLocation(int id) {
        String path = TextUtils.concat(ApplicationEx.cacheLocation,
                Constants.FEEDS_LOCATION, Integer.toString(id), File.separator)
                        .toString();
        if (Util.findFile(path, TextUtils.concat(Integer.toString(id),
                Constants.SMALL, Constants.PNG).toString()))
            return TextUtils.concat(path, Integer.toString(id), Constants.SMALL,
                    Constants.PNG).toString();
        else
            return Constants.EMPTY;
    }
    
    public synchronized int getFeedCount() {
        Cursor cur = db.rawQuery(TextUtils.concat("SELECT COUNT(*) FROM ",
                FEED_TABLE).toString(), new String[] {});
        int count = 0;
        if (cur.moveToFirst())
            count = cur.getInt(0);
        cur.close();
        return count;
    }
    
    public void feedDownloaded(int feedId, boolean isDownloaded) {
        ContentValues cv = new ContentValues();
        if (isDownloaded)
            cv.put(COL_FEED_DOWNLOADED, Constants.DOWNLOADED);
        else
            cv.put(COL_FEED_DOWNLOADED, Constants.TO_DOWNLOAD);
        updateRecord(cv, FEED_TABLE, TextUtils.concat(COL_FEED_ID,
                Constants.EQUAL, Integer.toString(feedId)).toString());
    }
    
    public void episodeDownloaded(int episodeId, boolean isDownloaded) {
        ContentValues cv = new ContentValues();
        if (isDownloaded)
            cv.put(COL_EPISODE_DOWNLOADED, Constants.DOWNLOADED);
        else
            cv.put(COL_EPISODE_DOWNLOADED, Constants.TO_DOWNLOAD);
        updateRecord(cv, EPISODE_TABLE, TextUtils.concat(COL_EPISODE_ID,
                Constants.EQUAL, Integer.toString(episodeId)).toString());
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
                TextUtils.concat(COL_EP_FEED_ID, Constants.EQUAL_QUESTION,
                        Constants.AND_SPACES, COL_EPISODE_READ,
                        Constants.EQUAL_QUESTION, Constants.AND_SPACES,
                        COL_EPISODE_DOWNLOADED, Constants.EQUAL_QUESTION)
                                .toString(),
                new String[] {Integer.toString(feedId), Integer.toString(read), 
                        Integer.toString(downloaded)}, null, null, null);
        int count = cur.getCount();
        cur.close();
        return count;
    }
    
    public synchronized ArrayList<String> getNewFeeds() {
        ArrayList<String> newFeeds = new ArrayList<String>();
        Cursor cur = db.query(FEED_TABLE, new String[] {COL_FEED_ADDRESS}, null, 
                null, null, null, TextUtils.concat(COL_FEED_ADDRESS,
                        Constants.DESC).toString());
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
                TextUtils.concat(COL_EP_FEED_ID, Constants.EQUAL_QUESTION,
                        Constants.AND_SPACES, COL_EPISODE_GOOGLE_ID,
                        Constants.EQUAL_QUESTION).toString(), 
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
    
    private class FeedMagic {
        public String feedAddress;
        public long magicInterval;
        public long lastUpdate;
        public long nextUpdate;
        
        private FeedMagic(String feedAddress, long lastUpdate,
                long magicInterval, long nextUpdate) {
            this.feedAddress = feedAddress;
            this.magicInterval = magicInterval;
            this.lastUpdate = lastUpdate;
            this.nextUpdate = nextUpdate;
        }
    }
    
    public synchronized Magics getNextMagicInterval() {
        Cursor cur = db.query(FEED_TABLE, new String[] {COL_FEED_MAGIC_INTERVAL, 
                    COL_FEED_ADDRESS, COL_FEED_LAST_UPDATE, 
                    COL_FEED_LAST_EP_TIME},
                    TextUtils.concat(COL_FEED_MAGIC_INTERVAL, Constants.GREATER)
                            .toString(), new String[] {Constants.NEG_ONE}, null,
                    null, TextUtils.concat(COL_FEED_MAGIC_INTERVAL,
                            Constants.ASC).toString());
        long magicInterval = 0;
        long lastUpdateTime = 0;
        long nextUpdateTime = 0;
        long lastEpTime = 0;
        long nextFeedTime = Constants.PLUS_MULTI;
        String feedAddress = null;
        ArrayList<String> addresses = new ArrayList<String>();
        long intervalSetting = Constants.MINUTE_MILLI * Integer.parseInt(
                Util.readStringPreference(R.string.interval_key, Constants.SIXTY));
        Magics currMagics = new Util.Magics();
        ArrayList<FeedMagic> feedMagics = new ArrayList<FeedMagic>();
        if (cur.moveToFirst()) {
            do {
                feedAddress = cur.getString(cur.getColumnIndex(
                        COL_FEED_ADDRESS));
                lastUpdateTime = cur.getLong(cur.getColumnIndex(
                        COL_FEED_LAST_UPDATE));
                magicInterval = cur.getLong(cur.getColumnIndex(
                        COL_FEED_MAGIC_INTERVAL));
                lastEpTime = cur.getLong(cur.getColumnIndex(
                        COL_FEED_LAST_EP_TIME));
                //Log.e(Constants.LOG_TAG, feedAddress + ": " + lastEpTime);
                nextUpdateTime = magicInterval;
                if (lastUpdateTime >= nextUpdateTime)
                    nextUpdateTime = lastUpdateTime + intervalSetting;
                if (nextUpdateTime <= nextFeedTime) {
                    nextFeedTime = nextUpdateTime;
                    //Log.w(Constants.LOG_TAG, "nextFeedTime: ");
                    //Log.w(Constants.LOG_TAG, "address: " + feedAddress);
                    //Log.w(Constants.LOG_TAG, Constants.EMPTY + ApplicationEx.dbHelper.getFeedTitle(feedAddress));
                    //Log.w(Constants.LOG_TAG, " -> " + nextFeedTime);
                }
                feedMagics.add(new FeedMagic(feedAddress, lastUpdateTime, magicInterval, nextUpdateTime));
            } while (cur.moveToNext());
        }
        cur.close();
        for (FeedMagic magic : feedMagics) {
            feedAddress = magic.feedAddress;
            Log.d(Constants.LOG_TAG, "address: " + feedAddress);
            Log.d(Constants.LOG_TAG, Constants.EMPTY + ApplicationEx.dbHelper.getFeedTitle(feedAddress));
            Log.d(Constants.LOG_TAG, "* " + magic.magicInterval);
            Log.d(Constants.LOG_TAG, " <- " + magic.lastUpdate);
            Log.d(Constants.LOG_TAG, " -> " + magic.nextUpdate);
            if (magic.nextUpdate <= System.currentTimeMillis() ||
                    magic.nextUpdate <= nextFeedTime + intervalSetting)
                addresses.add(feedAddress);
        }
        if (nextFeedTime < System.currentTimeMillis())
            nextFeedTime = System.currentTimeMillis() + 5000;
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
    
    public long getLong(String columnId, String table) {
        Cursor cur = db.query(table, new String[] {columnId}, null, null,
                null, null, null);
        long value = -1;
        if (cur.moveToFirst())
            value = cur.getLong(cur.getColumnIndex(columnId));
        cur.close();
        return value;
    }
    
    private boolean hasAppRecord() {
        Cursor cur = db.query(APP_TABLE,
                new String[] {DatabaseHelper.COL_APP_LAST_TAB}, null, null,
                null, null, null);
        boolean hasRecord = false;
        if (cur.moveToFirst())
            hasRecord = true;
        cur.close();
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
    
    public int getAppFeedListPosition() {
        int pos = getInt(COL_APP_FEED_LIST_POSITION, APP_TABLE);
        return pos <= 0 ? Constants.PAGE_SIZE : pos;
    }
    
    public void setAppFeedListPosition(int position) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_FEED_LIST_POSITION, position);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getAppEpisodePosition() {
        return getInt(COL_APP_EPISODE_POSITION, APP_TABLE);
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
    
    public int getCurrentSpeed() {
        return getInt(COL_APP_CURRENT_SPEED, APP_TABLE);
    }
    
    public void setCurrentSpeed(double speed) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_CURRENT_SPEED, speed);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public int getCurrentPlaylistType() {
        return getInt(COL_APP_CURRENT_PLAYLIST_TYPE, APP_TABLE);
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
    
    public long getNextMagicUpdate() {
        return getLong(COL_APP_NEXT_MAGIC_TIME, APP_TABLE);
    }
    
    public void setNextMagicUpdate(long update) {
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_NEXT_MAGIC_TIME, update);
        if (hasAppRecord())
            updateRecord(cv, APP_TABLE, null);
        else
            insertRecord(cv, APP_TABLE, null);
    }
    
    public void checkUpgrade() {
        Cursor cur = db.query(APP_TABLE, null, null, null, null, null, null);
        String[] colArray = cur.getColumnNames();
        List<String> colList = Arrays.asList(colArray);
        cur.close();
        String sqlString;
        if (!colList.contains(COL_APP_FEED_LIST_POSITION)) {
            sqlString = TextUtils.concat("ALTER TABLE ", APP_TABLE, " ADD ",
                    COL_APP_FEED_LIST_POSITION, " INTEGER DEFAULT 0")
                            .toString();
            try {
                db.execSQL(sqlString);
            } catch (SQLException e) {
                Log.e(Constants.LOG_TAG, "Bad SQL string: " + sqlString, e);
            }
        }
        if (!colList.contains(COL_APP_EPISODE_LIST_POSITION)) {
            sqlString = TextUtils.concat("ALTER TABLE ", APP_TABLE, " ADD ",
                    COL_APP_EPISODE_LIST_POSITION, " INTEGER DEFAULT 0")
                            .toString();
            try {
                db.execSQL(sqlString);
            } catch (SQLException e) {
                Log.e(Constants.LOG_TAG, "Bad SQL string: " + sqlString, e);
            }
        }
        if (!colList.contains(COL_APP_PREV_BRIGHTNESS_MODE)) {
            sqlString = TextUtils.concat("ALTER TABLE ", APP_TABLE, " ADD ",
                    COL_APP_PREV_BRIGHTNESS_MODE, " INTEGER DEFAULT -1")
                            .toString();
            try {
                db.execSQL(sqlString);
            } catch (SQLException e) {
                Log.e(Constants.LOG_TAG, "Bad SQL string: " + sqlString, e);
            }
        }
        if (!colList.contains(COL_APP_PREV_BRIGHTNESS_LEVEL)) {
            sqlString = TextUtils.concat("ALTER TABLE ", APP_TABLE, " ADD ", 
                    COL_APP_PREV_BRIGHTNESS_LEVEL, " INTEGER DEFAULT -1")
                            .toString();
            try {
                db.execSQL(sqlString);
            } catch (SQLException e) {
                Log.e(Constants.LOG_TAG, "Bad SQL string: " + sqlString, e);
            }
        }
        if (!colList.contains(COL_APP_CURRENT_SPEED)) {
            sqlString = TextUtils.concat("ALTER TABLE ", APP_TABLE, " ADD ", 
                    COL_APP_CURRENT_SPEED, " REAL DEFAULT 0").toString();
            try {
                db.execSQL(sqlString);
            } catch (SQLException e) {
                Log.e(Constants.LOG_TAG, "Bad SQL string: " + sqlString, e);
            }
        }
        if (!colList.contains(COL_APP_NEXT_MAGIC_TIME)) {
            sqlString = TextUtils.concat("ALTER TABLE ", APP_TABLE, " ADD ", 
                    COL_APP_NEXT_MAGIC_TIME, " INTEGER DEFAULT -1").toString();
            try {
                db.execSQL(sqlString);
            } catch (SQLException e) {
                Log.e(Constants.LOG_TAG, "Bad SQL string: " + sqlString, e);
            }
        }
        cur = db.query(FEED_TABLE, null, null, null, null, null, null);
        colArray = cur.getColumnNames();
        colList = Arrays.asList(colArray);
        cur.close();
        if (!colList.contains(COL_FEED_MAGIC_TYPE)) {
            sqlString = TextUtils.concat("ALTER TABLE ", FEED_TABLE, " ADD ", 
                    COL_FEED_MAGIC_TYPE, " TEXT DEFAULT ",
                    FeedPublishType.Daily.toString()).toString();
            try {
                db.execSQL(sqlString);
            } catch (SQLException e) {
                Log.e(Constants.LOG_TAG, "Bad SQL string: " + sqlString, e);
            }
        }
        cur = db.query(EPISODE_TABLE, null, null, null, null, null, null);
        colArray = cur.getColumnNames();
        colList = Arrays.asList(colArray);
        cur.close();
        if (!colList.contains(COL_EPISODE_GUID)) {
            sqlString = TextUtils.concat("ALTER TABLE ", EPISODE_TABLE, " ADD ", 
                    COL_EPISODE_GUID, " TEXT").toString();
            try {
                db.execSQL(sqlString);
            } catch (SQLException e) {
                Log.e(Constants.LOG_TAG, "Bad SQL string: " + sqlString, e);
            }
        }
    }

}