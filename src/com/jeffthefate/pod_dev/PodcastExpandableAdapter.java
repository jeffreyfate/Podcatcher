package com.jeffthefate.pod_dev;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.RejectedExecutionException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jeffthefate.pod_dev.service.DownloadService;
import com.jeffthefate.pod_dev.service.PlaybackService;
import com.jeffthefate.pod_dev.service.UpdateService;

public class PodcastExpandableAdapter extends BaseExpandableListAdapter {
    
    private Context mContext;
    private ArrayList<Feed> mPodcastFeeds = new ArrayList<Feed>();
    private ArrayList<Integer> epIdList = new ArrayList<Integer>();
    
    private AdapterChange callback;
    
    boolean updateImage = true;
    boolean mSync = false;
    
    private ViewHolder feedHolder;
    private ViewHolder episodeHolder;
    
    private static Bitmap temp;
    private static Resources resources;
    
    private int contextId;
    private boolean contextRead;
    private boolean contextDownloaded;
    private boolean contextFeed;
    private LinearLayout contextPlaying;
    private ImageView contextDownload;
    private ProgressBar contextBusy;
    
    private final int TASK_GET_IMAGE = 0;
    private final int TASK_MARK_FEED_READ = 1;
    private final int TASK_MARK_EPISODE_READ = 2;
    private final int TASK_REFRESH_FEEDS = 3;
    private final int TASK_UNSUBSCRIBE_FEED = 4;
    private final int TASK_DOWNLOAD_EPISODE = 5;
    private final int TASK_DELETE_EPISODE = 6;
    private final int TASK_GET_PODCASTS = 7;
    
    private ImageView leftPlayingEp;
    private ImageView rightPlayingEp;
    private ImageView leftPlayingFeed;
    private ImageView rightPlayingFeed;
    
    private LinearLayout playingEpisode;
    private LinearLayout playingFeed;
    
    protected static class ViewHolder {
        TextView text1;
        TextView text2;
        ImageView icon;
        RelativeLayoutEx button;
        LinearLayout playing;
        ImageView playingleft;
        ImageView playingright;
        ImageView download;
        ProgressBar busy;
        int id;
    }
    
    public interface AdapterChange {
        public void onAdapterEmptyChange();
        public void onAdapterProgressChange();
        public void onAdapterEmptyTextChange(String text);
    }
    
    public PodcastExpandableAdapter(Context context, int read, int downloaded,
            int sort, int sortType, int archive, boolean sync,
            AdapterChange callback) {
        mContext = context;
        mSync = sync;
        this.callback = callback;
        temp = BitmapFactory.decodeResource(resources, 
                R.drawable.ic_album_loading);
        resources = mContext.getResources();
        Task task = new Task(null, false, -1, null, read, downloaded, sort, 
                sortType, archive, TASK_GET_PODCASTS);
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                task.execute();
            else
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (RejectedExecutionException e) {}
    }
    
    @Override
    public int getGroupCount() {
        if (mPodcastFeeds != null)
            return mPodcastFeeds.size();
        return 0;
    }

    @Override
    public Object getGroup(int position) {
        if (mPodcastFeeds != null)
            return mPodcastFeeds.get(position);
        return null;
    }

    @Override
    public long getGroupId(int position) {
        if (mPodcastFeeds != null && position < mPodcastFeeds.size())
            return (long) mPodcastFeeds.get(position).getId();
        return -1;
    }
    
    @Override
    public Object getChild(int groupPosition, int childPosition) {
        if (mPodcastFeeds != null && groupPosition < mPodcastFeeds.size())
            return mPodcastFeeds.get(groupPosition).getEpisodes()
                    .get(childPosition);
        return null;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return ((Episode) getChild(groupPosition, childPosition)).getId();
    }
    
    public void startFeedPlaying() {
        if (playingFeed != null)
            playingFeed.setVisibility(View.VISIBLE);
    }
    
