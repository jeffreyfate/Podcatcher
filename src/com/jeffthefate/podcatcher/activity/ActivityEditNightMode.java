package com.jeffthefate.podcatcher.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.PluginBundleManagerNightMode;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.Util;

public class ActivityEditNightMode extends ActivityBase {
    
    private SeekBar nightModeSeek = null;
    private TextView nightModeText = null;
    private Button doneButton = null;
    private SharedPreferences sharedPrefs;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_night_mode);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(
                ApplicationEx.getApp());
        nightModeSeek = (SeekBar) findViewById(R.id.NightModeSeek);
        nightModeSeek.setProgress(0);
        nightModeSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekbar, int progress,
                    boolean fromUser) {
                switch (progress) {
                case 0:
                    nightModeText.setText(getString(R.string.off));
                    break;
                case 1:
                    nightModeText.setText(getString(R.string.dark));
                    break;
                case 2:
                    nightModeText.setText(getString(R.string.darker));
                    break;
                case 3:
                    nightModeText.setText(getString(R.string.darkest));
                    break;
                default:
                    nightModeText.setText(getString(R.string.off));
                    break;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekbar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekbar) {}
        });
        nightModeText = (TextView) findViewById(R.id.NightModeText);
        nightModeText.setText(getString(R.string.off));
        doneButton = (Button) findViewById(R.id.NightModeButton);
        doneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                int nightMode = nightModeSeek.getProgress();
                sharedPrefs.edit().putInt(getString(R.string.nightmode_key),
                        nightMode).apply();
                final Intent resultIntent = new Intent();
                final Bundle resultBundle =
                    PluginBundleManagerNightMode.generateBundle(
                            getApplicationContext(), nightMode);
                resultIntent.putExtra(
                        com.twofortyfouram.locale.Intent.EXTRA_BUNDLE,
                        resultBundle);
                /*
                 * The blurb is concise status text to be displayed in the host's UI.
                 */
                final String blurb = Util.generateBlurb(getApplicationContext(),
                        TextUtils.concat(Constants.NIGHT_MODE,
                                nightModeText.getText().toString()).toString());
                resultIntent.putExtra(
                        com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB,
                        blurb);

                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }
    
}