package com.jeffthefate.podcatcher;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.util.Log;

import com.jeffthefate.podcatcher.service.PlaybackService;

public class ProximityReader implements SensorEventListener {

    /** True when the Proximity-functionality is basically available. */
    boolean proximityAvailable = false;
    boolean isEnabled = false;
    SensorManager sensorMan;
    
    // Milliseconds
    private static final long MAX_DEBOUNCE = 1000;

    private int swipeCount = 0;
    
    private SwipeCountdown swipeCountdown;

    /**
     * Sets up an AccelerometerReader. Checks if Accelerometer is available on
     * this device and throws UnsupportedOperationException if not .
     *
     * @param doEnable :
     *            enables the devices Accelerometer
     *            initially (if sensor available)
     * @throws UnsupportedOperationException
     *             if Accelerometer is not available on this device.
     */
    public ProximityReader(boolean doEnable) throws UnsupportedOperationException {
        /* Check once here in the constructor if an
         * Accelerometer is available on this device. */
        sensorMan = (SensorManager) ApplicationEx.getApp().getSystemService(
                Context.SENSOR_SERVICE);
        if (!sensorMan.getSensorList(Sensor.TYPE_PROXIMITY).isEmpty())
            proximityAvailable = true;
        if (!proximityAvailable)
            throw new UnsupportedOperationException(
                    "Proximity sensor is not available.");
        if (doEnable)
            setEnableProximity(true);
    }

    /**
     * En/Dis-able the Accelerometer.
     *
     * @param doEnable
     *            <code>true</code> for enable.<br>
     *            <code>false</code> for disable.
     * @throws UnsupportedOperationException
     */
    public void setEnableProximity(boolean doEnable)
            throws UnsupportedOperationException {
        if (!proximityAvailable)
            throw new UnsupportedOperationException(
                    "Proximity sensor is not available.");
        /* If should be enabled and not already is */
        if (doEnable && !isEnabled) {
            sensorMan.registerListener(this, sensorMan.getDefaultSensor(
                    Sensor.TYPE_PROXIMITY),
                    SensorManager.SENSOR_DELAY_FASTEST);
            isEnabled = true;
        } else /* If should be disabled and not already is: */
            if (!doEnable && this.isEnabled) {
                sensorMan.unregisterListener(this);
                isEnabled = false;
            }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            // TODO Determine how to sense a swipe - some devices have more than
            // two states.  GNex has only 0.0 and 5.0 as value.
            if (event.values[0] == event.sensor.getMaximumRange())
                swipeCount++;
            if (swipeCountdown != null)
                swipeCountdown.cancel();
            swipeCountdown = new SwipeCountdown(MAX_DEBOUNCE, 100);
            swipeCountdown.start();
                /*
            ApplicationEx.getApp().sendBroadcast(new Intent(
                    PlaybackService.ACTION_TOGGLE_PLAYBACK));
                 */
        }
    }
    
    private class SwipeCountdown extends CountDownTimer {
        public SwipeCountdown(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            Log.i(Constants.LOG_TAG, "swipes: " + swipeCount);
            Log.i(Constants.LOG_TAG, "play/pause: " + Util.readStringPreference(R.string.playpauseswipe_key, "0"));
            Log.i(Constants.LOG_TAG, "forward: " + Util.readStringPreference(R.string.forwardswipe_key, "0"));
            Log.i(Constants.LOG_TAG, "backward: " + Util.readStringPreference(R.string.backwardswipe_key, "0"));
            switch (swipeCount) {
            case 1:
            case 2:
            case 3:
                if (Integer.parseInt(Util.readStringPreference(
                        R.string.playpauseswipe_key, Constants.ZERO)) ==
                        swipeCount)
                    ApplicationEx.getApp().sendBroadcast(
                            new Intent(PlaybackService.ACTION_TOGGLE_PLAYBACK));
                else if (Integer.parseInt(Util.readStringPreference(
                        R.string.forwardswipe_key, Constants.ZERO)) ==
                        swipeCount)
                    ApplicationEx.getApp().sendBroadcast(
                            new Intent(Util.readBooleanPreference(
                                    R.string.controls_key, false) ?
                                PlaybackService.ACTION_NEXT:
                                    PlaybackService.ACTION_AHEAD));
                else if (Integer.parseInt(Util.readStringPreference(
                        R.string.backwardswipe_key, Constants.ZERO)) ==
                        swipeCount)
                    ApplicationEx.getApp().sendBroadcast(
                            new Intent(Util.readBooleanPreference(
                                    R.string.controls_key, false) ?
                                PlaybackService.ACTION_PREVIOUS :
                                    PlaybackService.ACTION_BEHIND));
                break;
            default:
                break;
            }
            swipeCount = 0;
        }

        @Override
        public void onTick(long millisUntilFinished) {}
    }
    
}