    public void stopFeedPlaying() {
        if (playingFeed != null)
            playingFeed.setVisibility(View.INVISIBLE);
    }
    
    public void startEpisodePlaying() {
        if (playingEpisode != null)
            playingEpisode.setVisibility(View.VISIBLE);
    }
    
    public void stopEpisodePlaying() {
        if (playingEpisode != null)
            playingEpisode.setVisibility(View.INVISIBLE);
    }
    
    public void startLeftPlaying() {
        if (leftPlayingEp != null)
            ((AnimationDrawable) leftPlayingEp.getBackground()).start();
        if (leftPlayingFeed != null)
            ((AnimationDrawable) leftPlayingFeed.getBackground()).start();
    }
    
    public void stopLeftPlaying() {
        if (leftPlayingEp != null)
            ((AnimationDrawable) leftPlayingEp.getBackground()).stop();
        if (leftPlayingFeed != null)
            ((AnimationDrawable) leftPlayingFeed.getBackground()).stop();
    }
    
    public void startRightPlaying() {
        if (rightPlayingEp != null)
            ((AnimationDrawable) rightPlayingEp.getBackground()).start();
        if (rightPlayingFeed != null)
            ((AnimationDrawable) rightPlayingFeed.getBackground()).start();
    }
    
    public void stopRightPlaying() {
        if (rightPlayingEp != null)
            ((AnimationDrawable) rightPlayingEp.getBackground()).stop();
        if (rightPlayingFeed != null)
            ((AnimationDrawable) rightPlayingFeed.getBackground()).stop();
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
            boolean isLastChild, View convertView, ViewGroup parent) {
        final int groupPos = groupPosition;
        final int childPos = childPosition;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.row_noimage, parent, false);
            episodeHolder = new ViewHolder();
            episodeHolder.text1 = (TextView)convertView.findViewById(
                    R.id.text1);
            episodeHolder.text2 = (TextView)convertView.findViewById(
                    R.id.text2);
            episodeHolder.playing = 
                    (LinearLayout)convertView.findViewById(R.id.playing);
            episodeHolder.playingleft = 
                    (ImageView)convertView.findViewById(R.id.playingleft);
            episodeHolder.playingright = 
                    (ImageView)convertView.findViewById(R.id.playingright);
            episodeHolder.download =
                (ImageView)convertView.findViewById(R.id.download);
            episodeHolder.busy =
                (ProgressBar)convertView.findViewById(R.id.busy);
            episodeHolder.button = (RelativeLayoutEx)convertView.findViewById(
                    R.id.ButtonLayout);
            episodeHolder.id = (int) getChildId(groupPosition, childPosition);
            convertView.setTag(episodeHolder);
        }
        else {
            episodeHolder = (ViewHolder)convertView.getTag();
            if (episodeHolder.id != (int) getChildId(groupPosition,
                    childPosition)) {
                episodeHolder.id = (int) getChildId(groupPosition,
                        childPosition);
            }
        }
        episodeHolder.text1.setTag(childPosition);
        episodeHolder.text1.setText(Jsoup.parse(StringEscapeUtils.unescapeHtml4(
                getEpisodeTitle(groupPosition, childPosition) == null ? "" : 
                    getEpisodeTitle(groupPosition, childPosition))).text());
        if (getEpisodeRead(groupPosition, childPosition))
            episodeHolder.text1.setTextColor(Color.WHITE);
        else
            episodeHolder.text1.setTextColor(Color.GRAY);
        episodeHolder.text2.setTag(childPosition);
        episodeHolder.text2.setText(((Episode) getChild(
                groupPosition, childPosition)).getSubtitle());
        convertView.setBackgroundResource(R.drawable.color_btn_context_menu);
        convertView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchEpisodesAction(groupPos, childPos);
            }
        });
        episodeHolder.button.setBackgroundResource(
                R.drawable.color_btn_context_menu);
        episodeHolder.button.setTag(childPosition);
        episodeHolder.button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (childPos <
                        mPodcastFeeds.get(groupPos).getEpisodes().size()) {
                    contextId = (int) getChildId(groupPos, childPos);
                    contextRead = getEpisodeRead(groupPos, childPos);
                    contextDownloaded = getEpisodeDownloaded(
                            groupPos, childPos);
                    contextFeed = false;
                    contextPlaying = episodeHolder.playing;
                    contextDownload = episodeHolder.download;
                    contextBusy = episodeHolder.busy;
                    view.performLongClick();
                }
            }
        });
        episodeHolder.playingleft.setBackgroundResource(R.anim.playing_left);
        episodeHolder.playingright.setBackgroundResource(R.anim.playing_right);
        if (ApplicationEx.dbHelper.getCurrentPlaylistType() == 0
                && ApplicationEx.dbHelper.getCurrentEpisode() == 
                ((Episode) getChild(groupPosition, childPosition)).getId()) {
            playingEpisode = episodeHolder.playing;
            leftPlayingEp = episodeHolder.playingleft;
            rightPlayingEp = episodeHolder.playingright;
            episodeHolder.playing.setVisibility(View.VISIBLE);
            episodeHolder.download.setVisibility(View.INVISIBLE);
            if (ApplicationEx.getIsPlaying()) {
                ((AnimationDrawable) episodeHolder.playingleft.getBackground())
                        .start();
                ((AnimationDrawable) episodeHolder.playingright.getBackground())
                        .start();
            }
            else {
                ((AnimationDrawable) episodeHolder.playingleft.getBackground())
                        .stop();
                ((AnimationDrawable) episodeHolder.playingright.getBackground())
                        .stop();
            }
        }
        else {
            ((AnimationDrawable) episodeHolder.playingleft.getBackground())
                    .stop();
            ((AnimationDrawable) episodeHolder.playingright.getBackground())
                    .stop();
            episodeHolder.playing.setVisibility(View.INVISIBLE);
            if (((Episode) getChild(groupPosition, childPosition))
                    .getDownloaded())
                episodeHolder.download.setVisibility(View.VISIBLE);
            else
                episodeHolder.download.setVisibility(View.INVISIBLE);
        }
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if (mPodcastFeeds != null && groupPosition < mPodcastFeeds.size())
            return mPodcastFeeds.get(groupPosition).getEpisodes().size();
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, 
            View convertView, ViewGroup parent) {
        final int pos = groupPosition;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.row_feed, parent, false);
            feedHolder = new ViewHolder();
            feedHolder.text1 = (TextView)convertView.findViewById(R.id.text1);
            feedHolder.text2 = (TextView)convertView.findViewById(R.id.text2);
            feedHolder.icon = (ImageView)convertView.findViewById(R.id.image);
            feedHolder.playing = 
                    (LinearLayout)convertView.findViewById(R.id.playing);
            feedHolder.playingleft = 
                    (ImageView)convertView.findViewById(R.id.playingleft);
            feedHolder.playingright = 
                    (ImageView)convertView.findViewById(R.id.playingright);
            feedHolder.download =
                (ImageView)convertView.findViewById(R.id.download);
            feedHolder.busy =
                (ProgressBar)convertView.findViewById(R.id.busy);
            feedHolder.button = (RelativeLayoutEx)convertView.findViewById(
                    R.id.ButtonLayout);
            feedHolder.id = (int) getGroupId(groupPosition);
            convertView.setTag(feedHolder);
            updateImage = true;
        }
        else {
            feedHolder = (ViewHolder)convertView.getTag();
            if (feedHolder.id != (int) getGroupId(groupPosition)) {
                updateImage = true;
                feedHolder.id = (int) getGroupId(groupPosition);
            }
        }
        feedHolder.text1.setTag(groupPosition);
        feedHolder.text1.setText(Jsoup.parse(StringEscapeUtils.unescapeHtml4(
                getFeedTitle(groupPosition) == null ? "" : 
                    getFeedTitle(groupPosition))).text());
        if (getFeedRead(groupPosition))
            feedHolder.text1.setTextColor(Color.WHITE);
        else
            feedHolder.text1.setTextColor(Color.GRAY);
        feedHolder.text2.setTag(groupPosition);
        feedHolder.text2.setText(getNumUnread(groupPosition));
        final String url = getUrl(groupPosition);
        if (updateImage && cancelPotentialImage(url, feedHolder.icon)) {
            Task task = new Task(feedHolder.icon, false, -1, url, -1, -1, -1, 
                    -1, -1, TASK_GET_IMAGE);
            TempDrawable bitmapDrawable = new TempDrawable(task);
            feedHolder.icon.setImageDrawable(bitmapDrawable);
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    task.execute();
                else
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } catch (RejectedExecutionException e) {}
        }
        //convertView.setBackgroundResource(R.drawable.color_btn_context_menu);
        feedHolder.button.setBackgroundResource(
                R.drawable.color_btn_context_menu);
        feedHolder.button.setTag(groupPosition);
        feedHolder.button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pos < mPodcastFeeds.size()) {
                    contextRead = getFeedRead(pos);
                    contextDownloaded = getFeedDownloaded(pos);
                    contextFeed = true;
                    contextPlaying = feedHolder.playing;
                    contextDownload = feedHolder.download;
                    contextBusy = feedHolder.busy;
                    view.performLongClick();
                }
            }
        });
        feedHolder.playingleft.setBackgroundResource(R.anim.playing_left);
        feedHolder.playingright.setBackgroundResource(R.anim.playing_right);
        if (ApplicationEx.dbHelper.getCurrentPlaylistType() == 0) {
            int currEp = ApplicationEx.dbHelper.getCurrentEpisode();
            boolean currFeed = false;
            for (int i = 0; i < getChildrenCount(groupPosition); i++) {
                if ((int) getChildId(groupPosition, i) == currEp)
                    currFeed = true;
            }
            if (currFeed) {
                playingFeed = feedHolder.playing;
                leftPlayingFeed = feedHolder.playingleft;
                rightPlayingFeed = feedHolder.playingright;
                feedHolder.playing.setVisibility(View.VISIBLE);
                feedHolder.download.setVisibility(View.INVISIBLE);
                if (ApplicationEx.getIsPlaying()) {
                    ((AnimationDrawable) feedHolder.playingleft.getBackground())
                            .start();
                    ((AnimationDrawable) feedHolder.playingright
                            .getBackground()).start();
                }
                else {
                    ((AnimationDrawable) feedHolder.playingleft.getBackground())
                            .stop();
                    ((AnimationDrawable) feedHolder.playingright
                            .getBackground()).stop();
                }
            }
            else {
                ((AnimationDrawable) feedHolder.playingleft.getBackground())
                        .stop();
                ((AnimationDrawable) feedHolder.playingright.getBackground())
                        .stop();
                feedHolder.playing.setVisibility(View.INVISIBLE);
            }
        }
        return convertView;
    }
    
    public boolean getContextRead() {
        return contextRead;
    }
    
    public boolean getContextDownloaded() {
        return contextDownloaded;
    }
    
    public boolean getContextFeed() {
        return contextFeed;
    }
    
    public String getFeedUrl(int position) {
        int id = (int) getGroupId(position);
        return ApplicationEx.dbHelper.getString(id, 
                DatabaseHelper.COL_FEED_ADDRESS, DatabaseHelper.FEED_TABLE, 
                DatabaseHelper.COL_FEED_ID);
    }
    
    public void updateFeed(int groupPosition) {
        Intent updateIntent = new Intent(ApplicationEx.getApp(), 
                UpdateService.class);
        updateIntent.putExtra("sync", ApplicationEx.isSyncing() &&
                Util.readBooleanFromFile(Constants.GOOGLE_FILENAME));
        updateIntent.putExtra("feed", getFeedUrl(groupPosition));
        ApplicationEx.getApp().startService(updateIntent);
    }
    
    public void refreshFeeds(int readType, int downloaded, int sort,
            int sortType, int archive) {
        Task task = new Task(null, false, -1, "", readType, downloaded, sort,
                sortType, archive, TASK_REFRESH_FEEDS);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    public void markAllRead(boolean read, int readType, int downloaded, 
            int sort, int sortType, int groupPosition, int archive) {
        int id = (int) getGroupId(groupPosition);
        if (ApplicationEx.dbHelper.isFeedRead(id, read)) {
            Task task = new Task(null, read, id, "", readType, downloaded, sort, 
                    sortType, archive, TASK_MARK_FEED_READ);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                task.execute();
            else
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
    
    public void markEpisodeRead(boolean read, int readType, int downloaded, 
            int sort, int sortType, int childPosition, int groupPosition,
            int archive) {
        int id = (int) getChildId(groupPosition, childPosition);
        Task task = new Task(null, read, id, "", readType, downloaded, sort,
                sortType, archive, TASK_MARK_EPISODE_READ);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    public void unsubscribe(int readType, int downloaded, int sort, 
            int sortType, int groupPosition, int archive) {
        int id = (int) getGroupId(groupPosition);
        Task task = new Task(null, false, id, "", readType, downloaded, sort,
                sortType, archive, TASK_UNSUBSCRIBE_FEED);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    public void deleteEpisode(int readType, int downloaded, int sort, 
            int sortType, int childPosition, int groupPosition, int archive) {
        int id = (int) getChildId(groupPosition, childPosition);
        Task task = new Task(null, false, id, null, readType, downloaded, sort,
                        sortType, archive, TASK_DELETE_EPISODE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    public void downloadEpisode(int childPosition, int groupPosition) {
        int id = (int) getChildId(groupPosition, childPosition);
        String url = ApplicationEx.dbHelper.getEpisodeUrl(id);
        Task task = new Task(null, false, id, url, -1, -1, -1, -1, -1,
                        TASK_DOWNLOAD_EPISODE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    public void playEpisode(int childPosition, int groupPosition) {
        final ArrayList<Integer> epIdList = new ArrayList<Integer>();
        for (int i = 0; i < getChildrenCount(groupPosition); i++) {
            epIdList.add((int) getChildId(groupPosition, i));
        }
        final int epId = (int) getChildId(groupPosition, childPosition);
        if (ApplicationEx.dbHelper.isDownloaded(epId)) {
            Thread t = new Thread(){
                public void run(){
                    Intent playbackIntent = new Intent(mContext,
                            PlaybackService.class);
                    playbackIntent.putExtra("startEp", epId);
                    playbackIntent.putIntegerArrayListExtra("episodes", 
                            epIdList);
                    mContext.startService(playbackIntent);
                }
            };
            t.start();
            mContext.sendBroadcast(new Intent(Constants.ACTION_START_PLAYBACK));
        }
    }
    
    private void refreshEpisodeList() {
        epIdList.clear();
        int childId = -1;
        for (int i = 0; i < getGroupCount(); i++) {
            for (int j = 0; j < getChildrenCount(i); j++) {
                childId = (int) getChildId(i, j);
                // TODO Add ability to stream episodes here
                if (ApplicationEx.dbHelper.isDownloaded(childId))
                    epIdList.add(childId);
            }
        }
    }
    
    public void dispatchEpisodesAction(int groupPosition, int childPosition) {
        String epUrl = ((Episode) getChild(groupPosition, childPosition))
                .getUrl();
        refreshEpisodeList();
        final int epId = (int) getChildId(groupPosition, childPosition);
        if (ApplicationEx.dbHelper.isDownloaded(epId)) {
            Thread t = new Thread(){
                public void run(){
                    Intent playbackIntent = new Intent(mContext,
                            PlaybackService.class);
                    playbackIntent.putExtra("startEp", epId);
                    playbackIntent.putIntegerArrayListExtra("episodes", 
                            epIdList);
                    mContext.startService(playbackIntent);
                    ApplicationEx.dbHelper.setCurrentPlaylistType(0);
                }
            };
            t.start();
            mContext.sendBroadcast(new Intent(Constants.ACTION_START_PLAYBACK));
        }
        else if (Util.isMyServiceRunning(DownloadService.class.getName())) {
            Intent intent = new Intent(Constants.ACTION_START_DOWNLOAD);
            intent.putExtra("urls", new String[]{epUrl});
            mContext.sendBroadcast(intent);
        }
        else {
            Intent intent = 
                new Intent(ApplicationEx.getApp(), DownloadService.class);
            intent.putExtra("urls", new String[]{epUrl});
            mContext.startService(intent);
        }
    }
    
    private static boolean cancelPotentialImage(String feedUrl, 
            ImageView imageView) {
        Task bitmapImageTask = getBitmapImageTask(imageView);

        if (bitmapImageTask != null) {
            String bitmapUrl = bitmapImageTask.url;
            if (bitmapUrl == null || (!bitmapUrl.equals(feedUrl))) {
                bitmapImageTask.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }
    
    public void setEmptyText(int read, int downloaded, boolean isFeed) {
        if(getGroupCount() < 1) {
            if (ApplicationEx.dbHelper.getFeedCount() < 1) {
                if (mSync)
                    callback.onAdapterEmptyTextChange(ApplicationEx.getApp()
                            .getString(R.string.emptyFeedsSync));
                else
                    callback.onAdapterEmptyTextChange(ApplicationEx.getApp()
                            .getString(R.string.emptyFeeds));
            }
            else {
                String text = "";
                if (isFeed)
                    text = mContext.getString(R.string.emptyFeedsFiltered);
                else
                    text = mContext.getString(R.string.emptyEpisodes);
                if (read == Constants.UNREAD)
                    text = text.concat("\nUNREAD");
                if (downloaded == Constants.DOWNLOADED)
                    text = text.concat("\nDOWNLOADED");
                callback.onAdapterEmptyTextChange(text);
            }
        }
    }
    
    class Task extends AsyncTask<Void, Void, Void> {

        private String url;
        private final WeakReference<ImageView> imageViewReference;
        boolean read;
        int id;
        
        private Bitmap bitmap;
        
        private int readType;
        private int downloaded;
        private int sort;
        private int sortType;
        private int archive;
        private int task;

        public Task(ImageView imageView, boolean read, int id, String url, 
                int readType, int downloaded, int sort, int sortType,
                int archive, int task) {
            imageViewReference = new WeakReference<ImageView>(imageView);
            this.read = read;
            this.id = id;
            this.url = url;
            this.readType = readType;
            this.downloaded = downloaded;
            this.sort = sort;
            this.sortType = sortType;
            this.archive = archive;
            this.task = task;
        }
        
        @Override
        protected void onPreExecute() {
            callback.onAdapterEmptyChange();
            callback.onAdapterProgressChange();
            switch(task) {
            case TASK_GET_PODCASTS:
            case TASK_REFRESH_FEEDS:
                updateData(new ArrayList<Feed>());
                break;
            case TASK_MARK_FEED_READ:
                contextBusy.setVisibility(View.VISIBLE);
                contextPlaying.setVisibility(View.INVISIBLE);
                contextDownload.setVisibility(View.INVISIBLE);
                break;
            }
        }
        
        @Override
        protected Void doInBackground(Void... nothing) {
            switch(task) {
            case TASK_GET_IMAGE:
                bitmap = getBitmap(url);
                break;
            case TASK_MARK_FEED_READ:
                ArrayList<Integer> epIdList = 
                    ApplicationEx.dbHelper.markFeedEpisodesRead(id, read, 
                            archive);
                Util.deleteFeedEpisodes(id, epIdList);
                refresh();
                break;
            case TASK_MARK_EPISODE_READ:
                ApplicationEx.dbHelper.markRead(id, read);
                if (ApplicationEx.dbHelper.isDownloaded(id)) {
                    new File(ApplicationEx.dbHelper.getEpisodeLocation(id))
                            .delete();
                    ApplicationEx.dbHelper.episodeDownloaded(id, false);
                }
                refresh();
                break;
            case TASK_DOWNLOAD_EPISODE:
                if (!ApplicationEx.dbHelper.isDownloaded(id)) {
                    if (Util.isMyServiceRunning(
                            DownloadService.class.getName())) {
                        Intent intent = new Intent(
                                Constants.ACTION_START_DOWNLOAD);
                        intent.putExtra("urls", new String[]{url});
                        mContext.sendBroadcast(intent);
                    }
                    else {
                        Intent intent = new Intent(mContext,
                                DownloadService.class);
                        intent.putExtra("urls", new String[]{url});
                        mContext.startService(intent);
                    }
                }
                break;
            case TASK_UNSUBSCRIBE_FEED:
                url = ApplicationEx.dbHelper.getString(id, 
                        DatabaseHelper.COL_FEED_ADDRESS, 
                        DatabaseHelper.FEED_TABLE, DatabaseHelper.COL_FEED_ID);
                String title = ApplicationEx.dbHelper.getString(id, 
                        DatabaseHelper.COL_FEED_TITLE, 
                        DatabaseHelper.FEED_TABLE, DatabaseHelper.COL_FEED_ID);
                Util.deleteRecursive(new File(ApplicationEx.cacheLocation +
                        Constants.FEEDS_LOCATION + id + File.separator));
                if (mSync) {
                    Intent intent = new Intent(ApplicationEx.getApp(), 
                            UpdateService.class);
                    intent.putExtra("sync", mSync);
                    intent.putExtra("feed", url);
                    intent.putExtra("title", title);
                    intent.putExtra("unsubscribe", true);
                    ApplicationEx.getApp().startService(intent);
                }
                else
                    ApplicationEx.dbHelper.deleteFeed(id);
                refresh();
                break;
            case TASK_DELETE_EPISODE:
                contextBusy.setVisibility(View.VISIBLE);
                contextPlaying.setVisibility(View.INVISIBLE);
                contextDownload.setVisibility(View.INVISIBLE);
                if (ApplicationEx.dbHelper.isDownloaded(id)) {
                    ApplicationEx.dbHelper.markRead(id, true);
                    new File(ApplicationEx.dbHelper.getEpisodeLocation(id))
                            .delete();
                    ApplicationEx.dbHelper.episodeDownloaded(id, false);
                }
                refresh();
                break;
            case TASK_REFRESH_FEEDS:
            case TASK_GET_PODCASTS:
                refresh();
                break;
            }
            return null;
        }

        protected void onPostExecute(Void nothing) {
            switch(task) {
            case TASK_GET_IMAGE:
                if (imageViewReference != null) {
                    ImageView imageView = imageViewReference.get();
                    Task bitmapImageTask = getBitmapImageTask(imageView);
                    if (this == bitmapImageTask && bitmap != null)
                        imageView.setImageBitmap(bitmap);
                }
                break;
            case TASK_DOWNLOAD_EPISODE:
                break;
            case TASK_MARK_FEED_READ:
            case TASK_MARK_EPISODE_READ:
            case TASK_UNSUBSCRIBE_FEED:
            case TASK_DELETE_EPISODE:
                update();
                mContext.sendBroadcast(
                        new Intent(Constants.ACTION_REFRESH_EPISODES));
                contextBusy.setVisibility(View.INVISIBLE);
                if (ApplicationEx.dbHelper.getCurrentPlaylistType() == 0 &&
                    ApplicationEx.dbHelper.getCurrentEpisode() == contextId)
                    contextPlaying.setVisibility(View.VISIBLE);
                if (contextDownloaded)
                    contextDownload.setVisibility(View.VISIBLE);
                /*
                mContext.sendBroadcast(
                        new Intent(Constants.ACTION_REFRESH_FEEDS));
                */
                break;
            case TASK_REFRESH_FEEDS:
            case TASK_GET_PODCASTS:
                update();
                break;
            }
            updateImage = false;
            callback.onAdapterEmptyChange();
            callback.onAdapterProgressChange();
        }
        
        @Override
        protected void onProgressUpdate(Void... nothing) {
            setEmptyText(readType, downloaded, true);
        }
        
        private void refresh() {
            mPodcastFeeds = Util.getAllFeeds(readType, downloaded, sort, 
                    sortType, archive);
            publishProgress();
            if (ApplicationEx.dbHelper.getCurrentPlaylistType() == 0)
                refreshEpisodeList();
        }
        
        private void update() {
            notifyDataSetChanged();
            if (ApplicationEx.dbHelper.getCurrentPlaylistType() == 0) {
                refreshEpisodeList();
                Intent intent = new Intent(Constants.ACTION_NEW_DOWNLOAD);
                intent.putIntegerArrayListExtra("idList", epIdList);
                mContext.sendBroadcast(intent);
            }
        }
    }
    
    public void updateData(ArrayList<Feed> feeds) {
        mPodcastFeeds = feeds;
        notifyDataSetChanged();
    }
    
    static class TempDrawable extends BitmapDrawable {
        private final WeakReference<Task> bitmapImageTaskReference;

        public TempDrawable(Task bitmapDownloaderTask) {
            super(resources, temp);
            bitmapImageTaskReference =
                new WeakReference<Task>(bitmapDownloaderTask);
        }

        public Task getBitmapImageTask() {
            return bitmapImageTaskReference.get();
        }
    }
    
    private static Task getBitmapImageTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof TempDrawable) {
                TempDrawable downloadedDrawable = (TempDrawable)drawable;
                return downloadedDrawable.getBitmapImageTask();
            }
        }
        return null;
    }
    
    private String getUrl(int position) {
        return mPodcastFeeds.get(position).getUrl();
    }
    
    private String getEpisodeTitle(int groupPosition, int childPosition) {
        return ((Episode) getChild(groupPosition, childPosition)).getTitle();
    }
    
    private String getFeedTitle(int position) {
        return mPodcastFeeds.get(position).getTitle();
    }
    
    private String getNumUnread(int position) {
        return mPodcastFeeds.get(position).getSubtitle();
    }
    
    private boolean getFeedRead(int position) {
        return mPodcastFeeds.get(position).getUnread();
    }
    
    private boolean getEpisodeRead(int groupPosition, int childPosition) {
        return ((Episode) getChild(groupPosition, childPosition)).getUnread();
    }
    
    private boolean getFeedDownloaded(int position) {
        return mPodcastFeeds.get(position).getDownloaded();
    }
    
    private boolean getEpisodeDownloaded(int groupPosition, int childPosition) {
        return ((Episode) getChild(groupPosition, childPosition))
                .getDownloaded();
    }
    
    private Bitmap getBitmap(String url) {
        int feedId = ApplicationEx.dbHelper.getInt(url, 
                DatabaseHelper.COL_FEED_ID, DatabaseHelper.FEED_TABLE, 
                DatabaseHelper.COL_FEED_ADDRESS);
        String imageLocation = ApplicationEx.cacheLocation +
                Constants.FEEDS_LOCATION + feedId + File.separator;
        Bitmap bitmap = null;
        if (Util.findFile(imageLocation, feedId + "_small" + ".png")) {
            bitmap = BitmapFactory.decodeFile(imageLocation + feedId + "_small" 
                    + ".png");
        }
        return bitmap;
    }

}
