package com.jeffthefate.podcatcher.activity;

import java.util.ArrayList;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.jeffthefate.podcatcher.AccelerometerReader.Direction;
import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.Util;
import com.parse.Parse;

public class ActivityPreferencesTap extends SherlockPreferenceActivity
        implements OnSharedPreferenceChangeListener {
    
    private ListPreference playPref;
    private ListPreference backPref;
    private ListPreference nextPref;
    
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSherlock().getActionBar().setDisplayHomeAsUpEnabled(false);
        addPreferencesFromResource(R.xml.settings_tap);
        playPref = (ListPreference) findPreference(
                getString(R.string.playpausetap_key));
        backPref = (ListPreference) findPreference(
                getString(R.string.backwardtap_key));
        nextPref = (ListPreference) findPreference(
                getString(R.string.forwardtap_key));
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
        if (key.equals(getString(R.string.playpausetap_key)) ||
                key.equals(getString(R.string.backwardtap_key)) ||
                key.equals(getString(R.string.forwardtap_key))) {
            ArrayList<String> prefList = new ArrayList<String>();
            prefList.add(Direction.Left.toString());
            prefList.add(Direction.Right.toString());
            prefList.add(Direction.Top.toString());
            prefList.add(Direction.Bottom.toString());
            prefList.add(Direction.Front.toString());
            prefList.add(Direction.Back.toString());
            ArrayList<String> newList = new ArrayList<String>();
            String playValue = Util.readStringPreference(
                    R.string.playpausetap_key, Constants.NONE);
            String backValue = Util.readStringPreference(
                    R.string.backwardtap_key, Constants.NONE);
            String nextValue = Util.readStringPreference(
                    R.string.forwardtap_key, Constants.NONE);
            newList.add(playValue);
            newList.add(backValue);
            newList.add(nextValue);
            String newValue = sharedPrefs.getString(key, Constants.NONE);
            prefList.removeAll(newList);
            if (prefList.size() == 3)
                return;
            if (newValue.equals(playValue) &&
                    !key.equals(getString(R.string.playpausetap_key))) {
                sharedPrefs.edit().putString(
                        getString(R.string.playpausepress_key),
                        String.valueOf(prefList.get(0))).commit();
                playPref.setValue(String.valueOf(prefList.get(0)));
            }
            else if (newValue.equals(backValue) &&
                    !key.equals(getString(R.string.backwardtap_key))) {
                sharedPrefs.edit().putString(
                        getString(R.string.backwardtap_key),
                        String.valueOf(prefList.get(0))).commit();
                backPref.setValue(String.valueOf(prefList.get(0)));
            }
            else if (newValue.equals(nextValue) &&
                    !key.equals(getString(R.string.forwardtap_key))) {
                sharedPrefs.edit().putString(
                        getString(R.string.forwardtap_key),
                        String.valueOf(prefList.get(0))).commit();
                nextPref.setValue(String.valueOf(prefList.get(0)));
            }
        }
        else if (key.equals(getString(R.string.tap_key))) {
            if (sharedPrefs.getBoolean(key, false)) {
                if (ApplicationEx.accelReader == null)
                    ApplicationEx.setupTapRemote();
                else
                    ApplicationEx.accelReader.setEnableAccelerometer(true);
                /*
                if (!ApplicationEx.accelReader.isCalibrated()) {
                    AlertDialog.Builder builder =
                            new AlertDialog.Builder(this);
                    builder.setTitle(R.string.CalibrateTitle);
                    builder.setMessage(R.string.CalibrateSummary);
                    builder.setNeutralButton(R.string.ok,
                            new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                int which) {
                            AlertDialog aDialog = (AlertDialog) dialog;
                            ApplicationEx.accelReader.calibrate(aDialog);
                            aDialog.getButton(which).setEnabled(false);
                            aDialog.getButton(which).setText("Calibrating");
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                */
            }
            else {
                if (ApplicationEx.accelReader != null)
                    ApplicationEx.accelReader.setEnableAccelerometer(false);
            }
        }
        else if (key.equals(getString(R.string.tapsensitivity_key))) {
            ApplicationEx.accelReader.setThreshold(Float.parseFloat(
                    sharedPrefs.getString(key, Constants.FOUR)));
        }
    }
    
}