package com.jeffthefate.podcatcher.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckedTextView;

import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.PluginBundleManagerSwipe;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.Util;

public class ActivityEditSwipeRemote extends ActivityBase {
    
    private CheckedTextView checkBox = null;
    private Button doneButton = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_swipe);
        checkBox = (CheckedTextView) findViewById(R.id.SwipeCheck);
        checkBox.setChecked(ApplicationEx.sharedPrefs.getBoolean(
                getString(R.string.swipe_key), false));
        checkBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBox.toggle();
            }
        });
        doneButton = (Button) findViewById(R.id.SwipeButton);
        doneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                final Intent resultIntent = new Intent();
                final Bundle resultBundle =
                    PluginBundleManagerSwipe.generateBundle(
                            getApplicationContext(), checkBox.isChecked());
                resultIntent.putExtra(
                        com.twofortyfouram.locale.Intent.EXTRA_BUNDLE,
                        resultBundle);
                /*
                 * The blurb is concise status text to be displayed in the host's UI.
                 */
                
                final String blurb = Util.generateBlurb(getApplicationContext(),
                        TextUtils.concat(Constants.SWIPE_REMOTE,
                                checkBox.isChecked() ? Constants.ENABLED :
                                    Constants.DISABLED).toString());
                resultIntent.putExtra(
                        com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB,
                        blurb);

                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }
    
}