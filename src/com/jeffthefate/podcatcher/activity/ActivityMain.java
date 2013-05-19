package com.jeffthefate.podcatcher.activity;

import java.util.ArrayList;

import android.annotation.SuppressLint;
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.Util;
import com.jeffthefate.podcatcher.Util.Magics;
import com.jeffthefate.podcatcher.VersionedInvalidateOptions;
import com.jeffthefate.podcatcher.fragment.FragmentEpisodes;
import com.jeffthefate.podcatcher.fragment.FragmentFeeds;
import com.jeffthefate.podcatcher.service.PlaybackService;
import com.jeffthefate.podcatcher.service.PlaybackService.PlaybackBinder;
import com.jeffthefate.podcatcher.service.UpdateService;
import com.viewpagerindicator.TitlePageIndicator;

public class ActivityMain extends ActivityBase {
    
    ViewPager mViewPager;
    TabsAdapter mTabsAdapter;
    
    private static String currTab = Constants.FEEDS_TAB;
    
    private Menu mMenu;
    
    private Context mContext;
    
    private LinearLayout playbackControls = null;
    private RelativeLayout metaEpisode = null;
    private static ImageView playbackArtButton = null;
    private static TextView playbackMetaEpisode = null;
    private ImageView playbackRewButton = null;
    private static ImageView playbackPlayPauseButton = null;
    private ImageView playbackFfwdButton = null;
    private ImageView controlsBorder = null;
    
    PlayingReceiver playingReceiver;
    
    private boolean feedAtEnd = false;
    private boolean episodeAtEnd = false;
    
    private static Resources res;
    
    private UpdateEpisodeReceiver updateEpisodeReceiver;
    
