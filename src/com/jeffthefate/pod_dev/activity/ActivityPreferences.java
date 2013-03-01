package com.jeffthefate.pod_dev.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.Util;
import com.jeffthefate.pod_dev.Util.Magics;
import com.jeffthefate.pod_dev.VersionedActionBar;

public class ActivityPreferences extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VersionedActionBar.newInstance().create(this).setDisplayHomeAsUp();
        addPreferencesFromResource(R.xml.settings);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(ApplicationEx.getApp())
                .registerOnSharedPreferenceChangeListener(this);
        Preference downloadPref = findPreference(
                getString(R.string.downloads_key));
        downloadPref.setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference pref) {
                Intent intent = new Intent(ApplicationEx.getApp(), 
                        ActivityDownloads.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
        });
    }
    
    @Override
    public void onPause() {
        PreferenceManager.getDefaultSharedPreferences(ApplicationEx.getApp())
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {    
        switch (item.getItemId()) 
        {        
        case android.R.id.home:            
            Intent mainIntent = new Intent(this, ActivitySplash.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(mainIntent);
            return true;        
        default:            
            return super.onOptionsItemSelected(item);    
        }
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, 
            String key) {
        if (key.equals(getString(R.string.update_key))) {
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
        }
        else if (key.equals(getString(R.string.interval_key))) {
            Util.setRepeatingAlarm(Integer.parseInt(Util.readStringPreference(
                    R.string.interval_key, "60")), false);
        }
    }
    
}