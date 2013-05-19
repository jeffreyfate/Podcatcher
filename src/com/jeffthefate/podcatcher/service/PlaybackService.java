package com.jeffthefate.podcatcher.service;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
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
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.aocate.media.MediaPlayer;
import com.aocate.media.MediaPlayer.OnCompletionListener;
import com.aocate.media.MediaPlayer.OnErrorListener;
import com.aocate.media.MediaPlayer.OnInfoListener;
import com.aocate.media.MediaPlayer.OnPreparedListener;
import com.aocate.media.MediaPlayer.OnSeekCompleteListener;
import com.aocate.media.MediaPlayer.OnSpeedAdjustmentAvailableChangedListener;
import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.DatabaseHelper;
import com.jeffthefate.podcatcher.MediaButtonHelper;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.RemoteControlClientCompat;
import com.jeffthefate.podcatcher.RemoteControlHelper;
import com.jeffthefate.podcatcher.Util;
import com.jeffthefate.podcatcher.activity.ActivityMain;
import com.jeffthefate.podcatcher.activity.ActivityPlayback;
import com.jeffthefate.podcatcher.receiver.MusicIntentReceiver;

/**
 * Service that runs in the background.  Registers receivers for actions that
 * the app will respond to.  Also, handles starting the widget updates.
 * 
 * @author Jeff
 */
