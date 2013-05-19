package com.jeffthefate.podcatcher.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.Util;
import com.jeffthefate.podcatcher.Util.Magics;
import com.parse.Parse;

public class ActivityPreferences extends SherlockPreferenceActivity implements
        OnSharedPreferenceChangeListener {
    
    private CheckBoxPreference wifiPref;
    private CheckBoxPreference fourGPref;
    
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSherlock().getActionBar().setDisplayHomeAsUpEnabled(true);
        if (!((SensorManager) ApplicationEx.getApp().getSystemService(
                Context.SENSOR_SERVICE))
                    .getSensorList(Sensor.TYPE_ACCELEROMETER).isEmpty())
            addPreferencesFromResource(R.xml.settings);
        else
            addPreferencesFromResource(R.xml.settings_notap);
        Parse.initialize(this, Constants.PARSE_ID, Constants.PARSE_KEY);
    }
    
    @SuppressWarnings("deprecation")
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
        wifiPref = (CheckBoxPreference) findPreference(
                getString(R.string.wifi_key));
        fourGPref = (CheckBoxPreference) findPreference(
                getString(R.string.four_g_key));
        Preference remotePref = findPreference(
                getString(R.string.remote_settings));
        remotePref.setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference pref) {
                Intent intent = new Intent(ApplicationEx.getApp(), 
                        ActivityPreferencesRemote.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
        });
        Preference tapPref = findPreference(getString(R.string.tap_settings));
        tapPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(ApplicationEx.getApp(), 
                        ActivityPreferencesTap.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
        });
        Preference swipePref = findPreference(
                getString(R.string.swipe_settings));
        swipePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(ApplicationEx.getApp(), 
                        ActivityPreferencesSwipe.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
        });
        Preference tweakPref = findPreference(
                getString(R.string.tweak_settings));
        tweakPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(ApplicationEx.getApp(), 
                        ActivityPreferencesTweak.class);
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
            Intent mainIntent = new Intent(this, ActivityMain.class);
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
            if (Integer.parseInt(Util.readStringPreference(R.string.update_key,
                    Constants.ZERO)) == 1) {
                Util.setRepeatingAlarm(Integer.parseInt(
                        Util.readStringPreference(R.string.interval_key,
                                Constants.SIXTY)), false);
            }
            else if (Integer.parseInt(Util.readStringPreference(
                    R.string.update_key, Constants.ZERO)) == 2) {
                Magics currMagics = 
                    ApplicationEx.dbHelper.getNextMagicInterval();
                Util.setMagicAlarm(currMagics.getTime(), currMagics.getFeeds());
            }
        }
        else if (key.equals(getString(R.string.interval_key))) {
            Util.setRepeatingAlarm(Integer.parseInt(Util.readStringPreference(
                    R.string.interval_key, Constants.SIXTY)), false);
        }
        else if (key.equals(getString(R.string.sync_key))) {
            if (!sharedPrefs.getBoolean(key, false))
                Util.writeBufferToFile(Boolean.toString(false).getBytes(),
                        Constants.GOOGLE_FILENAME);
        }
        else if (key.equals(getString(R.string.archive_key))) {
            ApplicationEx.archive = sharedPrefs.getBoolean(key, false) ? 1 : 0;
        }
        else if (key.equals(getString(R.string.night_key))) {
            if (sharedPrefs.getString(key, Constants.ZERO).equals(
                    Constants.ZERO)) {
                Util.cancelSunriseAlarm();
                Util.cancelSunsetAlarm();
            }
            else {
                Util.setSunriseAlarm();
                Util.setSunsetAlarm();
            }
        }
        else if (key.equals(getString(R.string.wifi_key))) {
            if (sharedPrefs.getBoolean(key, false))
                fourGPref.setChecked(false);
        }
        else if (key.equals(getString(R.string.four_g_key))) {
            if (sharedPrefs.getBoolean(key, false))
                wifiPref.setChecked(false);
        }
    }
    
}