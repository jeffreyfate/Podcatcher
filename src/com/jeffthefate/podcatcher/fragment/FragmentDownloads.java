package com.jeffthefate.podcatcher.fragment;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.DownloadAdapter;
import com.jeffthefate.podcatcher.DownloadAdapter.AdapterChange;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.Util;
import com.jeffthefate.podcatcher.activity.ActivityBase;
import com.jeffthefate.podcatcher.activity.ActivityDownloads;
import com.jeffthefate.podcatcher.service.DownloadService;

public class FragmentDownloads extends FragmentBase implements AdapterChange {
    
    private DownloadAdapter mAdapter;
    
    private TextView emptyText;
    
    public static final int SET_LIST = 0;
    public static final int SET_PROGRESS = 1;
    
    private DownloadReceiver downloadReceiver;
    private ProgressReceiver progressReceiver;
    
    private MultiActionMode multiActionMode;
    
    public FragmentDownloads() {
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        downloadReceiver = new DownloadReceiver();
        progressReceiver = new ProgressReceiver();
        multiActionMode = new MultiActionMode();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.simple_list, container, false);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        registerForContextMenu(getListView());
        emptyText = (TextView) getView().findViewById(android.R.id.empty);
        if (mAdapter != null)
            mAdapter.updateData(Util.getDownloading(
                    ApplicationEx.downloadList), ApplicationEx.currEpId, false);
        else {
            mAdapter = new DownloadAdapter(getActivity(), 
                    Util.getDownloading(ApplicationEx.downloadList), 
                    emptyText, ApplicationEx.currEpId);
            setListAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
        }
        getListView().setSelectionFromTop(
                ApplicationEx.dbHelper.getAppDownloadPosition(), 0);
        NotificationManager nManager = (NotificationManager) getActivity()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.cancel(4444);
        ApplicationEx.getApp().registerReceiver(downloadReceiver,
                new IntentFilter(Constants.ACTION_UPDATE_DOWNLOADS));
        ApplicationEx.getApp().registerReceiver(progressReceiver,
                new IntentFilter(Constants.ACTION_UPDATE_PROGRESS));
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        DownloadAdapter downloadAdapter = 
                (DownloadAdapter) getListView().getAdapter();
        if (!downloadAdapter.getContextDownloading()) {
            if (downloadAdapter.getContextFailed())
                inflater.inflate(R.menu.download_context_failed, menu);
            else
                inflater.inflate(R.menu.download_context_queued, menu);
        }
        else {
            inflater.inflate(R.menu.download_context_downloading, menu);
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.id.CancelEpisode:
            ApplicationEx.getApp().sendBroadcast(
                    new Intent(Constants.ACTION_CANCEL_DOWNLOAD));
            return true;
        case R.id.RetryEpisode:
            downloadEpisode(ApplicationEx.dbHelper.getEpisodeUrl(
                    mAdapter.getContextId()));
            return true;
        case R.id.RemoveEpisode:
            removeEpisode(ApplicationEx.dbHelper.getEpisodeUrl(
                    mAdapter.getContextId()));
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }
    
    private void downloadEpisode(String epUrl) {
        // Send broadcast
        Intent intent = new Intent(Constants.ACTION_START_DOWNLOAD);
        intent.putExtra(Constants.URLS, new String[]{epUrl});
        if (Util.isMyServiceRunning(DownloadService.class.getName())) {
            getActivity().sendBroadcast(intent);
        }
        else {
            intent = new Intent(ApplicationEx.getApp(), DownloadService.class);
            intent.putExtra(Constants.URLS, new String[]{epUrl});
            getActivity().startService(intent);
        }
    }
    
    private void removeEpisode(String epUrl) {
        // Send broadcast to remove this episode from the queue
        Intent intent = new Intent(Constants.ACTION_REMOVE_DOWNLOAD);
        intent.putExtra(Constants.URLS, new String[]{epUrl});
        if (Util.isMyServiceRunning(DownloadService.class.getName())) {
            getActivity().sendBroadcast(intent);
        }
    }
    
    private class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mAdapter != null)
                mAdapter.updateData(Util.getDownloading(
                        ApplicationEx.downloadList), ApplicationEx.currEpId,
                        true);
            else {
                mAdapter = new DownloadAdapter(getActivity(), 
                        Util.getDownloading(ApplicationEx.downloadList), 
                        emptyText, ApplicationEx.currEpId);
                setListAdapter(mAdapter);
                mAdapter.notifyDataSetChanged();
            }
        }
    }
    
    private class ProgressReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(Constants.PROGRESS))
                mAdapter.setCurrentProgress(
                        intent.getIntExtra(Constants.PROGRESS, -1));
        }
    }
    
    @Override
    public void onPause() {
        ApplicationEx.dbHelper.setAppDownloadPosition(
                getListView().getFirstVisiblePosition());
        ApplicationEx.getApp().unregisterReceiver(downloadReceiver);
        ApplicationEx.getApp().unregisterReceiver(progressReceiver);
        super.onPause();
    }
    
    @Override
    public void showDetailsDialog(String[] values) {
        DialogFragment detailsDialog = FragmentEpisodeDetailsDialog.newInstance(
                values);
        try {
            detailsDialog.show(getActivity().getSupportFragmentManager(),
                    Constants.DIALOG_DETAILS);
        } catch (NullPointerException e) {}
    }

    @Override
    public void setChoiceMode(int choiceMode) {
        getListView().setChoiceMode(choiceMode);
        if (choiceMode == ListView.CHOICE_MODE_MULTIPLE) {
            ((ActivityBase) getActivity()).startActionMode(
                    multiActionMode);
            mAdapter.setMultiSelect(true);
        }
        else if (choiceMode == ListView.CHOICE_MODE_NONE)
            mAdapter.setMultiSelect(false);
    }

    @Override
    public int getChoiceMode() {
        return getListView().getChoiceMode();
    }
    
    private final class MultiActionMode implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            com.actionbarsherlock.view.MenuInflater inflater =
                    ((ActivityDownloads)getActivity()).getSupportMenuInflater();
            inflater.inflate(R.menu.multi_download_menu, menu);
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
            case R.id.delete:
                mAdapter.removeSelected();
                break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mAdapter.resetViews();
            setChoiceMode(ListView.CHOICE_MODE_NONE);
        }
        
    }

    @Override
    protected void updateRead() {}

    @Override
    protected void updateDownloaded() {}

    @Override
    protected void updateSort() {}

    @Override
    protected void updateGroup() {}
    
}
