package com.jeffthefate.pod_dev.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.Constants;
import com.jeffthefate.pod_dev.PodcastExpandableAdapter;
import com.jeffthefate.pod_dev.PodcastExpandableAdapter.AdapterChange;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.Util;
import com.jeffthefate.pod_dev.VersionedExpandGroup;

public class FragmentFeeds extends FragmentBase implements 
        OnCreateContextMenuListener, OnChildClickListener, 
        OnGroupCollapseListener, OnGroupExpandListener, OnGroupClickListener,
        AdapterChange {

    final private AdapterView.OnItemClickListener mOnClickListener = 
        new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, 
                    long id) {
                onListItemClick((ListView) parent, v, position, id);
        }
    };
    
    @Override
    public void onGroupExpand(int groupPosition) {
        mAdapter.stopFeedPlaying();
        mAdapter.startEpisodePlaying();
    }

    @Override
    public void onGroupCollapse(int groupPosition) {
        mAdapter.startFeedPlaying();
        mAdapter.stopEpisodePlaying();
    }
    
    @Override
    public boolean onGroupClick(ExpandableListView parent, View v,
            int groupPosition, long id) {
        if (!parent.isGroupExpanded(groupPosition)) {
            VersionedExpandGroup.newInstance().create(parent)
                    .expandGroup(groupPosition);
        }
        else {
            parent.collapseGroup(groupPosition);
        }
        return true;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, 
            int groupPosition, int childPosition, long id) {
        return false;
    }

    public void onListItemClick(ListView l, View v, int position, long id) {}
    
    private PodcastExpandableAdapter mAdapter;
    ExpandableListView mList;
    private NewFeedReceiver newFeedReceiver;
    protected UpdateImageReceiver updateImageReceiver;
    protected PlayStateReceiver playStateReceiver;
    RelativeLayout mEmptyView;
    AdapterChange adapterChange = this;
    
    private int sort;
    
    public void setListAdapter(PodcastExpandableAdapter adapter) {
        mAdapter = adapter;
        if (mList != null)
            mList.setAdapter(adapter);
    }
    
    public ExpandableListView getExpandableListView() {
        if (mList == null)
            setupList();
        return mList;
    }
    
    public PodcastExpandableAdapter getExpandableListAdapter() {
        return mAdapter;
    }
    
    public FragmentFeeds() {
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        newFeedReceiver = new NewFeedReceiver();
        updateImageReceiver = new UpdateImageReceiver();
        playStateReceiver = new PlayStateReceiver();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.simple_list_expand, container, false);
        emptyText = (TextView) v.findViewById(R.id.EmptyText);
        emptyProgress = (ProgressBar) v.findViewById(
                R.id.EmptyProgress);
        archive = Util.readBooleanPreference(R.string.archive_key, false) ?
                1 : 0;
        return v;
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
        }
        else {
            if (!adapter.getContextDownloaded()) {
                if (!adapter.getContextRead())
                    inflater.inflate(R.menu.episode_context_read_cloud, menu);
                else
                    inflater.inflate(R.menu.episode_context_unread_cloud, menu);
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
            ExpandableListView listView = getExpandableListView();
            mAdapter.updateFeed(
                    listView.getPackedPositionGroup(info.packedPosition));
            return true;
        case R.id.MarkAllRead:
            listView = getExpandableListView();
            mAdapter.markAllRead(true, read, downloaded, sort, sortType, 
                    listView.getPackedPositionGroup(info.packedPosition),
                    archive);
            return true;
        case R.id.MarkAllUnread:
            listView = getExpandableListView();
            mAdapter.markAllRead(false, read, downloaded, sort, sortType, 
                    listView.getPackedPositionGroup(info.packedPosition),
                    archive);
            return true;
        case R.id.Unsubscribe:
            listView = getExpandableListView();
            mAdapter.unsubscribe(read, downloaded, sort, sortType,
                    listView.getPackedPositionGroup(info.packedPosition),
                    archive);
            return true;
        case R.id.PlayEpisode:
            listView = getExpandableListView();
            mAdapter.playEpisode(listView.getPackedPositionChild(
                            info.packedPosition),
                    listView.getPackedPositionGroup(info.packedPosition));
            return true;
        case R.id.MarkEpisodeRead:
            listView = getExpandableListView();
            mAdapter.markEpisodeRead(true, read, downloaded, sort, 
                    sortType, listView.getPackedPositionChild(
                            info.packedPosition),
                    listView.getPackedPositionGroup(info.packedPosition),
                    archive);
            return true;
        case R.id.MarkEpisodeUnread:
            listView = getExpandableListView();
            mAdapter.markEpisodeRead(false, read, downloaded, sort, 
                    sortType, listView.getPackedPositionChild(
                            info.packedPosition),
                    listView.getPackedPositionGroup(info.packedPosition),
                    archive);
            return true;
        case R.id.DownloadEpisode:
            listView = getExpandableListView();
            mAdapter.downloadEpisode(listView.getPackedPositionChild(
                    info.packedPosition),
                listView.getPackedPositionGroup(info.packedPosition));
            return true;
        case R.id.DeleteEpisode:
            listView = getExpandableListView();
            mAdapter.deleteEpisode(read, downloaded, sort, sortType,
                    listView.getPackedPositionChild(info.packedPosition),
                    listView.getPackedPositionGroup(info.packedPosition),
                    archive);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }
    
    private void setupList() {
        mList = (ExpandableListView) getView().findViewById(
                android.R.id.list);
        mEmptyView = (RelativeLayout) getView().findViewById(
                android.R.id.empty);
        mList.setEmptyView(mEmptyView);
        mList.setOnItemClickListener(mOnClickListener);
        mList.setOnGroupClickListener(this);
        mList.setOnChildClickListener(this);
        mList.setOnGroupExpandListener(this);
        mList.setOnGroupCollapseListener(this);
        registerForContextMenu(mList);
        mList.setSelectionFromTop(ApplicationEx.dbHelper.getAppFeedPosition(),
                0);
        /*
        mList.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstItem, int itemCount,
                    int totalCount) {
                if (totalCount > 0 && (firstItem + itemCount) == totalCount &&
                        ((ActivityMain) getActivity()).getCurrentPosition() ==
                            0) {
                    if (((ActivityMain) getActivity()).getAnimated())
                        ((ActivityMain) getActivity()).unanimateControls();
                    ((ActivityMain) getActivity()).setFeedAtEnd(true);
                }
                else
                    ((ActivityMain) getActivity()).setFeedAtEnd(false);
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int state) {
                if (animateTask != null) {
                    animateTask.cancel(true);
                    ((ActivityMain) getActivity()).cancelAnimate();
                }
                if (!((ActivityMain) getActivity()).getFeedAtEnd()) {
                    animateTask = new AnimateTask(state);
                    animateTask.executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
            
        });
        */
    }

    @Override
    public void onResume() {
        super.onResume();
        setupList();
        read = ApplicationEx.dbHelper.getCurrentFeedRead();
        downloaded = ApplicationEx.dbHelper.getCurrentFeedDownloaded();
        sort = ApplicationEx.dbHelper.getCurrentFeedSort();
        sortType = ApplicationEx.dbHelper.getCurrentFeedSortType();
        mAdapter = new PodcastExpandableAdapter(getActivity(), read, downloaded,
                sort, sortType, archive, ApplicationEx.isSyncing(), this);
        setListAdapter(mAdapter);
        getActivity().registerReceiver(newFeedReceiver, 
                new IntentFilter(Constants.ACTION_REFRESH_FEEDS));
        getActivity().registerReceiver(updateImageReceiver, 
                new IntentFilter(Constants.ACTION_IMAGE_CHANGE));
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_START_PLAYBACK);
        intentFilter.addAction(Constants.ACTION_PAUSE_PLAYBACK);
        getActivity().registerReceiver(playStateReceiver, intentFilter);
        if (ApplicationEx.getIsPlaying()) {
            ((PodcastExpandableAdapter)getExpandableListAdapter())
                    .startLeftPlaying();
            ((PodcastExpandableAdapter)getExpandableListAdapter())
                    .startRightPlaying();
        }
        else {
            ((PodcastExpandableAdapter)getExpandableListAdapter())
                    .stopLeftPlaying();
            ((PodcastExpandableAdapter)getExpandableListAdapter())
                    .stopRightPlaying();
        }
    }
    
    @Override
    public void onPause() {
        ApplicationEx.dbHelper.setAppFeedPosition(
                getListView().getFirstVisiblePosition());
        getActivity().unregisterReceiver(newFeedReceiver);
        getActivity().unregisterReceiver(updateImageReceiver);
        getActivity().unregisterReceiver(playStateReceiver);
        super.onPause();
    }
    
    private void refreshFeeds() {
        int currPos = getExpandableListView().getFirstVisiblePosition();
        mAdapter.refreshFeeds(read, downloaded, sort, sortType, archive);
        getExpandableListView().setSelectionFromTop(currPos, 0);
    }
    
    public void updateRead() {
        read = read ^ Constants.READ;
        ApplicationEx.dbHelper.setCurrentFeedRead(read);
        refreshFeeds();
    }
    
    public void updateDownloaded() {
        downloaded = downloaded ^ Constants.TO_DOWNLOAD;
        ApplicationEx.dbHelper.setCurrentFeedDownloaded(downloaded);
        refreshFeeds();
    }
    
    public void updateGroup() {
        switch(sort) {
        case 5:
            sort = 6;
            break;
        case 6:
            sort = 5;
            break;
        default:
            sort = -1;
            break;
        }
        ApplicationEx.dbHelper.setCurrentFeedSort(sort);
        refreshFeeds();
    }
    
    public void updateSort() {
        switch(sortType) {
        case Constants.PLAYLIST_SORT_TYPE_ASC:
            sortType = Constants.PLAYLIST_SORT_TYPE_DESC;
            break;
        case Constants.PLAYLIST_SORT_TYPE_DESC:
            sortType = Constants.PLAYLIST_SORT_TYPE_ASC;
            break;
        default:
            sortType = Constants.PLAYLIST_SORT_TYPE_ASC;
            break;
        }
        ApplicationEx.dbHelper.setCurrentFeedSortType(sortType);
        refreshFeeds();
    }
    
    class NewFeedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_REFRESH_FEEDS)) {
                refreshFeeds();
            }
        }
    }
    
    class UpdateImageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_IMAGE_CHANGE)) {
                ((PodcastExpandableAdapter)getExpandableListAdapter())
                        .notifyDataSetChanged();
            }
        }
    }
    
    class PlayStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_START_PLAYBACK)) {
                ((PodcastExpandableAdapter)getExpandableListAdapter())
                        .startLeftPlaying();
                ((PodcastExpandableAdapter)getExpandableListAdapter())
                        .startRightPlaying();
            }
            else if (intent.getAction().equals(
                    Constants.ACTION_PAUSE_PLAYBACK)) {
                ((PodcastExpandableAdapter)getExpandableListAdapter())
                        .stopLeftPlaying();
                ((PodcastExpandableAdapter)getExpandableListAdapter())
                        .stopRightPlaying();
            }
        }
    }

    @Override
    public void onAdapterEmptyChange() {
        if (emptyText.getVisibility() == View.VISIBLE)
            emptyText.setVisibility(View.INVISIBLE);
        else
            emptyText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAdapterProgressChange() {
        if (emptyProgress.getVisibility() == View.VISIBLE)
            emptyProgress.setVisibility(View.INVISIBLE);
        else
            emptyProgress.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAdapterEmptyTextChange(String text) {
        emptyText.setText(text);
    }
    
}
