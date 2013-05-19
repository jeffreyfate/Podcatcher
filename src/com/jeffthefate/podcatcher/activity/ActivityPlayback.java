package com.jeffthefate.podcatcher.activity;

import java.text.DecimalFormat;

import android.annotation.SuppressLint;
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
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
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

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.FullScreenImageView;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.SpeedAdapter;
import com.jeffthefate.podcatcher.Util;
import com.jeffthefate.podcatcher.VersionedAnimateDetails;
import com.jeffthefate.podcatcher.VersionedView;
import com.jeffthefate.podcatcher.service.PlaybackService;
import com.jeffthefate.podcatcher.service.PlaybackService.PlaybackBinder;
import com.jeffthefate.podcatcher.service.UpdateService;

public class ActivityPlayback extends ActivityBase {
    
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
    private ImageView more = null;
    private TextView speedText = null;
    private ProgressBar speedProgress = null;
    private ImageView otherViewButtonTop = null;
    private ImageView otherViewButtonBottom = null;
    private ListView speedList = null;
    private LinearLayout speedListLayout = null;
    private EditText speedAddText = null;
    
    private RelativeLayout.LayoutParams currParams = null;
    
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
    
    private DecimalFormat twoDForm = new DecimalFormat(Constants.DEC_FORMAT);
    
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
        getSherlock().getActionBar().setDisplayHomeAsUpEnabled(true);
        feedImage = (FullScreenImageView) findViewById(R.id.FeedImage);
        feedImage.setImageBitmap(getBitmap(Constants.EMPTY));
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
        detailEpisodeDescription.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE)
                    more.setVisibility(View.INVISIBLE);
                return false;
            }
        });
        more = (ImageView) findViewById(R.id.DetailsEpisodeDescriptionMore);
        progressBar = (ProgressBar) findViewById(R.id.MediaProgressBar);
        seekBar = (SeekBar) findViewById(R.id.MediaSeekBar);
        seekBar.setThumb(res.getDrawable(R.drawable.scrubber_control));
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            private int progress;
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, 
                    boolean fromUser) {
                if (fromUser) {
                    this.progress = progress;
                    stopUpdateProgress();
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
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateSeek(progress);
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
                stopUpdateProgress();
                if (mBound && mService.isPlaying())
                    mService.rewind(v);
            }
        });
        rewButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                stopUpdateProgress();
                if (mBound && mService.isPlaying())
                    mService.previous();
                return true;
            }
        });
        playButton.setOnClickListener(new OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(View v) {
                stopUpdateProgress();
                if (mBound) {
                    boolean isPlaying = mService.togglePlayback(0, false);
                    if (isPlaying) {
                        playButton.setImageResource(
                                R.drawable.btn_playback_pause_normal_holo_dark);
                        textHandler.removeMessages(Constants.HOLO_BLUE);
                        textHandler.removeMessages(Color.LTGRAY);
                        elapsedText.setTextColor(Color.LTGRAY);
                        rewButton.setEnabled(true);
                        ffButton.setEnabled(true);
                        skipButton.setEnabled(true);
                    }
                    else {
                        playButton.setImageResource(
                                R.drawable.btn_playback_play_normal_holo_dark);
                        textHandler.sendEmptyMessage(Constants.HOLO_BLUE);
                        rewButton.setEnabled(false);
                        ffButton.setEnabled(false);
                        skipButton.setEnabled(false);
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
                stopUpdateProgress();
                if (mBound && mService.isPlaying())
                    mService.ffwd(v);
            }
        });
        ffButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                stopUpdateProgress();
                if (mBound && mService.isPlaying())
                    mService.next();
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
                        speedAddText.setText(Constants.PLUS);
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
                    mService.next();
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
        versionedView.create(more).setAlpha(0.0f);
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
                ((EditText)v).setText(Constants.EMPTY);
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
                        v.setText(Constants.PLUS);
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
                otherViewButtonBottom, episodeDetailsLayout, more);
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
            playbackIntent.putExtra(Constants.START_EP, 
                    ApplicationEx.dbHelper.getCurrentEpisode());
            playbackIntent.putIntegerArrayListExtra(Constants.EPS, 
                    Util.readCurrentPlaylist());
            playbackIntent.putExtra(Constants.INIT, init);
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
        speedText.setText(
                TextUtils.concat(Double.toString(speed), Constants.X));
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
        currEpisodeId = ApplicationEx.dbHelper.getCurrentEpisode();
        updateUi();
        setTimeTextInit();
        bindService(new Intent(ApplicationEx.getApp(), 
                PlaybackService.class), mConnection, 0);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_START_PLAYBACK);
        intentFilter.addAction(Constants.ACTION_PAUSE_PLAYBACK);
        intentFilter.addAction(Constants.ACTION_BACKWARD_PLAYBACK);
        intentFilter.addAction(Constants.ACTION_FORWARD_PLAYBACK);
        intentFilter.addAction(Constants.ACTION_SEEK_DONE);
        registerReceiver(playingReceiver, intentFilter);
        registerReceiver(speedReceiver, 
                new IntentFilter(Constants.ACTION_SPEED_CHANGED));
        registerReceiver(freezeReceiver, 
                new IntentFilter(Constants.ACTION_SPEED_FREEZE));
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
        unregisterReceiver(freezeReceiver);
        super.onPause();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.playing_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {    
        switch (item.getItemId()) {        
        case android.R.id.home:            
            Intent mainIntent = new Intent(this, ActivityMain.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(mainIntent);
            return true;
        case R.id.UpdateFeeds:
            Intent serviceIntent = new Intent(ApplicationEx.getApp(), 
                    UpdateService.class);
            serviceIntent.putExtra(Constants.SYNC, ApplicationEx.isSyncing() &&
                    Util.readBooleanFromFile(Constants.GOOGLE_FILENAME));
            startService(serviceIntent);
            return true;
        default:            
            return super.onOptionsItemSelected(item);    
        }
    }
    
    boolean mBound;
    
    private ServiceConnection mConnection = new ServiceConnection() {
        @SuppressLint("NewApi")
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackBinder binder = (PlaybackBinder) service;
            mService = binder.getService();
            currEpisodeId = mService.getEpisodeId();
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
                (mService.getSpeedList().size() + 1) * 30,
                res.getDisplayMetrics());
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
        int progress = mService.getCurrentProgress();
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
        if (currEpisodeId < 0)
            return;
        int duration = ApplicationEx.dbHelper.getEpisodeDuration(currEpisodeId);
        int progress = ApplicationEx.dbHelper.getEpisodeProgress(currEpisodeId);
        elapsedText.setText(Util.getTimeString(progress));
        remainingText.setText(Util.getTimeString(duration-progress));
        progressBar.setMax(duration);
        seekBar.setMax(duration);
        progressBar.setProgress(progress);
        seekBar.setProgress(progress);
        textHandler.sendEmptyMessage(Constants.HOLO_BLUE);
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

        Bitmap bitmap = null;
        
        @Override
        protected void onPreExecute() {
            if (currEpisode == null)
                return;
            episodeTitle.setText(currEpisode[1]);
            feedTitle.setText(currEpisode[2]);
            detailEpisodeTitle.setText(currEpisode[1]);
            detailFeedTitle.setText(currEpisode[2]);
            detailEpisodeDescription.setText(currEpisode[3]);
            detailEpisodeDescription.scrollTo(0, 0);
            int height = detailEpisodeDescription.getHeight();
            int lineHeight = detailEpisodeDescription.getLineHeight();
            int lines = detailEpisodeDescription.getLineCount();
            if (height >= (lineHeight * lines))
                more.setVisibility(View.GONE);
            else
                more.setVisibility(View.VISIBLE);
            if (ApplicationEx.getCurrentSpeed() > 0.0)
                speedText.setText(TextUtils.concat(twoDForm.format(
                        ApplicationEx.getCurrentSpeed()),Constants.X));
            else
                speedText.setText(TextUtils.concat(twoDForm.format(
                        ApplicationEx.dbHelper.getFeedSpeed(currEpisodeId)),
                        Constants.X));
        }
        
        @SuppressLint("NewApi")
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
            return null;
        }
        
        protected void onPostExecute(Void nothing) {
            if (mService != null && mService.isPlaying()) {
                playButton.setImageResource(
                        R.drawable.btn_playback_pause_normal_holo_dark);
                textHandler.removeMessages(Constants.HOLO_BLUE);
                textHandler.removeMessages(Color.LTGRAY);
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
        episodeTitle.setText(Constants.NO_DOWNLOADED);
        feedTitle.setVisibility(View.INVISIBLE);
        detailEpisodeTitle.setText(Constants.NO_DOWNLOADED);
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
        rewButton.setEnabled(true);
        ffButton.setEnabled(true);
        progressBar.setEnabled(true);
    }
    
    @SuppressLint("NewApi")
    private void updateUi() {
        getDetailsTask = new GetEpisodeDetailsTask();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            getDetailsTask.execute();
        else
            getDetailsTask.executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR);
        enableViews();
    }
    
    private void updateSeek(int progress) {
        mService.seekTo(progress, null);
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
            case PlaybackService.PLAYLIST_EMPTY:
                updateEmptyUi();
                break;
            case PlaybackService.SPEED_DISABLED:
                speedButton.setEnabled(false);
                speedButton.setBackgroundColor(Color.DKGRAY);
                break;
            case PlaybackService.SPEED_ENABLED:
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
    
    Messenger mMessenger = new Messenger(new PlaybackHandler());
    
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
            else if (intent.getAction().equals(
                    Constants.ACTION_BACKWARD_PLAYBACK) ||
                    intent.getAction().equals(
                            Constants.ACTION_FORWARD_PLAYBACK)) {
                stopUpdateProgress();
                int progress = mService.getCurrentProgress();
                elapsedText.setText(Util.getTimeString(progress));
                remainingText.setText(Util.getTimeString(
                        mService.getDuration()-progress));
                seekBar.setProgress(progress);
                progressBar.setProgress(progress);
                startUpdateProgress();
            }
            else if (intent.getAction().equals(Constants.ACTION_SEEK_DONE)) {
                startUpdateProgress();
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