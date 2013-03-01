package com.jeffthefate.pod_dev.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.StrictMode;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.Constants;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.Util;
import com.jeffthefate.pod_dev.VersionedActionBar;
import com.jeffthefate.pod_dev.VersionedInvalidateOptions;
import com.jeffthefate.pod_dev.fragment.FragmentEpisodes;
import com.jeffthefate.pod_dev.fragment.FragmentFeeds;
import com.jeffthefate.pod_dev.service.PlaybackService;
import com.jeffthefate.pod_dev.service.PlaybackService.PlaybackBinder;
import com.jeffthefate.pod_dev.service.UpdateService;
import com.viewpagerindicator.TitlePageIndicator;

public class ActivityMain extends FragmentActivity {
    
    ViewPager mViewPager;
    TabsAdapter mTabsAdapter;
    
    private static String currTab = Constants.FEEDS_TAB;
    
    private Menu mMenu;
    
    private Context mContext;
    
    private LinearLayout playbackControls = null;
    private LinearLayout metaEpisode = null;
    private static ImageView playbackArtButton = null;
    private static TextView playbackMetaEpisode = null;
    private ProgressBar playbackRewProgress = null;
    private ImageView playbackRewButton = null;
    private ProgressBar playbackPlayPauseProgress = null;
    private static ImageView playbackPlayPauseButton = null;
    private ProgressBar playbackFfwdProgress = null;
    private ImageView playbackFfwdButton = null;
    private ImageView controlsBorder = null;
    
    PlayingReceiver playingReceiver;
    
    private boolean animated = false;
    private boolean feedAtEnd = false;
    private boolean episodeAtEnd = false;
    
    private static Resources res;
    
    private UpdateEpisodeReceiver updateEpisodeReceiver;
    
