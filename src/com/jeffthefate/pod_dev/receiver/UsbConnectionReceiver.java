package com.jeffthefate.pod_dev.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import com.jeffthefate.pod_dev.ApplicationEx;
/**
 * Receives when the contexts and widgets should be updated.
 * 
 * @author Jeff Fate
 */
public class UsbConnectionReceiver extends BroadcastReceiver {
 
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean usbCharge = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) 
                == BatteryManager.BATTERY_PLUGGED_USB ? true : false;
        ApplicationEx.updateUsbConnectionState(usbCharge);
    }
 
}