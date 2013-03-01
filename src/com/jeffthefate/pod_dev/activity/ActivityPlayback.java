package com.jeffthefate.pod_dev.activity;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.Constants;
import com.jeffthefate.pod_dev.FullScreenImageView;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.SpeedAdapter;
import com.jeffthefate.pod_dev.Util;
import com.jeffthefate.pod_dev.VersionedActionBar;
import com.jeffthefate.pod_dev.VersionedAnimateDetails;
import com.jeffthefate.pod_dev.VersionedView;
import com.jeffthefate.pod_dev.service.PlaybackService;
import com.jeffthefate.pod_dev.service.PlaybackService.PlaybackBinder;

public class ActivityPlayback extends Activity {
    
    private RelativeLayout topLayout = null;
    private static FullScreenImageView feedImage = null;
    private LinearLayout progressLayout = null;
    private RelativeLayout timeLayout = null;
    private RelativeLayout episodeDetailsLayout = null;
    private static TextView episodeTitle = null;
    private static TextView feedTitle = null;
    private static TextView detailEpisodeTitle = null;
    private static TextView detailFeedTitle = null;
    private static TextView detailEpisodeDescription = null;
    private static SeekBar seekBar = null;
    private static ProgressBar progressBar = null;
    private static TextView elapsedText = null;
    private static TextView remainingText = null;
    private static ImageView rewButton = null;
    private static ImageView playButton = null;
    private static ImageView ffButton = null;
    private static ImageView skipButton = null;
    private static RelativeLayout speedButton = null;
    private TextView speedText = null;
    private ProgressBar speedProgress = null;
    private ImageView otherViewButtonTop = null;
    private ImageView otherViewButtonBottom = null;
    private ListView speedList = null;
    private LinearLayout speedListLayout = null;
    private EditText speedAddText = null;
    
    private RelativeLayout.LayoutParams currParams = null;
    
    public static final int SPEED_ENABLED = 2;
    public static final int SPEED_DISABLED = 3;
    public static final int PLAYLIST_EMPTY = 4;
    
    public static final int IS_PAUSED = 1;
    
    private boolean canUpdateSeek = true;
    
    private boolean isAnimated = false;
    
    private static TextColorHandler textHandler = new TextColorHandler();
    
    private PlayingReceiver playingReceiver;
    private SpeedReceiver speedReceiver;
    private FreezeReceiver freezeReceiver;
    
    private PlaybackService mService;
    
    private int currEpisodeId = -1;
    
    private GetEpisodeDetailsTask getDetailsTask;
    private GetEpisodeImageTask getImageTask;
    
    private Context mContext = this;
    
    private DecimalFormat twoDForm = new DecimalFormat("#.##");
    
    private String[] currEpisode;
    
    private static Resources res;
    
