package com.jeffthefate.podcatcher.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jeffthefate.podcatcher.R;

public abstract class FragmentBase extends ListFragment {
    
    protected TextView emptyText;
    protected ProgressBar emptyProgress;
    protected FragmentManager fMan;
    
    public FragmentBase() {
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        fMan = ((FragmentActivity)activity).getSupportFragmentManager();
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
    
    protected void showDialog(DialogFragment fragment, String label) {
        try {
            fMan.beginTransaction().add(fragment, label)
                    .commitAllowingStateLoss();
            fMan.executePendingTransactions();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
    
    protected abstract void updateRead();
    
    protected abstract void updateDownloaded();
    
    protected abstract void updateSort();
    
    protected abstract void updateGroup();
    
    public abstract void setChoiceMode(int choiceMode);
    
    public abstract int getChoiceMode();
    
}
