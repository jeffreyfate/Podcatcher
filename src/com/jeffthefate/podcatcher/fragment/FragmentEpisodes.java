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
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.PodcastAdapter;
import com.jeffthefate.podcatcher.PodcastAdapter.AdapterChange;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.Util;
import com.jeffthefate.podcatcher.activity.ActivityBase;
import com.jeffthefate.podcatcher.activity.ActivityMain;
import com.jeffthefate.podcatcher.service.PlaybackService;

public class FragmentEpisodes extends FragmentBase implements AdapterChange {
    
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
    
    private MultiActionMode multiActionMode;
    
    public FragmentEpisodes() {
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
        View v = inflater.inflate(R.layout.simple_list_episodes, container,
                false);
        durationText = (TextView) v.findViewById(R.id.TotalDurationText);
        totalText = (TextView) v.findViewById(R.id.TotalEpisodesText);
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
            ApplicationEx.epAdapter.dispatchEpisodesAction(info.position);
            return true;
        case R.id.MarkEpisodeRead:
            ApplicationEx.epAdapter.markEpisodeRead(true, ApplicationEx.epRead,
                    ApplicationEx.epDownloaded, ApplicationEx.epSortType,
                    info.position, ApplicationEx.archive);
            return true;
        case R.id.MarkEpisodeUnread:
            ApplicationEx.epAdapter.markEpisodeRead(false, ApplicationEx.epRead,
                    ApplicationEx.epDownloaded, ApplicationEx.epSortType,
                    info.position, ApplicationEx.archive);
            return true;
        case R.id.DownloadEpisode:
            ApplicationEx.epAdapter.downloadEpisode(info.position);
            return true;
        case R.id.DeleteEpisode:
            if (ApplicationEx.epAdapter.getContextPlaying()) {
                PlaybackService service =
                        ((ActivityMain) getActivity()).getPlaybackService();
                if (service != null)
                    service.next();
            }
            ApplicationEx.epAdapter.deleteEpisode(info.position,
                    ApplicationEx.epRead, ApplicationEx.epDownloaded,
                    ApplicationEx.epSortType, ApplicationEx.archive);
            return true;
        case R.id.EpisodeDetails:
            ApplicationEx.epAdapter.showDetails(info.position);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (ApplicationEx.epAdapter == null) {
            ApplicationEx.epAdapter = new PodcastAdapter(getActivity(),
                    ApplicationEx.epRead, ApplicationEx.epDownloaded,
                    ApplicationEx.epSortType, ApplicationEx.archive,
                    ApplicationEx.isSyncing(), this, this);
            setListAdapter(ApplicationEx.epAdapter);
        }
        else {
            setListAdapter(ApplicationEx.epAdapter);
            ApplicationEx.epAdapter.notifyDataSetChanged();
            if (ApplicationEx.epAdapter.getCount() == 0) 
                setEmptyVisible(true);
            try {
                getListView().setSelection(
                        ApplicationEx.epAdapter.getItemPosition(
                            ApplicationEx.dbHelper.getAppEpisodePosition()));
            } catch (IllegalStateException e) {}
        }
        durationText.setText(Util.getTimeString(
                ApplicationEx.epAdapter.getTotalDuration()));
        totalText.setText(TextUtils.concat(Integer.toString(
                ApplicationEx.epAdapter.getDownloadedCount()), Constants.OF,
                Integer.toString(ApplicationEx.epAdapter.getOrderCount())));
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_REFRESH_EPISODES);
        intentFilter.addAction(Constants.ACTION_IMAGE_CHANGE);
        getActivity().registerReceiver(refreshReceiver, intentFilter);
        getActivity().registerReceiver(durationReceiver, 
                new IntentFilter(Constants.ACTION_REFRESH_DURATION));
        intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_START_PLAYBACK);
        intentFilter.addAction(Constants.ACTION_PAUSE_PLAYBACK);
        getActivity().registerReceiver(playStateReceiver, intentFilter);
        if ((ApplicationEx.epRead & Constants.READ) == Constants.READ)
            readIndicator.setColorFilter(null);
        else
            readIndicator.setColorFilter(Color.DKGRAY,
                    PorterDuff.Mode.SRC_ATOP);
        if ((ApplicationEx.epDownloaded & Constants.DOWNLOADED) ==
                Constants.DOWNLOADED)
            downloadIndicator.setColorFilter(null);
        else
            downloadIndicator.setColorFilter(Color.DKGRAY,
                    PorterDuff.Mode.SRC_ATOP);
        if ((ApplicationEx.epDownloaded & Constants.TO_DOWNLOAD) ==
                Constants.TO_DOWNLOAD)
            cloudIndicator.setColorFilter(null);
        else
            cloudIndicator.setColorFilter(Color.DKGRAY,
                    PorterDuff.Mode.SRC_ATOP);
        switch (ApplicationEx.epSortType) {
        case Constants.PLAYLIST_SORT_TYPE_ASC:
            sortIndicator.setImageResource(R.drawable.sort_down);
            break;
        case Constants.PLAYLIST_SORT_TYPE_DESC:
            sortIndicator.setImageResource(R.drawable.sort_up);
            break;
        }
        ListView listView = getListView();
        registerForContextMenu(listView);
        listView.setOnScrollListener(new OnScrollListener() {
            int currentFirstItem;
            int currentItemCount;
            int currentScrollState;
            int currentTotalCount;
            
            @Override
            public void onScroll(AbsListView view, int firstItem, int itemCount,
                    int totalCount) {
                currentFirstItem = firstItem;
                currentItemCount = itemCount;
                currentTotalCount = totalCount;
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int state) {
                currentScrollState = state;
                checkScrollCompleted();
            }
            
            private void checkScrollCompleted() {
                if (currentFirstItem + currentItemCount == currentTotalCount &&
                        ApplicationEx.epAdapter.getOrderPosition(
                                currentFirstItem) + currentItemCount <=
                                ApplicationEx.epAdapter.getOrderCount() &&
                        ApplicationEx.epAdapter.getItem(currentTotalCount-1) ==
                                null &&
                        currentScrollState ==
                            OnScrollListener.SCROLL_STATE_IDLE) {
                    updatePaging(true);
                }
                else if (currentFirstItem == 0 &&
                        ApplicationEx.epAdapter.getOrderPosition(
                                currentFirstItem) < 0 && currentScrollState ==
                                OnScrollListener.SCROLL_STATE_IDLE) {
                    updatePaging(false);
                }
            }
        });
    }
    
    @Override
    public void onPause() {
        // TODO Set app episode position after scroll stops, not just on pause
        if (ApplicationEx.epAdapter.getCount() > 0)
            ApplicationEx.dbHelper.setAppEpisodePosition(
                    (int) ApplicationEx.epAdapter.getItemId(
                            getListView().getFirstVisiblePosition()));
        getActivity().unregisterReceiver(refreshReceiver);
        getActivity().unregisterReceiver(durationReceiver);
        getActivity().unregisterReceiver(playStateReceiver);
        ApplicationEx.epAdapter.clearContexts();
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
                ApplicationEx.epAdapter.downloadSelected();
                break;
            case R.id.delete:
                ApplicationEx.epAdapter.deleteSelected();
                break;
            case R.id.read:
                ApplicationEx.epAdapter.markSelectedRead(true);
                break;
            case R.id.unread:
                ApplicationEx.epAdapter.markSelectedRead(false);
                break;
            case R.id.lock:
                break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            ApplicationEx.epAdapter.resetViews();
            setChoiceMode(ListView.CHOICE_MODE_NONE);
        }
        
    }
    
    private void refreshEpisodes() {
        ApplicationEx.dbHelper.setAppEpisodePosition(
                (int) ApplicationEx.epAdapter.getItemId(
                        getListView().getFirstVisiblePosition()));
        Log.i(Constants.LOG_TAG, "getFirstVisiblePosition: " + getListView().getFirstVisiblePosition());
        ApplicationEx.epAdapter.refreshEpisodes(ApplicationEx.epRead,
                ApplicationEx.epDownloaded, ApplicationEx.epSortType,
                ApplicationEx.archive, this, false);
        getListView().setSelection(ApplicationEx.epAdapter.getItemPosition(
                ApplicationEx.dbHelper.getAppEpisodePosition()));
    }
    
    boolean down;
    int selection;
    int listSize;
    
    private void updatePaging(boolean down) {
        ApplicationEx.dbHelper.setAppEpisodePosition(-1);
        this.down = down;
        if (down)
            selection = (int) ApplicationEx.epAdapter.getItemId(39);
        else
            selection = (int) ApplicationEx.epAdapter.getItemId(1);
        listSize = ApplicationEx.epAdapter.getCount();
        ApplicationEx.epAdapter.refreshEpisodes(ApplicationEx.epRead,
                ApplicationEx.epDownloaded, ApplicationEx.epSortType,
                ApplicationEx.archive, this, down);
    }
    
    public void finishPaging(boolean fresh) {
        if (fresh)
            try {
                getListView().setSelection(
                        ApplicationEx.epAdapter.getItemPosition(
                            ApplicationEx.dbHelper.getAppEpisodePosition()));
            } catch (IllegalStateException e) {}
        else {
            if (down) {
                if (listSize == Constants.PAGE_SIZE*2 + 1) {
                    Log.w(Constants.LOG_TAG, "selection: " + selection);
                    Log.w(Constants.LOG_TAG, "getItemPosition: " + ApplicationEx.epAdapter.getItemPosition(selection));
                    Log.w(Constants.LOG_TAG, "listSize: " + getListView().getCount());
                    if (listSize == ApplicationEx.epAdapter.getOrderCount())
                        getListView().setSelection(
                                ApplicationEx.epAdapter.getItemPosition(
                                        selection));
                    else
                        getListView().setSelectionFromTop(
                            ApplicationEx.epAdapter.getItemPosition(selection)
                                    + 2,
                            getListView().getHeight()-(
                                    ApplicationEx.epAdapter.getRowHeight()*5));
                }
                else
                    getListView().setSelectionFromTop(
                        ApplicationEx.epAdapter.getItemPosition(selection) + 3,
                        getListView().getHeight()-(
                                ApplicationEx.epAdapter.getRowHeight()*5));
            }
            else if (!down) {
                if (listSize == Constants.PAGE_SIZE*2 + 1)
                    getListView().setSelection(
                        ApplicationEx.epAdapter.getItemPosition(selection)-1);
                else
                    getListView().setSelection(
                        ApplicationEx.epAdapter.getItemPosition(selection)-1);
            }
        }
    }
    
    @Override
    public void updateRead() {
        ApplicationEx.epRead = ApplicationEx.epRead ^ Constants.READ;
        if ((ApplicationEx.epRead & Constants.READ) == Constants.READ)
            readIndicator.setColorFilter(null);
        else
            readIndicator.setColorFilter(Color.DKGRAY,
                    PorterDuff.Mode.SRC_ATOP);
        ApplicationEx.dbHelper.setCurrentEpisodeRead(ApplicationEx.epRead);
        refreshEpisodes();
    }
    
    @Override
    public void updateDownloaded() {
        ApplicationEx.epDownloaded =
                ApplicationEx.epDownloaded ^ Constants.TO_DOWNLOAD;
        if ((ApplicationEx.epDownloaded & Constants.DOWNLOADED) ==
                Constants.DOWNLOADED)
            downloadIndicator.setColorFilter(null);
        else
            downloadIndicator.setColorFilter(Color.DKGRAY,
                    PorterDuff.Mode.SRC_ATOP);
        if ((ApplicationEx.epDownloaded & Constants.TO_DOWNLOAD) ==
                Constants.TO_DOWNLOAD)
            cloudIndicator.setColorFilter(null);
        else
            cloudIndicator.setColorFilter(Color.DKGRAY,
                    PorterDuff.Mode.SRC_ATOP);
        ApplicationEx.dbHelper.setCurrentEpisodeDownloaded(
                ApplicationEx.epDownloaded);
        refreshEpisodes();
    }
    
    @Override
    public void updateSort() {
        switch(ApplicationEx.epSortType) {
        case Constants.PLAYLIST_SORT_TYPE_ASC:
            ApplicationEx.epSortType = Constants.PLAYLIST_SORT_TYPE_DESC;
            sortIndicator.setImageResource(R.drawable.sort_up);
            break;
        case Constants.PLAYLIST_SORT_TYPE_DESC:
            ApplicationEx.epSortType = Constants.PLAYLIST_SORT_TYPE_ASC;
            sortIndicator.setImageResource(R.drawable.sort_down);
            break;
        default:
            ApplicationEx.epSortType = Constants.PLAYLIST_SORT_TYPE_ASC;
            sortIndicator.setImageResource(R.drawable.sort_down);
            break;
        }
        ApplicationEx.dbHelper.setCurrentEpisodeSortType(
                ApplicationEx.epSortType);
        refreshEpisodes();
    }
    
    @Override
    public void updateGroup() {}
    
    private class DurationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_REFRESH_DURATION)) {
                durationText.setText(Util.getTimeString(
                        ApplicationEx.epAdapter.getTotalDuration()));
                totalText.setText(TextUtils.concat(Integer.toString(
                        ApplicationEx.epAdapter.getDownloadedCount()),
                        Constants.OF, Integer.toString(
                                ApplicationEx.epAdapter.getOrderCount())));
            }
        }
    }
    
    class RefreshReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_REFRESH_EPISODES) ||
                    intent.getAction().equals(Constants.ACTION_IMAGE_CHANGE)) {
                refreshEpisodes();
            }
        }
    }
    
    class PlayStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_START_PLAYBACK)) {
                ApplicationEx.epAdapter.startLeftPlaying();
                ApplicationEx.epAdapter.startRightPlaying();
            }
            else if (intent.getAction().equals(
                    Constants.ACTION_PAUSE_PLAYBACK)) {
                ApplicationEx.epAdapter.stopLeftPlaying();
                ApplicationEx.epAdapter.stopRightPlaying();
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
    public void showDetailsDialog(String[] values) {
        DialogFragment detailsDialog = FragmentEpisodeDetailsDialog.newInstance(
                values);
        showDialog(detailsDialog, Constants.DIALOG_DETAILS);
    }

    @Override
    public void setChoiceMode(int choiceMode) {
        getListView().setChoiceMode(choiceMode);
        if (choiceMode == ListView.CHOICE_MODE_MULTIPLE) {
            ((ActivityBase) getActivity()).startActionMode(
                    multiActionMode);
            ApplicationEx.epAdapter.setMultiSelect(true);
        }
        else if (choiceMode == ListView.CHOICE_MODE_NONE)
            ApplicationEx.epAdapter.setMultiSelect(false);
    }

    @Override
    public int getChoiceMode() {
        return getListView().getChoiceMode();
    }
    
}
