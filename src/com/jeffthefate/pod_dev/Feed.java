package com.jeffthefate.pod_dev;

import java.util.ArrayList;

public class Feed extends Podcast {
    
    private ArrayList<Episode> mEpisodes;
    
    public Feed(int id, String title, String subtitle, boolean unread,
            boolean downloaded, String url, ArrayList<Episode> episodes) {
        super(id, title, subtitle, -1, unread, downloaded, url);
        mEpisodes = episodes;
    }
    
    public ArrayList<Episode> getEpisodes() {
        return mEpisodes;
    }

}
