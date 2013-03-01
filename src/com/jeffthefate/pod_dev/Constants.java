package com.jeffthefate.pod_dev;

import java.io.File;

public class Constants {
    
    public static final String LOG_TAG = "Podcatcher";
    
    public static final String AUTH_PARAMS = "GoogleLogin auth=";
    public static final String READER_BASE_URL  = 
        "http://www.google.com/reader/";
    public static final String API_URL = READER_BASE_URL + "api/0/";
    public static final String EDIT_TAG_URL = API_URL + "edit-tag";
    public static final String TOKEN_URL = API_URL + "token";
    public static final String STREAM_CONTENTS_URL = API_URL + 
        "stream/contents/";
    public static final String STREAM_ITEMS_CONTENTS_URL = API_URL + 
        "stream/items/contents";
    public static final String SUBSCRIPTION_EDIT_URL = API_URL +
        "subscription/edit";
    public static final String SUBSCRIPTION_LIST_URL = API_URL + 
        "subscription/list?output=json";
    public static final String FEED_URL = API_URL + 
        "stream/items/ids?output=json&r=o&n=100&includeAllDirectStreamIds=true"
        + "&merge=true";
    public static final String ITEM_URL = API_URL + "stream/items/contents?";
    public static final String READ_STATE_STREAMID = 
        "user/-/state/com.google/read";
    
    public static final String DOWNLOAD_LOCATION = File.separator +
        "Podcatcher" + File.separator;
    public static final String FEEDS_LOCATION = DOWNLOAD_LOCATION + "Feeds" + 
        File.separator;
    public static final String TEMP_LOCATION = DOWNLOAD_LOCATION + "TEMP" + 
    File.separator;
    
    public static final String ITEM_PREFIX = "tag:google.com,2005:reader/item/";
    
    public static final String SUBS_FILENAME = "gReaderSubscriptions.txt";
    public static final String PLAYLIST_FILENAME = "currentPlaylist.txt";
    public static final String UPDATE_LIST_FILENAME = "updatePlaylist.txt";
    public static final String SPEED_LIST_FILENAME = "speedList.txt";
    public static final String GOOGLE_FILENAME = "google.txt";
    public static final String CHECKTOKEN_FILENAME = "checktoken.txt";
    public static final String SPEED_FILENAME = "speed.txt";
    public static final String MAGICTIME_FILENAME = "magictime.txt";
    
    public static final int IMAGE_UPDATE_INTERVAL = 1209600000;
    
    public static final String ACCOUNT_TYPE = "com.google";
    
    public static final int REQUEST_CODE_FIRST_START = 1;
    public static final int REQUEST_CODE_CHOOSE_ACCOUNT = 2;
    public static final int REQUEST_CODE_AUTHORIZE_ACCOUNT = 3;
    
    public static final int RESULT_FIRST_START = -3;
    
    public static final String MPEG_TYPE = "audio/mpeg";
    public static final String M4A_TYPE = "audio/x-m4a";
    public static final String MP3_TYPE_1 = "audio/x-mp3";
    public static final String MP3_TYPE_2 = "audio/mp3";
    public static final String MP4_TYPE = "video/mp4";
    
    public static final int TO_DOWNLOAD = 1;
    public static final int DOWNLOADED = 2;
    
    public static final int REASON_CONNECTION = 0;
    public static final int REASON_WIFI = 1;
    public static final int REASON_SIZE = 2;
    public static final int REASON_USER = 3;
    
    public static final int READ = 1;
    public static final int UNREAD = 2;
    
    public static final String ACTION_CHECK_CONNECTION = 
        "com.jeffthefate.pod.ACTION_CHECK_CONNECTION";
    public static final String ACTION_NEW_DOWNLOAD = 
        "com.jeffthefate.pod.ACTION_NEW_DOWNLOAD";
    public static final String ACTION_START_DOWNLOAD = 
        "com.jeffthefate.pod.ACTION_START_DOWNLOAD";
    public static final String ACTION_REMOVE_DOWNLOAD = 
        "com.jeffthefate.pod.ACTION_REMOVE_DOWNLOAD";
    public static final String ACTION_REFRESH_FEEDS =
        "com.jeffthefate.pod.ACTION_NEW_FEED";
    public static final String ACTION_REFRESH_EPISODES =
        "com.jeffthefate.pod.ACTION_NEW_EPISODE";
    public static final String ACTION_REFRESH_DURATION =
        "com.jeffthefate.pod.ACTION_DURATION";
    public static final String ACTION_IMAGE_CHANGE = 
        "com.jeffthefate.pod.ACTION_IMAGE_CHANGE";
    public static final String ACTION_UPDATE_NOTIFICATION =
        "com.jeffthefate.pod.ACTION_UPDATE_NOTIFICATION";
    public static final String ACTION_START_PLAYBACK =
        "com.jeffthefate.pod.ACTION_START_PLAYBACK";
    public static final String ACTION_PAUSE_PLAYBACK =
        "com.jeffthefate.pod.ACTION_PAUSE_PLAYBACK";
    public static final String ACTION_SPEED_FREEZE =
        "com.jeffthefate.pod.ACTION_SPEED_FREEZE";
    public static final String ACTION_SPEED_CHANGED =
        "com.jeffthefate.pod.ACTION_SPEED_CHANGED";
    public static final String ACTION_SAVE_PROGRESS =
        "com.jeffthefate.pod.ACTION_SAVE_PROGRESS";
    
    public static final String SEARCH_URL_1 = 
        "http://itunes.apple.com/search?term=";
    public static final String SEARCH_URL_2 = "&country=";
    public static final String SEARCH_URL_3 = 
        "&media=podcast&entity=podcast&limit=";
    public static final String SEARCH_URL_4 = "&lang=";
    public static final String SEARCH_URL_5 = "&version=2&explicit=";
    
    public static final int PLAYLIST_MANUAL = -1;
    public static final int PLAYLIST_UNREAD = 1;
    public static final int PLAYLIST_READ = 2;
    public static final int PLAYLIST_STREAM = 4;
    
    public static final int PLAYLIST_SORT_MANUAL = -1;
    public static final int PLAYLIST_SORT_FEED = 1;
    public static final int PLAYLIST_SORT_EPISODE = 2;
    public static final int PLAYLIST_SORT_DATE = 4;
    
    public static final int PLAYLIST_SORT_TYPE_ASC = 1;
    public static final int PLAYLIST_SORT_TYPE_DESC = 2;
    
    public static final long SECOND_MILLI = 1000;
    public static final long MINUTE_MILLI = 60 * SECOND_MILLI;
    public static final long HOUR_MILLI = 60 * MINUTE_MILLI;
    public static final long DAY_MILLI = 24 * HOUR_MILLI;
    public static final long WEEK_MILLI = 7 * DAY_MILLI;
    
    public static final String FEEDS_TAB = "Feeds";
    public static final String EPISODES_TAB = "Episodes";
    
    public static final String NO_GOOGLE = "NO_GOOGLE";
    
    public static final long PLUS_MULTI = 90000000000000l;
    public static final long PLUS_DAY_OF_WEEK = 10000000000000l;
    
    public static final int HOLO_BLUE = 0xFF2FA8D5;
    
}
