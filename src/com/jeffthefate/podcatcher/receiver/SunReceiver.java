package com.jeffthefate.podcatcher.receiver;

import java.util.Calendar;
import java.util.TimeZone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.Util;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

/**
 * Receives when device is finished booting.  Starts the service that sets up
 * the receivers that start the service that updates contexts and widgets.
 * 
 * @author Jeff Fate
 */
public class SunReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Constants.ACTION_UPDATE_SUN_TIMES)) {
            android.location.Location lastKnownLoc = Util.getLastKnown();
            if (lastKnownLoc != null) {
                Location sunLoc = new Location(lastKnownLoc.getLatitude(),
                        lastKnownLoc.getLongitude());
                SunriseSunsetCalculator calculator =
                    new SunriseSunsetCalculator(sunLoc, TimeZone.getDefault());
                Calendar officialSunrise =
                    calculator.getOfficialSunriseCalendarForDate(
                            Calendar.getInstance());
                Calendar officialSunset =
                    calculator.getOfficialSunsetCalendarForDate(
                            Calendar.getInstance());
                ApplicationEx.sunriseTime = officialSunrise.getTimeInMillis();
                ApplicationEx.sunsetTime = officialSunset.getTimeInMillis();
            }
        }
        else if (intent.getAction().equals(Constants.ACTION_DISABLE_NIGHT))
            ApplicationEx.resetBrightness();
        else if (intent.getAction().equals(Constants.ACTION_ENABLE_NIGHT)) {
            int mode = Integer.parseInt(ApplicationEx.sharedPrefs.getString(
                    context.getString(R.string.night_key), Constants.ZERO));
            switch (mode) {
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
    }    
 
}