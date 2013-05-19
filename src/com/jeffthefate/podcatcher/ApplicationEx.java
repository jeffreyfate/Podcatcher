package com.jeffthefate.podcatcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;

import com.jeffthefate.podcatcher.Util.UpdateWaitCounter;
import com.jeffthefate.podcatcher.receiver.MusicIntentReceiver.ButtonCountdown;
import com.jeffthefate.stacktrace.ExceptionHandler.OnStacktraceListener;
import com.parse.Parse;
import com.parse.ParseObject;

/**
 * Used as a holder of many values and objects for the entire application.
 * 
 * @author Jeff Fate
 */
public class ApplicationEx extends Application implements OnStacktraceListener {
    /**
     * The application's context
     */
    private static Context app;
    private static boolean mCanAccessFeedsDir = false;
    private static boolean mExternalStorageAvailable = false;
    private static boolean mExternalStorageWriteable = false;
    public static DatabaseHelper dbHelper;
    private static boolean mHasWifi = false;
    private static boolean mHasConnection = false;
    private static boolean mInAirplaneMode = false;
    private static boolean mHeadsetPlugged = false;
    
    private static double mCurrentSpeed;
    private static boolean mIsPlaying;
    
    private static String[] mPrevEpisode;
    private static String[] mCurrEpisode;
    private static String[] mNextEpisode;
    
    public static String cacheLocation = null;
    
    private static boolean updateWait = false;
    public static UpdateWaitCounter waitCounter;
    
    public static ArrayList<String> downloadList;
    public static int currEpId;
    public static int currEpProgress = 0;
    public static int currEpTotal = 0;
    
    public static PodcastAdapter epAdapter;
    public static PodcastExpandableAdapter feedAdapter;
    
    public static int epRead;
    public static int epDownloaded;
    public static int epSortType;
    public static int feedRead;
    public static int feedDownloaded;
    public static int feedSortType;
    public static int feedSort;
    public static int archive;
    
    public static int lastKeyCode = -1;
    public static long lastKeyPress = -1;
    public static ArrayList<Long> keys = new ArrayList<Long>();
    public static ButtonCountdown buttonCountdown;
    public static SparseIntArray presses = new SparseIntArray();
    public static long speed = -1;
    
    public static SharedPreferences sharedPrefs;
    
    public static long sunriseTime = -1;
    public static long sunsetTime = -1;
    
    public static AccelerometerReader accelReader;
    public static ProximityReader proxReader;
    
