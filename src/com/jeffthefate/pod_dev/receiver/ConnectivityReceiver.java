package com.jeffthefate.pod_dev.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.Constants;
/**
 * Receives when the contexts and widgets should be updated.
 * 
 * @author Jeff Fate
 */
public class ConnectivityReceiver extends BroadcastReceiver {
    
    boolean wifiEnabled = false;
    ConnectivityManager connMan;
 
    @Override
    public void onReceive(Context context, Intent intent) {
        connMan = (ConnectivityManager) ApplicationEx.getApp()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (intent.getAction().equalsIgnoreCase(
                WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            switch (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 
                    WifiManager.WIFI_STATE_UNKNOWN)) {
            case WifiManager.WIFI_STATE_ENABLED:
                ApplicationEx.setWifi(true);
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                ApplicationEx.setWifi(false);
                break;
            case WifiManager.WIFI_STATE_ENABLING:
                ApplicationEx.setWifi(false);
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                ApplicationEx.setWifi(false);
                break;
            case WifiManager.WIFI_STATE_UNKNOWN:
                ApplicationEx.setWifi(false);
                break;
            }
        }
        else if (intent.getAction().equalsIgnoreCase(
                Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
            ApplicationEx.setAirplaneMode(intent.getBooleanExtra("state", 
                    ApplicationEx.inAirplaneMode()));
        }
        else if (intent.getAction().equalsIgnoreCase(
                ConnectivityManager.CONNECTIVITY_ACTION)) {
            ApplicationEx.setConnection(!intent.getBooleanExtra(
                    ConnectivityManager.EXTRA_NO_CONNECTIVITY, false));
        }
        ApplicationEx.getApp().sendBroadcast(
                new Intent(Constants.ACTION_CHECK_CONNECTION));
    }
 
}