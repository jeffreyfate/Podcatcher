package com.jeffthefate.pod_dev.fragment;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.service.PlaybackService;
import com.jeffthefate.pod_dev.service.PlaybackService.PlaybackBinder;

public abstract class FragmentBase extends ListFragment {
    
    protected int read;
    protected int downloaded;
    protected int sortType;
    protected int archive;
    
    protected TextView emptyText;
    protected ProgressBar emptyProgress;
    
    protected PlaybackService mService;
    protected boolean mBound = false;
    
    protected ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackBinder binder = (PlaybackBinder) service;
            mService = binder.getService();
            mBound = true;
            boolean isPlaying = mService.isPlaying();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };
    
    public FragmentBase() {
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        getActivity().bindService(new Intent(
                getActivity(), PlaybackService.class), mConnection, 0);
    }
    
    @Override
    public void onPause() {
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
        super.onPause();
    }
    
    protected void updateList(int buttonId) {
        switch(buttonId) {
        case R.id.ToggleRead:
            updateRead();
            break;
        case R.id.ToggleDownloaded:
            updateDownloaded();
            break;
        case R.id.SortOrder:
            updateSort();
            break;
        case R.id.SortBy:
            updateGroup();
            break;
        }
    }
    
    protected abstract void updateRead();
    
    protected abstract void updateDownloaded();
    
    protected abstract void updateSort();
    
    protected abstract void updateGroup();
    
}
