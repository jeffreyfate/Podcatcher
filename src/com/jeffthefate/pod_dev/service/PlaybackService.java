package com.jeffthefate.pod_dev.service;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.aocate.media.MediaPlayer;
import com.aocate.media.MediaPlayer.OnCompletionListener;
import com.aocate.media.MediaPlayer.OnErrorListener;
import com.aocate.media.MediaPlayer.OnInfoListener;
import com.aocate.media.MediaPlayer.OnPreparedListener;
import com.aocate.media.MediaPlayer.OnSpeedAdjustmentAvailableChangedListener;
import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.Constants;
import com.jeffthefate.pod_dev.DatabaseHelper;
import com.jeffthefate.pod_dev.MediaButtonHelper;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.RemoteControlClientCompat;
import com.jeffthefate.pod_dev.RemoteControlHelper;
import com.jeffthefate.pod_dev.Util;
import com.jeffthefate.pod_dev.VersionedNotificationBuilder;
import com.jeffthefate.pod_dev.activity.ActivityPlayback;
import com.jeffthefate.pod_dev.activity.ActivitySplash;
import com.jeffthefate.pod_dev.receiver.MusicIntentReceiver;

/**
 * Service that runs in the background.  Registers receivers for actions that
 * the app will respond to.  Also, handles starting the widget updates.
 * 
 * @author Jeff
 */
public class PlaybackService extends Service {
    
    public final static String ACTION_TOGGLE_PLAYBACK =
        "com.jeffthefate.pod.action.TOGGLE_PLAYBACK";
    public final static String ACTION_PLAY = "com.jeffthefate.pod.action.PLAY";
    public final static String ACTION_PAUSE =
        "com.jeffthefate.pod.action.PAUSE";
    public final static String ACTION_NEXT = "com.jeffthefate.pod.action.NEXT";
    public final static String ACTION_PREVIOUS =
        "com.jeffthefate.pod.action.PREVIOUS";
    public final static String ACTION_AHEAD =
        "com.jeffthefate.pod.action.AHEAD";
    public final static String ACTION_BEHIND =
        "com.jeffthefate.pod.action.BEHIND";
    
    int mEpisodeId = -1;
    int mEpisodeIndex = -1;
    Uri mMediaUri;
    
    VersionedNotificationBuilder nBuilder;
    NotificationManager nManager;
    PendingIntent playPendingIntent;
    AudioManager audioManager;
    MediaPlayer mediaPlayer;
    
    ArrayList<Integer> epIdList;
    
    UpdatePlaylistReceiver updatePlaylistReceiver;
    MediaButtonReceiver mediaButtonReceiver;
    AudioConnectionReceiver audioConnectionReceiver;
    PhoneReceiver phoneReceiver;
    
    private boolean isPrepared = false;
    private boolean isInit = false;
    
    public static ArrayList<Double> speedList;
    
    private double currSpeed;
    private int currPosition;
    private int currDuration;
    
    private ArrayList<Messenger> clients;
    
    private Context mContext = this;
    
    private ComponentName mMediaButtonReceiverComponent;
    private RemoteControlClientCompat mRemoteControlClientCompat;
    
    DecimalFormat twoDForm = new DecimalFormat("#.##");
    
