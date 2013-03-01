package com.jeffthefate.pod_dev;

import android.support.v4.app.Fragment;

public class FirstStartBaseLegacy {
    
    public interface OnFragmentChangeListener {
        public void onFragmentChange(Fragment newFragment, String tag);
        public void onReturnFromSettings();
    }
    
    public interface OnActivityEndListener {
        public void onActivityEnd();
    }

}
