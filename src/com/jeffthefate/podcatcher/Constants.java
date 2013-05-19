package com.jeffthefate.podcatcher;

import java.io.File;

import android.content.Context;
import android.text.TextUtils;

public class Constants {
    
    public static final String LOG_TAG = "Podcatcher";
    
    public static final int PAGE_SIZE = 20;
    
    public static final String AUTH_PARAMS = "GoogleLogin auth=";
    public static final String READER_BASE_URL  = 
        "http://www.google.com/reader/";
    public static final String API_URL = TextUtils.concat(READER_BASE_URL, "api/0/").toString();
    public static final String EDIT_TAG_URL = TextUtils.concat(API_URL, "edit-tag").toString();
    public static final String TOKEN_URL = TextUtils.concat(API_URL, "token").toString();
    public static final String STREAM_CONTENTS_URL = TextUtils.concat(API_URL,
        "stream/contents/").toString();
    public static final String STREAM_ITEMS_CONTENTS_URL = TextUtils.concat(
            API_URL, "stream/items/contents").toString();
    public static final String SUBSCRIPTION_EDIT_URL = TextUtils.concat(API_URL,
        "subscription/edit").toString();
    public static final String SUBSCRIPTION_LIST_URL = TextUtils.concat(API_URL,
        "subscription/list?output=json").toString();
    public static final String ITEMS_URL = TextUtils.concat(API_URL,
        "stream/items/ids?output=json&r=o&n=100&includeAllDirectStreamIds=true",
        "&merge=true").toString();
    public static final String ITEM_URL = TextUtils.concat(API_URL, "stream/items/contents?").toString();
    public static final String READ_STATE_STREAMID = 
        "user/-/state/com.google/read";
    
    public static final String DOWNLOAD_LOCATION = TextUtils.concat(
            File.separator, "Podcatcher", File.separator).toString();
    public static final String FEEDS_LOCATION = TextUtils.concat(
            DOWNLOAD_LOCATION, "Feeds", File.separator).toString();
    public static final String TEMP_LOCATION = TextUtils.concat(
            DOWNLOAD_LOCATION, "TEMP", File.separator).toString();
    
    public static final String TEMP_NAME = "temp.xml";
    
    public static final String ITEM_PREFIX = "tag:google.com,2005:reader/item/";
    
    public static final String SUBS_FILENAME = "gReaderSubscriptions.txt";
    public static final String PLAYLIST_FILENAME = "currentPlaylist.txt";
    public static final String UPDATE_LIST_FILENAME = "updatePlaylist.txt";
    public static final String SPEED_LIST_FILENAME = "speedList.txt";
    public static final String GOOGLE_FILENAME = "google.txt";
    public static final String CHECKTOKEN_FILENAME = "checktoken.txt";
    
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
    public static final String ACTION_UPDATE_DOWNLOADS =
        "com.jeffthefate.pod.ACTION_UPDATE_DOWNLOADS";
    public static final String ACTION_UPDATE_PROGRESS =
            "com.jeffthefate.pod.ACTION_UPDATE_PROGRESS";
    public static final String ACTION_CANCEL_DOWNLOAD = 
            "com.jeffthefate.pod.ACTION_CANCEL_DOWNLOAD";
    public static final String ACTION_CANCEL_ALL_DOWNLOADS = 
            "com.jeffthefate.pod.ACTION_CANCEL_ALL_DOWNLOADS";
    public static final String ACTION_REFRESH_FEEDS =
        "com.jeffthefate.pod.ACTION_REFRESH_FEEDS";
    public static final String ACTION_REFRESH_EPISODES =
        "com.jeffthefate.pod.ACTION_REFRESH_EPISODES";
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
    public static final String ACTION_FORWARD_PLAYBACK =
        "com.jeffthefate.pod.ACTION_FORWARD_PLAYBACK";
    public static final String ACTION_BACKWARD_PLAYBACK =
        "com.jeffthefate.pod.ACTION_BACKWARD_PLAYBACK";
    public static final String ACTION_SPEED_ENABLED =
            "com.jeffthefate.pod.ACTION_SPEED_ENABLED";
    public static final String ACTION_SPEED_DISABLED =
            "com.jeffthefate.pod.ACTION_SPEED_DISABLED";
    public static final String ACTION_EMPTY_PLAYLIST =
            "com.jeffthefate.pod.ACTION_EMPTY_PLAYLIST";
    public static final String ACTION_SPEED_FREEZE =
        "com.jeffthefate.pod.ACTION_SPEED_FREEZE";
    public static final String ACTION_SEEK_DONE =
        "com.jeffthefate.pod.ACTION_SEEK_DONE";
    public static final String ACTION_SPEED_CHANGED =
        "com.jeffthefate.pod.ACTION_SPEED_CHANGED";
    public static final String ACTION_SAVE_PROGRESS =
        "com.jeffthefate.pod.ACTION_SAVE_PROGRESS";
    public static final String ACTION_UPDATE_UI =
            "com.jeffthefate.pod.ACTION_UPDATE_UI";
    public static final String ACTION_CANCEL_UPDATE =
            "com.jeffthefate.pod.ACTION_CANCEL_UPDATE";
    public static final String ACTION_UPDATE_SUN_TIMES =
        "com.jeffthefate.pod.ACTION_UPDATE_SUN_TIMES";
    public static final String ACTION_DISABLE_NIGHT =
        "com.jeffthefate.pod.ACTION_DISABLE_NIGHT";
    public static final String ACTION_ENABLE_NIGHT =
        "com.jeffthefate.pod.ACTION_ENABLE_NIGHT";
    
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
    