    private static boolean isMainActive = false;
    
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        dbHelper = DatabaseHelper.getInstance();
        cacheLocation = getExternalCacheDir().getAbsolutePath();
        File path = new File(TextUtils.concat(
                cacheLocation, Constants.FEEDS_LOCATION).toString());
        path.setExecutable(true, false);
        path.setReadable(true, false);
        path.setWritable(true, false);
        path.mkdirs();
        path = new File(TextUtils.concat(
                cacheLocation, Constants.TEMP_LOCATION).toString());
        path.setExecutable(true, false);
        path.setReadable(true, false);
        path.setWritable(true, false);
        path.mkdirs();
        path = new File(TextUtils.concat(cacheLocation,
                Constants.DOWNLOAD_LOCATION, File.separator, Constants.NOMEDIA)
                .toString());
        try {
            path.createNewFile();
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, "Unable to create .nomedia file", e);
        }
        updateExternalStorageState();
        checkFileDirAccess();
        NetworkInfo nInfo = ((ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (nInfo == null)
            mHasConnection = false;
        else {
            mHasConnection = nInfo.isConnected();
            mHasWifi = nInfo.getType() == ConnectivityManager.TYPE_WIFI && 
                    mHasConnection;
        }
        mHeadsetPlugged = ((AudioManager) getSystemService(
                Context.AUDIO_SERVICE)).isWiredHeadsetOn() || isA2DPOn();
        mInAirplaneMode = getAirplaneMode();
        mPrevEpisode = dbHelper.getEpisode(dbHelper.getPrevEpisode());
        mCurrEpisode = dbHelper.getEpisode(dbHelper.getCurrentEpisode());
        mNextEpisode = dbHelper.getEpisode(dbHelper.getNextEpisode());
        waitCounter = new UpdateWaitCounter(Constants.WAIT_MSECS, 1000);
        dbHelper.checkUpgrade();
        downloadList = new ArrayList<String>();
        Parse.initialize(this, Constants.PARSE_ID, Constants.PARSE_KEY);
        epRead = dbHelper.getCurrentEpisodeRead();
        epDownloaded = dbHelper.getCurrentEpisodeDownloaded();
        epSortType = dbHelper.getCurrentEpisodeSortType();
        feedRead = dbHelper.getCurrentFeedRead();
        feedDownloaded = dbHelper.getCurrentFeedDownloaded();
        feedSortType = dbHelper.getCurrentFeedSortType();
        feedSort = dbHelper.getCurrentFeedSort();
        archive = Util.readBooleanPreference(R.string.archive_key, false) ?
                1 : 0;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Util.setDayAlarm();
        setupTapRemote();
        setupSwipeRemote();
    }
    /**
     * Used by other classes to get the application's global context.
     * @return  the context of the application
     */
    public static Context getApp() {
        return app;
    }
    /**
     * 
     */
    public static void updateExternalStorageState() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else
            mExternalStorageAvailable = mExternalStorageWriteable = false;
    }
    
    public static void updateUsbConnectionState() {
        checkFileDirAccess();
    }
    
    private static void checkFileDirAccess() {
        File dir = new File(TextUtils.concat(ApplicationEx.cacheLocation,
                Constants.FEEDS_LOCATION).toString());
        if(dir.exists() && dir.isDirectory())
            mCanAccessFeedsDir = true;
        else
            mCanAccessFeedsDir = false;
    }
    
    public static boolean isSyncing() {
        return Util.readBooleanPreference(R.string.sync_key, false);
    }
    
    public static boolean hasWifi() {
        return mHasWifi;
    }
    
    public static void setWifi(boolean hasWifi) {
        mHasWifi = hasWifi;
        mInAirplaneMode = getAirplaneMode();
    }
    
    public static boolean hasConnection() {
        return mHasConnection;
    }
    
    public static void setConnection(boolean hasConnection) {
        mHasConnection = hasConnection;
        mInAirplaneMode = getAirplaneMode();
    }
    
    public static boolean inAirplaneMode() {
        return mInAirplaneMode;
    }
    
    public static void setAirplaneMode(boolean inAirplaneMode) {
        mInAirplaneMode = inAirplaneMode;
    }
    
    public static boolean isA2DPOn() {
        if (((AudioManager) app.getSystemService(
                Context.AUDIO_SERVICE)).isBluetoothA2dpOn())
            return true;
        else
            return false;
    }
    
    public static boolean isPlugged() {
        return mHeadsetPlugged;
    }
    
    public static void setPlugged(boolean isPlugged) {
        mHeadsetPlugged = isPlugged;
    }
    
    @SuppressWarnings("deprecation")
    private static boolean getAirplaneMode() {
        return Settings.System.getInt(app.getContentResolver(), 
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }
    
    public static void setCurrentSpeed(double currSpeed) {
        mCurrentSpeed = currSpeed;
    }
    
    public static double getCurrentSpeed() {
        return mCurrentSpeed;
    }
    
    public static void setIsPlaying(boolean isPlaying) {
        mIsPlaying = isPlaying;
    }
    
    public static boolean getIsPlaying() {
        return mIsPlaying;
    }
    
    public static void setCurrentEpisode(int episodeId) {
        ApplicationEx.dbHelper.setCurrentEpisode(episodeId);
        mCurrEpisode = ApplicationEx.dbHelper.getEpisode(episodeId);
    }
    
    public static String[] getCurrentEpisode() {
        return mCurrEpisode;
    }
    
    public static void setPrevEpisode(String[] episode) {
        mPrevEpisode = episode;
    }
    
    public static String[] getPrevEpisode() {
        return mPrevEpisode;
    }
    
    public static void setNextEpisode(String[] episode) {
        mNextEpisode = episode;
    }
    
    public static String[] getNextEpisode() {
        return mNextEpisode;
    }
    
    public static boolean canAccessFeedsDir() {
        return mCanAccessFeedsDir;
    }
    
    public static boolean isExternalStorageAvailable() {
        return mExternalStorageAvailable;
    }
    
    public static boolean isExternalStorageWriteable() {
        return mExternalStorageWriteable;
    }
    
    public static boolean getUpdateWait() {
        return updateWait;
    }
    
    public static void setUpdateWait(boolean wait) {
        updateWait = wait;
    }
    
    public static boolean isMainActive() {
        return isMainActive;
    }
    
    public static void setMainActive(boolean isActive) {
        isMainActive = isActive;
    }
    
    public static void setBrightness(int mode, int level) {
        if (ApplicationEx.dbHelper.getInt(
                DatabaseHelper.COL_APP_PREV_BRIGHTNESS_MODE,
                DatabaseHelper.APP_TABLE) < 0) {
            ContentValues cv = new ContentValues();
            try {
                cv.put(DatabaseHelper.COL_APP_PREV_BRIGHTNESS_MODE,
                        Settings.System.getInt(app.getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS_MODE));
                cv.put(DatabaseHelper.COL_APP_PREV_BRIGHTNESS_LEVEL,
                        Settings.System.getInt(app.getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS));
            } catch (SettingNotFoundException e) {
                Log.e(Constants.LOG_TAG, "Brightness setting not found!", e);
                return;
            }
            ApplicationEx.dbHelper.updateRecord(cv, DatabaseHelper.APP_TABLE,
                    null);
        }
        Settings.System.putInt(app.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
        if (mode != Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
            Settings.System.putInt(app.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, level);
    }
    
    public static void resetBrightness() {
        int prevMode = ApplicationEx.dbHelper.getInt(
                DatabaseHelper.COL_APP_PREV_BRIGHTNESS_MODE,
                DatabaseHelper.APP_TABLE);
        if (prevMode > -1) {
            Settings.System.putInt(app.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE, prevMode);
            if (prevMode != Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                Settings.System.putInt(app.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS,
                        ApplicationEx.dbHelper.getInt(
                                DatabaseHelper.COL_APP_PREV_BRIGHTNESS_LEVEL,
                                DatabaseHelper.APP_TABLE));
        }
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_APP_PREV_BRIGHTNESS_MODE, -1);
        cv.put(DatabaseHelper.COL_APP_PREV_BRIGHTNESS_LEVEL, -1);
        ApplicationEx.dbHelper.updateRecord(cv, DatabaseHelper.APP_TABLE, null);
    }
    
    public static void setupTapRemote() {
        accelReader = new AccelerometerReader(Util.readBooleanPreference(
                R.string.tap_key, false));
    }
    
    public static void setupSwipeRemote() {
        proxReader = new ProximityReader(
                Util.readBooleanPreference(R.string.swipe_key, false));
    }
    
    @Override
    public void onStacktrace(String appPackage, String packageVersion,
            String deviceModel, String androidVersion, String stacktrace) {
        ParseObject object = new ParseObject(Constants.LOG);
        object.put(Constants.ANDROID_VERSION, androidVersion);
        object.put(Constants.APP_PACKAGE, appPackage);
        object.put(Constants.DEVICE_MODEL, deviceModel);
        object.put(Constants.PACKAGE_VERSION, packageVersion);
        object.put(Constants.STACK_TRACE, stacktrace);
        try {
            object.saveInBackground();
        } catch (ExceptionInInitializerError e) {};
    }
    
}