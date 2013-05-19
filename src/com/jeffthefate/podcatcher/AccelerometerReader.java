package com.jeffthefate.podcatcher;

import java.util.LinkedList;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.util.Log;

import com.jeffthefate.podcatcher.DetectTap.PulseState;
import com.jeffthefate.podcatcher.service.PlaybackService;

public class AccelerometerReader implements SensorEventListener {

    /** True when the Accelerometer-functionality is basically available. */
    boolean accelerometerAvailable = false;
    boolean isEnabled = false;
    SensorManager sensorMan;

    private DetectTap tapDetector;
    /*
    private LinkedList<PulseState> tapXStateQueue =
        new LinkedList<PulseState>();
    private long lastXNegativePulse = -1;
    private long lastXPositivePulse = -1;
    private LinkedList<PulseState> tapYStateQueue =
        new LinkedList<PulseState>();
    private long lastYNegativePulse = -1;
    private long lastYPositivePulse = -1;
    */
    private LinkedList<TapState> tapZStateQueue = new LinkedList<TapState>();
    private long lastZNegativePulse = -1;
    private long lastZPositivePulse = -1;
    
    private static final long MAX_DEBOUNCE = 250;
    private static final long MAX_WAIT = 1000;
    
    private int tapCount = 0;

    //private PulseState currentXState;
    //private PulseState currentYState;
    private PulseState currentZState;
    private long currentTapTime;

    private static final int MAX_QUEUE_LENGTH = 4;

    private int tapInterval = 0;

    //private int leftTapCount = 0;
    //private int rightTapCount = 0;
    //private int topTapCount = 0;
    //private int bottomTapCount = 0;
    private int frontTapCount = 0;
    private int backTapCount = 0;
    
    public enum Direction {
        None,
        Left,
        Right,
        Top,
        Bottom,
        Front,
        Back
    }
    
    private Direction directionalTap = Direction.None;

    private static final int MIN_TAP_INTERVAL = 2;

    int playTaps;
    int rewTaps;
    int ffwdTaps;
    
    private TapCountdown tapCountdown;
    
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
    public AccelerometerReader(boolean doEnable) throws UnsupportedOperationException {
        /* Check once here in the constructor if an
         * Accelerometer is available on this device. */
        sensorMan = (SensorManager) ApplicationEx.getApp().getSystemService(
                Context.SENSOR_SERVICE);
        if (!sensorMan.getSensorList(Sensor.TYPE_ACCELEROMETER).isEmpty())
            accelerometerAvailable = true;
        if (!accelerometerAvailable)
            throw new UnsupportedOperationException(
                    "Accelerometer is not available.");
        if (doEnable)
            setEnableAccelerometer(true);
        if (tapDetector == null)
            tapDetector = new DetectTap();
        tapDetector.setThreshold(Float.valueOf(
                ApplicationEx.sharedPrefs.getString(
                        ApplicationEx.getApp().getString(
                                R.string.tapsensitivity_key),
                                Constants.EIGHT)));
    }