    public static final int PLAYLIST_BY_NAME = 0;
    public static final int PLAYLIST_BY_DATE = 1;
    
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
    
    public static final int WAIT_MSECS = 10000;
    
    public static final String TITLE = "title";
    
    public static final String NEW_FEED = "New feed: ";
    
    public static final String EQUAL = "=";
    public static final String COLON = ":";
    
    public static final String PRE_HTTP = "http";
    
    public static final String UPDATE_NOTIFY_TITLE = "Updating";
    public static final String REMOVE_NOTIFY_TITLE = "Removing: ";
    
    public static final String AUTH_HEADER = "Authorization";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    
    public static final String CONTENT_TYPE_VALUE =
            "application/x-www-form-urlencoded";
    
    public static final String GOOGLE_USER_AGENT =
            "219396487960.apps.googleusercontent.com";
    
    public static final String FAILED_RESPOND = " failed to respond with code ";
    
    public static final String RANGE = "Range";
    public static final String RANGE_BYTES = "bytes=";
    public static final String RANGE_END = "-";
    
    public static final String PERCENT_OF = "% of ";
    public static final String MB = "MB";
    
    public static final String DOWNLOAD_SERVICE_NAME = "DownloadService";
    public static final String UPDATE_SERVICE_NAME = "UpdateService";
    
    public static final String DOWNLOAD_WAKE_LOCK = "DOWNLOAD WAKE_LOCK";
    public static final String UPDATE_WAKE_LOCK = "UPDATE WAKE_LOCK";
    
    public static final String NOMEDIA = ".nomedia";
    
    public static final String AND_SPACES = " AND ";
    public static final String EQUAL_QUESTION = "=?";
    public static final String ASC = " ASC";
    public static final String DESC = " DESC";
    public static final String EMPTY = "";
    public static final String SPACE = " ";
    public static final String URL_SPACE = "%20";
    public static final String COMMA_SPACE = ", ";
    public static final String SELECT = "SELECT ";
    public static final String FROM = " FROM ";
    public static final String WHERE = " WHERE ";
    public static final String GREATER = ">?";
    
    public static final String MP3 = ".mp3";
    public static final String M4A = ".m4a";
    public static final String MP4 = ".mp4";
    
    public static final String PNG = ".png";
    public static final String JPG = ".jpg";
    public static final String JPEG = ".jpeg";
    public static final String GIF = ".gif";
    
    public static final String PARSE_ID = "2yDyatsYbvoDic4jcxuq8FsZlxEhvjxLRt76xgbe";
    public static final String PARSE_KEY = "PZs58utAYzfnJugnqStwURCKzbPhXNBuNmuDObf6";
    
    public static final String FRAGMENT_DOWNLOADS = "fDownloads";
    public static final String FRAGMENT_ACCOUNT = "fAccount";
    public static final String FRAGMENT_FIRST_START = "fFirstStart";
    
    public static final String DIALOG_DETAILS = "dDetails";
    public static final String DIALOG_ACCOUNT = "dAccount";
    
    public static final String EP_LIST = "epList";
    public static final String CURR_EP_ID = "currEpId";
    public static final String FAILED = "failed";
    public static final String EPISODE = "episode";
    public static final String IMAGE_URL = "imageUrl";
    public static final String URL = "url";
    public static final String START_EP = "startEp";
    public static final String INIT = "init";
    public static final String ACTIVITY = "activity";
    public static final String SYNC = "sync";
    public static final String FEED_LIST = "feedList";
    public static final String ID_LIST = "idList";
    public static final String EPS = "episodes";
    public static final String TERM = "term";
    public static final String VALUES = "values";
    public static final String URLS = "urls";
    public static final String PROGRESS = "progress";
    public static final String STATE = "state";
    public static final String VIDEO = "video";
    public static final String FEED = "feed";
    public static final String UNSUBSCRIBE = "unsubscribe";
    public static final String FORCE = "force";
    
    public static final String CONTENT_LENGTH = "Content-Length";
    
