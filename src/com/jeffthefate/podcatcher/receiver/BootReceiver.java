package com.jeffthefate.podcatcher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.Util;

/**
 * Receives when device is finished booting.  Starts the service that sets up
 * the receivers that start the service that updates contexts and widgets.
 * 
 * @author Jeff Fate
 */
public class BootReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Integer.parseInt(Util.readStringPreference(R.string.update_key,
                Constants.ZERO)) == 1) {
            Util.setRepeatingAlarm(Integer.parseInt(Util.readStringPreference(
                    R.string.interval_key, Constants.SIXTY)), false);
        }
        else if (Integer.parseInt(Util.readStringPreference(
                R.string.update_key, Constants.ZERO)) == 2) {
            long nextUpdate = ApplicationEx.dbHelper.getNextMagicUpdate();
            Util.setMagicAlarm(nextUpdate < 0 ? System.currentTimeMillis() :
                    nextUpdate, Util.readUpdatePlaylist());
        }
    }    
 
}