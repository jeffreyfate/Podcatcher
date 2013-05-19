package com.jeffthefate.podcatcher.activity;

import java.util.ArrayList;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.R;
import com.parse.Parse;

public class ActivityPreferencesRemote extends SherlockPreferenceActivity
        implements OnSharedPreferenceChangeListener {
    
    private ListPreference playPref;
    private ListPreference backPref;
    private ListPreference nextPref;
    
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSherlock().getActionBar().setDisplayHomeAsUpEnabled(false);
        addPreferencesFromResource(R.xml.settings_remote);
        playPref = (ListPreference) findPreference(
                getString(R.string.playpausepress_key));
        backPref = (ListPreference) findPreference(
                getString(R.string.backwardpress_key));
        nextPref = (ListPreference) findPreference(
                getString(R.string.forwardpress_key));
        Parse.initialize(this, Constants.PARSE_ID, Constants.PARSE_KEY);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(ApplicationEx.getApp())
                .registerOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    public void onPause() {
        PreferenceManager.getDefaultSharedPreferences(ApplicationEx.getApp())
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, 
            String key) {
        if (key.equals(getString(R.string.playpausepress_key)) ||
                key.equals(getString(R.string.backwardpress_key)) ||
                key.equals(getString(R.string.forwardpress_key))) {
            ArrayList<Integer> prefList = new ArrayList<Integer>();
            prefList.add(1);
            prefList.add(2);
            prefList.add(3);
            ArrayList<Integer> newList = new ArrayList<Integer>();
            int playValue = Integer.parseInt(sharedPrefs.getString(
                    getString(R.string.playpausepress_key), Constants.ONE));
            int backValue = Integer.parseInt(sharedPrefs.getString(
                    getString(R.string.backwardpress_key), Constants.ONE));
            int nextValue = Integer.parseInt(sharedPrefs.getString(
                    getString(R.string.forwardpress_key), Constants.ONE));
            newList.add(playValue);
            newList.add(backValue);
            newList.add(nextValue);
            int newValue = Integer.parseInt(
                    sharedPrefs.getString(key, Constants.ONE));
            prefList.removeAll(newList);
            if (prefList.isEmpty())
                return;
            if (newValue == playValue &&
                    !key.equals(getString(R.string.playpausepress_key))) {
                sharedPrefs.edit().putString(
                        getString(R.string.playpausepress_key),
                        String.valueOf(prefList.get(0))).commit();
                playPref.setValue(String.valueOf(prefList.get(0)));
            }
            else if (newValue == backValue &&
                    !key.equals(getString(R.string.backwardpress_key))) {
                sharedPrefs.edit().putString(
                        getString(R.string.backwardpress_key),
                        String.valueOf(prefList.get(0))).commit();
                backPref.setValue(String.valueOf(prefList.get(0)));
            }
            else if (newValue == nextValue &&
                    !key.equals(getString(R.string.forwardpress_key))) {
                sharedPrefs.edit().putString(
                        getString(R.string.forwardpress_key),
                        String.valueOf(prefList.get(0))).commit();
                nextPref.setValue(String.valueOf(prefList.get(0)));
            }
        }
    }
    
}