    public static final String US_ASCII = "US-ASCII";
    public static final String UTF8 = "UTF-8";
    
    public static final String ITUNES_DURATION = "itunes|duration";
    public static final String ITUNES_SUMMARY = "itunes|summary";
    public static final String ITUNES_IMAGE = "itunes|image";
    public static final String ITUNES_AUTHOR = "itunes|author";
    public static final String IMAGE_URL_XML = "image > url";
    public static final String CHANNEL_XML = "channel > ";
    public static final String DESCRIPTION = "description";
    public static final String PUB_DATE = "pubDate";
    public static final String LINK = "link";
    public static final String TYPE = "type";
    public static final String LENGTH = "length";
    public static final String LAST_BUILD_DATE = "lastBuildDate";
    public static final String HREF = "href";
    public static final String ITEM = "item";
    public static final String ENCLOSURE = "enclosure";
    public static final String AUDIO = "audio";
    public static final String NULL = "null";
    public static final String GUID = "guid";
    
    public static final String NIGHT_MODE = "Night mode: ";
    public static final String SWIPE_REMOTE = "Swipe remote ";
    public static final String TAP_REMOTE = "Tap remote ";
    public static final String UPDATE_MODE = "Update mode: ";
    public static final String ENABLED = "enabled";
    public static final String DISABLED = "disabled";
    public static final String NEW = "New ";
    public static final String SAME = "Same ";
    public static final String SENSITIVITY = "sensitivity: ";
    
    public static final String EPISODES = " EPISODES";
    
    public static final String NEG_ONE = "-1";
    public static final String ZERO = "0";
    public static final String ONE = "1";
    public static final String TWO = "2";
    public static final String FOUR = "4";
    public static final String EIGHT = "8";
    public static final String TEN = "10";
    public static final String SIXTY = "60";
    public static final String SIX_HUNDRED = "600";
    
    public static final String DEC_FORMAT = "#.##";
    
    public static final String PLUS = "+";
    public static final String X = "x";
    public static final String UNDERSCORE = "_";
    public static final String PIPE = "| ";
    
    public static final String CHOOSE_EPISODE = "Choose episode from a list";
    public static final String NO_DOWNLOADED = "No downloaded episodes";
    
    public static final String SHOW_READ = "Show Read";
    public static final String HIDE_READ = "Hide Read";
    public static final String SHOW_CLOUD = "Show Cloud";
    public static final String HIDE_CLOUD = "Hide Cloud";
    public static final String NEWEST = "Newest";
    public static final String OLDEST = "Oldest";
    public static final String A_Z = "A > Z";
    public static final String Z_A = "Z > A";
    public static final String SORT_BY_LATEST = "Sort By Latest";
    public static final String SORT_BY_NAME = "Sort By Name";
    public static final String NONE = "None";
    
    public static final String YES = "Yes";
    
    public static final String OF = " of ";
    
    public static final String ERROR_GETTING_FEED =
            "Error getting feed, try again";
    public static final String NONE_FOUND = "None found";
    
    public static final String DOWNLOAD_STARTING = "Starting download";
    public static final String DOWNLOADING = "Downloading";
    public static final String DOWNLOAD_PENDING = "Download pending...";
    public static final String DOWNLOAD_FAILED = "Download failed";
    public static final String CANCEL_ALL = "Cancel All";
    
    public static final String VIEW_FAILED = "Touch to view failed episodes";
    public static final String RETRY_FAILED = "Touch to retry";
    public static final String SEARCH_MISSING =
            "Touch to search for a replacement";
    
    public static final String NEXT = "Next:";
    public static final String ELLIPSIS = "...";
    
    public static final String NOTHING = "nothing";
    
    public static final String UNABLE_TO_PLAY = "Unable to play";
    
    public static final String STARTING_UPDATE = "Starting update";
    public static final String UPDATING = "Updating";
    public static final String CANCEL = "Cancel";
    public static final String UPDATE_FAILED = "Update failed";
    public static final String FEED_MISSING = "Feed missing";
    
    public static final String UNABLE_EXTERNAL_STORAGE =
            "Unable to access external storage";
    
    public static final String SMALL = "_small";
    
    public static final String LOG = "Log";
    public static final String ANDROID_VERSION = "androidVersion";
    public static final String APP_PACKAGE = "appPackage";
    public static final String DEVICE_MODEL = "deviceModel";
    public static final String PACKAGE_VERSION = "packageVersion";
    public static final String STACK_TRACE = "stacktrace";
    
    public static final String UTC = "UTC";
    
    public static final String TRACK_NAME = "trackName";
    public static final String ARTIST_NAME = "artistName";
    public static final String ARTWORK_URL_100 = "artworkUrl100";
    public static final String FEED_URL = "feedUrl";
    