    @Override
    public void onCreate() {
        super.onCreate();
        clients = new ArrayList<Messenger>();
        updatePlaylistReceiver = new UpdatePlaylistReceiver();
        mediaButtonReceiver = new MediaButtonReceiver();
        audioConnectionReceiver = new AudioConnectionReceiver();
        phoneReceiver = new PhoneReceiver();
        registerReceiver(updatePlaylistReceiver, 
                new IntentFilter(Constants.ACTION_NEW_DOWNLOAD));
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        intentFilter.addAction(
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(audioConnectionReceiver, intentFilter);
        intentFilter = new IntentFilter();
        intentFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        intentFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        registerReceiver(phoneReceiver, intentFilter);
        speedList = Util.readSpeedList();
        if (speedList.isEmpty()) {
            speedList.add(Double.valueOf(twoDForm.format(0.5)));
            speedList.add(Double.valueOf(twoDForm.format(0.75)));
            speedList.add(Double.valueOf(twoDForm.format(1.0)));
            speedList.add(Double.valueOf(twoDForm.format(1.25)));
            speedList.add(Double.valueOf(twoDForm.format(1.5)));
        }
        currSpeed = Util.readFloatFromFile(Constants.SPEED_FILENAME);
        mEpisodeId = ApplicationEx.dbHelper.getCurrentEpisode();
        nBuilder = VersionedNotificationBuilder.newInstance().create(this);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            new SetupNotificationTask().execute();
        else
            new SetupNotificationTask().executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR);
        epIdList = Util.readCurrentPlaylist();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMediaButtonReceiverComponent = new ComponentName(this,
                MusicIntentReceiver.class);
            mRemoteControlResponder = new ComponentName(getPackageName(),
                    RemoteControlClientCompat.class.getName());
        initializeRemoteControlRegistrationMethods();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (ApplicationEx.isExternalStorageAvailable() && 
                ApplicationEx.canAccessFeedsDir()) {
            if (intent != null) {
                if (intent.hasExtra("episodes")) {
                    epIdList = intent.getIntegerArrayListExtra("episodes");
                    Util.persistCurrentPlaylist(epIdList);
                }
                if (intent.hasExtra("startEp")) {
                    if (mEpisodeId != intent.getIntExtra("startEp", -1)) {
                        if (mediaPlayer != null) {
                            if (mediaPlayer.isPlaying()) {
                                currPosition = mediaPlayer.getCurrentPosition();
                                if (Build.VERSION.SDK_INT <
                                        Build.VERSION_CODES.HONEYCOMB)
                                    new Util.SavePositionTask(currPosition,
                                            mEpisodeId).execute();
                                else
                                    new Util.SavePositionTask(currPosition,
                                            mEpisodeId).executeOnExecutor(
                                            AsyncTask.THREAD_POOL_EXECUTOR);
                                mediaPlayer.stop();
                                mediaPlayer = null;
                            }
                        }
                        mEpisodeId = intent.getIntExtra("startEp", -1);
                        ApplicationEx.setCurrentEpisode(
                                ApplicationEx.dbHelper.getEpisode(mEpisodeId));
                        if (Build.VERSION.SDK_INT <
                                Build.VERSION_CODES.HONEYCOMB)
                            new SetupNotificationTask().execute();
                        else
                            new SetupNotificationTask().executeOnExecutor(
                                    AsyncTask.THREAD_POOL_EXECUTOR);
                        ApplicationEx.dbHelper.setCurrentEpisode(mEpisodeId);
                    }
                }
            }
            if (epIdList != null) {
                if (intent.getBooleanExtra("init", true)) {
                    isInit = true;
                }
                else {
                    isInit = false;
                }
                if (epIdList.isEmpty() || mEpisodeId < 0) {
                    isInit = false;
                }
                if (isInit) {
                    if (mediaPlayer == null || !mediaPlayer.isPlaying()) {
                        if (Build.VERSION.SDK_INT <
                                Build.VERSION_CODES.HONEYCOMB)
                            new SetupNotificationTask().execute();
                        else
                            new SetupNotificationTask().executeOnExecutor(
                                    AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
                if (intent.getBooleanExtra("activity", true)) {
                    Intent playIntent = new Intent(ApplicationEx.getApp(),
                            ActivityPlayback.class);
                    playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP | 
                            Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(playIntent);
                }
                else
                    togglePlayback(1000);
            }
        }
        else {
            nManager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);
            nBuilder.setContentText("Unable to play");
            Intent notificationIntent = new Intent(this, ActivitySplash.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, 
                    notificationIntent, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            nBuilder.setContentIntent(pendingIntent);
            nManager.notify(1341, nBuilder.getNotification());
        }
        mEpisodeIndex = epIdList.indexOf(mEpisodeId);
        return Service.START_STICKY_COMPATIBILITY;
    }
    
    @Override
    public void onDestroy() {
        unregisterReceiver(updatePlaylistReceiver);
        unregisterReceiver(audioConnectionReceiver);
        unregisterReceiver(phoneReceiver);
        stopForeground(true);
        super.onDestroy();
    }
    
    private boolean needRegainFocus = false;

    OnAudioFocusChangeListener afChangeListener = 
            new OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                setupRemoteControl();
                if (mediaPlayer != null) {
                    if (!mediaPlayer.isPlaying() && needRegainFocus) {
                        sendBroadcast(
                                new Intent(Constants.ACTION_START_PLAYBACK));
                        mediaPlayer.start();
                        ApplicationEx.setIsPlaying(mediaPlayer.isPlaying());
                    }
                }
                else
                    togglePlayback(1000);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                togglePlayback(1000);
                int result = audioManager.abandonAudioFocus(afChangeListener);
                if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    // Nothing to do here?
                }
                needRegainFocus = true;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    ApplicationEx.setIsPlaying(mediaPlayer.isPlaying());
                    needRegainFocus = true;
                }
                else
                    ApplicationEx.setIsPlaying(false);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    //mediaPlayer.setVolume(0.5f, 0.5f);
                    mediaPlayer.pause();
                    ApplicationEx.setIsPlaying(mediaPlayer.isPlaying());
                    needRegainFocus = true;
                }
                else
                    ApplicationEx.setIsPlaying(false);
                break;
            }
        }
    };
    
    class UpdatePlaylistReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<Integer> idList = new ArrayList<Integer>();
            if (intent.hasExtra("idList")) {
                idList = intent.getIntegerArrayListExtra("idList");
                epIdList = idList;
                Util.persistCurrentPlaylist(epIdList);
            }
        }
    }
    
    class MediaButtonReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_TOGGLE_PLAYBACK))
                togglePlayback(1000);
            else if (action.equals(ACTION_PLAY))
                togglePlayback(1000);
            else if (action.equals(ACTION_PAUSE))
                togglePlayback(1000);
            else if (action.equals(ACTION_NEXT))
                skip();
            else if (action.equals(ACTION_PREVIOUS))
                previous();
            else if (action.equals(ACTION_AHEAD))
                ffwd();
            else if (action.equals(ACTION_BEHIND))
                rewind();
        }
    }
    
    public void initPlayer() {
        currPosition = ApplicationEx.dbHelper.getInt(mEpisodeId, 
                DatabaseHelper.COL_EPISODE_POSITION, 
                DatabaseHelper.EPISODE_TABLE, 
                DatabaseHelper.COL_EPISODE_ID);
        currSpeed = ApplicationEx.dbHelper.getFeedSpeed(mEpisodeId);
        ApplicationEx.setCurrentSpeed(currSpeed);
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying())
                togglePlayback(1000);
        }
        else {
            mediaPlayer = new MediaPlayer(ApplicationEx.getApp());
        }
        if (mediaPlayer.isPrestoLibraryInstalled()) {
            mediaPlayer.setUseService(true);
        }
        else {
            mediaPlayer.setUseService(false);
        }
        mediaPlayer.setOnSpeedAdjustmentAvailableChangedListener(
                new OnSpeedAdjustmentAvailableChangedListener() {
            @Override
            public void onSpeedAdjustmentAvailableChanged(MediaPlayer player,
                    boolean isAdjustmentAvailable) {
                if (isAdjustmentAvailable) {
                    setSpeed();
                    for (Messenger client : clients) {
                        Message mMessage = Message.obtain(null, 
                                ActivityPlayback.SPEED_ENABLED);
                        try {
                            client.send(mMessage);
                        } catch (RemoteException e) {
                            Log.e(Constants.LOG_TAG, "Can't connect to " +
                                    "PlaybackActivity", e);
                        }
                    }
                }
                else {
                    currSpeed = 1.0f;
                    setSpeed();
                    // TODO something in the UI here?
                    // Disable the speed button?
                    for (Messenger client : clients) {
                        Message mMessage = Message.obtain(null, 
                                ActivityPlayback.SPEED_DISABLED);
                        try {
                            client.send(mMessage);
                        } catch (RemoteException e) {
                            Log.e(Constants.LOG_TAG, "Can't connect to " +
                                    "PlaybackActivity", e);
                        }
                    }
                }
            }
        });
        mediaPlayer.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer player) {
                currDuration = mediaPlayer.getDuration();
                if (Build.VERSION.SDK_INT <
                        Build.VERSION_CODES.HONEYCOMB)
                    new SaveTrackInfoTask().execute();
                else
                    new SaveTrackInfoTask().executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR);
                if (currPosition >= 0) {
                    mediaPlayer.seekTo(currPosition);
                }
                else
                    currPosition = 0;
                if (isInit) {
                    getAudioFocus();
                }
                setSpeed();
            }
        });
        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer player) {
                completePlayback(false);
            }
        });
        mediaPlayer.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer player, int type, int extra) {
                Log.d(Constants.LOG_TAG, "type: " + type);
                Log.d(Constants.LOG_TAG, "extra: " + extra);
                if (mediaPlayer != null) {
                    // TODO notify user there was an error
                    // and figure out how to possibly handle the errors
                    //isError = true;
                    if (type == 1 && extra == 6) {
                        if (Build.VERSION.SDK_INT <
                                Build.VERSION_CODES.HONEYCOMB)
                            new ErrorCompleteTask().execute();
                        else
                            new ErrorCompleteTask().executeOnExecutor(
                                    AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                    else if (type == 1 && extra == 0) {
                        // TODO try a few times then give up?
                        if (Build.VERSION.SDK_INT <
                                Build.VERSION_CODES.HONEYCOMB)
                            new ErrorTask().execute();
                        else
                            new ErrorTask().executeOnExecutor(
                                    AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                    else if (type == -38 & extra == 0) {
                        ApplicationEx.dbHelper.episodeDownloaded(mEpisodeId,
                                false);
                        mEpisodeIndex = epIdList.indexOf(mEpisodeId);
                        epIdList.remove((Integer)mEpisodeId);
                        Util.persistCurrentPlaylist(epIdList);
                        sendBroadcast(
                                new Intent(Constants.ACTION_REFRESH_FEEDS));
                        sendBroadcast(
                                new Intent(Constants.ACTION_REFRESH_EPISODES));
                        sendBroadcast(
                                new Intent(Constants.ACTION_REFRESH_DURATION));
                        ApplicationEx.setIsPlaying(false);
                        isPrepared = false;
                        if (Build.VERSION.SDK_INT <
                                Build.VERSION_CODES.HONEYCOMB)
                            new Util.SavePositionTask(0, mEpisodeId).execute();
                        else
                            new Util.SavePositionTask(0, mEpisodeId)
                                    .executeOnExecutor(
                                    AsyncTask.THREAD_POOL_EXECUTOR);
                        if (epIdList.isEmpty()) {
                            ApplicationEx.dbHelper.setCurrentEpisode(-1);
                            stopForeground(true);
                            for (Messenger client : clients) {
                                Message mMessage = Message.obtain(null, 
                                        ActivityPlayback.PLAYLIST_EMPTY);
                                try {
                                    client.send(mMessage);
                                } catch (RemoteException e) {
                                    Log.e(Constants.LOG_TAG, "Can't connect " +
                                            "to PlaybackActivity", e);
                                }
                            }
                        }
                        else {
                            if (mEpisodeIndex >= epIdList.size()-1 ||
                                    mEpisodeIndex < 0)
                                mEpisodeIndex = 0;
                            mEpisodeId = epIdList.get(mEpisodeIndex);
                            if (Build.VERSION.SDK_INT <
                                    Build.VERSION_CODES.HONEYCOMB)
                                new SetupNotificationTask().execute();
                            else
                                new SetupNotificationTask().executeOnExecutor(
                                        AsyncTask.THREAD_POOL_EXECUTOR);
                            ApplicationEx.dbHelper.setCurrentEpisode(
                                    mEpisodeId);
                            ApplicationEx.setCurrentEpisode(
                                    ApplicationEx.dbHelper.getEpisode(
                                            mEpisodeId));
                            isInit = true;
                            if (mEpisodeId > -1) {
                                preparePlayer();
                            }
                        }
                    }
                    else {
                        if (Build.VERSION.SDK_INT <
                                Build.VERSION_CODES.HONEYCOMB)
                            new Util.SavePositionTask(currPosition, mEpisodeId)
                                    .execute();
                        else
                            new Util.SavePositionTask(currPosition, mEpisodeId)
                                    .executeOnExecutor(
                                    AsyncTask.THREAD_POOL_EXECUTOR);
                        stopForeground(true);
                        ApplicationEx.setIsPlaying(false);
                    }
                }
                return true;
            }
        });
        mediaPlayer.setOnInfoListener(new OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer player, int what, int extra) {
                Log.d(Constants.LOG_TAG, "what: " + what);
                Log.d(Constants.LOG_TAG, "extra: " + extra);
                return true;
            }
        });
        if (mEpisodeId >= 0)
            preparePlayer();        
    }
    
    private class ErrorTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            killPlayback();
            togglePlayback(1000);
            return null;
        }
    }
    
    private class ErrorCompleteTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            killPlayback();
            completePlayback(true);
            return null;
        }
    }
    
    private void completePlayback(boolean init) {
        mEpisodeIndex = epIdList.indexOf(mEpisodeId);
        epIdList.remove((Integer)mEpisodeId);
        Util.persistCurrentPlaylist(epIdList);
        ApplicationEx.dbHelper.markRead(mEpisodeId, true);
        new File(ApplicationEx.dbHelper.getEpisodeLocation(mEpisodeId))
                .delete();
        ApplicationEx.dbHelper.episodeDownloaded(mEpisodeId, false);
        sendBroadcast(new Intent(Constants.ACTION_REFRESH_FEEDS));
        sendBroadcast(new Intent(Constants.ACTION_REFRESH_EPISODES));
        sendBroadcast(new Intent(Constants.ACTION_REFRESH_DURATION));
        ApplicationEx.setIsPlaying(false);
        isPrepared = false;
        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.HONEYCOMB)
            new Util.SavePositionTask(0, mEpisodeId).execute();
        else
            new Util.SavePositionTask(0, mEpisodeId).executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR);
        if (epIdList.isEmpty()) {
            mEpisodeId = -1;
            ApplicationEx.setCurrentEpisode(
                    ApplicationEx.dbHelper.getEpisode(mEpisodeId));
            mRemoteControlClientCompat.setPlaybackState(
                    RemoteControlClientCompat.PLAYSTATE_PAUSED);
            audioManager.abandonAudioFocus(afChangeListener);
            teardownRemoteControl();
            needRegainFocus = false;
            sendBroadcast(new Intent(Constants.ACTION_PAUSE_PLAYBACK));
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            stopForeground(true);
            isInit = false;
            ApplicationEx.dbHelper.setCurrentEpisode(-1);
            for (Messenger client : clients) {
                Message mMessage = Message.obtain(null, 
                        ActivityPlayback.PLAYLIST_EMPTY);
                try {
                    client.send(mMessage);
                } catch (RemoteException e) {
                    Log.e(Constants.LOG_TAG, "Can't connect to " +
                            "PlaybackActivity", e);
                }
            }
        }
        else {
            if (mEpisodeIndex >= epIdList.size()-1 || mEpisodeIndex < 0)
                mEpisodeIndex = 0;
            mEpisodeId = epIdList.get(mEpisodeIndex);
            ApplicationEx.setCurrentEpisode(
                    ApplicationEx.dbHelper.getEpisode(mEpisodeId));
            if (Build.VERSION.SDK_INT <
                    Build.VERSION_CODES.HONEYCOMB)
                new SetupNotificationTask().execute();
            else
                new SetupNotificationTask().executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR);
            ApplicationEx.dbHelper.setCurrentEpisode(mEpisodeId);
            isInit = true;
            if (mEpisodeId > -1) {
                if (init) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                    initPlayer();
                }
                else
                    preparePlayer();
            }
        }
    }
    
    public void getAudioFocus() {
        int result = audioManager.requestAudioFocus(afChangeListener, 
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            nManager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);
            nBuilder.setContentText("Unable to play");
            Intent notificationIntent = 
                    new Intent(this, ActivitySplash.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationIntent, 
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            nBuilder.setContentIntent(pendingIntent);
            nManager.notify(1341, nBuilder.getNotification());
        }
        else {
            setupRemoteControl();
            sendBroadcast(new Intent(Constants.ACTION_START_PLAYBACK));
            mediaPlayer.start();
            ApplicationEx.setIsPlaying(mediaPlayer.isPlaying());
        }
    }
    
    private void teardownRemoteControl() {
        MediaButtonHelper.unregisterMediaButtonEventReceiverCompat(audioManager,
                mMediaButtonReceiverComponent);
        unregisterReceiver(mediaButtonReceiver);
    }
    
    private void setupRemoteControl() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_TOGGLE_PLAYBACK);
        intentFilter.addAction(ACTION_PLAY);
        intentFilter.addAction(ACTION_PAUSE);
        intentFilter.addAction(ACTION_NEXT);
        intentFilter.addAction(ACTION_PREVIOUS);
        intentFilter.addAction(ACTION_AHEAD);
        intentFilter.addAction(ACTION_BEHIND);
        registerReceiver(mediaButtonReceiver, intentFilter);
        // Use the media button APIs (if available) to register ourselves for media button
        // events
        String[] episode = ApplicationEx.getCurrentEpisode();
        MediaButtonHelper.registerMediaButtonEventReceiverCompat(
                audioManager, mMediaButtonReceiverComponent);

        // Use the remote control APIs (if available) to set the playback state

        if (mRemoteControlClientCompat == null) {
            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            intent.setComponent(mMediaButtonReceiverComponent);
            mRemoteControlClientCompat = new RemoteControlClientCompat(
                    PendingIntent.getBroadcast(this, 0, intent, 0));
            RemoteControlHelper.registerRemoteControlClient(audioManager,
                    mRemoteControlClientCompat);
        }

        mRemoteControlClientCompat.setPlaybackState(
                RemoteControlClientCompat.PLAYSTATE_PLAYING);

        mRemoteControlClientCompat.setTransportControlFlags(
                RemoteControlClientCompat.FLAG_KEY_MEDIA_PLAY_PAUSE |
                RemoteControlClientCompat.FLAG_KEY_MEDIA_PLAY |
                RemoteControlClientCompat.FLAG_KEY_MEDIA_PAUSE |
                RemoteControlClientCompat.FLAG_KEY_MEDIA_NEXT |
                RemoteControlClientCompat.FLAG_KEY_MEDIA_PREVIOUS |
                RemoteControlClientCompat.FLAG_KEY_MEDIA_FAST_FORWARD |
                RemoteControlClientCompat.FLAG_KEY_MEDIA_REWIND |
                RemoteControlClientCompat.FLAG_KEY_MEDIA_STOP);

        // Update the remote controls
        if (episode != null) {
            mRemoteControlClientCompat.editMetadata(true)
                .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM,
                        episode[2])
                .putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
                        episode[1])
                .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
                        mediaPlayer.getDuration())
                .putBitmap(RemoteControlClientCompat.MetadataEditorCompat
                        .METADATA_KEY_ARTWORK, episode[0] != null ?
                                BitmapFactory.decodeFile((episode[0])) : null)
                .apply();
        }
    }
    
    class SetupNotificationTask extends AsyncTask<Void, Void, Void> {
        
        @Override
        protected Void doInBackground(Void... nothing) {
            currPosition = ApplicationEx.dbHelper.getInt(mEpisodeId, 
                    DatabaseHelper.COL_EPISODE_POSITION, 
                    DatabaseHelper.EPISODE_TABLE, 
                    DatabaseHelper.COL_EPISODE_ID);
            currSpeed = ApplicationEx.dbHelper.getFeedSpeed(mEpisodeId);
            nBuilder.setContentTitle(
                    ApplicationEx.dbHelper.getEpisodeFeedTitle(mEpisodeId));
            nBuilder.setContentText(ApplicationEx.dbHelper.getEpisodeTitle(
                    mEpisodeId));
            nBuilder.setSmallIcon(R.drawable.ic_play_music_widget_holo);
            int feedId = ApplicationEx.dbHelper.getEpisodeFeedId(mEpisodeId);
            String imageLocation = ApplicationEx.cacheLocation +
                    Constants.FEEDS_LOCATION + feedId + File.separator;
            Bitmap bitmap = null;
            if (Util.findFile(imageLocation, feedId + "_small" + ".png")) {
                bitmap = BitmapFactory.decodeFile(imageLocation + feedId + 
                        "_small.png");
                nBuilder.setLargeIcon(bitmap);
            }
            nBuilder.setWhen(System.currentTimeMillis());
            Intent notificationIntent = new Intent(mContext, 
                    ActivityPlayback.class);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP | 
                    Intent.FLAG_ACTIVITY_NO_HISTORY);
            playPendingIntent = PendingIntent.getActivity(mContext, 0, 
                    notificationIntent, 0);
            nBuilder.setContentIntent(playPendingIntent);
            return null;
        }
        
        protected void onPostExecute(Void nothing) {
            if (isInit)
                startForeground(1339, nBuilder.getNotification());
        }
        
    }
    
    class SaveTrackInfoTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... nothing) {
            ApplicationEx.dbHelper.setCurrentEpisodeDuration(currDuration);
            ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.COL_EPISODE_DURATION, currDuration);
            ApplicationEx.dbHelper.updateRecord(cv, 
                    DatabaseHelper.EPISODE_TABLE, DatabaseHelper.COL_EPISODE_ID 
                    + "=" + mEpisodeId);
            return null;
        }
    }
    
    public void preparePlayer() {
        if (mediaPlayer != null)
            mediaPlayer.reset();
        File externalFile = new File(ApplicationEx.dbHelper
                .getEpisodeLocation(mEpisodeId));
        mMediaUri = Uri.fromFile(externalFile);
        try {
            mediaPlayer.setDataSource(mContext, mMediaUri);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();
        } catch (IllegalArgumentException e) {
            Log.e(Constants.LOG_TAG, "Bad URI", e);
        } catch (IllegalStateException e) {
            Log.e(Constants.LOG_TAG, "Illegal state", e);
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, "IO exception", e);
        }
    }
    
    public boolean killPlayback() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying())
                mediaPlayer.stop();
            currPosition = mediaPlayer.getCurrentPosition() - 1000;
            mediaPlayer.reset();
            if (Build.VERSION.SDK_INT <
                    Build.VERSION_CODES.HONEYCOMB)
                new Util.SavePositionTask(currPosition, mEpisodeId).execute();
            else
                new Util.SavePositionTask(currPosition, mEpisodeId)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            stopForeground(true);
            isInit = false;
            ApplicationEx.setIsPlaying(false);
            return true;
        }
        else
            return false;
    }
    
    public boolean togglePlayback(int offset) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mRemoteControlClientCompat.setPlaybackState(
                    RemoteControlClientCompat.PLAYSTATE_PAUSED);
            mediaPlayer.stop();
            currPosition = mediaPlayer.getCurrentPosition() - offset;
            sendBroadcast(new Intent(Constants.ACTION_PAUSE_PLAYBACK));
            mediaPlayer.release();
            mediaPlayer = null;
            if (Build.VERSION.SDK_INT <
                    Build.VERSION_CODES.HONEYCOMB)
                new Util.SavePositionTask(currPosition, mEpisodeId).execute();
            else
                new Util.SavePositionTask(currPosition, mEpisodeId)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            stopForeground(true);
            isInit = false;
            ApplicationEx.setIsPlaying(false);
            return false;
        }
        isInit = true;
        if (mEpisodeId > -1) {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            if (Build.VERSION.SDK_INT <
                    Build.VERSION_CODES.HONEYCOMB)
                new SetupNotificationTask().execute();
            else
                new SetupNotificationTask().executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR);
            initPlayer();
            return true;
        }
        return false;
    }
    
    public void seekTo(int newPosition) {
        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.HONEYCOMB)
            new Util.SavePositionTask(newPosition, mEpisodeId).execute();
        else
            new Util.SavePositionTask(newPosition, mEpisodeId)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        if (mediaPlayer != null)
            mediaPlayer.seekTo(newPosition);
    }
    
    public int getDuration() {
        return currDuration;
    }
    
    public int getEpisodeId() {
        return mEpisodeId;
    }
    
    public boolean isNull() {
        return mediaPlayer == null ? true : false;
    }
    
    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }
        return -1;
    }
    
    public int getCurrentProgress() {
        return currPosition;
    }
    
    public double getCurrentSpeed() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentSpeedMultiplier();
        }
        return currSpeed;
    }
    
    public double getEpisodeSpeed() {
        return currSpeed;
    }
    
    public boolean isPlaying() {
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }
    
    private int rewindMsec = 30000;
    private int ffwdMsec = 30000;
    
    public boolean rewind() {
        if (mediaPlayer != null) {
            int seekTo = mediaPlayer.getCurrentPosition()-rewindMsec;
            if (seekTo < 0)
                seekTo = 0;
            mediaPlayer.seekTo(seekTo);
            currPosition = mediaPlayer.getCurrentPosition();
            if (Build.VERSION.SDK_INT <
                    Build.VERSION_CODES.HONEYCOMB)
                new Util.SavePositionTask(currPosition, mEpisodeId).execute();
            else
                new Util.SavePositionTask(currPosition, mEpisodeId)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return true;
        }
        return false;
    }
    
    public boolean previous() {
        if (mEpisodeIndex > 0) {
            mEpisodeIndex--;
            mEpisodeId = epIdList.get(mEpisodeIndex);
            ApplicationEx.setCurrentEpisode(
                    ApplicationEx.dbHelper.getEpisode(mEpisodeId));
            if (Build.VERSION.SDK_INT <
                    Build.VERSION_CODES.HONEYCOMB)
                new SetupNotificationTask().execute();
            else
                new SetupNotificationTask().executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR);
            ApplicationEx.dbHelper.setCurrentEpisode(mEpisodeId);
            ApplicationEx.setCurrentEpisode(
                    ApplicationEx.dbHelper.getEpisode(mEpisodeId));
            isInit = true;
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    currPosition = mediaPlayer.getCurrentPosition() - 2000;
                    if (Build.VERSION.SDK_INT <
                            Build.VERSION_CODES.HONEYCOMB)
                        new Util.SavePositionTask(currPosition, mEpisodeId)
                                .execute();
                    else
                        new Util.SavePositionTask(currPosition, mEpisodeId)
                                .executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR);
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                if (mEpisodeId > -1) {
                    preparePlayer();
                }
            }
            else {
                initPlayer();
            }
        }
        return false;
    }
    
    public boolean ffwd() {
        if (mediaPlayer != null) {
            int seekTo = mediaPlayer.getCurrentPosition()+ffwdMsec;
            if (seekTo >= mediaPlayer.getDuration())
                seekTo = mediaPlayer.getDuration()-1000;
            mediaPlayer.seekTo(seekTo);
            currPosition = mediaPlayer.getCurrentPosition();
            if (Build.VERSION.SDK_INT <
                    Build.VERSION_CODES.HONEYCOMB)
                new Util.SavePositionTask(currPosition, mEpisodeId).execute();
            else
                new Util.SavePositionTask(currPosition, mEpisodeId)
                        .executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR);
            return true;
        }
        return false;
    }
    
    public boolean skip() {
        if (mediaPlayer != null) {
            int seekTo = mediaPlayer.getDuration();
            mediaPlayer.seekTo(seekTo);
            currPosition = mediaPlayer.getCurrentPosition();
            if (Build.VERSION.SDK_INT <
                    Build.VERSION_CODES.HONEYCOMB)
                new Util.SavePositionTask(currPosition, mEpisodeId).execute();
            else
                new Util.SavePositionTask(currPosition, mEpisodeId)
                        .executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR);
            return true;
        }
        return false;
    }
    
    public boolean isInit() {
        return isInit;
    }
    
    public void addClient(Messenger messenger) {
        clients.add(messenger);
    }
    
    public void removeClient(Messenger messenger) {
        clients.remove(messenger);
    }
    
    public double getSpeed() {
        return currSpeed;
    }
    
    public ArrayList<Double> getSpeedList() {
        return speedList;
    }
    
    public void setSpeedList(ArrayList<Double> speedList) {
        this.speedList = speedList;
        Util.persistSpeedList(this.speedList);
    }
    
    public void setSpeed(int index) {
        currSpeed = speedList.get(index);
        setSpeed();
    }
    
    public void addSpeed(double newSpeed) {
        Double tempSpeed;
        for (int i = 0; i <= speedList.size(); i++) {
            if (i == speedList.size()) {
                speedList.add(Double.valueOf(twoDForm.format(newSpeed)));
                break;
            }
            tempSpeed = speedList.get(i);
            if (newSpeed < tempSpeed) {
                speedList.add(speedList.indexOf(tempSpeed), 
                        Double.valueOf(twoDForm.format(newSpeed)));
                break;
            }
        }
        Util.persistSpeedList(speedList);
    }
    
    class SaveSpeedTask extends AsyncTask<Void, Void, Void> {
        
        private double speed;
        private int episodeId;
        
        public SaveSpeedTask(double speed, int episodeId) {
            this.speed = speed;
            this.episodeId = episodeId;
        }
        
        @Override
        protected Void doInBackground(Void... nothing) {
            Util.writeBufferToFile(Float.toString((float)speed).getBytes(),
                    Constants.SPEED_FILENAME);
            ApplicationEx.dbHelper.setFeedSpeed(speed, episodeId);
            ApplicationEx.setCurrentSpeed(currSpeed);
            sendBroadcast(new Intent(Constants.ACTION_SPEED_CHANGED));
            return null;
        }
    }
    
    private void setSpeed() {
        mediaPlayer.setPlaybackSpeed((float) currSpeed);
        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.HONEYCOMB)
            new SaveSpeedTask(currSpeed, mEpisodeId).execute();
        else
            new SaveSpeedTask(currSpeed, mEpisodeId).executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    class AudioConnectionReceiver extends BroadcastReceiver {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isPlugged = ApplicationEx.isPlugged();
            int headsetState = -1;
            if (intent.getAction().equalsIgnoreCase(
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                    togglePlayback(3000);
            }
            else if (intent.getAction().equalsIgnoreCase(
                    Intent.ACTION_HEADSET_PLUG)) {
                isPlugged = (intent.getIntExtra("state", 0) == 1 ? true : false)
                        || ApplicationEx.isA2DPOn();
            }
            else if (intent.getAction().equalsIgnoreCase(
                    BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                isPlugged = ApplicationEx.isA2DPOn() || 
                        ((AudioManager) getSystemService(Context.AUDIO_SERVICE))
                            .isWiredHeadsetOn();
            }
            if (!isPlugged && isPlugged != ApplicationEx.isPlugged() &&
                    mediaPlayer != null && mediaPlayer.isPlaying()) {
                togglePlayback(3000);
            }
            ApplicationEx.setPlugged(isPlugged);
        }
     
    }
    
    class PhoneReceiver extends BroadcastReceiver {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(
                    TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                int callState = ((TelephonyManager)getSystemService(
                        Context.TELEPHONY_SERVICE)).getCallState();
            }
        }
     
    }
    
    private final IBinder mBinder = new PlaybackBinder();
    
    public class PlaybackBinder extends Binder {
        public PlaybackService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PlaybackService.this;
        }
    }
    
    private static Method mRegisterMediaButtonEventReceiver;
    private static Method mUnregisterMediaButtonEventReceiver;
    private ComponentName mRemoteControlResponder;
    
    private void registerRemoteControl() {
        try {
            if (mRegisterMediaButtonEventReceiver == null) {
                return;
            }
            mRegisterMediaButtonEventReceiver.invoke(audioManager,
                    mRemoteControlResponder);
        } catch (InvocationTargetException ite) {
            /* unpack original exception when possible */
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                /* unexpected checked exception; wrap and re-throw */
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            Log.e(Constants.LOG_TAG, "unexpected " + ie);
        }
    }
    
    private void unregisterRemoteControl() {
        try {
            if (mUnregisterMediaButtonEventReceiver == null) {
                return;
            }
            mUnregisterMediaButtonEventReceiver.invoke(audioManager,
                    mRemoteControlResponder);
        } catch (InvocationTargetException ite) {
            /* unpack original exception when possible */
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                /* unexpected checked exception; wrap and re-throw */
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            System.err.println("unexpected " + ie);  
        }
    }
    
    private static void initializeRemoteControlRegistrationMethods() {
        try {
           if (mRegisterMediaButtonEventReceiver == null) {
              mRegisterMediaButtonEventReceiver = AudioManager.class.getMethod(
                    "registerRemoteControlClient",
                    new Class[] { ComponentName.class } );
           }
           if (mUnregisterMediaButtonEventReceiver == null) {
              mUnregisterMediaButtonEventReceiver =
                  AudioManager.class.getMethod("unregisterRemoteControlClient",
                          new Class[] { ComponentName.class } );
           }
           /* success, this device will take advantage of better remote */
           /* control event handling                                    */
        } catch (NoSuchMethodException nsme) {
           /* failure, still using the legacy behavior, but this app    */
           /* is future-proof!                                          */
        }
     }

}

