package com.jeffthefate.podcatcher.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.PodcastExpandableAdapter;
import com.jeffthefate.podcatcher.PodcastExpandableAdapter.AdapterChange;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.Util;
import com.jeffthefate.podcatcher.VersionedExpandGroup;
import com.jeffthefate.podcatcher.activity.ActivityBase;
import com.jeffthefate.podcatcher.activity.ActivityMain;
import com.jeffthefate.podcatcher.service.PlaybackService;

public class FragmentFeeds extends FragmentBase implements 
        OnCreateContextMenuListener, OnChildClickListener, 
        OnGroupCollapseListener, OnGroupExpandListener, OnGroupClickListener,
        AdapterChange {

    private ExpandableListView mList;
    
    private RefreshReceiver refreshReceiver;
    private DurationReceiver durationReceiver;
    protected PlayStateReceiver playStateReceiver;
    
    private TextView durationText;
    private TextView totalText;
    
    private ImageView readIndicator;
    private ImageView cloudIndicator;
    private ImageView downloadIndicator;
    private ImageView sortIndicator;
    private ImageView bottomBorder;
    
    RelativeLayout mEmptyView;
    AdapterChange adapterChange = this;
    
    private MultiActionMode multiActionMode;
    
    public FragmentFeeds() {
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshReceiver = new RefreshReceiver();
        durationReceiver = new DurationReceiver();
        playStateReceiver = new PlayStateReceiver();
        multiActionMode = new MultiActionMode();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.simple_list_expand, container,
                false);
        durationText = (TextView) v.findViewById(R.id.TotalDurationText);
        totalText = (TextView) v.findViewById(R.id.TotalEpisodesText);
        emptyText = (TextView) v.findViewById(R.id.EmptyText);
        emptyProgress = (ProgressBar) v.findViewById(
                R.id.EmptyProgress);
        readIndicator = (ImageView) v.findViewById(
                R.id.FeedsReadIndicator);
        downloadIndicator = (ImageView) v.findViewById(
                R.id.FeedsDownloadIndicator);
        cloudIndicator = (ImageView) v.findViewById(
                R.id.FeedsCloudIndicator);
        sortIndicator = (ImageView) v.findViewById(
                R.id.FeedsSortIndicator);
        bottomBorder = (ImageView) v.findViewById(R.id.BottomBorder);
        bottomBorder.setBackgroundColor(Constants.HOLO_BLUE);
        return v;
    }
    
    private void setupList() {
        mList = (ExpandableListView) getListView();
        mEmptyView = (RelativeLayout) getView().findViewById(
                android.R.id.empty);
        mList.setEmptyView(mEmptyView);
        mList.setOnItemClickListener(mOnClickListener);
        mList.setOnGroupClickListener(this);
        mList.setOnChildClickListener(this);
        mList.setOnGroupExpandListener(this);
        mList.setOnGroupCollapseListener(this);
        registerForContextMenu(mList);
    }

    @Override
    public void onResume() {
        super.onResume();
        setupList();
        if (ApplicationEx.feedAdapter == null) {
            ApplicationEx.feedAdapter = new PodcastExpandableAdapter(
                    getActivity(), ApplicationEx.feedRead,
                    ApplicationEx.feedDownloaded, ApplicationEx.feedSort,
                    ApplicationEx.feedSortType, ApplicationEx.archive,
                    ApplicationEx.isSyncing(), this);
            setListAdapter(ApplicationEx.feedAdapter);
        }
        else {
            setListAdapter(ApplicationEx.feedAdapter);
            ApplicationEx.feedAdapter.notifyDataSetChanged();
            if (ApplicationEx.feedAdapter.getGroupCount() == 0) 
                setEmptyVisible(true);
        }
        durationText.setText(Util.getTimeString(
                ApplicationEx.feedAdapter.getTotalDuration()));
        totalText.setText(TextUtils.concat(Integer.toString(
                ApplicationEx.feedAdapter.getDownloadedCount()), Constants.OF,
                Integer.toString(ApplicationEx.feedAdapter.getTotalCount())));
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_REFRESH_FEEDS);
        intentFilter.addAction(Constants.ACTION_IMAGE_CHANGE);
        getActivity().registerReceiver(refreshReceiver, intentFilter);
        getActivity().registerReceiver(durationReceiver, 
                new IntentFilter(Constants.ACTION_REFRESH_DURATION));
        intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_START_PLAYBACK);
        intentFilter.addAction(Constants.ACTION_PAUSE_PLAYBACK);
        getActivity().registerReceiver(playStateReceiver, intentFilter);
        if ((ApplicationEx.feedRead & Constants.READ) == Constants.READ)
            readIndicator.setColorFilter(null);
        else
            readIndicator.setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_ATOP);
        if ((ApplicationEx.feedDownloaded & Constants.DOWNLOADED) ==
                Constants.DOWNLOADED)
            downloadIndicator.setColorFilter(null);
        else
            downloadIndicator.setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_ATOP);
        if ((ApplicationEx.feedDownloaded & Constants.TO_DOWNLOAD) ==
                Constants.TO_DOWNLOAD)
            cloudIndicator.setColorFilter(null);
        else
            cloudIndicator.setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_ATOP);
        switch (ApplicationEx.feedSortType) {
        case Constants.PLAYLIST_SORT_TYPE_ASC:
            sortIndicator.setImageResource(R.drawable.sort_down);
            break;
        case Constants.PLAYLIST_SORT_TYPE_DESC:
            sortIndicator.setImageResource(R.drawable.sort_up);
            break;
        }
    }
    
    @Override
    public void onPause() {
        ApplicationEx.dbHelper.setAppFeedPosition(
                getListView().getFirstVisiblePosition());
        getActivity().unregisterReceiver(refreshReceiver);
        getActivity().unregisterReceiver(durationReceiver);
        getActivity().unregisterReceiver(playStateReceiver);
        ApplicationEx.feedAdapter.clearContexts();
        super.onPause();
    }
    
    private final class MultiActionMode implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            com.actionbarsherlock.view.MenuInflater inflater =
                    ((ActivityMain)getActivity()).getSupportMenuInflater();
            inflater.inflate(R.menu.multi_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode,
                com.actionbarsherlock.view.MenuItem item) {
            switch (item.getItemId()) {
            case R.id.download:
                ApplicationEx.feedAdapter.downloadSelected();
                break;
            case R.id.delete:
                ApplicationEx.feedAdapter.deleteSelected();
                break;
            case R.id.read:
                ApplicationEx.feedAdapter.markSelectedRead(true);
                break;
            case R.id.unread:
                ApplicationEx.feedAdapter.markSelectedRead(false);
                break;
            case R.id.lock:
                break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            ApplicationEx.feedAdapter.resetViews();
            setChoiceMode(ListView.CHOICE_MODE_NONE);
        }
        
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        PodcastExpandableAdapter adapter = ((PodcastExpandableAdapter) 
                getExpandableListView().getExpandableListAdapter());
        if (adapter.getContextFeed()) {
            if (!adapter.getContextRead())
                inflater.inflate(R.menu.feed_context_unread, menu);
            else
                inflater.inflate(R.menu.feed_context_read, menu);
            if (!ApplicationEx.sharedPrefs.getBoolean(
                    getString(R.string.tweaks_key), false))
                menu.removeItem(R.id.ForceUpdateFeed);
        }
        else {
            if (!adapter.getContextDownloaded()) {
                if (!adapter.getContextRead())
                    inflater.inflate(R.menu.episode_context_read_cloud, menu);
                else
                    inflater.inflate(R.menu.episode_context_unread_cloud, menu);
                if ((Util.readBooleanPreference(R.string.wifi_key, false) ?
                        !Util.hasWifi() : false) ||
                    (Util.readBooleanPreference(R.string.power_key, false) ?
                         Util.getConnectedState() <= 0 : false) ||
                    (Util.readBooleanPreference(R.string.four_g_key, false) ?
                        !Util.has4G() : false)) {
                MenuItem item = menu.findItem(R.id.DownloadEpisode);
                if (item != null)
                    item.setEnabled(false);
            }
            }
            else {
                if (!adapter.getContextRead())
                    inflater.inflate(R.menu.episode_context_read_downloaded,
                            menu);
                else
                    inflater.inflate(R.menu.episode_context_unread_downloaded,
                            menu);
            }
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListContextMenuInfo info;
        if (item.getMenuInfo().getClass().equals(
                ExpandableListContextMenuInfo.class))
            info = (ExpandableListContextMenuInfo) item.getMenuInfo();
        else
            return false;
        switch(item.getItemId()) {
        case R.id.UpdateFeed:
            ApplicationEx.feedAdapter.updateFeed(
                    ExpandableListView.getPackedPositionGroup(
                            info.packedPosition), false);
            return true;
        case R.id.ForceUpdateFeed:
            ApplicationEx.feedAdapter.updateFeed(
                    ExpandableListView.getPackedPositionGroup(
                            info.packedPosition), true);
            return true;
        case R.id.MarkAllRead:
            ApplicationEx.feedAdapter.markAllRead(true, ApplicationEx.feedRead,
                    ApplicationEx.feedDownloaded, ApplicationEx.feedSort,
                    ApplicationEx.feedSortType, 
                    ExpandableListView.getPackedPositionGroup(
                            info.packedPosition), ApplicationEx.archive);
            return true;
        case R.id.MarkAllUnread:
            ApplicationEx.feedAdapter.markAllRead(false, ApplicationEx.feedRead,
                    ApplicationEx.feedDownloaded, ApplicationEx.feedSort,
                    ApplicationEx.feedSortType, 
                    ExpandableListView.getPackedPositionGroup(
                            info.packedPosition), ApplicationEx.archive);
            return true;
        case R.id.Unsubscribe:
            ApplicationEx.feedAdapter.unsubscribe(ApplicationEx.feedRead,
                    ApplicationEx.feedDownloaded, ApplicationEx.feedSort,
                    ApplicationEx.feedSortType,
                    ExpandableListView.getPackedPositionGroup(
                            info.packedPosition), ApplicationEx.archive);
            return true;
        case R.id.PlayEpisode:
            ApplicationEx.feedAdapter.playEpisode(
                    ExpandableListView.getPackedPositionChild(
                            info.packedPosition),
                    ExpandableListView.getPackedPositionGroup(
                            info.packedPosition));
            return true;
        case R.id.MarkEpisodeRead:
            ApplicationEx.feedAdapter.markEpisodeRead(true,
                    ApplicationEx.feedRead,
                    ApplicationEx.feedDownloaded, ApplicationEx.feedSort,
                    ApplicationEx.feedSortType,
                    ExpandableListView.getPackedPositionChild(
                            info.packedPosition),
                    ExpandableListView.getPackedPositionGroup(
                            info.packedPosition), ApplicationEx.archive);
            return true;
        case R.id.MarkEpisodeUnread:
            ApplicationEx.feedAdapter.markEpisodeRead(false,
                    ApplicationEx.feedRead,
                    ApplicationEx.feedDownloaded, ApplicationEx.feedSort,
                    ApplicationEx.feedSortType,
                    ExpandableListView.getPackedPositionChild(
                            info.packedPosition),
                    ExpandableListView.getPackedPositionGroup(
                            info.packedPosition), ApplicationEx.archive);
            return true;
        case R.id.DownloadEpisode:
            ApplicationEx.feedAdapter.downloadEpisode(
                    ExpandableListView.getPackedPositionChild(
                            info.packedPosition),
                    ExpandableListView.getPackedPositionGroup(
                            info.packedPosition));
            return true;
        case R.id.DeleteEpisode:
            if (ApplicationEx.feedAdapter.getContextPlaying()) {
                PlaybackService service =
                        ((ActivityMain) getActivity()).getPlaybackService();
                if (service != null)
                    service.next();
            }
            ApplicationEx.feedAdapter.deleteEpisode(ApplicationEx.feedRead,
                    ApplicationEx.feedDownloaded, ApplicationEx.feedSort,
                    ApplicationEx.feedSortType,
                    ExpandableListView.getPackedPositionChild(
                            info.packedPosition),
                    ExpandableListView.getPackedPositionGroup(
                            info.packedPosition), ApplicationEx.archive);
            return true;
        case R.id.EpisodeDetails:
            ApplicationEx.feedAdapter.showDetails(
                    ExpandableListView.getPackedPositionGroup(
                            info.packedPosition),
                    ExpandableListView.getPackedPositionChild(
                            info.packedPosition), false);
            return true;
        case R.id.FeedDetails:
            ApplicationEx.feedAdapter.showDetails(
                    ExpandableListView.getPackedPositionGroup(
                            info.packedPosition),
                    ExpandableListView.getPackedPositionChild(
                            info.packedPosition), true);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }
    
    final private AdapterView.OnItemClickListener mOnClickListener = 
        new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, 
                    long id) {
                onListItemClick((ListView) parent, v, position, id);
        }
    };
    
    @Override
    public void onGroupExpand(int groupPosition) {
        ApplicationEx.feedAdapter.stopFeedPlaying();
        ApplicationEx.feedAdapter.startEpisodePlaying();
    }

    @Override
    public void onGroupCollapse(int groupPosition) {
        ApplicationEx.feedAdapter.startFeedPlaying();
        ApplicationEx.feedAdapter.stopEpisodePlaying();
    }
    
    @Override
    public boolean onGroupClick(ExpandableListView parent, View v,
            int groupPosition, long id) {
        if (!parent.isGroupExpanded(groupPosition))
            VersionedExpandGroup.newInstance().create(parent)
                    .expandGroup(groupPosition);
        else
            parent.collapseGroup(groupPosition);
        return true;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, 
            int groupPosition, int childPosition, long id) {
        return false;
    }

    public void onListItemClick(ListView l, View v, int position, long id) {}
    
    public void setListAdapter(PodcastExpandableAdapter adapter) {
        if (mList != null) {
            mList.setAdapter(adapter);
            mList.setSelection(ApplicationEx.dbHelper.getAppFeedPosition());
        }
    }
    
    public ExpandableListView getExpandableListView() {
        if (mList == null)
            setupList();
        return mList;
    }
    
    private void refreshFeeds() {
        int currPos = getExpandableListView().getFirstVisiblePosition();
        ApplicationEx.feedAdapter.refreshFeeds(ApplicationEx.feedRead,
                ApplicationEx.feedDownloaded, ApplicationEx.feedSort,
                ApplicationEx.feedSortType, ApplicationEx.archive);
        getExpandableListView().setSelectionFromTop(currPos, 0);
    }
    
    public void updateRead() {
        ApplicationEx.feedRead = ApplicationEx.feedRead ^ Constants.READ;
        if ((ApplicationEx.feedRead & Constants.READ) == Constants.READ)
            readIndicator.setColorFilter(null);
        else
            readIndicator.setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_ATOP);
        ApplicationEx.dbHelper.setCurrentFeedRead(ApplicationEx.feedRead);
        refreshFeeds();
    }
    
    public void updateDownloaded() {
        ApplicationEx.feedDownloaded =
                ApplicationEx.feedDownloaded ^ Constants.TO_DOWNLOAD;
        if ((ApplicationEx.feedDownloaded & Constants.DOWNLOADED) ==
                Constants.DOWNLOADED)
            downloadIndicator.setColorFilter(null);
        else
            downloadIndicator.setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_ATOP);
        if ((ApplicationEx.feedDownloaded & Constants.TO_DOWNLOAD) ==
                Constants.TO_DOWNLOAD)
            cloudIndicator.setColorFilter(null);
        else
            cloudIndicator.setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_ATOP);
        ApplicationEx.dbHelper.setCurrentFeedDownloaded(
                ApplicationEx.feedDownloaded);
        refreshFeeds();
    }
    
    public void updateGroup() {
        switch(ApplicationEx.feedSort) {
        case 5:
            ApplicationEx.feedSort = 6;
            break;
        case 6:
            ApplicationEx.feedSort = 5;
            break;
        default:
            ApplicationEx.feedSort = -1;
            break;
        }
        ApplicationEx.dbHelper.setCurrentFeedSort(ApplicationEx.feedSort);
        refreshFeeds();
    }
    
    public void updateSort() {
        switch(ApplicationEx.feedSortType) {
        case Constants.PLAYLIST_SORT_TYPE_ASC:
            ApplicationEx.feedSortType = Constants.PLAYLIST_SORT_TYPE_DESC;
            sortIndicator.setImageResource(R.drawable.sort_up);
            break;
        case Constants.PLAYLIST_SORT_TYPE_DESC:
            ApplicationEx.feedSortType = Constants.PLAYLIST_SORT_TYPE_ASC;
            sortIndicator.setImageResource(R.drawable.sort_down);
            break;
        default:
            ApplicationEx.feedSortType = Constants.PLAYLIST_SORT_TYPE_ASC;
            sortIndicator.setImageResource(R.drawable.sort_down);
            break;
        }
        ApplicationEx.dbHelper.setCurrentFeedSortType(
                ApplicationEx.feedSortType);
        refreshFeeds();
    }
    
    private class DurationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_REFRESH_DURATION)) {
                durationText.setText(Util.getTimeString(
                        ApplicationEx.feedAdapter.getTotalDuration()));
                totalText.setText(TextUtils.concat(Integer.toString(
                        ApplicationEx.feedAdapter.getDownloadedCount()),
                        Constants.OF, Integer.toString(
                                ApplicationEx.feedAdapter.getTotalCount())));
            }
        }
    }
    
    class RefreshReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_REFRESH_FEEDS) ||
                    intent.getAction().equals(Constants.ACTION_IMAGE_CHANGE)) {
                refreshFeeds();
            }
        }
    }
    
    class PlayStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_START_PLAYBACK)) {
                ApplicationEx.feedAdapter.startLeftPlaying();
                ApplicationEx.feedAdapter.startRightPlaying();
            }
            else if (intent.getAction().equals(
                    Constants.ACTION_PAUSE_PLAYBACK)) {
                ApplicationEx.feedAdapter.stopLeftPlaying();
                ApplicationEx.feedAdapter.stopRightPlaying();
            }
        }
    }

    @Override
    public void setEmptyVisible(boolean visible) {
        emptyText.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void setProgressVisible(boolean visible) {
        emptyProgress.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void changeEmptyText(String text) {
        emptyText.setText(text);
    }
    
    @Override
    public boolean isEmptyVisible() {
        return emptyText.getVisibility() == View.VISIBLE ? true : false;
    }
    
    @Override
    public void setChoiceMode(int choiceMode) {
        getExpandableListView().setChoiceMode(choiceMode);
        if (choiceMode == ListView.CHOICE_MODE_MULTIPLE) {
            ((ActivityBase) getActivity()).startActionMode(
                    multiActionMode);
            ApplicationEx.feedAdapter.setMultiSelect(true);
        }
        else if (choiceMode == ListView.CHOICE_MODE_NONE)
            ApplicationEx.feedAdapter.setMultiSelect(false);
    }
    
    @Override
    public int getChoiceMode() {
        return getExpandableListView().getChoiceMode();
    }

    @Override
    public void showDetailsDialog(String[] values, boolean isFeed) {
        DialogFragment detailsDialog = null;
        if (!isFeed)
            detailsDialog = FragmentEpisodeDetailsDialog.newInstance(values);
        else
            detailsDialog = FragmentFeedDetailsDialog.newInstance(values);
        showDialog(detailsDialog, Constants.DIALOG_DETAILS);
    }
    
}