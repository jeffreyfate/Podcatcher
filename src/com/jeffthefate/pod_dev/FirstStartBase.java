package com.jeffthefate.pod_dev;

import android.support.v4.app.Fragment;

public class FirstStartBase {
    
    public interface OnFragmentChangeListener {
        public void onFragmentChange(Fragment newFragment, String tag);
    }
    
    public interface OnActivityEndListener {
        public void onActivityEnd();
    }

}