    /**
     * En/Dis-able the Accelerometer.
     *
     * @param doEnable
     *            <code>true</code> for enable.<br>
     *            <code>false</code> for disable.
     * @throws UnsupportedOperationException
     */
    public void setEnableAccelerometer(boolean doEnable)
            throws UnsupportedOperationException {
        if (!accelerometerAvailable)
            throw new UnsupportedOperationException(
                    "Accelerometer is not available.");
        /* If should be enabled and not already is: */
        if (doEnable && !isEnabled) {
            sensorMan.registerListener(this, sensorMan.getDefaultSensor(
                    Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_FASTEST);
            isEnabled = true;
        } else /* If should be disabled and not already is: */
            if (!doEnable && this.isEnabled) {
                sensorMan.unregisterListener(this);
                isEnabled = false;
            }
    }
    
    public void setThreshold(float newThreshold) {
        tapDetector.setThreshold(newThreshold);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //float x = event.values[0];
            //float y = event.values[1];
            float z = event.values[2];
            // Only keep small amount of history X, Y, Z data.
            /*
            if (tapXStateQueue.size() >= MAX_QUEUE_LENGTH)
                tapXStateQueue.pollLast();
            if (tapYStateQueue.size() >= MAX_QUEUE_LENGTH)
                tapYStateQueue.pollLast();
                */
            if (tapZStateQueue.size() >= MAX_QUEUE_LENGTH)
                tapZStateQueue.pollLast();

            // Use the state machine to detect positive or negative pulse on
            // x, y, z axes. 
            // Put the current state in a first in first out queue. 
            /*
            currentXState = tapDetector.detectXPulse(x);
            if (currentXState == PulseState.NegativePulse)
                lastXNegativePulse = System.currentTimeMillis();
            else if (currentXState == PulseState.PositivePulse)
                lastXPositivePulse = System.currentTimeMillis();
            tapXStateQueue.addFirst(currentXState);
            currentYState = tapDetector.detectYPulse(y);
            if (currentYState == PulseState.NegativePulse)
                lastYNegativePulse = System.currentTimeMillis();
            else if (currentYState == PulseState.PositivePulse)
                lastYPositivePulse = System.currentTimeMillis();
            tapYStateQueue.addFirst(currentYState);
            */
            currentZState = tapDetector.detectZPulse(z);
            currentTapTime = System.currentTimeMillis();
            if (currentZState == PulseState.NegativePulse &&
                    currentTapTime - lastZNegativePulse > MAX_DEBOUNCE) {
                lastZNegativePulse = currentTapTime;
                tapZStateQueue.addFirst(
                        new TapState(currentZState, currentTapTime));
                Log.v(Constants.LOG_TAG, "currentZState: " + currentZState.toString());
                tapInterval++;
            }
            else if (currentZState == PulseState.PositivePulse &&
                    currentTapTime - lastZPositivePulse > MAX_DEBOUNCE) {
                lastZPositivePulse = currentTapTime;
                tapZStateQueue.addFirst(
                        new TapState(currentZState, currentTapTime));
                Log.v(Constants.LOG_TAG, "currentZState: " + currentZState.toString());
                tapInterval++;
            }
            /*
            if (tapXStateQueue.size() >= MAX_QUEUE_LENGTH &&
                    tapInterval >= MIN_TAP_INTERVAL) {
                // Left Tap
                // We identify x direction negative pulse by the X state. 
                // The sequence for left tap is a negative pulse followed by
                // a positive pulse.     
                // So, the state sequence could be : 2,1 or 2,0,1,
                // or 2,0,0,1.
                if (((tapXStateQueue.get(MAX_QUEUE_LENGTH - 3) ==
                        PulseState.NegativePulse &&
                    tapXStateQueue.get(MAX_QUEUE_LENGTH - 2) ==
                        PulseState.NonActive &&  
                    tapXStateQueue.get(MAX_QUEUE_LENGTH - 1) ==
                        PulseState.PositivePulse)
                    || (tapXStateQueue.get(MAX_QUEUE_LENGTH - 4) ==
                        PulseState.NegativePulse && 
                    tapXStateQueue.get(MAX_QUEUE_LENGTH - 3) ==
                        PulseState.NonActive && 
                    tapXStateQueue.get(MAX_QUEUE_LENGTH - 2) ==
                        PulseState.NonActive &&   
                    tapXStateQueue.get(MAX_QUEUE_LENGTH - 1) ==
                        PulseState.PositivePulse)
                    || (tapXStateQueue.get(MAX_QUEUE_LENGTH - 2) ==
                        PulseState.NegativePulse && 
                    tapXStateQueue.get(MAX_QUEUE_LENGTH - 1) ==
                        PulseState.PositivePulse)) &&
                    (System.currentTimeMillis()-lastXNegativePulse) <=
                        MAX_DEBOUNCE) {
                    leftTapCount++;
                    Log.d(Constants.LOG_TAG, leftTapCount + " Left Tap");
                    tapInterval = 0;
                    directionalTap = Direction.Left;
                }
                // Right tap
                // We identify x direction positive pulse by the X state
                // sequence from state machine. The sequence for right tap
                // is a positive pulse followed by a negative pulse. So, the
                // state sequence could be : 1,2 or 1,0,2, or 1,0,0,2. 
                if (((tapXStateQueue.get(MAX_QUEUE_LENGTH - 3) ==
                        PulseState.PositivePulse && 
                    tapXStateQueue.get(MAX_QUEUE_LENGTH - 2) ==
                        PulseState.NonActive && 
                    tapXStateQueue.get(MAX_QUEUE_LENGTH - 1) ==
                        PulseState.NegativePulse)
                    || (tapXStateQueue.get(MAX_QUEUE_LENGTH - 4) ==
                        PulseState.PositivePulse && 
                    tapXStateQueue.get(MAX_QUEUE_LENGTH - 3) ==
                        PulseState.NonActive && 
                    tapXStateQueue.get(MAX_QUEUE_LENGTH - 2) ==
                        PulseState.NonActive && 
                    tapXStateQueue.get(MAX_QUEUE_LENGTH - 1) ==
                        PulseState.NegativePulse)
                    || (tapXStateQueue.get(MAX_QUEUE_LENGTH - 2) ==
                        PulseState.PositivePulse && 
                    tapXStateQueue.get(MAX_QUEUE_LENGTH - 1) ==
                        PulseState.NegativePulse)) &&
                    (System.currentTimeMillis()-lastXPositivePulse) <=
                        MAX_DEBOUNCE) {
                    rightTapCount++;
                    Log.d(Constants.LOG_TAG, rightTapCount + " Right Tap");
                    tapInterval = 0;
                    directionalTap = Direction.Right;
                }
            }
            if (tapYStateQueue.size() >= MAX_QUEUE_LENGTH &&
                    tapInterval >= MIN_TAP_INTERVAL) {
                // Bottom Tap
                // We identify y direction negative pulse by the Y state. 
                // The sequence for top tap is a negative pulse followed by
                // a positive pulse.     
                // So, the state sequence could be : 2,1 or 2,0,1,
                // or 2,0,0,1.
                if (((tapYStateQueue.get(MAX_QUEUE_LENGTH - 3) ==
                        PulseState.NegativePulse &&
                    tapYStateQueue.get(MAX_QUEUE_LENGTH - 2) ==
                        PulseState.NonActive &&  
                    tapYStateQueue.get(MAX_QUEUE_LENGTH - 1) ==
                        PulseState.PositivePulse)
                    || (tapYStateQueue.get(MAX_QUEUE_LENGTH - 4) ==
                        PulseState.NegativePulse && 
                    tapYStateQueue.get(MAX_QUEUE_LENGTH - 3) ==
                        PulseState.NonActive && 
                    tapYStateQueue.get(MAX_QUEUE_LENGTH - 2) ==
                        PulseState.NonActive &&   
                    tapYStateQueue.get(MAX_QUEUE_LENGTH - 1) ==
                        PulseState.PositivePulse)
                    || (tapYStateQueue.get(MAX_QUEUE_LENGTH - 2) ==
                        PulseState.NegativePulse && 
                    tapYStateQueue.get(MAX_QUEUE_LENGTH - 1) ==
                        PulseState.PositivePulse)) &&
                    (System.currentTimeMillis()-lastYNegativePulse) <=
                        MAX_DEBOUNCE) {
                    bottomTapCount++;
                    Log.d(Constants.LOG_TAG, bottomTapCount + " Bottom Tap");
                    tapInterval = 0;
                    directionalTap = Direction.Bottom;
                }
                // Top tap
                // We identify y direction positive pulse by the Y state
                // sequence from state machine. The sequence for bottom tap
                // is a positive pulse followed by a negative pulse. So, the
                // state sequence could be : 1,2 or 1,0,2, or 1,0,0,2. 
                if (((tapYStateQueue.get(MAX_QUEUE_LENGTH - 3) ==
                        PulseState.PositivePulse && 
                    tapYStateQueue.get(MAX_QUEUE_LENGTH - 2) ==
                        PulseState.NonActive && 
                    tapYStateQueue.get(MAX_QUEUE_LENGTH - 1) ==
                        PulseState.NegativePulse)
                    || (tapYStateQueue.get(MAX_QUEUE_LENGTH - 4) ==
                        PulseState.PositivePulse && 
                    tapYStateQueue.get(MAX_QUEUE_LENGTH - 3) ==
                        PulseState.NonActive && 
                    tapYStateQueue.get(MAX_QUEUE_LENGTH - 2) ==
                        PulseState.NonActive && 
                    tapYStateQueue.get(MAX_QUEUE_LENGTH - 1) ==
                        PulseState.NegativePulse)
                    || (tapYStateQueue.get(MAX_QUEUE_LENGTH - 2) ==
                        PulseState.PositivePulse && 
                    tapYStateQueue.get(MAX_QUEUE_LENGTH - 1) ==
                        PulseState.NegativePulse)) &&
                    (System.currentTimeMillis()-lastYPositivePulse) <=
                        MAX_DEBOUNCE) {
                    topTapCount++;
                    Log.d(Constants.LOG_TAG, topTapCount + " Top Tap");
                    tapInterval = 0;
                    directionalTap = Direction.Top;
                }
            }
            */
            if (tapZStateQueue.size() >= MAX_QUEUE_LENGTH &&
                    tapInterval >= MIN_TAP_INTERVAL) {
                // Back Tap
                // We identify z direction negative pulse by the Z state. 
                // The sequence for front tap is a negative pulse followed
                // by a positive pulse.     
                // So, the state sequence could be : 2,1 or 2,0,1,
                // or 2,0,0,1.
                if (((tapZStateQueue.get(MAX_QUEUE_LENGTH - 3).getState() ==
                        PulseState.NegativePulse &&
                    tapZStateQueue.get(MAX_QUEUE_LENGTH - 2).getState() ==
                        PulseState.NonActive &&  
                    tapZStateQueue.get(MAX_QUEUE_LENGTH - 1).getState() ==
                        PulseState.PositivePulse)
                    || (tapZStateQueue.get(MAX_QUEUE_LENGTH - 4).getState() ==
                        PulseState.NegativePulse && 
                    tapZStateQueue.get(MAX_QUEUE_LENGTH - 3).getState() ==
                        PulseState.NonActive && 
                    tapZStateQueue.get(MAX_QUEUE_LENGTH - 2).getState() ==
                        PulseState.NonActive &&   
                    tapZStateQueue.get(MAX_QUEUE_LENGTH - 1).getState() ==
                        PulseState.PositivePulse)
                    || (tapZStateQueue.get(MAX_QUEUE_LENGTH - 2).getState() ==
                        PulseState.NegativePulse && 
                    tapZStateQueue.get(MAX_QUEUE_LENGTH - 1).getState() ==
                        PulseState.PositivePulse)) &&
                    (System.currentTimeMillis()-lastZNegativePulse) <=
                        MAX_DEBOUNCE) {
                    backTapCount++;
                    Log.d(Constants.LOG_TAG, backTapCount + " Back Tap");
                    tapInterval = 0;
                    directionalTap = Direction.Back;
                }
                // Front tap
                // We identify z direction positive pulse by the Z state
                // sequence from state machine. The sequence for back tap
                // is a positive pulse followed by a negative pulse. So, the
                // state sequence could be : 1,2 or 1,0,2, or 1,0,0,2. 
                if (((tapZStateQueue.get(MAX_QUEUE_LENGTH - 3).getState() ==
                        PulseState.PositivePulse && 
                    tapZStateQueue.get(MAX_QUEUE_LENGTH - 2).getState() ==
                        PulseState.NonActive && 
                    tapZStateQueue.get(MAX_QUEUE_LENGTH - 1).getState() ==
                        PulseState.NegativePulse)
                    || (tapZStateQueue.get(MAX_QUEUE_LENGTH - 4).getState() ==
                        PulseState.PositivePulse && 
                    tapZStateQueue.get(MAX_QUEUE_LENGTH - 3).getState() ==
                        PulseState.NonActive && 
                    tapZStateQueue.get(MAX_QUEUE_LENGTH - 2).getState() ==
                        PulseState.NonActive && 
                    tapZStateQueue.get(MAX_QUEUE_LENGTH - 1).getState() ==
                        PulseState.NegativePulse)
                    || (tapZStateQueue.get(MAX_QUEUE_LENGTH - 2).getState() ==
                        PulseState.PositivePulse && 
                    tapZStateQueue.get(MAX_QUEUE_LENGTH - 1).getState() ==
                        PulseState.NegativePulse)) &&
                    (System.currentTimeMillis()-lastZPositivePulse) <=
                        MAX_DEBOUNCE) {
                    frontTapCount++;
                    Log.d(Constants.LOG_TAG, frontTapCount + " Front Tap");
                    tapInterval = 0;
                    directionalTap = Direction.Front;
                }
            }
                /*
            ApplicationEx.getApp().sendBroadcast(new Intent(
                    PlaybackService.ACTION_TOGGLE_PLAYBACK));
                 */
            if (directionalTap == Direction.Front ||
                    directionalTap == Direction.Back) {
                tapCount++;
                if (tapCountdown != null)
                    tapCountdown.cancel();
                tapCountdown = new TapCountdown(MAX_WAIT, 100);
                tapCountdown.start();
            }
        }
        directionalTap = Direction.None;
    }
    
