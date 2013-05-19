package com.jeffthefate.podcatcher.activity;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.R;
import com.parse.Parse;

public class ActivityPreferencesTweak extends SherlockPreferenceActivity {
    
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSherlock().getActionBar().setDisplayHomeAsUpEnabled(false);
        addPreferencesFromResource(R.xml.settings_tweak);
        Parse.initialize(this, Constants.PARSE_ID, Constants.PARSE_KEY);
    }
    
}