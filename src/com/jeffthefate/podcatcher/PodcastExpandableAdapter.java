package com.jeffthefate.podcatcher;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.RejectedExecutionException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;

import android.annotation.SuppressLint;
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
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jeffthefate.podcatcher.service.DownloadService;
import com.jeffthefate.podcatcher.service.PlaybackService;
import com.jeffthefate.podcatcher.service.UpdateService;

public class PodcastExpandableAdapter extends BaseExpandableListAdapter {
    
    private Context mContext;
    
    private AdapterChange callback;
    
    private long duration;
    boolean updateImage = true;
    private int readType;
    private int sort;
    private int sortType;
    private int downloaded;
    private int archive;
    boolean mSync = false;
    
    private static Bitmap temp;
    private static Resources resources;
    
    private ArrayList<Feed> mPodcastFeeds = new ArrayList<Feed>();
    private ArrayList<Integer> epIdList = new ArrayList<Integer>();
    private ArrayList<Integer> contextGroupIds = new ArrayList<Integer>();
    private ArrayList<Integer> contextChildIds = new ArrayList<Integer>();
    private ArrayList<Integer> groupShowBusys = new ArrayList<Integer>();
    private ArrayList<Integer> childShowBusys = new ArrayList<Integer>();
    private boolean contextRead;
    private boolean contextDownloaded;
    private boolean contextPlaying;
    private boolean contextFeed;
    private SparseArray<LinearLayout> contextGroupPlayings =
            new SparseArray<LinearLayout>();
    private SparseArray<ImageView> contextGroupDownloads =
            new SparseArray<ImageView>();
    private SparseArray<ProgressBar> contextGroupBusys =
            new SparseArray<ProgressBar>();
    private SparseArray<LinearLayout> contextChildPlayings =
            new SparseArray<LinearLayout>();
    private SparseArray<ImageView> contextChildDownloads =
            new SparseArray<ImageView>();
    private SparseArray<ProgressBar> contextChildBusys =
            new SparseArray<ProgressBar>();
    private SparseBooleanArray contextChecked = new SparseBooleanArray();
    private ArrayList<View> listViews = new ArrayList<View>();
    
    private boolean multiSelect = false;
    
    private final int TASK_GET_IMAGE = 0;
    private final int TASK_MARK_FEED_READ = 1;
    private final int TASK_MARK_EPISODE_READ = 2;
    private final int TASK_REFRESH_FEEDS = 3;
    private final int TASK_UNSUBSCRIBE_FEED = 4;
    private final int TASK_DOWNLOAD_EPISODE = 5;
    private final int TASK_DELETE_EPISODE = 6;
    private final int TASK_GET_PODCASTS = 7;
    private final int TASK_DELETE_EPISODES = 8;
    private final int TASK_MARK_EPISODES_READ = 9;
    
    private ImageView leftPlayingEp;
    private ImageView rightPlayingEp;
    private ImageView leftPlayingFeed;
    private ImageView rightPlayingFeed;
    
    private LinearLayout playingEpisode;
    private LinearLayout playingFeed;
    
    private MemoryCache memoryCache = new MemoryCache();
    
