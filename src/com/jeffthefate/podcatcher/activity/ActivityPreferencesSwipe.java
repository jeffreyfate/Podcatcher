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
import com.jeffthefate.podcatcher.Util;
import com.parse.Parse;

public class ActivityPreferencesSwipe extends SherlockPreferenceActivity
        implements OnSharedPreferenceChangeListener {
    
    private ListPreference playPref;
    private ListPreference backPref;
    private ListPreference nextPref;
    
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSherlock().getActionBar().setDisplayHomeAsUpEnabled(false);
        addPreferencesFromResource(R.xml.settings_swipe);
        playPref = (ListPreference) findPreference(
                getString(R.string.playpauseswipe_key));
        backPref = (ListPreference) findPreference(
                getString(R.string.backwardswipe_key));
        nextPref = (ListPreference) findPreference(
                getString(R.string.forwardswipe_key));
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
        if (key.equals(getString(R.string.playpauseswipe_key)) ||
                key.equals(getString(R.string.backwardswipe_key)) ||
                key.equals(getString(R.string.forwardswipe_key))) {
            ArrayList<Integer> prefList = new ArrayList<Integer>();
            prefList.add(1);
            prefList.add(2);
            prefList.add(3);
            ArrayList<Integer> newList = new ArrayList<Integer>();
            int playValue = Integer.parseInt(Util.readStringPreference(
                    R.string.playpauseswipe_key, Constants.ONE));
            int backValue = Integer.parseInt(Util.readStringPreference(
                    R.string.backwardswipe_key, Constants.ONE));
            int nextValue = Integer.parseInt(Util.readStringPreference(
                    R.string.forwardswipe_key, Constants.ONE));
            newList.add(playValue);
            newList.add(backValue);
            newList.add(nextValue);
            int newValue = Integer.parseInt(
                    sharedPrefs.getString(key, Constants.ONE));
            prefList.removeAll(newList);
            if (prefList.isEmpty())
                return;
            if (newValue == playValue &&
                    !key.equals(getString(R.string.playpauseswipe_key))) {
                sharedPrefs.edit().putString(
                        getString(R.string.playpauseswipe_key),
                        String.valueOf(prefList.get(0))).commit();
                playPref.setValue(String.valueOf(prefList.get(0)));
            }
            else if (newValue == backValue &&
                    !key.equals(getString(R.string.backwardswipe_key))) {
                sharedPrefs.edit().putString(
                        getString(R.string.backwardswipe_key),
                        String.valueOf(prefList.get(0))).commit();
                backPref.setValue(String.valueOf(prefList.get(0)));
            }
            else if (newValue == nextValue &&
                    !key.equals(getString(R.string.forwardswipe_key))) {
                sharedPrefs.edit().putString(
                        getString(R.string.forwardswipe_key),
                        String.valueOf(prefList.get(0))).commit();
                nextPref.setValue(String.valueOf(prefList.get(0)));
            }
        }
        else if (key.equals(getString(R.string.swipe_key))) {
            if (sharedPrefs.getBoolean(key, false)) {
                if (ApplicationEx.proxReader == null)
                    ApplicationEx.setupSwipeRemote();
                else
                    ApplicationEx.proxReader.setEnableProximity(true);
            }
            else {
                if (ApplicationEx.proxReader != null)
                    ApplicationEx.proxReader.setEnableProximity(false);
            }
        }
    }
    
}