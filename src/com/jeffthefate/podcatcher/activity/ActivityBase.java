package com.jeffthefate.podcatcher.activity;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.jeffthefate.podcatcher.Constants;
import com.parse.Parse;

public class ActivityBase extends SherlockFragmentActivity {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Parse.initialize(this, Constants.PARSE_ID, Constants.PARSE_KEY);
    }
    
}