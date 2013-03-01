package com.jeffthefate.pod_dev.fragment;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.Util;
import com.jeffthefate.pod_dev.Util.Magics;
import com.jeffthefate.pod_dev.activity.ActivityFirstStart;

public class FragmentSetup extends PreferenceFragment {
    
    Preference finishPref;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);

        addPreferencesFromResource(R.xml.settings_firststart);
        
        finishPref = findPreference(getString(R.string.finish_key));
        finishPref.setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference pref) {
                if (Util.readBooleanPreference(R.string.sync_key, false))
                    ((ActivityFirstStart)getActivity()).mFragmentCallback
                            .onFragmentChange(new FragmentAccount(),"fAccount");
                else {
                    if (Integer.parseInt(
                            Util.readStringPreference(R.string.update_key, "0"))
                                == 1) {
                        Util.setRepeatingAlarm(Integer.parseInt(
                                Util.readStringPreference(R.string.interval_key,
                                        "60")), false);
                    }
                    else if (Integer.parseInt(
                            Util.readStringPreference(R.string.update_key, "0"))
                                == 2) {
                        Magics currMagics = 
                            ApplicationEx.dbHelper.getNextMagicInterval(false);
                        Util.setMagicAlarm(currMagics.getTime(),
                                currMagics.getFeeds());
                    }
                    ((ActivityFirstStart)getActivity()).mActivityCallback
                            .onActivityEnd();
                }
                return true;
            }
        });
    }
    
}