package com.jeffthefate.pod_dev;

public class Episode extends Podcast {
    
    int duration = 0;
    
    public Episode(int id, String title, String feed, long pub, boolean unread,
            boolean downloaded, String url, int duration) {
        super(id, title, feed, pub, unread, downloaded, url);
        this.duration = duration;
    }
    
    public int getDuration() {
        return duration;
    }

}