    private VersionedAnimateDetails animateDetails;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        res = getResources();
        sendBroadcast(new Intent(Constants.ACTION_SAVE_PROGRESS));
        playingReceiver = new PlayingReceiver();
        speedReceiver = new SpeedReceiver();
        freezeReceiver = new FreezeReceiver();
        setContentView(R.layout.playback);
        VersionedActionBar.newInstance().create(this).setDisplayHomeAsUp();
        topLayout = (RelativeLayout) findViewById(R.id.PlaybackTopLayout);
        feedImage = (FullScreenImageView) findViewById(R.id.FeedImage);
        feedImage.setImageBitmap(getBitmap(""));
        feedImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                animate();
            }
        });
        progressLayout = (LinearLayout) findViewById(R.id.ProgressLayout);
        timeLayout = (RelativeLayout) findViewById(R.id.TimeLayout);
        episodeDetailsLayout = 
                (RelativeLayout) findViewById(R.id.EpisodeDetailsLayout);
        episodeTitle = (TextView) findViewById(R.id.EpisodeTitle);
        feedTitle = (TextView) findViewById(R.id.FeedTitle);
        detailEpisodeTitle = (TextView) findViewById(R.id.DetailsEpisodeTitle);
        detailFeedTitle = (TextView) findViewById(R.id.DetailsFeedTitle);
        detailEpisodeDescription = (TextView) findViewById(
                R.id.DetailsEpisodeDescription);
        progressBar = (ProgressBar) findViewById(R.id.MediaProgressBar);
        seekBar = (SeekBar) findViewById(R.id.MediaSeekBar);
        seekBar.setThumb(res.getDrawable(R.drawable.scrubber_control));
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, 
                    boolean fromUser) {
                if (fromUser) {
                    elapsedText.setText(Util.getTimeString(progress));
                    int duration = mService.getDuration();
                    if (duration == -1)
                        remainingText.setText(Util.getTimeString(0));
                    else
                        remainingText.setText(
                                Util.getTimeString(duration-progress));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                canUpdateSeek = false;
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                canUpdateSeek = true;
                updateSeek();
                startUpdateProgress();
            }
        });
        elapsedText = (TextView) findViewById(R.id.PlayElapsedText);
        remainingText = (TextView) findViewById(R.id.PlayRemainingText);
        speedButton = (RelativeLayout) findViewById(R.id.SpeedButton);
        speedButton.setBackgroundColor(Constants.HOLO_BLUE);
        speedText = (TextView) findViewById(R.id.SpeedText);
        speedProgress = (ProgressBar) findViewById(R.id.SpeedProgress);
        rewButton = (ImageView) findViewById(R.id.RewButton);
        playButton = (ImageView) findViewById(R.id.PlayButton);
        ffButton = (ImageView) findViewById(R.id.FwdButton);
        skipButton = (ImageView) findViewById(R.id.SkipButton);
        otherViewButtonTop = (ImageView) findViewById(R.id.OtherViewButtonTop);
        otherViewButtonBottom = (ImageView) findViewById(
                R.id.OtherViewButtonBottom);
        rewButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBound && mService.isPlaying()) {
                    mService.rewind();
                    startUpdateProgress();
                }
            }
        });
        rewButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                if (mBound && mService.isPlaying())
                    mService.previous();
                return true;
            }
        });
        playButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopUpdateProgress();
                if (mBound) {
                    boolean isPlaying = mService.togglePlayback(1000);
                    if (isPlaying) {
                        playButton.setImageResource(
                                R.drawable.btn_playback_pause_normal_holo_dark);
                        textHandler.removeMessages(Constants.HOLO_BLUE);
                        textHandler.removeMessages(Color.LTGRAY);
                        elapsedText.setTextColor(Color.LTGRAY);
                        //startUpdateProgress();
                    }
                    else {
                        playButton.setImageResource(
                                R.drawable.btn_playback_play_normal_holo_dark);
                        textHandler.sendEmptyMessage(Constants.HOLO_BLUE);
                    }
                }
                else {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                        new PlaybackTask(true).execute();
                    else
                        new PlaybackTask(true).executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        });
        ffButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBound && mService.isPlaying()) {
                    mService.ffwd();
                    startUpdateProgress();
                }
            }
        });
        ffButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                if (mBound && mService.isPlaying())
                    mService.skip();
                return true;
            }
        });
        speedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBound && mService.isPlaying()) {
                    if (speedListLayout.getVisibility() == View.INVISIBLE) {
                        speedListLayout.setVisibility(View.VISIBLE);
                        speedListLayout.bringToFront();
                        speedAddText.clearFocus();
                    }
                    else if (speedListLayout.getVisibility() == View.VISIBLE) {
                        speedListLayout.setVisibility(View.INVISIBLE);
                        speedAddText.setText("+");
                        speedAddText.setCursorVisible(false);
                        speedAddText.clearFocus();
                    }
                }
            }
        });
        skipButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBound && mService.isPlaying())
                    mService.skip();
            }
        });
        otherViewButtonTop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                animate();
            }
        });
        otherViewButtonBottom.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                animate();
            }
        });
        VersionedView versionedView = VersionedView.newInstance();
        versionedView.create(episodeDetailsLayout).setAlpha(0.0f);
        versionedView.create(timeLayout).setAlpha(0.0f);
        versionedView.create(skipButton).setAlpha(0.0f);
        versionedView.create(speedButton).setAlpha(0.0f);
        versionedView.create(seekBar).setAlpha(0.0f);
        skipButton.setEnabled(false);
        speedButton.setEnabled(false);
        seekBar.setEnabled(false);
        speedListLayout = (LinearLayout) findViewById(R.id.SpeedList);
        currParams = (RelativeLayout.LayoutParams) speedListLayout
                .getLayoutParams();
        speedListLayout.setVisibility(View.INVISIBLE);
        speedList = (ListView) findViewById(android.R.id.list);
        speedAddText = (EditText) findViewById(R.id.AddSpeedEdit);
        speedAddText.setBackgroundColor(Constants.HOLO_BLUE);
        speedAddText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((EditText)v).setText("");
                ((EditText)v).setCursorVisible(true);
            } 
        });
        speedAddText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                    KeyEvent event) {
                double entry;
                if (actionId == EditorInfo.IME_ACTION_DONE && mBound) {
                    try {
                        entry = Double.parseDouble(
                                v.getEditableText().toString());
                    } catch (NumberFormatException e) {
                        return false;
                    }
                    if (entry > 0.0 &&
                            !mService.getSpeedList().contains(entry)) {
                        if (entry > 2.0)
                            entry = 2.0;
                        mService.addSpeed(entry);
                        v.setText("+");
                        ((EditText)v).setCursorVisible(false);
                        ((SpeedAdapter)speedList.getAdapter())
                                .setSpeedList(mService.getSpeedList());
                        resizeSpeedList();
                        InputMethodManager imm =
                            (InputMethodManager) getSystemService(
                                    Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        return true;
                    }
                    else return false;
                }
                else
                    return false;
            } 
        });
        animateDetails = VersionedAnimateDetails.newInstance().create(
                detailEpisodeDescription, speedButton, skipButton, seekBar,
                progressBar, feedImage, progressLayout, timeLayout,
                otherViewButtonBottom, episodeDetailsLayout);
    }
    
    class PlaybackTask extends AsyncTask<Void, Void, Void> {
        private boolean init = false;
        
        public PlaybackTask(boolean init) {
            this.init = init;
        }
        
        @Override
        protected Void doInBackground(Void... nothing) {
            Intent playbackIntent = new Intent(mContext,
                    PlaybackService.class);
            playbackIntent.putExtra("startEp", 
                    ApplicationEx.dbHelper.getCurrentEpisode());
            playbackIntent.putIntegerArrayListExtra("episodes", 
                    Util.readCurrentPlaylist());
            playbackIntent.putExtra("init", init);
            startService(playbackIntent);
            return null;
        }
    }
    
    private void freezeSpeedButton() {
        speedProgress.setVisibility(View.VISIBLE);
        speedText.setVisibility(View.INVISIBLE);
        speedButton.setEnabled(false);
    }
    
    private void unFreezeSpeedButton() {
        speed = Double.valueOf(twoDForm.format(mService.getSpeed()));
        speedText.setText(speed+"x");
        speedButton.setEnabled(true);
        if (mService.isPlaying())
            startUpdateProgress();
        speedProgress.setVisibility(View.INVISIBLE);
        speedText.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        currEpisode = ApplicationEx.getCurrentEpisode();
        bindService(new Intent(ApplicationEx.getApp(), 
                PlaybackService.class), mConnection, 0);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_START_PLAYBACK);
        intentFilter.addAction(Constants.ACTION_PAUSE_PLAYBACK);
        registerReceiver(playingReceiver, intentFilter);
        registerReceiver(speedReceiver, 
                new IntentFilter(Constants.ACTION_SPEED_CHANGED));
        registerReceiver(freezeReceiver, 
                new IntentFilter(Constants.ACTION_SPEED_FREEZE));
        currEpisodeId = ApplicationEx.dbHelper.getCurrentEpisode();
        episodeTitle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setFocusable(!v.isFocusable());
                v.setSelected(!v.isSelected());
            } 
        });
    }
    
    @Override
    public void onPause() {
        if (speedListLayout.getVisibility() == View.VISIBLE) {
            speedListLayout.setVisibility(View.INVISIBLE);
            speedAddText.clearFocus();
        }
        episodeTitle.setFocusable(false);
        episodeTitle.setSelected(false);
        textHandler.removeMessages(Constants.HOLO_BLUE);
        textHandler.removeMessages(Color.LTGRAY);
        elapsedText.setTextColor(Color.LTGRAY);
        stopUpdateProgress();
        if (mBound) {
            mService.removeClient(mMessenger);
            unbindService(mConnection);
            mBound = false;
        }
        if (getDetailsTask != null)
            getDetailsTask.cancel(true);
        if (getImageTask != null)
            getImageTask.cancel(true);
        unregisterReceiver(playingReceiver);
        unregisterReceiver(speedReceiver);
        super.onPause();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {    
        switch (item.getItemId()) 
        {        
        case android.R.id.home:            
            Intent mainIntent = new Intent(this, ActivitySplash.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(mainIntent);
            return true;        
        default:            
            return super.onOptionsItemSelected(item);    
        }
    }
    
    boolean mBound;
    
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackBinder binder = (PlaybackBinder) service;
            mService = binder.getService();
            currEpisodeId = mService.getEpisodeId();
            int currPosition = mService.getCurrentPosition();
            if (currPosition < 0)
                currPosition = ApplicationEx.dbHelper.getEpisodeProgress(
                        currEpisodeId);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                new Util.SavePositionTask(currPosition, currEpisodeId)
                        .execute();
            else
                new Util.SavePositionTask(currPosition, currEpisodeId)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            mService.addClient(mMessenger);
            mBound = true;
            if (mService.isInit() && mService.isNull()) {
                mService.initPlayer();
                updateUi();
            }
            else {
                if (currEpisodeId < 0)
                    updateEmptyUi();
                else if (!mService.isPlaying()) {
                    if (mService.isNull())
                        mService.initPlayer();
                    else
                        mService.preparePlayer();
                    updateUi();
                    setTimeTextInit();
                    textHandler.sendEmptyMessage(Constants.HOLO_BLUE);
                }
                else {
                    updateUi();
                    speed = mService.getCurrentSpeed();
                    startUpdateProgress();
                }
            }
            resizeSpeedList();
            speedList.setAdapter(new SpeedAdapter(mContext, R.layout.row_speed,
                    android.R.id.text1, mService.getSpeedList(), mService,
                    speedListLayout));
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };
    
    private void resizeSpeedList() {
        RelativeLayout.LayoutParams newParams = currParams;
        int height = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (mService.getSpeedList().size()+1)*30, res.getDisplayMetrics());
        int width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 64, res.getDisplayMetrics());
        newParams.width = width;
        newParams.height = height;
        speedListLayout.setLayoutParams(newParams);
    }
    
    private double speed = 1.0;
    
    private class MyCount extends CountDownTimer {
        
        long duration;
        long updateInterval;

        public MyCount(long timeLeft, long updateInterval, long duration) {
            super((long) (timeLeft/speed), (long) (updateInterval/speed));
            this.updateInterval = (long) (updateInterval/speed);
            this.duration = (long) (duration/speed);
        }

        @Override
        public void onFinish() {
            int ending = (int) (duration*speed);
            seekBar.setProgress(ending);
            progressBar.setProgress(ending);
            elapsedText.setText(Util.getTimeString(ending));
            remainingText.setText(Util.getTimeString(0));
        }

        @Override
        public void onTick(long position) {
            int pos = (int) ((duration-position)*speed);
            if (pos == 0 || pos >= updateInterval) {
                seekBar.setProgress(pos);
                progressBar.setProgress(pos);
                elapsedText.setText(Util.getTimeString(pos));
                remainingText.setText(
                        Util.getTimeString((int) (position*speed)));
            }
        }
    }
    
    private MyCount count;

    private void startUpdateProgress() {
        stopUpdateProgress();
        int duration = mService.getDuration();
        int progress = mService.getCurrentPosition();
        elapsedText.setText(Util.getTimeString(progress));
        remainingText.setText(Util.getTimeString(duration-progress));
        progressBar.setMax(duration);
        seekBar.setMax(duration);
        progressBar.setProgress(progress);
        seekBar.setProgress(progress);
        if (mService.isPlaying()) {
            count = new MyCount(duration-progress, 1000, duration);
            count.start();
        }
    }
    
    private void setTimeTextInit() {
        int duration = ApplicationEx.dbHelper.getEpisodeDuration(currEpisodeId);
        int progress = ApplicationEx.dbHelper.getEpisodeProgress(currEpisodeId);
        elapsedText.setText(Util.getTimeString(progress));
        remainingText.setText(Util.getTimeString(duration-progress));
        progressBar.setMax(duration);
        seekBar.setMax(duration);
        progressBar.setProgress(progress);
        seekBar.setProgress(progress);
    }
    
    private void animate() {
        if (!isAnimated)
            isAnimated = animateDetails.animate();
        else
            isAnimated = animateDetails.unanimate();
    }
    
    private void stopUpdateProgress() {
        if (count != null)
            count.cancel();
    }
    
    class GetEpisodeDetailsTask extends AsyncTask<Void, Void, Void> {

        String text;
        Bitmap bitmap = null;
        
        @Override
        protected Void doInBackground(Void... params) {
            if (currEpisode == null)
                return null;
            else {
                getImageTask = new GetEpisodeImageTask();
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    getImageTask.execute();
                else
                    getImageTask.executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR);
            }
            if (ApplicationEx.getCurrentSpeed() > 0.0)
                text = Double.valueOf(twoDForm.format(
                        ApplicationEx.getCurrentSpeed()))+"x";
            else
                text = Double.valueOf(twoDForm.format(
                        ApplicationEx.dbHelper.getFeedSpeed(currEpisodeId))) +
                            "x";
            return null;
        }
        
        protected void onPostExecute(Void nothing) {
            if (currEpisode == null)
                return;
            episodeTitle.setText(currEpisode[1]);
            feedTitle.setText(currEpisode[2]);
            detailEpisodeTitle.setText(currEpisode[1]);
            detailFeedTitle.setText(currEpisode[2]);
            detailEpisodeDescription.setText(currEpisode[3]);
            if (mService != null && !mService.isNull()) {
                speedText.setText(Double.valueOf(twoDForm.format(
                        mService.getCurrentSpeed()))+"x");
            }
            else {
                speedText.setText(text);
            }
            if (mService != null && mService.isPlaying()) {
                playButton.setImageResource(
                        R.drawable.btn_playback_pause_normal_holo_dark);
            }
            else
                playButton.setImageResource(
                        R.drawable.btn_playback_play_normal_holo_dark);
        }
    }
    
    class GetEpisodeImageTask extends AsyncTask<Void, Void, Void> {

        String text;
        Bitmap bitmap = null;
        
        @Override
        protected Void doInBackground(Void... params) {
            if (currEpisode[0] != null)
                bitmap = getBitmap(currEpisode[0]);
            return null;
        }
        
        protected void onPostExecute(Void nothing) {
            if (bitmap != null)
                feedImage.setImageBitmap(bitmap);
            
        }
    }
    
    private static void updateEmptyUi() {
        feedImage.setImageBitmap(BitmapFactory.decodeResource(res, 
                R.drawable.album_frame_normal));
        episodeTitle.setText("No downloaded episodes");
        feedTitle.setVisibility(View.INVISIBLE);
        detailEpisodeTitle.setText("No downloaded episodes");
        detailFeedTitle.setVisibility(View.INVISIBLE);
        detailEpisodeDescription.setVisibility(View.INVISIBLE);
        playButton.setImageResource(
                R.drawable.btn_playback_play_normal_holo_dark);
        playButton.setEnabled(false);
        speedButton.setEnabled(false);
        rewButton.setEnabled(false);
        ffButton.setEnabled(false);
        skipButton.setEnabled(false);
        progressBar.setEnabled(false);
        progressBar.setMax(0);
        progressBar.setProgress(0);
        seekBar.setEnabled(false);
        seekBar.setMax(0);
        seekBar.setProgress(0);
        textHandler.removeMessages(Constants.HOLO_BLUE);
        textHandler.removeMessages(Color.LTGRAY);
        elapsedText.setText(Util.getTimeString(0));
        remainingText.setText(Util.getTimeString(0));
    }
    
    private void enableViews() {
        feedTitle.setVisibility(View.VISIBLE);
        detailFeedTitle.setVisibility(View.VISIBLE);
        detailEpisodeDescription.setVisibility(View.VISIBLE);
        playButton.setEnabled(true);
        //speedButton.setEnabled(true);
        rewButton.setEnabled(true);
        ffButton.setEnabled(true);
        //skipButton.setEnabled(true);
        progressBar.setEnabled(true);
        //seekBar.setEnabled(true);
    }
    
    private void updateUi() {
        getDetailsTask = new GetEpisodeDetailsTask();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            getDetailsTask.execute();
        else
            getDetailsTask.executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR);
        enableViews();
    }
    
    private void updateSeek() {
        if (canUpdateSeek) {
            mService.seekTo(seekBar.getProgress());
        }
    }
    
    private static class TextColorHandler extends Handler {
        
        public void handleMessage(Message msg) {
            Message message = Message.obtain();
            message.setTarget(textHandler);
            switch(msg.what) {
            case Constants.HOLO_BLUE:
                elapsedText.setTextColor(Color.LTGRAY);
                message.what = Color.LTGRAY;
                break;
            case Color.LTGRAY:
                elapsedText.setTextColor(res.getColor(
                        android.R.color.holo_blue_dark));
                message.what = Constants.HOLO_BLUE;
                break;
            default:
                elapsedText.setTextColor(Color.LTGRAY);
                message.what = Color.LTGRAY;
                break;
            }
            sendMessageDelayed(message, 500);
        }
    }
    
    private static class PlaybackHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case PLAYLIST_EMPTY:
                updateEmptyUi();
                break;
            case SPEED_DISABLED:
                speedButton.setEnabled(false);
                speedButton.setBackgroundColor(Color.DKGRAY);
                break;
            case SPEED_ENABLED:
                speedButton.setEnabled(true);
                speedButton.setBackgroundColor(
                        res.getColor(android.R.color.holo_blue_dark));
                break;
            default:
                super.handleMessage(msg);
                break;
            }
        }
    }
    
    final Messenger mMessenger = new Messenger(new PlaybackHandler());
    
    class PlayingReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_START_PLAYBACK)) {
                int savedEpId = ApplicationEx.dbHelper.getCurrentEpisode();
                if (savedEpId != currEpisodeId) {
                    currEpisode = ApplicationEx.getCurrentEpisode();
                    updateUi();
                }
                if (mService != null) {
                    playButton.setImageResource(
                            R.drawable.btn_playback_pause_normal_holo_dark);
                    textHandler.removeMessages(Constants.HOLO_BLUE);
                    textHandler.removeMessages(Color.LTGRAY);
                    elapsedText.setTextColor(Color.LTGRAY);
                    startUpdateProgress();
                }
            }
            else if (intent.getAction().equals(
                    Constants.ACTION_PAUSE_PLAYBACK)) {
                stopUpdateProgress();
                int progress = mService.getCurrentProgress();
                elapsedText.setText(Util.getTimeString(progress));
                remainingText.setText(Util.getTimeString(
                        mService.getDuration()-progress));
                playButton.setImageResource(
                        R.drawable.btn_playback_play_normal_holo_dark);
                textHandler.sendEmptyMessage(Constants.HOLO_BLUE);
            }
        }
    }
    
    class SpeedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mService != null) {
                if (speed != mService.getSpeed()) {
                    stopUpdateProgress();
                    updateUi();
                    unFreezeSpeedButton();
                    speedAddText.clearFocus();
                }
                else
                    resizeSpeedList();
            }
        }
    }
    
    class FreezeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mService != null) {
                stopUpdateProgress();
                freezeSpeedButton();
            }
        }
    }
    
    private Bitmap getBitmap(String pathName) {
        Bitmap decoded = BitmapFactory.decodeFile(pathName);
        if (decoded == null)
            decoded = BitmapFactory.decodeResource(res,
                    R.drawable.album_frame_normal);
        return decoded;
    }
    
}