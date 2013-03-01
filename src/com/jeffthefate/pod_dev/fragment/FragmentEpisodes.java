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
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.Constants;
import com.jeffthefate.pod_dev.PodcastAdapter;
import com.jeffthefate.pod_dev.PodcastAdapter.AdapterChange;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.Util;

public class FragmentEpisodes extends FragmentBase implements AdapterChange {
    
    private PodcastAdapter mAdapter;
    private NewEpisodeReceiver newEpisodeReceiver;
    private DurationReceiver durationReceiver;
    protected UpdateImageReceiver updateImageReceiver;
    protected PlayStateReceiver playStateReceiver;
    
    private TextView durationText;
    
    private ImageView readIndicator;
    private ImageView cloudIndicator;
    private ImageView downloadIndicator;
    private ImageView sortIndicator;
    private ImageView bottomBorder;
    
    public FragmentEpisodes() {
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        newEpisodeReceiver = new NewEpisodeReceiver();
        durationReceiver = new DurationReceiver();
        updateImageReceiver = new UpdateImageReceiver();
        playStateReceiver = new PlayStateReceiver();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.simple_list_episodes, container,
                false);
        durationText = (TextView) v.findViewById(R.id.TotalDurationText);
        emptyText = (TextView) v.findViewById(R.id.EmptyText);
        emptyProgress = (ProgressBar) v.findViewById(
                R.id.EmptyProgress);
        readIndicator = (ImageView) v.findViewById(
                R.id.EpisodesReadIndicator);
        downloadIndicator = (ImageView) v.findViewById(
                R.id.EpisodesDownloadIndicator);
        cloudIndicator = (ImageView) v.findViewById(
                R.id.EpisodesCloudIndicator);
        sortIndicator = (ImageView) v.findViewById(
                R.id.EpisodesSortIndicator);
        bottomBorder = (ImageView) v.findViewById(R.id.BottomBorder);
        bottomBorder.setBackgroundColor(Constants.HOLO_BLUE);
        return v;
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        if (!((PodcastAdapter) getListView().getAdapter())
                .getContextDownloaded()) {
            if (!((PodcastAdapter) getListView().getAdapter())
                    .getContextRead())
                inflater.inflate(R.menu.episode_context_read_cloud, menu);
            else
                inflater.inflate(R.menu.episode_context_unread_cloud, menu);
        }
        else {
            if (!((PodcastAdapter) getListView().getAdapter())
                    .getContextRead())
                inflater.inflate(R.menu.episode_context_read_downloaded, menu);
            else
                inflater.inflate(R.menu.episode_context_unread_downloaded,
                        menu);
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info;
        if (item.getMenuInfo().getClass().equals(AdapterContextMenuInfo.class))
            info = (AdapterContextMenuInfo) item.getMenuInfo();
        else
            return false;
        switch(item.getItemId()) {
        case R.id.PlayEpisode:
            mAdapter.dispatchEpisodesAction(info.position);
            return true;
        case R.id.MarkEpisodeRead:
            mAdapter.markEpisodeRead(true, read, downloaded, sortType, 
                    info.position, archive);
            return true;
        case R.id.MarkEpisodeUnread:
            mAdapter.markEpisodeRead(false, read, downloaded, sortType, 
                    info.position, archive);
            return true;
        case R.id.DownloadEpisode:
            mAdapter.downloadEpisode(info.position);
            return true;
        case R.id.DeleteEpisode:
            mAdapter.deleteEpisode(info.position, read, downloaded, sortType, 
                    archive);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        archive = Util.readBooleanPreference(R.string.archive_key, false) ?
                1 : 0;
        read = ApplicationEx.dbHelper.getCurrentEpisodeRead();
        downloaded = ApplicationEx.dbHelper.getCurrentEpisodeDownloaded();
        sortType = ApplicationEx.dbHelper.getCurrentEpisodeSortType();
        mAdapter = new PodcastAdapter(getActivity(), read, downloaded, 
                sortType, archive, false, Constants.EPISODES_TAB,
                ApplicationEx.isSyncing(), this);
        setListAdapter(mAdapter);
        getActivity().registerReceiver(newEpisodeReceiver, 
                new IntentFilter(Constants.ACTION_REFRESH_EPISODES));
        getActivity().registerReceiver(durationReceiver, 
                new IntentFilter(Constants.ACTION_REFRESH_DURATION));
        getActivity().registerReceiver(updateImageReceiver, 
                new IntentFilter(Constants.ACTION_IMAGE_CHANGE));
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_START_PLAYBACK);
        intentFilter.addAction(Constants.ACTION_PAUSE_PLAYBACK);
        getActivity().registerReceiver(playStateReceiver, intentFilter);
        if (ApplicationEx.getIsPlaying()) {
            ((PodcastAdapter)getListAdapter()).startLeftPlaying();
            ((PodcastAdapter)getListAdapter()).startRightPlaying();
        }
        else {
            ((PodcastAdapter)getListAdapter()).stopLeftPlaying();
            ((PodcastAdapter)getListAdapter()).stopRightPlaying();
        }
        if ((read & Constants.READ) == Constants.READ)
            readIndicator.setVisibility(View.VISIBLE);
        else
            readIndicator.setVisibility(View.INVISIBLE);
        if ((downloaded & Constants.DOWNLOADED) == Constants.DOWNLOADED)
            downloadIndicator.setVisibility(View.VISIBLE);
        else
            downloadIndicator.setVisibility(View.INVISIBLE);
        if ((downloaded & Constants.TO_DOWNLOAD) == Constants.TO_DOWNLOAD)
            cloudIndicator.setVisibility(View.VISIBLE);
        else
            cloudIndicator.setVisibility(View.INVISIBLE);
        switch (sortType) {
        case Constants.PLAYLIST_SORT_TYPE_ASC:
            sortIndicator.setImageResource(R.drawable.sort_down);
            break;
        case Constants.PLAYLIST_SORT_TYPE_DESC:
            sortIndicator.setImageResource(R.drawable.sort_up);
            break;
        }
        ListView eListView = getListView();
        registerForContextMenu(eListView);
        eListView.setSelectionFromTop(
                ApplicationEx.dbHelper.getAppEpisodePosition(), 0);
        /*
        eListView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstItem, int itemCount,
                    int totalCount) {
                if (totalCount > 0 && (firstItem + itemCount) == totalCount &&
                        ((ActivityMain) getActivity()).getCurrentPosition() ==
                            1) {
                    if (((ActivityMain) getActivity()).getAnimated())
                        ((ActivityMain) getActivity()).unanimateControls();
                    ((ActivityMain) getActivity()).setEpisodeAtEnd(true);
                }
                else
                    ((ActivityMain) getActivity()).setEpisodeAtEnd(false);
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int state) {
                if (animateTask != null) {
                    animateTask.cancel(true);
                    ((ActivityMain) getActivity()).cancelAnimate();
                }
                if (!((ActivityMain) getActivity()).getEpisodeAtEnd()) {
                    animateTask = new AnimateTask(state);
                    animateTask.executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
            
        });
        */
    }
    
