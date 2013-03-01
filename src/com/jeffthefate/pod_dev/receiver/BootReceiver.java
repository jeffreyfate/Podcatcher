package com.jeffthefate.pod_dev.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jeffthefate.pod_dev.Constants;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.Util;

/**
 * Receives when device is finished booting.  Starts the service that sets up
 * the receivers that start the service that updates contexts and widgets.
 * 
 * @author Jeff Fate
 */
public class BootReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Integer.parseInt(
                Util.readStringPreference(R.string.update_key, "0")) == 1) {
            Util.setRepeatingAlarm(Integer.parseInt(Util.readStringPreference(
                    R.string.interval_key, "60")), false);
        }
        else if (Integer.parseInt(Util.readStringPreference(
                R.string.update_key, "0")) == 2) {
            Util.setMagicAlarm(Util.readLongFromFile(
                    Constants.MAGICTIME_FILENAME), Util.readUpdatePlaylist());
        }
    }    
 
}