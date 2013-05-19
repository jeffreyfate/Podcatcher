package com.jeffthefate.podcatcher.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.PluginBundleManagerTap;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.Util;

public class ActivityEditTapRemote extends ActivityBase {
    
    private CheckedTextView tapBox = null;
    private CheckedTextView sensitivityBox = null;
    private TextView sensitivityText = null;
    private SeekBar sensitivityBar = null;
    private Button doneButton = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_tap);
        tapBox = (CheckedTextView) findViewById(R.id.TapCheck);
        tapBox.setChecked(ApplicationEx.sharedPrefs.getBoolean(
                getString(R.string.tap_key), false));
        tapBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tapBox.toggle();
            }
        });
        sensitivityText = (TextView) findViewById(R.id.TapText);
        sensitivityBox = (CheckedTextView) findViewById(R.id.SensitivityCheck);
        sensitivityBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sensitivityBox.toggle();
                if (sensitivityBox.isChecked())
                    sensitivityBar.setEnabled(true);
                else
                    sensitivityBar.setEnabled(false);
            }
        });
        sensitivityBar = (SeekBar) findViewById(R.id.SensitivityBar);
        sensitivityBar.setProgress(Integer.parseInt(
                ApplicationEx.sharedPrefs.getString(
                        getString(R.string.tapsensitivity_key),
                        Constants.ZERO))-2);
        sensitivityText.setText(Integer.toString(sensitivityBar.getProgress()
                + 2));
        sensitivityBar.setOnSeekBarChangeListener(
                new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekbar, int progress,
                    boolean fromUser) {
                sensitivityText.setText(Integer.toString(progress + 2));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekbar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekbar) {}
        });
        sensitivityBar.setEnabled(false);
        doneButton = (Button) findViewById(R.id.TapButton);
        doneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                final Intent resultIntent = new Intent();
                final Bundle resultBundle =
                    PluginBundleManagerTap.generateBundle(
                            getApplicationContext(), tapBox.isChecked(),
                            sensitivityBox.isChecked(),
                            sensitivityBar.getProgress() + 2);
                resultIntent.putExtra(
                        com.twofortyfouram.locale.Intent.EXTRA_BUNDLE,
                        resultBundle);
                /*
                 * The blurb is concise status text to be displayed in the host's UI.
                 */
                
                final String blurb = Util.generateBlurb(getApplicationContext(),
                        TextUtils.concat(Constants.TAP_REMOTE,
                                tapBox.isChecked() ? Constants.ENABLED :
                                    Constants.DISABLED, Constants.COMMA_SPACE,
                                sensitivityBox.isChecked() ? Constants.NEW :
                                    Constants.SAME, Constants.SENSITIVITY,
                                Integer.toString(
                                        sensitivityBar.getProgress() + 2))
                                .toString());
                resultIntent.putExtra(
                        com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB,
                        blurb);

                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }
    
}