/*   
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jeffthefate.podcatcher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.KeyEvent;

import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.Util;
import com.jeffthefate.podcatcher.service.PlaybackService;

/**
 * Receives broadcasted intents. In particular, we are interested in the
 * android.media.AUDIO_BECOMING_NOISY and android.intent.action.MEDIA_BUTTON
 * intents, which is broadcast, for example, when the user disconnects the
 * headphones. This class works because we are declaring it in a 
 * &lt;receiver&gt; tag in AndroidManifest.xml.
 */
public class MusicIntentReceiver extends BroadcastReceiver {
    private Context context;
    
    public class ButtonCountdown extends CountDownTimer {
        public ButtonCountdown(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            Log.i(Constants.LOG_TAG, "ButtonCountdown finished");
            /* Press the right key based on how many times the button has
             * been pressed */
            switch(ApplicationEx.presses.indexOfValue(
                    ApplicationEx.keys.size())) {
            case 0:
                Log.v(Constants.LOG_TAG, "pressing PLAY_PAUSE");
                context.sendBroadcast(new Intent(
                        PlaybackService.ACTION_TOGGLE_PLAYBACK));
                break;
            case 1:
                Log.v(Constants.LOG_TAG, "pressing PREVIOUS");
                context.sendBroadcast(
                        new Intent(Util.readBooleanPreference(
                                R.string.controls_key, false) ?
                            PlaybackService.ACTION_PREVIOUS :
                                PlaybackService.ACTION_BEHIND));
                break;
            case 2:
                Log.v(Constants.LOG_TAG, "pressing NEXT");
                context.sendBroadcast(
                        new Intent(Util.readBooleanPreference(
                                R.string.controls_key, false) ?
                            PlaybackService.ACTION_NEXT:
                                PlaybackService.ACTION_AHEAD));
                break;
            }
            ApplicationEx.keys.clear();
        }

        @Override
        public void onTick(long millisUntilFinished) {}
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
            KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(
                    Intent.EXTRA_KEY_EVENT);
            if (keyEvent == null ||
                    keyEvent.getAction() != KeyEvent.ACTION_DOWN)
                return;
            /* If remote enabled and this is the first catch of headset press */
            
            /* If the app is to catch the press, either from the key sent from
             * here or from another remote source */
            Log.e(Constants.LOG_TAG, keyEvent.toString());
            switch (keyEvent.getKeyCode()) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
                Log.i(Constants.LOG_TAG, "KEYCODE_HEADSETHOOK");
                if (Util.readBooleanPreference(R.string.remote_key, false)) {
                    /* Keep track of the preferences for the key actions */
                    ApplicationEx.presses.put(0, Integer.parseInt(
                            Util.readStringPreference(
                                R.string.playpausepress_key, Constants.ONE)));
                    ApplicationEx.presses.put(1, Integer.parseInt(
                            Util.readStringPreference(
                                R.string.backwardpress_key, Constants.ONE)));
                    ApplicationEx.presses.put(2, Integer.parseInt(
                            Util.readStringPreference(
                                R.string.forwardpress_key, Constants.ONE)));
                    /* Get the time between button presses */
                    ApplicationEx.speed = Long.parseLong(
                            Util.readStringPreference(R.string.remotespeed_key,
                                    Constants.SIX_HUNDRED));
                    /* Cancel countdown */
                    if (ApplicationEx.buttonCountdown != null) {
                        Log.v(Constants.LOG_TAG, "canceling buttonCountdown");
                        ApplicationEx.buttonCountdown.cancel();
                    }
                    /* Start countdown with the button press speed */
                    ApplicationEx.buttonCountdown = new ButtonCountdown(
                            ApplicationEx.speed, 100);
                    ApplicationEx.buttonCountdown.start();
                    /* Add a new press */
                    ApplicationEx.keys.add(System.currentTimeMillis());
                    break;
                }
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                Log.i(Constants.LOG_TAG, "KEYCODE_MEDIA_PLAY_PAUSE");
                context.sendBroadcast(new Intent(
                        PlaybackService.ACTION_TOGGLE_PLAYBACK));
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                context.sendBroadcast(
                        new Intent(PlaybackService.ACTION_PLAY));
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                context.sendBroadcast(
                        new Intent(PlaybackService.ACTION_PAUSE));
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                context.sendBroadcast(
                        new Intent(Util.readBooleanPreference(
                                R.string.controls_key, false) ?
                            PlaybackService.ACTION_NEXT:
                                PlaybackService.ACTION_AHEAD));
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                context.sendBroadcast(
                        new Intent(Util.readBooleanPreference(
                                R.string.controls_key, false) ?
                            PlaybackService.ACTION_PREVIOUS :
                                PlaybackService.ACTION_BEHIND));
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                context.sendBroadcast(
                        new Intent(PlaybackService.ACTION_AHEAD));
                break;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                context.sendBroadcast(
                        new Intent(PlaybackService.ACTION_BEHIND));
                break;
            }
        }
    }
}