    private static boolean isPlaylistEmpty = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyDialog()
                .penaltyLog()
                .permitDiskReads()
                .permitDiskWrites()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        super.onCreate(savedInstanceState);
        android.os.Process.setThreadPriority(-20);
        setContentView(R.layout.main);
        mViewPager = (ViewPager)findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this);
        mViewPager.setAdapter(mTabsAdapter);
        TitlePageIndicator mIndicator = (TitlePageIndicator)findViewById(
                R.id.titles);
        mIndicator.setViewPager(mViewPager);
        res = getResources();
        mIndicator.setFooterColor(res.getColor(android.R.color.holo_blue_dark));
        mIndicator.setOnPageChangeListener(mTabsAdapter);
        VersionedActionBar.newInstance().create(this).setDisplayHome();
        mTabsAdapter.addTab(Constants.FEEDS_TAB, FragmentFeeds.class, null);
        mTabsAdapter.addTab(Constants.EPISODES_TAB, FragmentEpisodes.class,
                null);
        playbackControls = (LinearLayout) findViewById(R.id.PlaybackControls);
        playbackControls.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                return true;
            } 
        });
        metaEpisode = (LinearLayout) findViewById(R.id.MetaLayout);
        playbackArtButton = (ImageView) findViewById(R.id.PlaybackArtButton);
        playbackMetaEpisode = (TextView) findViewById(R.id.MetaEpisode);
        playbackRewProgress = (ProgressBar) findViewById(
                R.id.PlaybackRewProgress);
        playbackRewButton = (ImageView) findViewById(R.id.PlaybackRewButton);
        playbackPlayPauseProgress = (ProgressBar) findViewById(
                R.id.PlaybackPlayPauseProgress);
        playbackPlayPauseButton = (ImageView) findViewById(
                R.id.PlaybackPlayPauseButton);
        playbackFfwdProgress = (ProgressBar) findViewById(
                R.id.PlaybackFfwdProgress);
        playbackFfwdButton = (ImageView) findViewById(R.id.PlaybackFfwdButton);
        controlsBorder = (ImageView) findViewById(
                R.id.PlaybackControlsTopBorder);
        controlsBorder.setBackgroundColor(
                res.getColor(android.R.color.holo_blue_dark));
        mContext = this;
        playingReceiver = new PlayingReceiver();
        metaEpisode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    new PlaybackTask(false, true).execute();
                else
                    new PlaybackTask(false, true).executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
        playbackPlayPauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mBound)
                    mService.togglePlayback(1000);
                else {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                        new PlaybackTask(true, false).execute();
                    else
                        new PlaybackTask(true, false).executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR);
                    playbackPlayPauseProgress.setVisibility(View.VISIBLE);
                    playbackPlayPauseButton.setVisibility(View.INVISIBLE);
                }
            }
        });
        playbackRewButton.setEnabled(false);
        playbackRewButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                playbackRewProgress.setVisibility(View.VISIBLE);
                playbackRewButton.setVisibility(View.INVISIBLE);
                if (mBound)
                    mService.rewind();
                playbackRewProgress.setVisibility(View.INVISIBLE);
                playbackRewButton.setVisibility(View.VISIBLE);
            }
        });
        playbackFfwdButton.setEnabled(false);
        playbackFfwdButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                playbackFfwdProgress.setVisibility(View.VISIBLE);
                playbackFfwdButton.setVisibility(View.INVISIBLE);
                if (mBound)
                    mService.ffwd();
                playbackFfwdProgress.setVisibility(View.INVISIBLE);
                playbackFfwdButton.setVisibility(View.VISIBLE);
            }
        });
        playbackFfwdButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                if (mBound && mService.isPlaying())
                    mService.skip();
                return true;
            }
        });
        updateEpisodeReceiver = new UpdateEpisodeReceiver();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        bindService(new Intent(ApplicationEx.getApp(), 
                PlaybackService.class), mConnection, 0);
        mViewPager.setCurrentItem(ApplicationEx.dbHelper.getAppLastTab(),
                false);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_START_PLAYBACK);
        intentFilter.addAction(Constants.ACTION_PAUSE_PLAYBACK);
        registerReceiver(playingReceiver, intentFilter);
        String[] episode = ApplicationEx.getCurrentEpisode();
        if (episode != null) {
            if (episode[4] != null)
                playbackArtButton.setImageBitmap(
                        BitmapFactory.decodeFile(episode[4]));
            if (episode[1] != null)
                playbackMetaEpisode.setText(episode[1]);
        }
        else {
            playbackMetaEpisode.setText("No downloaded episodes");
            playbackArtButton.setImageBitmap(
                    BitmapFactory.decodeResource(res, 
                            R.drawable.ic_album_loading));
            playbackPlayPauseButton.setEnabled(false);
            isPlaylistEmpty = true;
        }
        playbackPlayPauseButton.setEnabled(false);
        if (mBound && mService != null & mService.isPlaying())
            playbackPlayPauseButton.setImageResource(
                    R.drawable.btn_playback_pause_normal_holo_dark);
        else
            playbackPlayPauseButton.setImageResource(
                    R.drawable.btn_playback_play_normal_holo_dark);
        registerReceiver(updateEpisodeReceiver, 
                new IntentFilter(Constants.ACTION_NEW_DOWNLOAD));
    }
    
    @Override
    public void onPause() {
        ApplicationEx.dbHelper.setAppLastTab(mViewPager.getCurrentItem());
        unregisterReceiver(playingReceiver);
        unregisterReceiver(updateEpisodeReceiver);
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        super.onPause();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (currTab.equals(Constants.FEEDS_TAB))
            inflater.inflate(R.menu.feed_menu, menu);
        else if (currTab.equals(Constants.EPISODES_TAB))
            inflater.inflate(R.menu.episode_menu, menu);
        mMenu = menu;
        return true;
    }
    
    class PlayingReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_START_PLAYBACK)) {
                String[] episode = ApplicationEx.getCurrentEpisode();
                if (episode != null) {
                    if (episode[4] != null)
                        playbackArtButton.setImageBitmap(
                                BitmapFactory.decodeFile(episode[0]));
                    if (episode[1] != null)
                        playbackMetaEpisode.setText(episode[1]);
                }
                else {
                    isPlaylistEmpty = true;
                    playbackArtButton.setImageBitmap(
                            BitmapFactory.decodeResource(res, 
                                    R.drawable.ic_album_loading));
                    playbackMetaEpisode.setText("No downloaded episodes");
                    playbackPlayPauseButton.setEnabled(false);
                }
                playbackPlayPauseButton.setImageResource(
                        R.drawable.btn_playback_pause_normal_holo_dark);
                playbackPlayPauseProgress.setVisibility(View.INVISIBLE);
                playbackPlayPauseButton.setVisibility(View.VISIBLE);
            }
            else if (intent.getAction().equals(
                    Constants.ACTION_PAUSE_PLAYBACK)) {
                playbackPlayPauseButton.setImageResource(
                        R.drawable.btn_playback_play_normal_holo_dark);
                playbackPlayPauseProgress.setVisibility(View.INVISIBLE);
                playbackPlayPauseButton.setVisibility(View.VISIBLE);
            }
        }
    }
    
    boolean mBound;
    private static PlaybackService mService;
    
    final Messenger mMessenger = new Messenger(new PlaybackHandler());
    
    private static class PlaybackHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case ActivityPlayback.PLAYLIST_EMPTY:
                if (mService.getEpisodeId() < 0) {
                    isPlaylistEmpty = true;
                    playbackArtButton.setImageBitmap(
                            BitmapFactory.decodeResource(res, 
                                    R.drawable.ic_album_loading));
                    playbackMetaEpisode.setText("No downloaded episodes");
                    playbackPlayPauseButton.setEnabled(false);
                }
                break;
            default:
                super.handleMessage(msg);
                break;
            }
        }
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackBinder binder = (PlaybackBinder) service;
            mService = binder.getService();
            mService.addClient(mMessenger);
            mBound = true;
            if (mService.isPlaying()) {
                playbackPlayPauseButton.setEnabled(true);
                playbackFfwdButton.setEnabled(true);
                playbackRewButton.setEnabled(true);
                playbackPlayPauseButton.setImageResource(
                        R.drawable.btn_playback_pause_normal_holo_dark);
                playbackPlayPauseProgress.setVisibility(View.INVISIBLE);
                playbackPlayPauseButton.setVisibility(View.VISIBLE);
            }
            else {
                if (!mService.isInit()) {
                    if (mService.getEpisodeId() >= 0) {
                        playbackPlayPauseButton.setEnabled(true);
                        playbackFfwdButton.setEnabled(true);
                        playbackRewButton.setEnabled(true);
                    }
                    playbackPlayPauseButton.setImageResource(
                            R.drawable.btn_playback_play_normal_holo_dark);
                    playbackPlayPauseProgress.setVisibility(View.INVISIBLE);
                    playbackPlayPauseButton.setVisibility(View.VISIBLE);
                }
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };
    
    private String getMenuTitle(int itemId) {
        int currentSort = ApplicationEx.dbHelper.getCurrentFeedSort();
        switch(itemId) {
        case R.id.ToggleRead:
            if (currTab.equals(Constants.FEEDS_TAB))
                return ApplicationEx.dbHelper.getCurrentFeedRead() ==
                    Constants.UNREAD ? "Show Read" : "Hide Read";
            else if (currTab.equals(Constants.EPISODES_TAB))
                return ApplicationEx.dbHelper.getCurrentEpisodeRead() ==
                    Constants.UNREAD ? "Show Read" : "Hide Read";
        case R.id.ToggleDownloaded:
            if (currTab.equals(Constants.FEEDS_TAB))
                return ApplicationEx.dbHelper.getCurrentFeedDownloaded() ==
                    Constants.DOWNLOADED ? "Show Cloud" : "Hide Cloud";
            else if (currTab.equals(Constants.EPISODES_TAB))
                return ApplicationEx.dbHelper.getCurrentEpisodeDownloaded() ==
                    Constants.DOWNLOADED ? "Show Cloud" : "Hide Cloud";
        case R.id.SortOrder:
            int currentSortType = -1;
            if (currTab.equals(Constants.FEEDS_TAB))
                currentSortType = 
                    ApplicationEx.dbHelper.getCurrentFeedSortType();
            else if (currTab.equals(Constants.EPISODES_TAB))
                currentSortType = 
                    ApplicationEx.dbHelper.getCurrentEpisodeSortType();
            switch(currentSortType) {
            case 0|Constants.PLAYLIST_SORT_TYPE_ASC:
                if (currTab.equals(Constants.FEEDS_TAB)) {
                    if (currentSort == (Constants.PLAYLIST_SORT_FEED|
                                Constants.PLAYLIST_SORT_DATE))
                        return "Z > A";
                    else if (currentSort == (Constants.PLAYLIST_SORT_EPISODE|
                                    Constants.PLAYLIST_SORT_DATE))
                        return "Newest";
                }
                else
                    return "Newest";
            case 0|Constants.PLAYLIST_SORT_TYPE_DESC:
                if (currTab.equals(Constants.FEEDS_TAB)) {
                    if (currentSort == (Constants.PLAYLIST_SORT_FEED|
                                Constants.PLAYLIST_SORT_DATE))
                        return "A > Z";
                    else if (currentSort == (Constants.PLAYLIST_SORT_EPISODE|
                            Constants.PLAYLIST_SORT_DATE))
                        return "Oldest";
                }
                else
                    return "Oldest";
            case 2|Constants.PLAYLIST_SORT_TYPE_ASC:
                return "Newest";
            case 6^Constants.PLAYLIST_SORT_TYPE_DESC:
                return "Oldest";
            }
        case R.id.SortBy:
            return currentSort == (Constants.PLAYLIST_SORT_FEED|
                            Constants.PLAYLIST_SORT_DATE) ? 
                                "Sort By Latest" : "Sort By Name";
        default:
            return "";
        }
    }
    
    class PlaybackTask extends AsyncTask<Void, Void, Void> {
        private boolean init = false;
        private boolean activity = false;
        
        public PlaybackTask(boolean init, boolean activity) {
            this.init = init;
            this.activity = activity;
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
            playbackIntent.putExtra("activity", activity);
            startService(playbackIntent);
            return null;
        }
        
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mMenu.findItem(R.id.ToggleRead).setTitle(getMenuTitle(R.id.ToggleRead));
        mMenu.findItem(R.id.ToggleDownloaded).setTitle(
                getMenuTitle(R.id.ToggleDownloaded));
        mMenu.findItem(R.id.SortOrder).setTitle(getMenuTitle(R.id.SortOrder));
        if (currTab.equals(Constants.FEEDS_TAB))
            mMenu.findItem(R.id.SortBy).setTitle(getMenuTitle(R.id.SortBy));
        Fragment currFrag = null;
        switch(item.getItemId()) {
        case R.id.NowPlayingMenuItem:
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                new PlaybackTask(false, true).execute();
            else
                new PlaybackTask(false, true).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR);
            break;
        case R.id.ToggleRead:
            switch(mViewPager.getCurrentItem()) {
            case 0:
                currFrag = mTabsAdapter.getFragment(0);
                if (currFrag != null)
                    ((FragmentFeeds)currFrag).updateRead();
                break;
            case 1:
                currFrag = mTabsAdapter.getFragment(1);
                if (currFrag != null)
                    ((FragmentEpisodes)currFrag).updateRead();
                break;
            }
            break;
        case R.id.ToggleDownloaded:
            switch(mViewPager.getCurrentItem()) {
            case 0:
                currFrag = mTabsAdapter.getFragment(0);
                if (currFrag != null)
                    ((FragmentFeeds)currFrag).updateDownloaded();
                break;
            case 1:
                currFrag = mTabsAdapter.getFragment(1);
                if (currFrag != null)
                    ((FragmentEpisodes)currFrag).updateDownloaded();
                break;
            }
            break;
        case R.id.SortOrder:
            switch(mViewPager.getCurrentItem()) {
            case 0:
                currFrag = mTabsAdapter.getFragment(0);
                if (currFrag != null)
                    ((FragmentFeeds)currFrag).updateSort();
                break;
            case 1:
                currFrag = mTabsAdapter.getFragment(1);
                if (currFrag != null)
                    ((FragmentEpisodes)currFrag).updateSort();
                break;
            }
            break;
        case R.id.SortBy:
            switch(mViewPager.getCurrentItem()) {
            case 0:
                ((FragmentFeeds)mTabsAdapter.getFragment(0)).updateGroup();
                break;
            }
            break;
        case R.id.UpdateFeeds:
            Intent serviceIntent = new Intent(ApplicationEx.getApp(), 
                    UpdateService.class);
            serviceIntent.putExtra("sync", ApplicationEx.isSyncing() &&
                    Util.readBooleanFromFile(Constants.GOOGLE_FILENAME));
            startService(serviceIntent);
            break;
        case R.id.AddFeed:
            startActivity(new Intent(this, ActivitySearchFeed.class));
            break;
        case R.id.SettingsMenu:
            startActivity(new Intent(this, ActivityPreferences.class));
            break;
        default:
            super.onOptionsItemSelected(item);
            break;
        }
        return true;
    }
    
    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost.  It relies on a
     * trick.  Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show.  This is not sufficient for switching
     * between pages.  So instead we make the content part of the tab host
     * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
     * view to show as the tab content.  It listens to changes in tabs, and takes
     * care of switch to the correct paged in the ViewPager whenever the selected
     * tab changes.
     */
    public class TabsAdapter extends FragmentPagerAdapter
            implements ViewPager.OnPageChangeListener {
        private final Context mContext;
        private final Activity activity;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
        private Map<Integer, Fragment> mPageReferenceMap = 
            new HashMap<Integer, Fragment>(2);
        private FragmentFeeds feedFragment;
        private FragmentEpisodes episodeFragment;

        final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;
            private final String name;

            TabInfo(Class<?> _class, Bundle _args, String _name) {
                clss = _class;
                args = _args;
                name = _name;
            }
        }

        public TabsAdapter(FragmentActivity activity) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            this.activity = activity;
        }

        public void addTab(String name, Class<?> clss, Bundle args) {
            TabInfo info = new TabInfo(clss, args, name);
            mTabs.add(info);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            if (info.clss.getName().equals(FragmentFeeds.class.getName()) &&
                    mPageReferenceMap.get(0) == null) {
                feedFragment = (FragmentFeeds) Fragment.instantiate(mContext, 
                        info.clss.getName(), info.args);
                mPageReferenceMap.put(0, feedFragment);
                return feedFragment;
            }
            else if (info.clss.getName().equals(
                    FragmentEpisodes.class.getName()) && 
                        mPageReferenceMap.get(1) == null) {
                episodeFragment = (FragmentEpisodes) Fragment.instantiate(
                        mContext, info.clss.getName(), info.args);
                mPageReferenceMap.put(1, episodeFragment);
                return episodeFragment;
            }
            else
                return null;
        }
        
        public Fragment getFragment(int key) {
            TabInfo info = mTabs.get(key);
            if (mPageReferenceMap.get(key) == null) {
                if (key == 0) {
                    feedFragment = (FragmentFeeds) Fragment.instantiate(
                            mContext, info.clss.getName(), info.args);
                    mPageReferenceMap.put(key, feedFragment);
                    return feedFragment;
                }
                else if (key == 1) {
                    episodeFragment = (FragmentEpisodes) Fragment.instantiate(
                            mContext, info.clss.getName(), info.args);
                    mPageReferenceMap.put(key, episodeFragment);
                    return episodeFragment;
                }
            }
            return mPageReferenceMap.get(key);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, 
                int positionOffsetPixels) {
            mMenu.findItem(R.id.EpisodeFilter).setEnabled(false);
        }

        @Override
        public void onPageSelected(int position) {
            mViewPager.setCurrentItem(position);
            currTab = mTabs.get(position).name;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == 0) {
                currTab = mTabs.get(mViewPager.getCurrentItem()).name;
            }
            VersionedInvalidateOptions.newInstance().create(activity)
                    .invalidateOptionsMenu();
            mMenu.findItem(R.id.EpisodeFilter).setEnabled(true);
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return mTabs.get(position).name;
        }
    }
    
    public int getCurrentPosition() {
        return mViewPager.getCurrentItem();
    }
    
    public boolean getFeedAtEnd() {
        return feedAtEnd;
    }
    
    public void setFeedAtEnd(boolean feedAtEnd) {
        this.feedAtEnd = feedAtEnd;
    }
    
    public boolean getEpisodeAtEnd() {
        return episodeAtEnd;
    }
    
    public void setEpisodeAtEnd(boolean episodeAtEnd) {
        this.episodeAtEnd = episodeAtEnd;
    }
    
    private class UpdateEpisodeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<Integer> idList = new ArrayList<Integer>();
            if (intent.hasExtra("idList")) {
                idList = intent.getIntegerArrayListExtra("idList");
            }
            if (!idList.isEmpty() && !isPlaylistEmpty) {
                String[] episode = ApplicationEx.getCurrentEpisode();
                if (episode != null) {
                    if (episode[4] != null)
                        playbackArtButton.setImageBitmap(
                                BitmapFactory.decodeFile(episode[0]));
                    if (episode[1] != null)
                        playbackMetaEpisode.setText(episode[1]);
                    playbackPlayPauseButton.setEnabled(true);
                    playbackRewButton.setEnabled(true);
                    playbackFfwdButton.setEnabled(true);
                }
            }
            else {
                playbackArtButton.setImageBitmap(
                        BitmapFactory.decodeResource(res, 
                                R.drawable.ic_album_loading));
                playbackMetaEpisode.setText("No downloaded episodes");
                playbackPlayPauseButton.setEnabled(false);
            }
        }
    }
    
}