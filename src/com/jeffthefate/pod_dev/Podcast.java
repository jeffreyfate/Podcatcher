package com.jeffthefate.pod_dev;

public class Podcast {

    private int mId;
    private String mTitle;
    private String mSubtitle;
    private long mPub;
    private String mUrl;
    private boolean mUnread;
    private boolean mDownloaded;
    
    public Podcast(int id, String title, String feed, long pub, boolean unread,
            boolean downloaded, String url) {
        mId = id;
        mTitle = title;
        mSubtitle = feed;
        mPub = pub;
        mUnread = unread;
        mDownloaded = downloaded;
        mUrl = url;
    }

    public int getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }
    
    public String getSubtitle() {
        return mSubtitle;
    }

    public long getPub() {
        return mPub;
    }
    
    public String getUrl() {
        return mUrl;
    }
    
    public boolean getUnread() {
        return mUnread;
    }
    
    public boolean getDownloaded() {
        return mDownloaded;
    }
    
}