public class PlaybackService extends Service implements OnSeekCompleteListener,
        OnSpeedAdjustmentAvailableChangedListener, OnPreparedListener,
        OnCompletionListener, OnErrorListener, OnInfoListener {
    
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
    
    public static final int SPEED_ENABLED = 0;
    public static final int SPEED_DISABLED = 1;
    public static final int PLAYLIST_EMPTY = 2;
    
    int mEpisodeId = -1;
    int mEpisodeIndex = -1;
    Uri mMediaUri;
    
    Builder nBuilderOngoing;
    Builder nBuilderSingle;
    NotificationManager nManager;
    PendingIntent playPendingIntent;
    MediaPlayer mediaPlayer;
    
    ArrayList<Integer> epIdList;
    
    UpdatePlaylistReceiver updatePlaylistReceiver;
    MediaButtonReceiver mediaButtonReceiver;
    AudioConnectionReceiver audioConnectionReceiver;
    
    private boolean isInit = false;
    private boolean isError = false;
    
    public static ArrayList<Double> speedList;
    
    private double currSpeed;
    private int currPosition;
    private int currDuration;
    
    private ArrayList<Messenger> clients;
    
    private Context mContext = this;
    public AudioManager audioManager;
    private ComponentName mMediaButtonReceiverComponent;
    public RemoteControlClientCompat mRemoteControlClientCompat;
    
    DecimalFormat twoDForm = new DecimalFormat(Constants.DEC_FORMAT);
    
    private boolean newPlayer = false;
    private boolean speedAdjustmentAvailable = false;
    
    private class SpeedTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... nothing) {
            while (!speedAdjustmentAvailable) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return null;
        }
        
        protected void onPostExecute(Void nothing) {
            setupPlayback();
        }
    }
    
    private PowerManager.WakeLock wakeLock;
    
    @SuppressLint("NewApi")
    @Override
    public void onCreate() {
        super.onCreate();
        nManager = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        PowerManager pm = 
                (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                Constants.DOWNLOAD_WAKE_LOCK);
        clients = new ArrayList<Messenger>();
        updatePlaylistReceiver = new UpdatePlaylistReceiver();
        mediaButtonReceiver = new MediaButtonReceiver();
        audioConnectionReceiver = new AudioConnectionReceiver();
        registerReceiver(updatePlaylistReceiver, 
                new IntentFilter(Constants.ACTION_NEW_DOWNLOAD));
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        intentFilter.addAction(
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(audioConnectionReceiver, intentFilter);
        speedList = Util.readSpeedList();
        if (speedList.isEmpty()) {
            speedList.add(Double.valueOf(twoDForm.format(0.5)));
            speedList.add(Double.valueOf(twoDForm.format(0.75)));
            speedList.add(Double.valueOf(twoDForm.format(1.0)));
            speedList.add(Double.valueOf(twoDForm.format(1.25)));
            speedList.add(Double.valueOf(twoDForm.format(1.5)));
            Util.persistSpeedList(PlaybackService.speedList);
        }
        currSpeed = ApplicationEx.dbHelper.getCurrentSpeed();
        mEpisodeId = ApplicationEx.dbHelper.getCurrentEpisode();
        currDuration = ApplicationEx.dbHelper.getEpisodeDuration(mEpisodeId);
        nBuilderOngoing = new NotificationCompat.Builder(
                ApplicationEx.getApp())
            .setSmallIcon(R.drawable.ic_notification_playback);
        Intent serviceIntent = new Intent(ApplicationEx.getApp(), 
                UpdateService.class);
        serviceIntent.putExtra(Constants.SYNC, ApplicationEx.isSyncing() &&
                Util.readBooleanFromFile(Constants.GOOGLE_FILENAME));
        nBuilderOngoing.addAction(
                R.drawable.btn_playback_rew_normal_holo_dark, null,
                PendingIntent.getBroadcast(ApplicationEx.getApp(), 0,
                        new Intent(Util.readBooleanPreference(
                            R.string.controls_key, false) ?
                                PlaybackService.ACTION_PREVIOUS
                                    : PlaybackService.ACTION_BEHIND), 0));
        nBuilderOngoing.addAction(
                R.drawable.btn_playback_pause_normal_holo_dark, null,
                PendingIntent.getBroadcast(ApplicationEx.getApp(), 0,
                        new Intent(ACTION_TOGGLE_PLAYBACK), 0));
        nBuilderOngoing.addAction(
                R.drawable.btn_playback_ff_normal_holo_dark, null,
                PendingIntent.getBroadcast(ApplicationEx.getApp(), 0,
                        new Intent(Util.readBooleanPreference(
                            R.string.controls_key, false) ?
                                    PlaybackService.ACTION_NEXT :
                                        PlaybackService.ACTION_AHEAD), 0));
        Intent notificationIntent = new Intent(mContext, 
                ActivityPlayback.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP | 
                Intent.FLAG_ACTIVITY_NO_HISTORY);
        playPendingIntent = PendingIntent.getActivity(mContext, 0, 
                notificationIntent, 0);
        nBuilderOngoing.setContentIntent(playPendingIntent);
        nBuilderSingle = new NotificationCompat.Builder(ApplicationEx.getApp());
        epIdList = Util.readCurrentPlaylist();
        intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_TOGGLE_PLAYBACK);
        intentFilter.addAction(ACTION_PLAY);
        intentFilter.addAction(ACTION_PAUSE);
        intentFilter.addAction(ACTION_NEXT);
        intentFilter.addAction(ACTION_PREVIOUS);
        intentFilter.addAction(ACTION_AHEAD);
        intentFilter.addAction(ACTION_BEHIND);
        registerReceiver(mediaButtonReceiver, intentFilter);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMediaButtonReceiverComponent = new ComponentName(this,
                MusicIntentReceiver.class);
    }
    
    @SuppressLint("NewApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (ApplicationEx.isExternalStorageAvailable() && 
                ApplicationEx.canAccessFeedsDir()) {
            if (intent != null) {
                if (intent.hasExtra(Constants.EPS)) {
                    epIdList = intent.getIntegerArrayListExtra(Constants.EPS);
                    Util.persistCurrentPlaylist(epIdList);
                }
                if (intent.hasExtra(Constants.START_EP)) {
                    if (mEpisodeId != intent.getIntExtra(Constants.START_EP,
                            -1)) {
                        if (mediaPlayer != null) {
                            if (mediaPlayer.isPlaying()) {
                                currPosition = mediaPlayer.getCurrentPosition();
                                Util.savePosition(currPosition, mEpisodeId);
                                mediaPlayer.stop();
                            }
                        }
                        mEpisodeId = intent.getIntExtra(Constants.START_EP, -1);
                        ApplicationEx.setCurrentEpisode(mEpisodeId);
                        updateNotification();
                    }
                }
            }
            if (epIdList != null) {
                isInit = intent.getBooleanExtra(Constants.INIT, true);
                if (mEpisodeId < 0) {
                    if (epIdList.isEmpty())
                        isInit = false;
                    else {
                        mEpisodeId = epIdList.get(0);
                        currDuration =
                            ApplicationEx.dbHelper.getEpisodeDuration(
                                    mEpisodeId);
                    }
                }
                if (isInit) {
                    if (mediaPlayer == null || !mediaPlayer.isPlaying())
                        togglePlayback(0, false);
                }
                if (intent.getBooleanExtra(Constants.ACTIVITY, true)) {
                    Intent playIntent = new Intent(ApplicationEx.getApp(),
                            ActivityPlayback.class);
                    playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP | 
                            Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(playIntent);
                }
            }
        }
        else {
            nBuilderSingle.setContentText(Constants.UNABLE_TO_PLAY);
            Intent notificationIntent = new Intent(this, ActivityMain.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, 
                    notificationIntent, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            nBuilderSingle.setContentIntent(pendingIntent);
            nManager.notify(1341, nBuilderSingle.build());
        }
        mEpisodeIndex = epIdList.indexOf(mEpisodeId);
        return Service.START_STICKY_COMPATIBILITY;
    }
    
    @Override
    public void onDestroy() {
        unregisterReceiver(updatePlaylistReceiver);
        unregisterReceiver(audioConnectionReceiver);
        unregisterReceiver(mediaButtonReceiver);
        stopForeground(true);
        super.onDestroy();
    }
    
    private boolean canDuck = false;
    
    OnAudioFocusChangeListener afChangeListener = 
            new OnAudioFocusChangeListener() {
        @SuppressLint("NewApi")
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
            // Gain back focus after lost - by some other entity
            case AudioManager.AUDIOFOCUS_GAIN:
                if (ApplicationEx.getIsPlaying() && !canDuck)
                    togglePlayback(0, false);
                canDuck = false;
                break;
            // Lost focus to some other entity for undetermined time
            case AudioManager.AUDIOFOCUS_LOSS:
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                        togglePlayback(0, false);
                teardownRemoteControl();
                wakeLock.release();
                break;
            // Lost focus to another entity for short time
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                    togglePlayback(0, true);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                switch (Integer.parseInt(ApplicationEx.sharedPrefs.getString(
                        getString(R.string.notification_key), "0"))) {
                case 0:
                    canDuck = true;
                    break;
                case 1:
                    canDuck = false;
                    togglePlayback(0, true);
                    break;
                case 2:
                    canDuck = true;
                    mediaPlayer.setVolume(0.5f, 0.5f);
                    break;
                default:
                    break;
                }
            }
        }
    };
    
    class UpdatePlaylistReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<Integer> idList = new ArrayList<Integer>();
            if (intent.hasExtra(Constants.ID_LIST)) {
                idList = intent.getIntegerArrayListExtra(Constants.ID_LIST);
                epIdList = idList;
                Util.persistCurrentPlaylist(epIdList);
                if (epIdList.isEmpty()) {
                    mEpisodeId = -1;
                    currDuration = 0;
                    ApplicationEx.setCurrentEpisode(mEpisodeId);
                    if (mRemoteControlClientCompat != null)
                        mRemoteControlClientCompat.setPlaybackState(
                                RemoteControlClientCompat.PLAYSTATE_PAUSED);
                    teardownRemoteControl();
                    audioManager.abandonAudioFocus(afChangeListener);
                    sendBroadcast(new Intent(Constants.ACTION_PAUSE_PLAYBACK));
                    mediaPlayer.release();
                    stopForeground(true);
                    isInit = false;
                    for (Messenger client : clients) {
                        Message mMessage = Message.obtain(null, PLAYLIST_EMPTY);
                        try {
                            client.send(mMessage);
                        } catch (RemoteException e) {
                            Log.e(Constants.LOG_TAG, "Can't connect to " +
                                    "PlaybackActivity", e);
                        }
                    }
                    broadcastEmptyPlaylist();
                }
            }
        }
    }
    
    class MediaButtonReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_TOGGLE_PLAYBACK))
                togglePlayback(0, false);
            else if (action.equals(ACTION_PLAY))
                togglePlayback(0, false);
            else if (action.equals(ACTION_PAUSE))
                togglePlayback(0, false);
            else if (action.equals(ACTION_NEXT))
                next();
            else if (action.equals(ACTION_PREVIOUS))
                previous();
            else if (action.equals(ACTION_AHEAD))
                ffwd(null);
            else if (action.equals(ACTION_BEHIND))
                rewind(null);
        }
    }
    
    @Override
    public void onSpeedAdjustmentAvailableChanged(MediaPlayer player,
            boolean isAdjustmentAvailable) {
        Log.e(Constants.LOG_TAG, "onSpeedAdjustmentAvailableChanged: " + isAdjustmentAvailable);
        if (isAdjustmentAvailable) {
            for (Messenger client : clients) {
                Message mMessage = Message.obtain(null, SPEED_ENABLED);
                try {
                    client.send(mMessage);
                } catch (RemoteException e) {
                    Log.e(Constants.LOG_TAG, "Can't connect to " +
                            "PlaybackActivity", e);
                }
            }
        }
        else {
            // It was just available
            if (speedAdjustmentAvailable) {
                // Try to setup the player and play again
                initPlayer();
            }
            else {
                // TODO something in the UI here?
                // Disable the speed button?
                for (Messenger client : clients) {
                    Message mMessage = Message.obtain(null, SPEED_DISABLED);
                    try {
                        client.send(mMessage);
                    } catch (RemoteException e) {
                        Log.e(Constants.LOG_TAG, "Can't connect to " +
                                "PlaybackActivity", e);
                    }
                }
            }
        }
        speedAdjustmentAvailable = isAdjustmentAvailable;
        newPlayer = false;
    }
    
    @SuppressLint("NewApi")
    @Override
    public void onPrepared(MediaPlayer player) {
        isError = false;
        if (newPlayer) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                new SpeedTask().execute();
            else
                new SpeedTask().executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else
            setupPlayback();
    }
    
    @Override
    public void onCompletion(MediaPlayer player) {
        completePlayback(false);
    }
    
    @SuppressLint("NewApi")
    @Override
    public boolean onError(MediaPlayer player, int type, int extra) {
        Log.d(Constants.LOG_TAG, "type: " + type);
        Log.d(Constants.LOG_TAG, "extra: " + extra);
        if (mediaPlayer != null) {
            // TODO notify user there was an error?
            // and figure out how to possibly handle the errors
            //isError = true;
            if (type == 1 && extra == 6) {
                killPlayback();
                completePlayback(true);
            }
            else if (type == 1 && extra == 0 && !isError) {
                // TODO try a few times then give up?
                isError = true;
                killPlayback();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
                initPlayer();
            }
            else if (type == -38 && extra == 0) {
                ApplicationEx.dbHelper.markRead(mEpisodeId, true);
                new File(ApplicationEx.dbHelper.getEpisodeLocation(
                        mEpisodeId)).delete();
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
                Util.savePosition(0, mEpisodeId);
                if (epIdList.isEmpty()) {
                    ApplicationEx.dbHelper.setCurrentEpisode(-1);
                    stopForeground(true);
                    for (Messenger client : clients) {
                        Message mMessage = Message.obtain(null, 
                                PLAYLIST_EMPTY);
                        try {
                            client.send(mMessage);
                        } catch (RemoteException e) {
                            Log.e(Constants.LOG_TAG, "Can't connect " +
                                    "to PlaybackActivity", e);
                        }
                    }
                    broadcastEmptyPlaylist();
                }
                else {
                    if (mEpisodeIndex >= epIdList.size()-1 ||
                            mEpisodeIndex < 0)
                        mEpisodeIndex = 0;
                    mEpisodeId = epIdList.get(mEpisodeIndex);
                    ApplicationEx.setCurrentEpisode(mEpisodeId);
                    updateNotification();
                    isInit = true;
                    if (mEpisodeId > -1) {
                        preparePlayer();
                    }
                }
            }
            else {
                Util.savePosition(currPosition, mEpisodeId);
                stopForeground(true);
                ApplicationEx.setIsPlaying(false);
            }
        }
        return true;
    }
    
    @Override
    public boolean onInfo(MediaPlayer player, int what, int extra) {
        Log.d(Constants.LOG_TAG, "what: " + what);
        Log.d(Constants.LOG_TAG, "extra: " + extra);
        return true;
    }
    
    @Override
    public void onSeekComplete(MediaPlayer arg0) {
        Log.i(Constants.LOG_TAG, "seek complete");
        sendBroadcast(new Intent(Constants.ACTION_SEEK_DONE));
    }
    
    public void initPlayer() {
        currPosition = ApplicationEx.dbHelper.getEpisodeProgress(mEpisodeId);
        currSpeed = ApplicationEx.dbHelper.getFeedSpeed(mEpisodeId);
        ApplicationEx.setCurrentSpeed(currSpeed);
        if (mediaPlayer != null) {
            if (!isError && mediaPlayer.isPlaying())
                togglePlayback(0, false);
        }
        else {
            mediaPlayer = new MediaPlayer(ApplicationEx.getApp(), true);
            newPlayer = true;
        }
        mediaPlayer.setOnSpeedAdjustmentAvailableChangedListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        if (mEpisodeId >= 0)
            preparePlayer();        
    }
    
    @SuppressLint("NewApi")
    private void completePlayback(boolean init) {
        mEpisodeIndex = epIdList.indexOf(mEpisodeId);
        mEpisodeIndex++;
        // TODO Use setting values
        /*
        ApplicationEx.dbHelper.markRead(mEpisodeId, true);
        new File(ApplicationEx.dbHelper.getEpisodeLocation(mEpisodeId))
                .delete();
        ApplicationEx.dbHelper.episodeDownloaded(mEpisodeId, false);
        epIdList.remove((Integer)mEpisodeId);
        Util.persistCurrentPlaylist(epIdList);
        */
        sendBroadcast(new Intent(Constants.ACTION_REFRESH_FEEDS));
        sendBroadcast(new Intent(Constants.ACTION_REFRESH_EPISODES));
        sendBroadcast(new Intent(Constants.ACTION_REFRESH_DURATION));
        ApplicationEx.setIsPlaying(false);
        Util.savePosition(0, mEpisodeId);
        if (epIdList.isEmpty()) {
            mEpisodeId = -1;
            currDuration = 0;
            ApplicationEx.setCurrentEpisode(mEpisodeId);
            if (mRemoteControlClientCompat != null)
                mRemoteControlClientCompat.setPlaybackState(
                        RemoteControlClientCompat.PLAYSTATE_PAUSED);
            teardownRemoteControl();
            audioManager.abandonAudioFocus(afChangeListener);
            sendBroadcast(new Intent(Constants.ACTION_PAUSE_PLAYBACK));
            mediaPlayer.release();
            stopForeground(true);
            isInit = false;
            for (Messenger client : clients) {
                Message mMessage = Message.obtain(null, PLAYLIST_EMPTY);
                try {
                    client.send(mMessage);
                } catch (RemoteException e) {
                    Log.e(Constants.LOG_TAG, "Can't connect to " +
                            "PlaybackActivity", e);
                }
            }
            broadcastEmptyPlaylist();
        }
        // TODO Use setting to determine continuous playback
        else if (epIdList.size() > 1) {
            if (mEpisodeIndex >= epIdList.size() || mEpisodeIndex < 0)
                mEpisodeIndex = 0;
            mEpisodeId = epIdList.get(mEpisodeIndex);
            ApplicationEx.setCurrentEpisode(mEpisodeId);
            updateNotification();
            isInit = true;
            if (mEpisodeId > -1) {
                if (init)
                    initPlayer();
                else
                    preparePlayer();
            }
        }
    }
    
    @SuppressLint("NewApi")
    private void setupPlayback() {
        if (currPosition <= 0)
            currPosition = 1;
        mediaPlayer.seekTo(currPosition);
        if (isInit)
            getAudioFocus();
    }
    
    public void getAudioFocus() {
        int result = audioManager.requestAudioFocus(afChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            nBuilderSingle.setContentText(Constants.UNABLE_TO_PLAY);
            Intent notificationIntent = new Intent(this, ActivityMain.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationIntent, 
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            nBuilderSingle.setContentIntent(pendingIntent);
            nManager.notify(1341, nBuilderSingle.build());
        }
        else {
            wakeLock.acquire();
            setupRemoteControl();
            if (speedAdjustmentAvailable)
                setSpeed();
            mediaPlayer.setVolume(1.0f, 1.0f);
            mediaPlayer.start();
            sendBroadcast(new Intent(Constants.ACTION_START_PLAYBACK));
            ApplicationEx.setIsPlaying(true);
        }
    }
    
    private void teardownRemoteControl() {
        MediaButtonHelper.unregisterMediaButtonEventReceiverCompat(audioManager,
                mMediaButtonReceiverComponent);
    }
    
    private void setupRemoteControl() {
        MediaButtonHelper.registerMediaButtonEventReceiverCompat(
                audioManager, mMediaButtonReceiverComponent);
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
        String[] episode = ApplicationEx.getCurrentEpisode();
        if (episode != null) {
            mRemoteControlClientCompat.editMetadata(true)
                .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM,
                        episode[2])
                .putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
                        episode[1])
                .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
                        Long.parseLong(episode[5]))
                .putBitmap(RemoteControlClientCompat.MetadataEditorCompat
                        .METADATA_KEY_ARTWORK, episode[0] != null ?
                                BitmapFactory.decodeFile((episode[0])) : null)
                .apply();
        }
    }
    
    class SetupNotificationTask extends AsyncTask<Void, Void, Void> {
        
        @Override
        protected Void doInBackground(Void... nothing) {
            currDuration = ApplicationEx.dbHelper.getEpisodeDuration(mEpisodeId);
            currPosition = ApplicationEx.dbHelper.getEpisodeProgress(mEpisodeId);
            currSpeed = ApplicationEx.dbHelper.getFeedSpeed(mEpisodeId);
            nBuilderOngoing.setContentTitle(
                    ApplicationEx.getCurrentEpisode()[1]);
            nBuilderOngoing.setContentText(
                    ApplicationEx.getCurrentEpisode()[2]);
            nBuilderOngoing.setStyle(new NotificationCompat.BigTextStyle()
                    .setBigContentTitle(ApplicationEx.getCurrentEpisode()[1])
                    .bigText(ApplicationEx.getCurrentEpisode()[3])
                    .setSummaryText(ApplicationEx.getCurrentEpisode()[2]));
            int feedId = ApplicationEx.dbHelper.getEpisodeFeedId(mEpisodeId);
            String imageLocation = TextUtils.concat(ApplicationEx.cacheLocation,
                    Constants.FEEDS_LOCATION, Integer.toString(feedId),
                    File.separator).toString();
            Bitmap bitmap = null;
            if (Util.findFile(imageLocation, TextUtils.concat(
                    Integer.toString(feedId), Constants.SMALL, Constants.PNG)
                            .toString())) {
                bitmap = BitmapFactory.decodeFile(TextUtils.concat(
                        imageLocation, Integer.toString(feedId),
                        Constants.SMALL, Constants.PNG).toString());
                nBuilderOngoing.setLargeIcon(bitmap);
            }
            nBuilderOngoing.setWhen(ApplicationEx.dbHelper.getLong(
                    Integer.toString(mEpisodeId),
                    DatabaseHelper.COL_EPISODE_PUB,
                    DatabaseHelper.EPISODE_TABLE,
                    DatabaseHelper.COL_EPISODE_ID));
            return null;
        }
        
        protected void onPostExecute(Void nothing) {
            if (isInit)
                startForeground(1339, nBuilderOngoing.build());
        }
        
    }
    
    public void preparePlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.reset();
            } catch (NullPointerException e) {
                // TODO Error to user
            }
        }
        File externalFile = new File(ApplicationEx.dbHelper
                .getEpisodeLocation(mEpisodeId));
        mMediaUri = Uri.fromFile(externalFile);
        currPosition = ApplicationEx.dbHelper.getEpisodeProgress(mEpisodeId);
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
    
    @SuppressLint("NewApi")
    public void killPlayback() {
        /*
        currPosition = mediaPlayer.getCurrentPosition();
        Log.v(Constants.LOG_TAG, "saving position: " + currPosition);
        Util.savePosition(currPosition, mEpisodeId);
        */
        if (mediaPlayer.isPlaying())
            mediaPlayer.stop();
        stopForeground(true);
        isInit = false;
        ApplicationEx.setIsPlaying(false);
    }
    
    @SuppressLint("NewApi")
    public boolean togglePlayback(int offset, boolean isTransient) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.setVolume(1.0f, 1.0f);
            if (mRemoteControlClientCompat != null)
                mRemoteControlClientCompat.setPlaybackState(
                        RemoteControlClientCompat.PLAYSTATE_PAUSED);
            currPosition = mediaPlayer.getCurrentPosition() - offset;
            Util.savePosition(currPosition, mEpisodeId);
            mediaPlayer.stop();
            sendBroadcast(new Intent(Constants.ACTION_PAUSE_PLAYBACK));
            stopForeground(true);
            isInit = false;
            if (isTransient)
                ApplicationEx.setIsPlaying(true);
            else
                ApplicationEx.setIsPlaying(false);
            return false;
        }
        isInit = true;
        if (mEpisodeId > -1) {
            ApplicationEx.setCurrentEpisode(mEpisodeId);
            updateNotification();
            initPlayer();
            return true;
        }
        return false;
    }
    
    @SuppressLint("NewApi")
    public void seekTo(int newPosition, View image) {
        if (newPosition <= 0)
            newPosition = 1;
        currPosition = newPosition;
        if (image != null)
            image.setEnabled(false);
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seekTo(newPosition);
            } catch (IllegalStateException e) {}
        }
        Util.savePosition(currPosition, mEpisodeId);
        if (image != null)
            image.setEnabled(true);
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
    
    public int getCurrentProgress() {
        currPosition = mediaPlayer.getCurrentPosition();
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
    
    @SuppressLint("NewApi")
    public boolean rewind(View image) {
        if (mediaPlayer != null) {
            int seekTo = mediaPlayer.getCurrentPosition() -
                    Integer.parseInt(ApplicationEx.sharedPrefs.getString(
                            getString(R.string.rewsecs_key), "30000"));
            seekTo(seekTo, image);
            sendBroadcast(new Intent(Constants.ACTION_BACKWARD_PLAYBACK));
            return true;
        }
        return false;
    }
    
    @SuppressLint("NewApi")
    public boolean previous() {
        if (mEpisodeIndex > 0) {
            mEpisodeIndex--;
            mEpisodeId = epIdList.get(mEpisodeIndex);
            ApplicationEx.setCurrentEpisode(mEpisodeId);
            updateNotification();
            isInit = true;
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    currPosition = mediaPlayer.getCurrentPosition();
                    Util.savePosition(currPosition, mEpisodeId);
                    mediaPlayer.stop();
                }
                if (mEpisodeId > -1)
                    preparePlayer();
            }
            else
                initPlayer();
        }
        return false;
    }
    
    @SuppressLint("NewApi")
    public boolean next() {
        completePlayback(false);
        return false;
    }
    
    @SuppressLint("NewApi")
    public boolean ffwd(View image) {
        if (mediaPlayer != null) {
            int seekTo = mediaPlayer.getCurrentPosition() +
                    Integer.parseInt(ApplicationEx.sharedPrefs.getString(
                            getString(R.string.ffwdsecs_key), "30000"));
            if (seekTo >= currDuration)
                seekTo = currDuration;
            seekTo(seekTo, image);
            sendBroadcast(new Intent(Constants.ACTION_FORWARD_PLAYBACK));
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
        PlaybackService.speedList = speedList;
        Util.persistSpeedList(PlaybackService.speedList);
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
            ApplicationEx.dbHelper.setCurrentSpeed(speed);
            ApplicationEx.dbHelper.setFeedSpeed(speed, episodeId);
            ApplicationEx.setCurrentSpeed(currSpeed);
            sendBroadcast(new Intent(Constants.ACTION_SPEED_CHANGED));
            return null;
        }
    }
    
    @SuppressLint("NewApi")
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
        
        @SuppressWarnings("deprecation")
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isPlugged = ApplicationEx.isPlugged();
            boolean pausePlayback = ApplicationEx.sharedPrefs.getBoolean(
                    getString(R.string.headphone_key), false);
            int pauseTime = Integer.parseInt(
                    ApplicationEx.sharedPrefs.getString(
                    getString(R.string.headphonerewind_key), "0")) * 1000;
            if (intent.getAction().equalsIgnoreCase(
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                Log.v(Constants.LOG_TAG, "ACTION_AUDIO_BECOMING_NOISY");
                if (mediaPlayer != null && mediaPlayer.isPlaying() &&
                        pausePlayback)
                    togglePlayback(pauseTime, false);
            }
            else if (intent.getAction().equalsIgnoreCase(
                    Intent.ACTION_HEADSET_PLUG)) {
                Log.v(Constants.LOG_TAG, "ACTION_HEADSET_PLUG");
                isPlugged = (intent.getIntExtra(Constants.STATE, 0) == 1 ? true : false)
                        || ApplicationEx.isA2DPOn();
            }
            else if (intent.getAction().equalsIgnoreCase(
                    BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                isPlugged = ApplicationEx.isA2DPOn() || 
                        ((AudioManager) getSystemService(Context.AUDIO_SERVICE))
                            .isWiredHeadsetOn();
            }
            Log.v(Constants.LOG_TAG, "isPlugged: " + isPlugged);
            Log.v(Constants.LOG_TAG, "ApplicationEx.isPlugged(): " + ApplicationEx.isPlugged());
            if (!isPlugged && isPlugged != ApplicationEx.isPlugged() &&
                    mediaPlayer != null && mediaPlayer.isPlaying() &&
                    pausePlayback) {
                togglePlayback(pauseTime, false);
            }
            ApplicationEx.setPlugged(isPlugged);
        }
     
    }
    
    private final IBinder mBinder = new PlaybackBinder();
    
    public class PlaybackBinder extends Binder {
        public PlaybackService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PlaybackService.this;
        }
    }
    
    private void broadcastEmptyPlaylist() {
        Intent intent = new Intent(Constants.ACTION_NEW_DOWNLOAD);
        intent.putIntegerArrayListExtra(Constants.ID_LIST,
                new ArrayList<Integer>());
        ApplicationEx.getApp().sendBroadcast(intent);
    }
    
    @SuppressLint("NewApi")
    private void updateNotification() {
        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.HONEYCOMB)
            new SetupNotificationTask().execute();
        else
            new SetupNotificationTask().executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR);
    }

}