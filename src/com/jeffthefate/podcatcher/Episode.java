package com.jeffthefate.podcatcher;

public class Episode extends Podcast {
    
    int duration = 0;
    boolean locked = false;
    int feedId;
    
    public Episode(int id, String title, String feed, long pub, boolean unread,
            boolean downloaded, String url, int duration, boolean locked,
            int feedId) {
        super(id, title, feed, pub, unread, downloaded, url);
        this.duration = duration;
        this.locked = locked;
        this.feedId = feedId;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public boolean getLocked() {
        return locked;
    }
    
    public int getFeedId() {
        return feedId;
    }

}
