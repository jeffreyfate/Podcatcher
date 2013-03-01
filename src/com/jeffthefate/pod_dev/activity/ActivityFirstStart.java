package com.jeffthefate.pod_dev.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.FirstStartBase.OnActivityEndListener;
import com.jeffthefate.pod_dev.FirstStartBase.OnFragmentChangeListener;
import com.jeffthefate.pod_dev.Constants;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.Util;
import com.jeffthefate.pod_dev.Util.Magics;
import com.jeffthefate.pod_dev.fragment.FragmentAccount;
import com.jeffthefate.pod_dev.fragment.FragmentFirstStart;

public class ActivityFirstStart extends FragmentActivity implements
        OnFragmentChangeListener, OnActivityEndListener {
    
    FragmentManager fMan;
    
    public OnFragmentChangeListener mFragmentCallback = this;
    public OnActivityEndListener mActivityCallback = this;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fMan = getSupportFragmentManager();
        fMan.beginTransaction().replace(android.R.id.content, 
                new FragmentFirstStart(), "fFirstStart").commit();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        Log.i(Constants.LOG_TAG, "requestCode: " + requestCode);
        Log.i(Constants.LOG_TAG, "resultCode: " + resultCode);
        if (resultCode == Activity.RESULT_FIRST_USER) {
            if (Util.readBooleanPreference(R.string.sync_key, false))
                onFragmentChange(new FragmentAccount(),"fAccount");
            else
                onActivityEnd();
        }
    }

    @Override
    public void onFragmentChange(Fragment newFragment, String tag) {
        fMan.beginTransaction().replace(android.R.id.content, newFragment, tag)
                .commit();
    }
    
    @Override
    public void onActivityEnd() {
        Util.cancelRepeatingAlarm();
        if (Integer.parseInt(
                Util.readStringPreference(R.string.update_key, "0")) == 1) {
            Util.setRepeatingAlarm(Integer.parseInt(
                    Util.readStringPreference(R.string.interval_key, "60")),
                    false);
        }
        else if (Integer.parseInt(
                Util.readStringPreference(R.string.update_key, "0")) == 2) {
            Magics currMagics = 
                ApplicationEx.dbHelper.getNextMagicInterval(false);
            Util.setMagicAlarm(currMagics.getTime(), currMagics.getFeeds());
        }
        setResult(Activity.RESULT_FIRST_USER, null);
        finish();
    }
    
}