    @Override
    public void onPause() {
        ApplicationEx.dbHelper.setAppEpisodePosition(
                getListView().getFirstVisiblePosition());
        getActivity().unregisterReceiver(newEpisodeReceiver);
        getActivity().unregisterReceiver(durationReceiver);
        getActivity().unregisterReceiver(updateImageReceiver);
        getActivity().unregisterReceiver(playStateReceiver);
        super.onPause();
    }
    
    private void refreshEpisodes() {
        int currPos = getListView().getFirstVisiblePosition();
        mAdapter.refreshEpisodes(read, downloaded, sortType, archive);
        getListView().setSelectionFromTop(currPos, 0);
    }
    
    @Override
    public void updateRead() {
        read = read ^ Constants.READ;
        if ((read & Constants.READ) == Constants.READ)
            readIndicator.setVisibility(View.VISIBLE);
        else
            readIndicator.setVisibility(View.INVISIBLE);
        ApplicationEx.dbHelper.setCurrentEpisodeRead(read);
        refreshEpisodes();
    }
    
    @Override
    public void updateDownloaded() {
        downloaded = downloaded ^ Constants.TO_DOWNLOAD;
        if ((downloaded & Constants.DOWNLOADED) == Constants.DOWNLOADED)
            downloadIndicator.setVisibility(View.VISIBLE);
        else
            downloadIndicator.setVisibility(View.INVISIBLE);
        if ((downloaded & Constants.TO_DOWNLOAD) == Constants.TO_DOWNLOAD)
            cloudIndicator.setVisibility(View.VISIBLE);
        else
            cloudIndicator.setVisibility(View.INVISIBLE);
        ApplicationEx.dbHelper.setCurrentEpisodeDownloaded(downloaded);
        refreshEpisodes();
    }
    
    @Override
    public void updateSort() {
        switch(sortType) {
        case Constants.PLAYLIST_SORT_TYPE_ASC:
            sortType = Constants.PLAYLIST_SORT_TYPE_DESC;
            sortIndicator.setImageResource(R.drawable.sort_up);
            break;
        case Constants.PLAYLIST_SORT_TYPE_DESC:
            sortType = Constants.PLAYLIST_SORT_TYPE_ASC;
            sortIndicator.setImageResource(R.drawable.sort_down);
            break;
        default:
            sortType = Constants.PLAYLIST_SORT_TYPE_ASC;
            sortIndicator.setImageResource(R.drawable.sort_down);
            break;
        }
        ApplicationEx.dbHelper.setCurrentEpisodeSortType(sortType);
        refreshEpisodes();
    }
    
    @Override
    public void updateGroup() {}
    
    class DurationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_REFRESH_DURATION)) {
                durationText.setText(Util.getTimeString(
                        mAdapter.getTotalDuration()));
            }
        }
    }
    
    class NewEpisodeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_REFRESH_EPISODES)) {
                refreshEpisodes();
            }
        }
    }
    
    class UpdateImageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_IMAGE_CHANGE)) {
                ((PodcastAdapter)getListAdapter()).notifyDataSetChanged();
            }
        }
    }
    
    class PlayStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_START_PLAYBACK)) {
                ((PodcastAdapter)getListAdapter()).startLeftPlaying();
                ((PodcastAdapter)getListAdapter()).startRightPlaying();
            }
            else if (intent.getAction().equals(
                    Constants.ACTION_PAUSE_PLAYBACK)) {
                ((PodcastAdapter)getListAdapter()).stopLeftPlaying();
                ((PodcastAdapter)getListAdapter()).stopRightPlaying();
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
