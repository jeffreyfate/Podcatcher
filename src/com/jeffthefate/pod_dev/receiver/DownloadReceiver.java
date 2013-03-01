package com.jeffthefate.pod_dev.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jeffthefate.pod_dev.service.DownloadService;
/**
 * Receives when the contexts and widgets should be updated.
 * 
 * @author Jeff Fate
 */
public class DownloadReceiver extends BroadcastReceiver {
 
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent newIntent = new Intent(context, DownloadService.class);
        if (intent != null && intent.hasExtra("urls"))
            newIntent.putExtra("urls", intent.getStringArrayListExtra("urls"));
        context.startService(newIntent);
    }
 
}