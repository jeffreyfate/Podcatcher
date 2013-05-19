package com.jeffthefate.podcatcher.receiver;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
/**
 * Receives when the contexts and widgets should be updated.
 * 
 * @author Jeff Fate
 */
public class RefreshReceiver extends BroadcastReceiver {
 
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Constants.ACTION_NEW_DOWNLOAD)) {
            ArrayList<Integer> idList = new ArrayList<Integer>();
            if (intent.hasExtra(Constants.ID_LIST))
                idList = intent.getIntegerArrayListExtra(Constants.ID_LIST);
            Intent updateintent = new Intent(Constants.ACTION_UPDATE_UI);
            updateintent.putIntegerArrayListExtra(Constants.ID_LIST, idList);
            ApplicationEx.getApp().sendBroadcast(updateintent);
        }
        else if (intent.getAction().equals(Constants.ACTION_REFRESH_EPISODES) &&
                ApplicationEx.epAdapter != null &&
                !ApplicationEx.isMainActive()) {
            ApplicationEx.epAdapter.refreshEpisodes(ApplicationEx.epRead,
                    ApplicationEx.epDownloaded, ApplicationEx.epSortType,
                    ApplicationEx.archive, null, false);
        }
        else if (intent.getAction().equals(Constants.ACTION_REFRESH_FEEDS) &&
                ApplicationEx.feedAdapter != null &&
                !ApplicationEx.isMainActive()) {
            ApplicationEx.feedAdapter.refreshFeeds(ApplicationEx.feedRead,
                    ApplicationEx.feedDownloaded, ApplicationEx.feedSort,
                    ApplicationEx.feedSortType, ApplicationEx.archive);
        }
    }
    
}