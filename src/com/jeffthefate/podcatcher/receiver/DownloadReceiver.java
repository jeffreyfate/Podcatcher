package com.jeffthefate.podcatcher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.service.DownloadService;
/**
 * Receives when the contexts and widgets should be updated.
 * 
 * @author Jeff Fate
 */
public class DownloadReceiver extends BroadcastReceiver {
 
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent newIntent = new Intent(context, DownloadService.class);
        if (intent != null && intent.hasExtra(Constants.URLS))
            newIntent.putExtra(Constants.URLS,
                    intent.getStringArrayListExtra(Constants.URLS));
        context.startService(newIntent);
    }
 
}