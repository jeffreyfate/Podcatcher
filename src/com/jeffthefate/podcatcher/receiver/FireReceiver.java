package com.jeffthefate.podcatcher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.PluginBundleManagerNightMode;
import com.jeffthefate.podcatcher.PluginBundleManagerSwipe;
import com.jeffthefate.podcatcher.PluginBundleManagerTap;
import com.jeffthefate.podcatcher.PluginBundleManagerUpdate;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.Util;
import com.jeffthefate.podcatcher.Util.Magics;

public final class FireReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(final Context context, final Intent intent)
    {
        if (com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING.equals(
                intent.getAction())) {
            Bundle extras = intent.getExtras();
            Bundle result = extras.getBundle(
                    com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
            if (PluginBundleManagerNightMode.isBundleValid(result)) {
                switch (result.getInt(Constants.BUNDLE_EXTRA_INT_NIGHT_MODE)) {
                case 0:
                    ApplicationEx.resetBrightness();
                    break;
                case 1:
                    ApplicationEx.setBrightness(
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL, 50);
                    break;
                case 2:
                    ApplicationEx.setBrightness(
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL, 25);
                    break;
                case 3:
                    ApplicationEx.setBrightness(
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL, 0);
                    break;
                default:
                    ApplicationEx.resetBrightness();
                    break;
                }
            }
            if (PluginBundleManagerUpdate.isBundleValid(result)) {
                Util.cancelRepeatingAlarm();
                switch (result.getInt(Constants.BUNDLE_EXTRA_INT_UPDATE)) {
                case 0:
                    Util.persistPreference(Constants.ZERO, R.string.update_key);
                    break;
                case 1:
                    Util.setRepeatingAlarm(Integer.parseInt(
                            Util.readStringPreference(R.string.interval_key,
                                    Constants.SIXTY)), false);
                    Util.persistPreference(Constants.ONE, R.string.update_key);
                    break;
                case 2:
                    Magics currMagics = 
                        ApplicationEx.dbHelper.getNextMagicInterval();
                    Util.setMagicAlarm(currMagics.getTime(),
                            currMagics.getFeeds());
                    Util.persistPreference(Constants.TWO, R.string.update_key);
                    break;
                default:
                    break;
                }
                    
            }
            if (PluginBundleManagerTap.isBundleValid(result)) {
                ApplicationEx.sharedPrefs.edit().putBoolean(
                        context.getString(R.string.tap_key),
                        result.getBoolean(Constants.BUNDLE_EXTRA_BOOLEAN_TAP))
                    .apply();
            }
            if (PluginBundleManagerSwipe.isBundleValid(result)) {
                ApplicationEx.sharedPrefs.edit().putBoolean(
                        context.getString(R.string.swipe_key),
                        result.getBoolean(Constants.BUNDLE_EXTRA_BOOLEAN_SWIPE))
                    .apply();
            }
        }
    }
}