package com.jeffthefate.podcatcher.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioGroup;

import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.PluginBundleManagerUpdate;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.Util;

public class ActivityEditUpdate extends ActivityBase {
    
    private RadioGroup radioGroup = null;
    private Button doneButton = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_update);
        radioGroup = (RadioGroup) findViewById(R.id.radio_group);
        radioGroup.check(R.id.radio_manual);
        doneButton = (Button) findViewById(R.id.UpdateButton);
        doneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                int updateSelected = radioGroup.getCheckedRadioButtonId();
                int updateMode = -1;
                String updateString = null;
                switch (updateSelected) {
                case R.id.radio_manual:
                    updateMode = 0;
                    updateString = getString(R.string.Manual);
                    break;
                case R.id.radio_interval:
                    updateMode = 1;
                    updateString = getString(R.string.Interval);
                    break;
                case R.id.radio_magic:
                    updateMode = 2;
                    updateString = getString(R.string.Magic);
                    break;
                default:
                    updateMode = 0;
                    updateString = getString(R.string.Manual);
                    break;
                }
                final Intent resultIntent = new Intent();
                final Bundle resultBundle =
                    PluginBundleManagerUpdate.generateBundle(
                            getApplicationContext(), updateMode);
                resultIntent.putExtra(
                        com.twofortyfouram.locale.Intent.EXTRA_BUNDLE,
                        resultBundle);
                /*
                 * The blurb is concise status text to be displayed in the host's UI.
                 */
                
                final String blurb = Util.generateBlurb(getApplicationContext(),
                        TextUtils.concat(Constants.UPDATE_MODE, updateString)
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