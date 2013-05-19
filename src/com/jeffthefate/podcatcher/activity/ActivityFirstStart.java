package com.jeffthefate.podcatcher.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.Util;
import com.jeffthefate.podcatcher.Util.Magics;
import com.jeffthefate.podcatcher.fragment.FragmentFirstStart;

public class ActivityFirstStart extends ActivityBase {
    
    FragmentManager fMan;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fMan = getSupportFragmentManager();
        fMan.beginTransaction().replace(android.R.id.content, 
                new FragmentFirstStart(), Constants.FRAGMENT_FIRST_START)
            .commitAllowingStateLoss();
        fMan.executePendingTransactions();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (Util.readBooleanPreference(R.string.sync_key, false)) {
            Util.cancelRepeatingAlarm();
            if (Integer.parseInt(Util.readStringPreference(R.string.update_key,
                    Constants.ZERO)) == 1) {
                Util.setRepeatingAlarm(Integer.parseInt(Util.readStringPreference(
                        R.string.interval_key, Constants.SIXTY)), false);
            }
            else if (Integer.parseInt(Util.readStringPreference(R.string.update_key,
                    Constants.ZERO)) == 2) {
                Magics currMagics = 
                    ApplicationEx.dbHelper.getNextMagicInterval();
                Util.setMagicAlarm(currMagics.getTime(), currMagics.getFeeds());
            }
            finish();
        }
    }
    
}