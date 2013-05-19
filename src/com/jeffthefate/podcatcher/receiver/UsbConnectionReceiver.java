package com.jeffthefate.podcatcher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jeffthefate.podcatcher.ApplicationEx;
/**
 * Receives when the contexts and widgets should be updated.
 * 
 * @author Jeff Fate
 */
public class UsbConnectionReceiver extends BroadcastReceiver {
 
    @Override
    public void onReceive(Context context, Intent intent) {
        ApplicationEx.updateUsbConnectionState();
    }
 
}