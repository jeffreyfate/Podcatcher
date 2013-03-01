package com.jeffthefate.pod_dev.activity;

import android.accounts.AccountManager;
import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.Constants;
import com.jeffthefate.pod_dev.R;

public class ActivitySetup extends PreferenceActivity {
    
    CheckBoxPreference syncPref;
    Preference finishPref;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_firststart);
        syncPref = (CheckBoxPreference) findPreference(
                getString(R.string.sync_key));
        if (AccountManager.get(ApplicationEx.getApp())
                .getAccountsByType(Constants.ACCOUNT_TYPE).length < 1) {
            syncPref.setChecked(false);
            syncPref.setEnabled(false);
        }
        finishPref = findPreference(getString(R.string.finish_key));
        finishPref.setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference pref) {
                setResult(Activity.RESULT_FIRST_USER);
                finish();
                return true;
            }
        });
    }
    
}