    private class TapCountdown extends CountDownTimer {
        public TapCountdown(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            Log.i(Constants.LOG_TAG, "taps: " + tapCount);
            if (Integer.parseInt(Util.getStringPref(R.string.playpausetap_key,
                    Constants.ZERO)) == tapCount)
                ApplicationEx.getApp().sendBroadcast(
                        new Intent(PlaybackService.ACTION_TOGGLE_PLAYBACK));
            else if (Integer.parseInt(Util.getStringPref(
                    R.string.forwardtap_key, Constants.ZERO)) == tapCount)
                ApplicationEx.getApp().sendBroadcast(
                        new Intent(Util.readBooleanPreference(
                                R.string.controls_key, false) ?
                            PlaybackService.ACTION_NEXT:
                                PlaybackService.ACTION_AHEAD));
            else if (Integer.parseInt(Util.getStringPref(
                    R.string.backwardtap_key, Constants.ZERO)) == tapCount)
                ApplicationEx.getApp().sendBroadcast(
                        new Intent(Util.readBooleanPreference(
                                R.string.controls_key, false) ?
                            PlaybackService.ACTION_PREVIOUS :
                                PlaybackService.ACTION_BEHIND));
            tapCount = 0;
        }

        @Override
        public void onTick(long millisUntilFinished) {}
    }
    
    class TapState {
        private PulseState state;
        private long stateTime;
        
        TapState(PulseState state, long stateTime) {
            this.state = state;
            this.stateTime = stateTime;
        }
        
        public PulseState getState() {
            return state;
        }
        
        public long getStateTime() {
            return stateTime;
        }
    }
    
}