    public static final String ANDROID = "Android";
    
    public static final String REGISTER_MEDIA_BUTTON =
            "registerMediaButtonEventReceiver";
    public static final String UNREGISTER_MEDIA_BUTTON =
            "unregisterMediaButtonEventReceiver";
    public static final String REGISTER_REMOTE_CONTROL =
            "registerRemoteControlClient";
    public static final String UNREGISTER_REMOTE_CONTROL =
            "unregisterRemoteControlClient";
    public static final String EDIT_METADATA = "editMetadata";
    public static final String SET_PLAYBACK_STATE = "setPlaybackState";
    public static final String SET_TRANSPORT_CONTROL_FLAGS =
            "setTransportControlFlags";
    public static final String REMOTE_CONTROL_CLIENT =
            "android.media.RemoteControlClient";
    
    public static final String PUT_STRING = "putString";
    public static final String PUT_BITMAP = "putBitmap";
    public static final String PUT_LONG = "putLong";
    public static final String CLEAR = "clear";
    public static final String APPLY = "apply";
    
    public static final String UNREAD_TEXT = "\nUNREAD";
    public static final String DOWNLOADED_TEXT = "\nDOWNLOADED";
    
    public static final String ADDING_FEED = "Adding feed";
    
    public static final String XML = ".xml";
    
    public static final String SIMPLE_DATE = "EEE, dd MMM yyyy kk:mm:ss Z";
    public static final String SIMPLE_DATE_UTC = "EEE, dd MMM yyyy kk:mm:ss";
    public static final String SIMPLE_TODAY = "'Today' kk:mm";
    public static final String SIMPLE_DAY = "EEEE";
    public static final String SIMPLE_MONTH = "dd MMM";
    public static final String SIMPLE_YEAR = "dd MMM yyyy";
    public static final String SIMPLE_END_TIME = "EEE kk:mm";
    public static final String SIMPLE_END_TIME_TODAY = "kk:mm";
    public static final String SIMPLE_FEED_DATE = "EEE, dd MMM yyyy kk:mm";
    
    public static final String NEW_SPACE = " new";
    
    public static final String ERROR = "Error";
    
    public static final String CONNECTION_URL = "http://www.android.com";
    
    public static final String ALPHA = "alpha";
    public static final String SCALE_X = "scaleX";
    public static final String SCALE_Y = "scaleY";
    
    public static final String RESULTS = "results";
    
    /**
     * Type: {@code int}.
     * <p>
     * Night mode to set.
     */
    public static final String BUNDLE_EXTRA_INT_NIGHT_MODE =
        "com.jeffthefate.pod.extra.INT_NIGHT_MODE";
    
    /**
     * Type: {@code int}.
     * <p>
     * Update set.
     */
    public static final String BUNDLE_EXTRA_INT_UPDATE =
        "com.jeffthefate.pod.extra.INT_UPDATE";
    
    /**
     * Type: {@code boolean}.
     * <p>
     * Update set.
     */
    public static final String BUNDLE_EXTRA_BOOLEAN_TAP =
        "com.jeffthefate.pod.extra.BOOLEAN_TAP";
    
    /**
     * Type: {@code boolean}.
     * <p>
     * Update sensitivity.
     */
    public static final String BUNDLE_EXTRA_BOOLEAN_SENSITIVITY =
        "com.jeffthefate.pod.extra.BOOLEAN_SENSITIVITY";
    
    /**
     * Type: {@code int}.
     * <p>
     * Update set.
     */
    public static final String BUNDLE_EXTRA_INT_SENSITIVITY =
        "com.jeffthefate.pod.extra.INT_SENSITIVITY";
    
    /**
     * Type: {@code boolean}.
     * <p>
     * Update set.
     */
    public static final String BUNDLE_EXTRA_BOOLEAN_SWIPE =
        "com.jeffthefate.pod.extra.BOOLEAN_SWIPE";
    
    /**
     * Type: {@code int}.
     * <p>
     * versionCode of the plug-in that saved the Bundle.
     */
    /*
     * This extra is not strictly required, however it makes backward and forward compatibility significantly
     * easier. For example, suppose a bug is found in how some version of the plug-in stored its Bundle. By
     * having the version, the plug-in can better detect when such bugs occur.
     */
    public static final String BUNDLE_EXTRA_INT_VERSION_CODE =
            "com.jeffthefate.pod.extra.INT_VERSION_CODE"; //$NON-NLS-1$

    
    public static int getVersionCode(final Context context)
    {
        if (null == context)
            throw new IllegalArgumentException("context cannot be null"); //$NON-NLS-1$
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (final UnsupportedOperationException e) {
            /*
             * This exception is thrown by test contexts
             */
            return 1;
        }
        catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Private constructor prevents instantiation.
     *
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private Constants()
    {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
    
}