    private Task task;
    
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
        public boolean isEmptyVisible();
        public void setEmptyVisible(boolean visible);
        public void setProgressVisible(boolean visible);
        public void changeEmptyText(String text);
        public void showDetailsDialog(String[] values, boolean isFeed);
    }
    
    @SuppressLint("NewApi")
    public PodcastExpandableAdapter(Context context, int read, int downloaded,
            int sort, int sortType, int archive, boolean sync,
            AdapterChange callback) {
        mContext = context;
        this.readType = read;
        this.downloaded = downloaded;
        this.sort = sort;
        this.sortType = sortType;
        this.archive = archive;
        mSync = sync;
        this.callback = callback;
        temp = BitmapFactory.decodeResource(resources, 
                R.drawable.ic_album_loading);
        resources = mContext.getResources();
        if (task != null)
            task.cancel(true);
        task = new Task(null, false, -1, null, read, downloaded, sort, 
                sortType, archive, TASK_GET_PODCASTS, true);
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
        if (mPodcastFeeds != null && !mPodcastFeeds.isEmpty() &&
                mPodcastFeeds.size() > position &&
                mPodcastFeeds.get(position) != null)
            return mPodcastFeeds.get(position);
        return null;
    }

    @Override
    public long getGroupId(int position) {
        if (mPodcastFeeds != null && !mPodcastFeeds.isEmpty() &&
                mPodcastFeeds.size() > position &&
                mPodcastFeeds.get(position) != null)
            return (long) mPodcastFeeds.get(position).getId();
        return -1;
    }
    
    @Override
    public Object getChild(int groupPosition, int childPosition) {
        if (mPodcastFeeds != null && !mPodcastFeeds.isEmpty() &&
                mPodcastFeeds.size() > groupPosition &&
                mPodcastFeeds.get(groupPosition) != null)
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
    
    public void setMultiSelect(boolean multiSelect) {
        this.multiSelect = multiSelect;
    }
    
    public void resetViews() {
        for (View view : listViews) {
            view.setBackgroundResource(R.drawable.color_btn_context_menu);
        }
        contextChecked.clear();
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
            boolean isLastChild, View convertView, ViewGroup parent) {
        final int groupPos = groupPosition;
        final int childPos = childPosition;
        final ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.row_noimage, parent, false);
            holder = new ViewHolder();
            holder.text1 = (TextView)convertView.findViewById(
                    R.id.text1);
            holder.text2 = (TextView)convertView.findViewById(
                    R.id.text2);
            holder.playing = 
                    (LinearLayout)convertView.findViewById(R.id.playing);
            holder.playingleft = 
                    (ImageView)convertView.findViewById(R.id.playingleft);
            holder.playingright = 
                    (ImageView)convertView.findViewById(R.id.playingright);
            holder.download =
                (ImageView)convertView.findViewById(R.id.download);
            holder.busy =
                (ProgressBar)convertView.findViewById(R.id.busy);
            holder.button = (RelativeLayoutEx)convertView.findViewById(
                    R.id.ButtonLayout);
            holder.id = (int) getChildId(groupPosition, childPosition);
            convertView.setTag(holder);
            listViews.add(convertView);
        }
        else {
            holder = (ViewHolder)convertView.getTag();
            if (holder.id != (int) getChildId(groupPosition,
                    childPosition)) {
                holder.id = (int) getChildId(groupPosition,
                        childPosition);
            }
        }
        if (contextChildIds.contains(
                (int) getChildId(groupPosition, childPosition)) &&
                childShowBusys.contains(
                        (int) getChildId(groupPosition, childPosition))) {
            holder.busy.setVisibility(View.VISIBLE);
            contextChildBusys.put(
                    (int) getChildId(groupPosition, childPosition),
                    holder.busy);
            holder.playing.setVisibility(View.INVISIBLE);
            contextChildPlayings.put(
                    (int) getChildId(groupPosition, childPosition),
                    holder.playing);
            holder.download.setVisibility(View.INVISIBLE);
            contextChildDownloads.put(
                    (int) getChildId(groupPosition, childPosition),
                    holder.download);
        }
        else
            holder.busy.setVisibility(View.INVISIBLE);
        holder.text1.setTag(childPosition);
        holder.text1.setText(Jsoup.parse(StringEscapeUtils.unescapeHtml4(
                getEpisodeTitle(groupPosition, childPosition) == null ? Constants.EMPTY : 
                    getEpisodeTitle(groupPosition, childPosition))).text());
        if (getEpisodeRead(groupPosition, childPosition))
            holder.text1.setTextColor(Color.WHITE);
        else
            holder.text1.setTextColor(Color.GRAY);
        holder.text2.setTag(childPosition);
        holder.text2.setText(((Episode) getChild(
                groupPosition, childPosition)).getSubtitle());
        if (!contextChecked.get(holder.id))
            convertView.setBackgroundResource(
                    R.drawable.color_btn_context_menu);
        else
            convertView.setBackgroundResource(R.drawable.color_btn_row_checked);
        convertView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!contextChecked.get(holder.id)) {
                    if (!multiSelect)
                        dispatchEpisodesAction(groupPos, childPos);
                    else {
                        view.setBackgroundResource(
                                R.drawable.color_btn_row_checked);
                        contextChecked.put(holder.id, true);
                        contextChildBusys.put(holder.id, holder.busy);
                        contextChildPlayings.put(holder.id, holder.playing);
                        contextChildDownloads.put(holder.id, holder.download);
                    }
                }
                else {
                    contextChildBusys.delete(holder.id);
                    contextChildPlayings.delete(holder.id);
                    contextChildDownloads.delete(holder.id);
                    contextChecked.delete(holder.id);
                    view.setBackgroundResource(
                            R.drawable.color_btn_context_menu);
                }
            }
        });
        holder.button.setBackgroundResource(
                R.drawable.color_btn_context_menu);
        holder.button.setTag(childPosition);
        holder.button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (childPos <
                        mPodcastFeeds.get(groupPos).getEpisodes().size()) {
                    contextChildIds.add((int) getChildId(groupPos, childPos));
                    contextRead = getEpisodeRead(groupPos, childPos);
                    contextDownloaded = getEpisodeDownloaded(
                            groupPos, childPos);
                    contextPlaying = getEpisodePlaying(groupPos, childPos);
                    contextFeed = false;
                    contextChildPlayings.put(
                            (int) getChildId(groupPos, childPos),
                            holder.playing);
                    contextChildDownloads.put(
                            (int) getChildId(groupPos, childPos),
                            holder.download);
                    contextChildBusys.put(
                            (int) getChildId(groupPos, childPos),
                            holder.busy);
                    view.performLongClick();
                }
            }
        });
        holder.playingleft.setBackgroundResource(R.anim.playing_left);
        holder.playingright.setBackgroundResource(R.anim.playing_right);
        if (ApplicationEx.dbHelper.getCurrentPlaylistType() ==
                    Constants.PLAYLIST_BY_NAME
                && ApplicationEx.dbHelper.getCurrentEpisode() == 
                ((Episode) getChild(groupPosition, childPosition)).getId()) {
            playingEpisode = holder.playing;
            leftPlayingEp = holder.playingleft;
            rightPlayingEp = holder.playingright;
            holder.playing.setVisibility(View.VISIBLE);
            holder.download.setVisibility(View.INVISIBLE);
            if (ApplicationEx.getIsPlaying()) {
                ((AnimationDrawable) holder.playingleft.getBackground())
                        .start();
                ((AnimationDrawable) holder.playingright.getBackground())
                        .start();
            }
            else {
                ((AnimationDrawable) holder.playingleft.getBackground())
                        .stop();
                ((AnimationDrawable) holder.playingright.getBackground())
                        .stop();
            }
        }
        else {
            ((AnimationDrawable) holder.playingleft.getBackground())
                    .stop();
            ((AnimationDrawable) holder.playingright.getBackground())
                    .stop();
            holder.playing.setVisibility(View.INVISIBLE);
            if (((Episode) getChild(groupPosition, childPosition))
                    .getDownloaded())
                holder.download.setVisibility(View.VISIBLE);
            else
                holder.download.setVisibility(View.INVISIBLE);
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

    @SuppressLint("NewApi")
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, 
            View convertView, ViewGroup parent) {
        final int pos = groupPosition;
        final ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.row_feed, parent, false);
            holder = new ViewHolder();
            holder.text1 = (TextView)convertView.findViewById(R.id.text1);
            holder.text2 = (TextView)convertView.findViewById(R.id.text2);
            holder.icon = (ImageView)convertView.findViewById(R.id.image);
            holder.playing = 
                    (LinearLayout)convertView.findViewById(R.id.playing);
            holder.playingleft = 
                    (ImageView)convertView.findViewById(R.id.playingleft);
            holder.playingright = 
                    (ImageView)convertView.findViewById(R.id.playingright);
            holder.download =
                (ImageView)convertView.findViewById(R.id.download);
            holder.busy =
                (ProgressBar)convertView.findViewById(R.id.busy);
            holder.button = (RelativeLayoutEx)convertView.findViewById(
                    R.id.ButtonLayout);
            holder.id = (int) getGroupId(groupPosition);
            convertView.setTag(holder);
            updateImage = true;
        }
        else {
            holder = (ViewHolder)convertView.getTag();
            if (holder.id != (int) getGroupId(groupPosition)) {
                updateImage = true;
                holder.id = (int) getGroupId(groupPosition);
            }
        }
        if (contextGroupIds.contains((int) getGroupId(groupPosition)) &&
                groupShowBusys.contains((int) getGroupId(groupPosition))) {
            holder.busy.setVisibility(View.VISIBLE);
            contextGroupBusys.put((int) getGroupId(groupPosition), holder.busy);
            holder.playing.setVisibility(View.INVISIBLE);
            contextGroupPlayings.put((int) getGroupId(groupPosition),
                    holder.playing);
            holder.download.setVisibility(View.INVISIBLE);
            contextGroupDownloads.put((int) getGroupId(groupPosition),
                    holder.download);
        }
        else
            holder.busy.setVisibility(View.INVISIBLE);
        holder.text1.setTag(groupPosition);
        holder.text1.setText(Jsoup.parse(StringEscapeUtils.unescapeHtml4(
                getFeedTitle(groupPosition) == null ? Constants.EMPTY : 
                    getFeedTitle(groupPosition))).text());
        if (getFeedRead(groupPosition))
            holder.text1.setTextColor(Color.WHITE);
        else
            holder.text1.setTextColor(Color.GRAY);
        holder.text2.setTag(groupPosition);
        holder.text2.setText(getNumUnread(groupPosition));
        final String url = getUrl(groupPosition);
        if (updateImage && cancelPotentialImage(url, holder.icon)) {
            Task task = new Task(holder.icon, false,
                    getFeedId(groupPosition), url, -1, -1, -1, -1, -1,
                    TASK_GET_IMAGE, true);
            TempDrawable bitmapDrawable = new TempDrawable(task);
            holder.icon.setImageDrawable(bitmapDrawable);
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    task.execute();
                else
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } catch (RejectedExecutionException e) {}
        }
        //convertView.setBackgroundResource(R.drawable.color_btn_context_menu);
        holder.button.setBackgroundResource(
                R.drawable.color_btn_context_menu);
        holder.button.setTag(groupPosition);
        holder.button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pos < mPodcastFeeds.size()) {
                    contextGroupIds.add((int) getGroupId(pos));
                    contextRead = getFeedRead(pos);
                    contextDownloaded = getFeedDownloaded(pos);
                    contextFeed = true;
                    contextGroupPlayings.put(
                            (int) getGroupId(pos), holder.playing);
                    contextGroupDownloads.put((int) getGroupId(pos),
                            holder.download);
                    contextGroupBusys.put((int) getGroupId(pos), holder.busy);
                    view.performLongClick();
                }
            }
        });
        holder.playingleft.setBackgroundResource(R.anim.playing_left);
        holder.playingright.setBackgroundResource(R.anim.playing_right);
        if (((Feed)getGroup(groupPosition)).getDownloaded())
            holder.download.setVisibility(View.VISIBLE);
        else
            holder.download.setVisibility(View.INVISIBLE);
        if (ApplicationEx.dbHelper.getCurrentPlaylistType() ==
                Constants.PLAYLIST_BY_NAME) {
            int currEp = ApplicationEx.dbHelper.getCurrentEpisode();
            boolean currFeed = false;
            for (int i = 0; i < getChildrenCount(groupPosition); i++) {
                if ((int) getChildId(groupPosition, i) == currEp) {
                    currFeed = true;
                    break;
                }
            }
            if (currFeed) {
                playingFeed = holder.playing;
                leftPlayingFeed = holder.playingleft;
                rightPlayingFeed = holder.playingright;
                holder.playing.setVisibility(View.VISIBLE);
                holder.download.setVisibility(View.INVISIBLE);
                if (ApplicationEx.getIsPlaying()) {
                    ((AnimationDrawable) holder.playingleft.getBackground())
                            .start();
                    ((AnimationDrawable) holder.playingright
                            .getBackground()).start();
                }
                else {
                    ((AnimationDrawable) holder.playingleft.getBackground())
                            .stop();
                    ((AnimationDrawable) holder.playingright
                            .getBackground()).stop();
                }
            }
            else {
                ((AnimationDrawable) holder.playingleft.getBackground())
                        .stop();
                ((AnimationDrawable) holder.playingright.getBackground())
                        .stop();
                holder.playing.setVisibility(View.INVISIBLE);
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
    
    public boolean getContextPlaying() {
        return contextPlaying;
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
    
    public void updateFeed(int groupPosition, boolean force) {
        Intent updateIntent = new Intent(ApplicationEx.getApp(), 
                UpdateService.class);
        updateIntent.putExtra(Constants.SYNC, ApplicationEx.isSyncing() &&
                Util.readBooleanFromFile(Constants.GOOGLE_FILENAME));
        updateIntent.putExtra(Constants.FEED, getFeedUrl(groupPosition));
        updateIntent.putExtra(Constants.FORCE, force);
        ApplicationEx.getApp().startService(updateIntent);
    }
    
    @SuppressLint("NewApi")
    public void refreshFeeds(int readType, int downloaded, int sort,
            int sortType, int archive) {
        Task task;
        if (this.readType != readType || this.downloaded != downloaded ||
                this.sortType != sortType || this.archive != archive)
            task = new Task(null, false, -1, Constants.EMPTY, readType, downloaded, sort,
                    sortType, archive, TASK_GET_PODCASTS, true);
        else
            task = new Task(null, false, -1, Constants.EMPTY, readType, downloaded, sort,
                    sortType, archive, TASK_REFRESH_FEEDS, true);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        this.readType = readType;
        this.downloaded = downloaded;
        this.sort = sort;
        this.sortType = sortType;
        this.archive = archive;
    }
    
    @SuppressLint("NewApi")
    public void markAllRead(boolean read, int readType, int downloaded, 
            int sort, int sortType, int groupPosition, int archive) {
        int id = (int) getGroupId(groupPosition);
        groupShowBusys.add(id);
        if (ApplicationEx.dbHelper.isFeedRead(id, read)) {
            Task task = new Task(null, read, id, Constants.EMPTY, readType,
                    downloaded, sort, sortType, archive, TASK_MARK_FEED_READ,
                    true);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                task.execute();
            else
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
    
    @SuppressLint("NewApi")
    public void markSelectedRead(boolean read) {
        if (task != null)
            task.cancel(true);
        task = new Task(null, read, -1, null, readType, downloaded, sort,
                sortType, archive, TASK_MARK_EPISODES_READ, false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    @SuppressLint("NewApi")
    public void markEpisodeRead(boolean read, int readType, int downloaded, 
            int sort, int sortType, int childPosition, int groupPosition,
            int archive) {
        int id = (int) getChildId(groupPosition, childPosition);
        childShowBusys.add(id);
        Task task = new Task(null, read, id, Constants.EMPTY, readType,
                downloaded, sort, sortType, archive, TASK_MARK_EPISODE_READ,
                false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    @SuppressLint("NewApi")
    public void unsubscribe(int readType, int downloaded, int sort, 
            int sortType, int groupPosition, int archive) {
        int id = (int) getGroupId(groupPosition);
        groupShowBusys.add(id);
        Task task = new Task(null, false, id, Constants.EMPTY, readType, downloaded, sort,
                sortType, archive, TASK_UNSUBSCRIBE_FEED, true);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    @SuppressLint("NewApi")
    public void deleteEpisode(int readType, int downloaded, int sort, 
            int sortType, int childPosition, int groupPosition, int archive) {
        int id = (int) getChildId(groupPosition, childPosition);
        childShowBusys.add(id);
        Task task = new Task(null, false, id, null, readType, downloaded, sort,
                        sortType, archive, TASK_DELETE_EPISODE, false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    @SuppressLint("NewApi")
    public void deleteSelected() {
        if (task != null)
            task.cancel(true);
        task = new Task(null, false, -1, null, readType, downloaded, sort,
                sortType, archive, TASK_DELETE_EPISODES, false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    @SuppressLint("NewApi")
    public void downloadEpisode(int childPosition, int groupPosition) {
        int id = (int) getChildId(groupPosition, childPosition);
        String url = ApplicationEx.dbHelper.getEpisodeUrl(id);
        Task task = new Task(null, false, id, url, -1, -1, -1, -1, -1,
                        TASK_DOWNLOAD_EPISODE, false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    public void downloadSelected() {
        ArrayList<String> ids = new ArrayList<String>();
        for (int i = 0; i < contextChecked.size(); i++) {
            if (!ApplicationEx.dbHelper.isDownloaded(contextChecked.keyAt(i)))
                ids.add(ApplicationEx.dbHelper.getEpisodeUrl(
                        contextChecked.keyAt(i)));
        }
        String[] urlArray = new String[ids.size()];
        ids.toArray(urlArray);
        if (Util.isMyServiceRunning(DownloadService.class.getName())) {
            Intent intent = new Intent(Constants.ACTION_START_DOWNLOAD);
            intent.putExtra(Constants.URLS, urlArray);
            mContext.sendBroadcast(intent);
        }
        else {
            Intent intent = new Intent(mContext, DownloadService.class);
            intent.putExtra(Constants.URLS, urlArray);
            mContext.startService(intent);
        }
        resetViews();
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
                    playbackIntent.putExtra(Constants.START_EP, epId);
                    playbackIntent.putIntegerArrayListExtra(Constants.EPS, 
                            epIdList);
                    mContext.startService(playbackIntent);
                }
            };
            t.start();
            mContext.sendBroadcast(new Intent(Constants.ACTION_START_PLAYBACK));
        }
    }
    
    public void showDetails(int groupPos, int childPos, boolean isFeed) {
        String[] values = new String[0];
        if (!isFeed)
            values = ApplicationEx.dbHelper.getEpisode(
                    (int) getChildId(groupPos, childPos));
        else
            values = ApplicationEx.dbHelper.getFeed((int) getGroupId(groupPos));
        callback.showDetailsDialog(values, isFeed);
    }
    
    public int getTotalCount() {
        int total = 0;
        for (int i = 0; i < getGroupCount(); i++) {
            for (int j = 0; j < getChildrenCount(i); j++) {
                total++;
            }
        }
        return total;
    }
    
    public void dispatchEpisodesAction(int groupPosition, int childPosition) {
        String epUrl = ((Episode) getChild(groupPosition, childPosition))
                .getUrl();
        epIdList = Util.getFeedEpisodeIdList(true);
        final int epId = (int) getChildId(groupPosition, childPosition);
        if (ApplicationEx.dbHelper.isDownloaded(epId)) {
            Thread t = new Thread(){
                public void run(){
                    memoryCache.clear();
                    Intent playbackIntent = new Intent(mContext,
                            PlaybackService.class);
                    playbackIntent.putExtra(Constants.START_EP, epId);
                    playbackIntent.putIntegerArrayListExtra(Constants.EPS, 
                            epIdList);
                    mContext.startService(playbackIntent);
                    ApplicationEx.dbHelper.setCurrentPlaylistType(
                            Constants.PLAYLIST_BY_NAME);
                }
            };
            t.start();
            mContext.sendBroadcast(new Intent(Constants.ACTION_START_PLAYBACK));
        }
        else if (Util.isMyServiceRunning(DownloadService.class.getName())) {
            Intent intent = new Intent(Constants.ACTION_START_DOWNLOAD);
            intent.putExtra(Constants.URLS, new String[]{epUrl});
            mContext.sendBroadcast(intent);
        }
        else {
            Intent intent = 
                new Intent(ApplicationEx.getApp(), DownloadService.class);
            intent.putExtra(Constants.URLS, new String[]{epUrl});
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
    
    public String getEmptyText(int read, int downloaded, boolean isFeed) {
        if(getGroupCount() < 1) {
            if (ApplicationEx.dbHelper.getFeedCount() < 1) {
                if (mSync)
                    return ApplicationEx.getApp().getString(
                            R.string.emptyFeedsSync);
                else
                    return ApplicationEx.getApp().getString(
                            R.string.emptyFeeds);
            }
            else {
                String text = Constants.EMPTY;
                if (isFeed)
                    text = mContext.getString(R.string.emptyFeedsFiltered);
                else
                    text = mContext.getString(R.string.emptyEpisodes);
                if (read == Constants.UNREAD)
                    text = text.concat(Constants.UNREAD_TEXT);
                if (downloaded == Constants.DOWNLOADED)
                    text = text.concat(Constants.DOWNLOADED_TEXT);
                return text;
            }
        }
        return Constants.EMPTY;
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
        private String emptyText;
        
        private Task bitmapImageTask;
        private ImageView imageView;
        
        private boolean group = false;

        public Task(ImageView imageView, boolean read, int id, String url, 
                int readType, int downloaded, int sort, int sortType,
                int archive, int task, boolean group) {
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
            this.group = group;
        }
        
        @Override
        protected void onPreExecute() {
            switch(task) {
            case TASK_MARK_EPISODES_READ:
            case TASK_DELETE_EPISODES:
                int tempId;
                for (int i = 0; i < contextChecked.size(); i++) {
                    tempId = contextChecked.keyAt(i);
                    childShowBusys.add(tempId);
                    contextChildIds.add(tempId);
                    contextChildBusys.get(tempId).setVisibility(View.VISIBLE);
                    contextChildPlayings.get(tempId).setVisibility(
                            View.INVISIBLE);
                    contextChildDownloads.get(tempId).setVisibility(
                            View.INVISIBLE);
                }
                break;
            case TASK_MARK_FEED_READ:
            case TASK_DELETE_EPISODE:
            case TASK_UNSUBSCRIBE_FEED:
                if (group) {
                    contextGroupBusys.get(id).setVisibility(View.VISIBLE);
                    contextGroupPlayings.get(id).setVisibility(View.INVISIBLE);
                    contextGroupDownloads.get(id).setVisibility(View.INVISIBLE);
                }
                else {
                    contextChildBusys.get(id).setVisibility(View.VISIBLE);
                    contextChildPlayings.get(id).setVisibility(View.INVISIBLE);
                    contextChildDownloads.get(id).setVisibility(View.INVISIBLE);
                }
                break;
            case TASK_GET_PODCASTS:
                callback.setProgressVisible(true);
                callback.setEmptyVisible(false);
                mPodcastFeeds.clear();
                notifyDataSetChanged();
                break;
            default:
                break;
            }
        }
        
        @Override
        protected Void doInBackground(Void... nothing) {
            switch(task) {
            case TASK_GET_IMAGE:
                bitmap = getBitmap(id);
                if (imageViewReference != null) {
                    imageView = imageViewReference.get();
                    bitmapImageTask = getBitmapImageTask(imageView);
                    publishProgress();
                }
                break;
            case TASK_MARK_FEED_READ:
                ArrayList<Integer> epIdList = 
                        ApplicationEx.dbHelper.markFeedEpisodesRead(id, read, 
                                archive);
                if (read)
                    Util.deleteFeedEpisodes(id, epIdList);
                refresh();
                duration = Util.getAllEpisodesDuration();
                break;
            case TASK_MARK_EPISODES_READ:
                int tempId;
                for (int i = 0; i < contextChecked.size(); i++) {
                    tempId = contextChecked.keyAt(i);
                    ApplicationEx.dbHelper.markRead(tempId, read);
                }
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
                duration = Util.getAllEpisodesDuration();
                break;
            case TASK_DOWNLOAD_EPISODE:
                if (!ApplicationEx.dbHelper.isDownloaded(id)) {
                    if (Util.isMyServiceRunning(
                            DownloadService.class.getName())) {
                        Intent intent = new Intent(
                                Constants.ACTION_START_DOWNLOAD);
                        intent.putExtra(Constants.URLS, new String[]{url});
                        mContext.sendBroadcast(intent);
                    }
                    else {
                        Intent intent = new Intent(mContext,
                                DownloadService.class);
                        intent.putExtra(Constants.URLS, new String[]{url});
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
                Util.deleteRecursive(new File(TextUtils.concat(
                        ApplicationEx.cacheLocation, Constants.FEEDS_LOCATION,
                        Integer.toString(id), File.separator).toString()));
                if (mSync) {
                    Intent intent = new Intent(ApplicationEx.getApp(), 
                            UpdateService.class);
                    intent.putExtra(Constants.SYNC, mSync);
                    intent.putExtra(Constants.FEED, url);
                    intent.putExtra(Constants.TITLE, title);
                    intent.putExtra(Constants.UNSUBSCRIBE, true);
                    ApplicationEx.getApp().startService(intent);
                }
                else
                    ApplicationEx.dbHelper.deleteFeed(id);
                refresh();
                duration = Util.getAllEpisodesDuration();
                break;
            case TASK_DELETE_EPISODE:
                if (ApplicationEx.dbHelper.isDownloaded(id)) {
                    new File(ApplicationEx.dbHelper.getEpisodeLocation(id))
                            .delete();
                    ApplicationEx.dbHelper.episodeDownloaded(id, false);
                    // Update the current playlist (downloaded episodes)
                    Intent intent = new Intent(
                            Constants.ACTION_NEW_DOWNLOAD);
                    // Set the date sorted playlist, if currently set
                    if (ApplicationEx.dbHelper.getCurrentPlaylistType() ==
                            Constants.PLAYLIST_BY_DATE)
                        intent.putIntegerArrayListExtra(Constants.ID_LIST, 
                                Util.getEpisodeIdList(true, -1, -1));
                    // Otherwise, the name sorted playlist
                    else
                        intent.putIntegerArrayListExtra(Constants.ID_LIST, 
                                Util.getFeedEpisodeIdList(true));
                    // Send the playlist broadcast
                    ApplicationEx.getApp().sendBroadcast(intent);
                    duration = Util.getAllEpisodesDuration();
                    refresh();
                }
                break;
            case TASK_DELETE_EPISODES:
                for (int i = 0; i < contextChecked.size(); i++) {
                    tempId = contextChecked.keyAt(i);
                    if (ApplicationEx.dbHelper.isDownloaded(tempId)) {
                        new File(ApplicationEx.dbHelper.getEpisodeLocation(
                                tempId)).delete();
                        ApplicationEx.dbHelper.episodeDownloaded(tempId, false);
                        if (ApplicationEx.dbHelper.getCurrentEpisode() ==
                                tempId)
                            ApplicationEx.setCurrentEpisode(-1);
                    }
                }
                // Update the current playlist (downloaded episodes)
                Intent intent = new Intent(Constants.ACTION_NEW_DOWNLOAD);
                // Set the name sorted playlist, if currently set
                if (ApplicationEx.dbHelper.getCurrentPlaylistType() ==
                        Constants.PLAYLIST_BY_NAME)
                    intent.putIntegerArrayListExtra(Constants.ID_LIST, 
                            Util.getFeedEpisodeIdList(true));
                // Send the playlist broadcast
                ApplicationEx.getApp().sendBroadcast(intent);
                duration = Util.getAllEpisodesDuration();
                refresh();
                break;
            case TASK_REFRESH_FEEDS:
            case TASK_GET_PODCASTS:
                refresh();
                duration = Util.getAllEpisodesDuration();
                break;
            }
            return null;
        }

        protected void onPostExecute(Void nothing) {
            switch(task) {
            case TASK_GET_IMAGE:
            case TASK_DOWNLOAD_EPISODE:
                break;
            case TASK_MARK_FEED_READ:
            case TASK_MARK_EPISODE_READ:
            case TASK_UNSUBSCRIBE_FEED:
            case TASK_DELETE_EPISODE:
                if (group) {
                    groupShowBusys.remove((Integer)id);
                    contextGroupIds.remove((Integer)id);
                    if (contextGroupBusys.get(id) != null)
                        contextGroupBusys.get(id).setVisibility(View.INVISIBLE);
                    contextGroupBusys.remove((Integer)id);
                    contextGroupPlayings.remove((Integer)id);
                    contextGroupDownloads.remove((Integer)id);
                }
                else {
                    childShowBusys.remove((Integer)id);
                    contextChildIds.remove((Integer)id);
                    if (contextChildBusys.get(id) != null)
                        contextChildBusys.get(id).setVisibility(View.INVISIBLE);
                    contextChildBusys.remove((Integer)id);
                    contextChildPlayings.remove((Integer)id);
                    contextChildDownloads.remove((Integer)id);
                }
                if (ApplicationEx.dbHelper.getCurrentPlaylistType() ==
                        Constants.PLAYLIST_BY_NAME) {
                    Intent intent = new Intent(Constants.ACTION_NEW_DOWNLOAD);
                    intent.putIntegerArrayListExtra(Constants.ID_LIST,
                            epIdList);
                    mContext.sendBroadcast(intent);
                }
                mContext.sendBroadcast(
                        new Intent(Constants.ACTION_REFRESH_EPISODES));
                break;
            case TASK_MARK_EPISODES_READ:
            case TASK_DELETE_EPISODES:
                int tempId;
                for (int i = 0; i < contextChecked.size(); i++) {
                    tempId = contextChecked.keyAt(i);
                    childShowBusys.remove((Integer)tempId);
                    contextChildIds.remove((Integer)tempId);
                    contextChildBusys.get(tempId).setVisibility(View.INVISIBLE);
                    contextChildBusys.remove((Integer)tempId);
                    contextChildPlayings.remove((Integer)tempId);
                    contextChildDownloads.remove((Integer)tempId);
                }
                if (ApplicationEx.dbHelper.getCurrentPlaylistType() ==
                        Constants.PLAYLIST_BY_DATE) {
                    Intent intent = new Intent(Constants.ACTION_NEW_DOWNLOAD);
                    intent.putIntegerArrayListExtra(Constants.ID_LIST,
                            epIdList);
                    mContext.sendBroadcast(intent);
                }
                mContext.sendBroadcast(
                        new Intent(Constants.ACTION_REFRESH_FEEDS));
                mContext.sendBroadcast(
                        new Intent(Constants.ACTION_REFRESH_DURATION));
                callback.setEmptyVisible(true);
                resetViews();
                break;
            case TASK_REFRESH_FEEDS:
            case TASK_GET_PODCASTS:
                mContext.sendBroadcast(
                        new Intent(Constants.ACTION_REFRESH_DURATION));
                callback.setEmptyVisible(true);
                callback.setProgressVisible(false);
                if (ApplicationEx.dbHelper.getCurrentPlaylistType() ==
                        Constants.PLAYLIST_BY_NAME) {
                    Intent intent = new Intent(Constants.ACTION_NEW_DOWNLOAD);
                    intent.putIntegerArrayListExtra(Constants.ID_LIST,
                            epIdList);
                    mContext.sendBroadcast(intent);
                }
                break;
            }
            updateImage = false;
        }
        
        @Override
        protected void onProgressUpdate(Void... nothing) {
            switch(task) {
            case TASK_GET_IMAGE:
                if (imageViewReference != null && this == bitmapImageTask &&
                        bitmap != null)
                    imageView.setImageBitmap(bitmap);
                break;
            default:
                notifyDataSetChanged();
                callback.changeEmptyText(emptyText);
                break;
            }
        }
        
        private void refresh() {
            mPodcastFeeds = Util.getAllFeeds(readType, downloaded, sort, 
                    sortType, archive);
            epIdList = Util.getFeedEpisodeIdList(true);
            emptyText = getEmptyText(readType, downloaded, false);
            publishProgress();
        }
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
    
    private int getFeedId(int position) {
        return mPodcastFeeds.get(position).getId();
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
    
    private boolean getEpisodePlaying(int groupPosition, int childPosition) {
        if (ApplicationEx.dbHelper.getCurrentEpisode() == 
                    ((Episode) getChild(groupPosition, childPosition)).getId())
            return true;
        return false;
    }
    
    public long getTotalDuration() {
        return duration;
    }
    
    public int getDownloadedCount() {
        return epIdList.size();
    }
    
    public void clearContexts() {
        contextGroupIds.clear();
        contextChildIds.clear();
        groupShowBusys.clear();
        childShowBusys.clear();
        contextGroupPlayings.clear();
        contextChildPlayings.clear();
        contextGroupDownloads.clear();
        contextChildDownloads.clear();
        contextGroupBusys.clear();
        contextChildBusys.clear();
    }
    
    private Bitmap getBitmap(int feedId) {
        Bitmap bitmap = memoryCache.get(feedId);
        if (bitmap != null)
            return bitmap;
        String imageLocation = TextUtils.concat(ApplicationEx.cacheLocation,
                Constants.FEEDS_LOCATION, Integer.toString(feedId),
                File.separator).toString();
        if (Util.findFile(imageLocation, TextUtils.concat(
                Integer.toString(feedId), Constants.SMALL, Constants.PNG)
                        .toString())) {
            bitmap = BitmapFactory.decodeFile(TextUtils.concat(imageLocation,
                    Integer.toString(feedId), Constants.SMALL, Constants.PNG)
                            .toString());
        }
        return bitmap;
    }

}