    private boolean isPlaylistEmpty = false;
    
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
        getSherlock().getActionBar().setDisplayShowHomeEnabled(true);
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
        metaEpisode = (RelativeLayout) findViewById(R.id.MetaLayout);
        playbackArtButton = (ImageView) findViewById(R.id.PlaybackArtButton);
        playbackMetaEpisode = (TextView) findViewById(R.id.MetaEpisode);
        playbackRewButton = (ImageView) findViewById(R.id.PlaybackRewButton);
        playbackPlayPauseButton = (ImageView) findViewById(
                R.id.PlaybackPlayPauseButton);
        playbackFfwdButton = (ImageView) findViewById(R.id.PlaybackFfwdButton);
        controlsBorder = (ImageView) findViewById(
                R.id.PlaybackControlsTopBorder);
        controlsBorder.setBackgroundColor(
                res.getColor(android.R.color.holo_blue_dark));
        mContext = this;
        playingReceiver = new PlayingReceiver();
        metaEpisode.setOnClickListener(new OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(View arg0) {
                /*
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    new PlaybackTask(false, true).execute();
                else
                    new PlaybackTask(false, true).executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR);
                */
                Intent playIntent = new Intent(ApplicationEx.getApp(),
                        ActivityPlayback.class);
                playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP | 
                        Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(playIntent);
            }
        });
        playbackPlayPauseButton.setOnClickListener(new OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(View arg0) {
                if (mBound)
                    mService.togglePlayback(0, false);
                else {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                        new PlaybackTask(true, false).execute();
                    else
                        new PlaybackTask(true, false).executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        });
        playbackRewButton.setEnabled(false);
        playbackRewButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBound)
                    mService.rewind(view);
            }
        });
        playbackFfwdButton.setEnabled(false);
        playbackFfwdButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBound)
                    mService.ffwd(view);
            }
        });
        playbackFfwdButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                if (mBound && mService.isPlaying())
                    mService.next();
                return true;
            }
        });
        updateEpisodeReceiver = new UpdateEpisodeReceiver();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        ApplicationEx.setMainActive(true);
        ApplicationEx.dbHelper.getNextMagicInterval();
        // An account hasn't been set yet - first use
        if (!Util.containsPreference(R.string.sync_key))
            startActivity(new Intent(getApplicationContext(), 
                    ActivityFirstStart.class));
        else if (!ApplicationEx.getUpdateWait())
            startUpdate();
        bindService(new Intent(ApplicationEx.getApp(), PlaybackService.class),
                mConnection, 0);
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
        else if (episode == null &&
                !Util.getEpisodeIdList(true, -1, -1).isEmpty()) {
            playbackMetaEpisode.setText(Constants.CHOOSE_EPISODE);
            playbackArtButton.setImageBitmap(
                    BitmapFactory.decodeResource(res, 
                            R.drawable.ic_album_loading));
            playbackPlayPauseButton.setEnabled(false);
            isPlaylistEmpty = true;
        }
        else {
            playbackMetaEpisode.setText(Constants.NO_DOWNLOADED);
            playbackArtButton.setImageBitmap(
                    BitmapFactory.decodeResource(res, 
                            R.drawable.ic_album_loading));
            playbackPlayPauseButton.setEnabled(false);
            isPlaylistEmpty = true;
        }
        if (mBound && mService != null & mService.isPlaying()) {
            playbackPlayPauseButton.setImageResource(
                    R.drawable.btn_playback_pause_normal_holo_dark);
            playbackFfwdButton.setEnabled(true);
            playbackRewButton.setEnabled(true);
        }
        else {
            playbackPlayPauseButton.setImageResource(
                    R.drawable.btn_playback_play_normal_holo_dark);
            playbackFfwdButton.setEnabled(false);
            playbackRewButton.setEnabled(false);
        }
        registerReceiver(updateEpisodeReceiver, 
                new IntentFilter(Constants.ACTION_UPDATE_UI));
    }
    
    @Override
    public void onPause() {
        ApplicationEx.dbHelper.setAppLastTab(mViewPager.getCurrentItem());
        unregisterReceiver(playingReceiver);
        unregisterReceiver(updateEpisodeReceiver);
        if (mBound) {
            mService.removeClient(mMessenger);
            unbindService(mConnection);
            mBound = false;
        }
        ApplicationEx.setMainActive(false);
        super.onPause();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        if (currTab.equals(Constants.FEEDS_TAB))
            inflater.inflate(R.menu.feed_menu, menu);
        else if (currTab.equals(Constants.EPISODES_TAB))
            inflater.inflate(R.menu.episode_menu, menu);
        mMenu = menu;
        return true;
    }
    
    @SuppressLint("NewApi")
    private void startUpdate() {
        if (Integer.parseInt(Util.readStringPreference(
                R.string.update_key, Constants.ZERO)) == 1)
            Util.setRepeatingAlarm(Integer.parseInt(
                    Util.readStringPreference(R.string.interval_key,
                            Constants.SIXTY)), true);
        else if (Integer.parseInt(Util.readStringPreference(
                R.string.update_key, Constants.ZERO)) == 2) {
            Magics currMagics = 
                ApplicationEx.dbHelper.getNextMagicInterval();
            Util.setMagicAlarm(currMagics.getTime(), currMagics.getFeeds());
        }
        /*
        Intent intent = new Intent(getApplicationContext(), 
                ActivityMain.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
        */
        ApplicationEx.waitCounter.start();
    }
    /*
    public class GetApiTokenTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... nothing) {
            mAccountInfo = ApplicationEx.dbHelper.getAccount();
            if (mAccountInfo == null)
                startActivityForResult(new Intent(getApplicationContext(), 
                        ActivityAccount.class), 
                    Constants.REQUEST_CODE_CHOOSE_ACCOUNT);
            else if (mAccountInfo[0] != null && mAccountInfo[2] != null) {
                getAuthToken();
            }
            return null;
        }
    }
    
    private void getAuthToken() {
        Account[] mAccounts = AccountManager.get(getApplicationContext())
                    .getAccountsByType(mAccountInfo[2]);
        for (Account account : mAccounts) {
            if (account.name.equals(mAccountInfo[0])) {
                mAccount = account;
                break;
            }
        }
        GetAuthTokenCallback callback = new GetAuthTokenCallback();
        VersionedGetAuthToken.newInstance().create(mAccount, callback)
                .getAuthToken();
    }
    
    public class GetAuthTokenCallback implements
            AccountManagerCallback<Bundle> {
        
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                bundle = (Bundle) result.getResult();
                Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
                if(intent != null) {
                    startActivity(intent);
                } else {
                    onGetAuthToken(bundle);
                }
            } catch (OperationCanceledException e) {
                Log.e(Constants.LOG_TAG, "Authentication request canceled", e);
            } catch (AuthenticatorException e) {
                Log.e(Constants.LOG_TAG, "Invalid authentication", e);
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, "Authentication server failed to " + 
                        "respond", e);
            }
        }
        
        protected void onGetAuthToken(Bundle bundle) {
            Util.saveAccountInfo(bundle, mAccountInfo);
            resultCode = -2;
            if (Integer.parseInt(Util.readStringPreference(
                    R.string.update_key, Constants.ZERO)) == 1)
                Util.setRepeatingAlarm(Integer.parseInt(
                        Util.readStringPreference(R.string.interval_key,
                                Constants.SIXTY)), true);
            Intent intent = new Intent(getApplicationContext(), 
                    ActivityMain.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        }
    };
    */
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
                    playbackMetaEpisode.setText(Constants.NO_DOWNLOADED);
                    playbackPlayPauseButton.setEnabled(false);
                    playbackFfwdButton.setEnabled(false);
                    playbackRewButton.setEnabled(false);
                }
                playbackPlayPauseButton.setImageResource(
                        R.drawable.btn_playback_pause_normal_holo_dark);
                playbackPlayPauseButton.setVisibility(View.VISIBLE);
                playbackFfwdButton.setEnabled(true);
                playbackRewButton.setEnabled(true);
            }
            else if (intent.getAction().equals(
                    Constants.ACTION_PAUSE_PLAYBACK)) {
                playbackPlayPauseButton.setImageResource(
                        R.drawable.btn_playback_play_normal_holo_dark);
                playbackPlayPauseButton.setVisibility(View.VISIBLE);
                playbackFfwdButton.setEnabled(false);
                playbackRewButton.setEnabled(false);
            }
        }
    }
    
    boolean mBound;
    private static PlaybackService mService;
    
    public PlaybackService getPlaybackService() {
        return mService;
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
                playbackPlayPauseButton.setVisibility(View.VISIBLE);
            }
            else {
                if (!mService.isInit()) {
                    if (mService.getEpisodeId() >= 0) {
                        playbackPlayPauseButton.setEnabled(true);
                        playbackFfwdButton.setEnabled(true);
                        playbackRewButton.setEnabled(true);
                    }
                    else {
                        isPlaylistEmpty = true;
                        playbackArtButton.setImageBitmap(
                                BitmapFactory.decodeResource(res, 
                                        R.drawable.ic_album_loading));
                        playbackMetaEpisode.setText(Constants.NO_DOWNLOADED);
                        playbackPlayPauseButton.setEnabled(false);
                        playbackFfwdButton.setEnabled(false);
                        playbackRewButton.setEnabled(false);
                    }
                    playbackPlayPauseButton.setImageResource(
                            R.drawable.btn_playback_play_normal_holo_dark);
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
                    Constants.UNREAD ? Constants.SHOW_READ :
                        Constants.HIDE_READ;
            else if (currTab.equals(Constants.EPISODES_TAB))
                return ApplicationEx.dbHelper.getCurrentEpisodeRead() ==
                    Constants.UNREAD ? Constants.SHOW_READ :
                        Constants.HIDE_READ;
        case R.id.ToggleDownloaded:
            if (currTab.equals(Constants.FEEDS_TAB))
                return ApplicationEx.dbHelper.getCurrentFeedDownloaded() ==
                    Constants.DOWNLOADED ? Constants.SHOW_CLOUD :
                        Constants.HIDE_CLOUD;
            else if (currTab.equals(Constants.EPISODES_TAB))
                return ApplicationEx.dbHelper.getCurrentEpisodeDownloaded() ==
                    Constants.DOWNLOADED ? Constants.SHOW_CLOUD :
                        Constants.HIDE_CLOUD;
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
                        return Constants.Z_A;
                    else if (currentSort == (Constants.PLAYLIST_SORT_EPISODE|
                                    Constants.PLAYLIST_SORT_DATE))
                        return Constants.NEWEST;
                }
                else
                    return Constants.NEWEST;
            case 0|Constants.PLAYLIST_SORT_TYPE_DESC:
                if (currTab.equals(Constants.FEEDS_TAB)) {
                    if (currentSort == (Constants.PLAYLIST_SORT_FEED|
                                Constants.PLAYLIST_SORT_DATE))
                        return Constants.A_Z;
                    else if (currentSort == (Constants.PLAYLIST_SORT_EPISODE|
                            Constants.PLAYLIST_SORT_DATE))
                        return Constants.OLDEST;
                }
                else
                    return Constants.OLDEST;
            case 2|Constants.PLAYLIST_SORT_TYPE_ASC:
                return Constants.NEWEST;
            case 6^Constants.PLAYLIST_SORT_TYPE_DESC:
                return Constants.OLDEST;
            }
        case R.id.SortBy:
            return currentSort == (Constants.PLAYLIST_SORT_FEED |
                    Constants.PLAYLIST_SORT_DATE) ? Constants.SORT_BY_LATEST :
                        Constants.SORT_BY_NAME;
        default:
            return Constants.EMPTY;
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
            playbackIntent.putExtra(Constants.START_EP, 
                    ApplicationEx.dbHelper.getCurrentEpisode());
            playbackIntent.putIntegerArrayListExtra(Constants.EPISODES, 
                    Util.readCurrentPlaylist());
            playbackIntent.putExtra(Constants.INIT, init);
            playbackIntent.putExtra(Constants.ACTIVITY, activity);
            startService(playbackIntent);
            return null;
        }
        
    }
    
    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            mMenu.findItem(R.id.ToggleRead).setTitle(
                    getMenuTitle(R.id.ToggleRead));
            mMenu.findItem(R.id.ToggleDownloaded).setTitle(
                    getMenuTitle(R.id.ToggleDownloaded));
            mMenu.findItem(R.id.SortOrder).setTitle(
                    getMenuTitle(R.id.SortOrder));
            if (currTab.equals(Constants.FEEDS_TAB))
                mMenu.findItem(R.id.SortBy).setTitle(
                        getMenuTitle(R.id.SortBy));
            Fragment currFrag = null;
            switch(item.getItemId()) {
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
                serviceIntent.putExtra(Constants.SYNC,
                        ApplicationEx.isSyncing() &&
                        Util.readBooleanFromFile(Constants.GOOGLE_FILENAME));
                serviceIntent.putExtra(Constants.FEED_LIST,
                        ApplicationEx.dbHelper.getFeedAddresses(1));
                startService(serviceIntent);
                break;
            case R.id.MultiSelect:
                switch(mViewPager.getCurrentItem()) {
                case 0:
                    if (((FragmentFeeds)mTabsAdapter.getFragment(0))
                            .getChoiceMode() != ListView.CHOICE_MODE_MULTIPLE)
                        ((FragmentFeeds)mTabsAdapter.getFragment(0))
                            .setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                    else
                        ((FragmentFeeds)mTabsAdapter.getFragment(0))
                            .setChoiceMode(ListView.CHOICE_MODE_NONE);
                    break;
                case 1:
                    if (ApplicationEx.epAdapter.getCount() > 0)
                        ApplicationEx.dbHelper.setAppEpisodePosition(
                            (int) ApplicationEx.epAdapter.getItemId(
                                ((FragmentEpisodes)mTabsAdapter
                                        .getFragment(1))
                                    .getListView().getFirstVisiblePosition()));
                    ApplicationEx.epAdapter.clearContexts();
                    if (((FragmentEpisodes)mTabsAdapter.getFragment(1))
                            .getChoiceMode() != ListView.CHOICE_MODE_MULTIPLE) {
                        ((FragmentEpisodes)mTabsAdapter.getFragment(1))
                            .setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                    }
                    else {
                        ((FragmentEpisodes)mTabsAdapter.getFragment(1))
                            .setChoiceMode(ListView.CHOICE_MODE_NONE);
                    }
                    break;
                }
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
        } catch (IllegalStateException e) {
            // TODO
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
        private SparseArray<Fragment> mPageReferenceMap =
                new SparseArray<Fragment>(2);
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
            if (mMenu != null && mMenu.findItem(R.id.EpisodeFilter) != null)
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
            if (intent.hasExtra(Constants.ID_LIST)) {
                idList = intent.getIntegerArrayListExtra(Constants.ID_LIST);
            }
            if (!idList.isEmpty() || !isPlaylistEmpty) {
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
                playbackMetaEpisode.setText(Constants.NO_DOWNLOADED);
                playbackPlayPauseButton.setEnabled(false);
            }
        }
    }
    
    private static class PlaybackHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case PlaybackService.PLAYLIST_EMPTY:
                playbackArtButton.setImageBitmap(
                        BitmapFactory.decodeResource(res, 
                                R.drawable.ic_album_loading));
                playbackMetaEpisode.setText(Constants.NO_DOWNLOADED);
                playbackPlayPauseButton.setEnabled(false);
                break;
            default:
                super.handleMessage(msg);
                break;
            }
        }
    }
    
    final Messenger mMessenger = new Messenger(new PlaybackHandler());
    
}