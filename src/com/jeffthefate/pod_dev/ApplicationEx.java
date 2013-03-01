package com.jeffthefate.pod_dev;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

/**
 * Used as a holder of many values and objects for the entire application.
 * 
 * @author Jeff Fate
 */
public class ApplicationEx extends Application {
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
    
    private UpdateEpisodeReceiver updateEpisodeReceiver;
    
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        dbHelper = DatabaseHelper.getInstance();
        cacheLocation = getExternalCacheDir().getAbsolutePath();
        File path = new File(cacheLocation +
                Constants.FEEDS_LOCATION);
        path.setExecutable(true, false);
        path.setReadable(true, false);
        path.setWritable(true, false);
        path.mkdirs();
        path = new File(cacheLocation + Constants.TEMP_LOCATION);
        path.setExecutable(true, false);
        path.setReadable(true, false);
        path.setWritable(true, false);
        path.mkdirs();
        path = new File(cacheLocation + Constants.DOWNLOAD_LOCATION +
                File.separator + ".nomedia");
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
        mPrevEpisode = dbHelper.getEpisode(dbHelper.getNextEpisode());
        updateEpisodeReceiver = new UpdateEpisodeReceiver();
        registerReceiver(updateEpisodeReceiver, 
                new IntentFilter(Constants.ACTION_NEW_DOWNLOAD));
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
    
    public static void updateUsbConnectionState(boolean usbCharge) {
        if (usbCharge)
            checkFileDirAccess();
    }
    
    private static void checkFileDirAccess() {
        File dir = new File(ApplicationEx.cacheLocation +
                Constants.FEEDS_LOCATION);
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
    
    public static void setCurrentEpisode(String[] episode) {
        mCurrEpisode = episode;
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
    
    private class UpdateEpisodeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<Integer> idList = new ArrayList<Integer>();
            if (intent.hasExtra("idList")) {
                idList = intent.getIntegerArrayListExtra("idList");
            }
            if (!idList.isEmpty() && getCurrentEpisode() == null)
                setCurrentEpisode(dbHelper.getEpisode(idList.get(0)));
        }
    }
    
}