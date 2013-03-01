package com.jeffthefate.pod_dev.fragment;

import java.util.ArrayList;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;

import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.Constants;
import com.jeffthefate.pod_dev.DownloadAdapter;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.Util;
import com.jeffthefate.pod_dev.activity.ActivityDownloads;
import com.jeffthefate.pod_dev.service.DownloadService;
import com.jeffthefate.pod_dev.service.DownloadService.DownloadBinder;

public class FragmentDownloads extends ListFragment {
    
    private DownloadAdapter mAdapter;
    
    ArrayList<String> epList;
    private int currEpId;
    
    private TextView emptyText;
    
    public static final int SET_LIST = 0;
    public static final int SET_PROGRESS = 1;
    
    public FragmentDownloads() {
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
        currEpId = ((ActivityDownloads)getActivity()).getCurrEpId();
        if (Util.isMyServiceRunning(DownloadService.class.getName()))
            getActivity().bindService(new Intent(ApplicationEx.getApp(), 
                DownloadService.class), mConnection, 0);
        else if (((ActivityDownloads)getActivity()).getFailed())
            epList = ((ActivityDownloads)getActivity()).getEpList();
        else
            epList = new ArrayList<String>();
        if (epList != null) {
            mAdapter = new DownloadAdapter(getActivity(), 
                    Util.getDownloading(epList), emptyText, currEpId);
            setListAdapter(mAdapter);
        }
        getListView().setSelectionFromTop(
                ApplicationEx.dbHelper.getAppDownloadPosition(), 0);
        NotificationManager nManager = (NotificationManager) getActivity()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.cancel(4444);
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
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        switch(item.getItemId()) {
        case R.id.CancelEpisode:
            dService.cancelDownload();
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
        intent.putExtra("urls", new String[]{epUrl});
        if (Util.isMyServiceRunning(DownloadService.class.getName())) {
            getActivity().sendBroadcast(intent);
        }
        else {
            intent = new Intent(ApplicationEx.getApp(), DownloadService.class);
            intent.putExtra("urls", new String[]{epUrl});
            getActivity().startService(intent);
        }
    }
    
    private void removeEpisode(String epUrl) {
        // Send broadcast
        Intent intent = new Intent(Constants.ACTION_REMOVE_DOWNLOAD);
        intent.putExtra("urls", new String[]{epUrl});
        if (Util.isMyServiceRunning(DownloadService.class.getName())) {
            getActivity().sendBroadcast(intent);
        }
    }
    
    class DownloadHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case SET_LIST:
                epList = dService.getCurrentList();
                if (mAdapter != null) {
                    mAdapter.updateData(Util.getDownloading(epList),
                            dService.getCurrentId());
                }
                else {
                    mAdapter = new DownloadAdapter(getActivity(), 
                            Util.getDownloading(epList), emptyText,
                            dService.getCurrentId());
                    setListAdapter(mAdapter);
                    mAdapter.notifyDataSetChanged();
                }
                mAdapter.setCurrentProgress(0);
                break;
            case SET_PROGRESS:
                mAdapter.setCurrentProgress(msg.getData().getInt("progress"));
            default:
                super.handleMessage(msg);
                break;
            }
        }
    }
    
    DownloadService dService;
    boolean mBound;
    
    final Messenger mMessenger = new Messenger(new DownloadHandler());
    
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DownloadBinder binder = (DownloadBinder) service;
            dService = binder.getService();
            dService.addClient(mMessenger);
            mBound = true;
            if (mAdapter != null)
                mAdapter.updateData(Util.getDownloading(
                        dService.getCurrentList()), dService.getCurrentId());
            else {
                mAdapter = new DownloadAdapter(getActivity(), 
                        Util.getDownloading(dService.getCurrentList()), 
                        emptyText, dService.getCurrentId());
                setListAdapter(mAdapter);
                mAdapter.notifyDataSetChanged();
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };
    
    @Override
    public void onPause() {
        ((ActivityDownloads)getActivity()).setEpList(epList);
        ApplicationEx.dbHelper.setAppDownloadPosition(
                getListView().getFirstVisiblePosition());
        if (mBound) {
            dService.removeClient(mMessenger);
            getActivity().unbindService(mConnection);
            mBound = false;
        }
        super.onPause();
    }
    
}
