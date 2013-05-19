package com.jeffthefate.podcatcher;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.TreeMap;
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
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jeffthefate.podcatcher.fragment.FragmentEpisodes;
import com.jeffthefate.podcatcher.service.DownloadService;
import com.jeffthefate.podcatcher.service.PlaybackService;

public class PodcastAdapter extends BaseAdapter {
    
    private Context uiContext;
    private int readType;
    private int sortType;
    private int downloaded;
    private int archive;
    private boolean sync = false;
    private AdapterChange callback;
    
    private static Bitmap temp;
    private static Resources resources;
    
    private ArrayList<Episode> mEpisodes = new ArrayList<Episode>();
    private ArrayList<Integer> contextIds = new ArrayList<Integer>();
    private ArrayList<Integer> showBusys = new ArrayList<Integer>();
    private ArrayList<Integer> epIdList = new ArrayList<Integer>();
    private SparseIntArray epIdMap = new SparseIntArray();
    private TreeMap<Integer,SimpleEntry<Integer,Bitmap>> map =
            new TreeMap<Integer,SimpleEntry<Integer,Bitmap>>();
    
    private long mDuration;
    private boolean updateImage = true;
    private int rowHeight;
    
    private int contextPosition;
    private boolean contextRead;
    private boolean contextDownloaded;
    private boolean contextPlaying;
    private SparseArray<LinearLayout> contextPlayings =
            new SparseArray<LinearLayout>();
    private SparseArray<ImageView> contextDownloads =
            new SparseArray<ImageView>();
    private SparseArray<ProgressBar> contextBusys =
            new SparseArray<ProgressBar>();
    private SparseBooleanArray contextChecked = new SparseBooleanArray();
    private ArrayList<View> listViews = new ArrayList<View>();
    
    private boolean multiSelect = false;
    
    private final int TASK_GET_IMAGE = 0;
    private final int TASK_MARK_EPISODE_READ = 1;
    private final int TASK_REFRESH_EPISODES = 2;
    private final int TASK_DOWNLOAD_EPISODE = 3;
    private final int TASK_DELETE_EPISODE = 4;
    private final int TASK_GET_PODCASTS = 5;
    private final int TASK_DELETE_EPISODES = 6;
    private final int TASK_MARK_EPISODES_READ = 7;
    
    private ImageView leftPlaying;
    private ImageView rightPlaying;
    
    private MemoryCache memoryCache = new MemoryCache();
    
    protected static class ViewHolder {
        TextView text1;
        TextView text2;
        ImageView icon;
        RelativeLayoutEx button;
        LinearLayout playing;
        ImageView playingleft;
        ImageView playingright;
        ImageView download;
        ImageView lock;
        ProgressBar busy;
        int id = -1;
        TextView loadingTextTop;
        TextView loadingTextBottom;
    }
    
    public interface AdapterChange {
        public boolean isEmptyVisible();
        public void setEmptyVisible(boolean visible);
        public void setProgressVisible(boolean visible);
        public void changeEmptyText(String text);
        public void showDetailsDialog(String[] values);
    }
    
    @SuppressLint("NewApi")
    public PodcastAdapter(Context uiContext, int readType, int downloaded,
            int sortType, int archive, boolean sync, AdapterChange callback,
            FragmentEpisodes fragment) {
        this.uiContext = uiContext;
        this.readType = readType;
        this.downloaded = downloaded;
        this.sortType = sortType;
        this.archive = archive;
        this.sync = sync;
        this.callback = callback;
        resources = uiContext.getResources();
        temp = BitmapFactory.decodeResource(resources, 
                R.drawable.ic_album_loading);
        Task task = new Task(null, false, -1, -1, null, readType, downloaded, 
                sortType, archive, TASK_GET_PODCASTS, fragment, -1, false,
                false);
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                task.execute();
            else
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (RejectedExecutionException e) {}
    }
    
    public int getOffset() {
        return offset;
    }
    
    @Override
    public int getCount() {
        if (mEpisodes != null)
            return mEpisodes.size();
        return 0;
    }
    
    public int getOrderCount() {
        return epIdMap.size();
    }
    
    public int getOrderPosition(int position) {
        if (mEpisodes != null && !mEpisodes.isEmpty() &&
                mEpisodes.size() > position && mEpisodes.get(position) != null)
            return epIdMap.indexOfValue(mEpisodes.get(position).getId());
        return -1;
    }
    
    public int getDownloadedCount() {
        return epIdList.size();
    }
    
    public ArrayList<Integer> getPlaylist() {
        return epIdList;
    }

    @Override
    public Object getItem(int position) {
        if (mEpisodes != null && !mEpisodes.isEmpty() &&
                mEpisodes.size() > position && mEpisodes.get(position) != null)
            return mEpisodes.get(position);
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (mEpisodes != null && !mEpisodes.isEmpty() &&
                mEpisodes.size() > position && mEpisodes.get(position) != null)
            return (long) mEpisodes.get(position).getId();
        return -1;
    }
    
    private int getFeedId(int position) {
        if (mEpisodes != null && !mEpisodes.isEmpty() &&
                mEpisodes.size() > position && mEpisodes.get(position) != null)
            return mEpisodes.get(position).getFeedId();
        return -1;
    }
    
    public int getItemPosition(int id) {
        int position = 0;
        for (int i = 0; i < mEpisodes.size(); i++) {
            if (mEpisodes.get(i) != null && mEpisodes.get(i).getId() == id) {
                position = i;
                break;
            }
        }
        if (getItem(position) == null) {
            if (position == 0)
                position = 1;
        }
        return position;
    }
    
    public int getRowHeight() {
        return rowHeight;
    }
    
    public void startLeftPlaying() {
        if (leftPlaying != null)
            ((AnimationDrawable) leftPlaying.getBackground()).start();
        else
            Log.e(Constants.LOG_TAG, "leftPlaying is null");
    }
    
    public void stopLeftPlaying() {
        if (leftPlaying != null)
            ((AnimationDrawable) leftPlaying.getBackground()).stop();
        else
            Log.e(Constants.LOG_TAG, "leftPlaying is null");
    }
    
    public void startRightPlaying() {
        if (rightPlaying != null)
            ((AnimationDrawable) rightPlaying.getBackground()).start();
        else
            Log.e(Constants.LOG_TAG, "rightPlaying is null");
    }
    
    public void stopRightPlaying() {
        if (rightPlaying != null)
            ((AnimationDrawable) rightPlaying.getBackground()).stop();
        else
            Log.e(Constants.LOG_TAG, "rightPlaying is null");
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

    @SuppressLint("NewApi")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= mEpisodes.size())
            return convertView;
        final int pos = position;
        int orderPosition;
        final ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(uiContext).inflate(R.layout.row,
                    parent, false);
            rowHeight = convertView.getHeight();
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
            holder.lock =
                (ImageView) convertView.findViewById(R.id.lock);
            holder.busy =
                    (ProgressBar)convertView.findViewById(R.id.busy);
            holder.button = (RelativeLayoutEx)convertView.findViewById(
                    R.id.ButtonLayout);
            holder.loadingTextTop = (TextView)convertView.findViewById(
                    R.id.LoadingMoreTextTop);
            holder.loadingTextBottom = (TextView)convertView.findViewById(
                    R.id.LoadingMoreTextBottom);
            if (getItem(position) != null)
                holder.id = (int) getItemId(position);
            else {
                if (position == 0)
                    holder.loadingTextTop.setVisibility(View.VISIBLE);
                else
                    holder.loadingTextBottom.setVisibility(View.VISIBLE);
                holder.text1.setVisibility(View.GONE);
                holder.text2.setVisibility(View.GONE);
                holder.icon.setVisibility(View.GONE);
                holder.playing.setVisibility(View.GONE);
                holder.download.setVisibility(View.GONE);
                holder.lock.setVisibility(View.GONE);
                holder.button.setVisibility(View.GONE);
                convertView.setTag(holder);
                return convertView;
            }
            convertView.setTag(holder);
            listViews.add(convertView);
            updateImage = true;
        }
        else {
            holder = (ViewHolder)convertView.getTag();
            if (getItem(position) != null) {
                holder.id = (int) getItemId(position);
                updateImage = true;
                holder.text1.setVisibility(View.VISIBLE);
                holder.text2.setVisibility(View.VISIBLE);
                holder.icon.setVisibility(View.VISIBLE);
                holder.button.setVisibility(View.VISIBLE);
                holder.loadingTextTop.setVisibility(View.GONE);
                holder.loadingTextBottom.setVisibility(View.GONE);
                if (contextChecked.get(holder.id)) {
                    contextBusys.put(holder.id, holder.busy);
                    contextPlayings.put(holder.id, holder.playing);
                    contextDownloads.put(holder.id, holder.download);
                }
            }
            else {
                if (position == 0)
                    holder.loadingTextTop.setVisibility(View.VISIBLE);
                else
                    holder.loadingTextBottom.setVisibility(View.VISIBLE);
                holder.text1.setVisibility(View.GONE);
                holder.text2.setVisibility(View.GONE);
                holder.icon.setVisibility(View.GONE);
                holder.playing.setVisibility(View.GONE);
                holder.download.setVisibility(View.GONE);
                holder.lock.setVisibility(View.GONE);
                holder.button.setVisibility(View.GONE);
                return convertView;
            }
        }
        orderPosition = epIdMap.indexOfValue(holder.id);
        if (contextIds.contains((int) getItemId(position)) &&
                showBusys.contains((int) getItemId(position))) {
            holder.busy.setVisibility(View.VISIBLE);
            contextBusys.put((int) getItemId(position), holder.busy);
            holder.playing.setVisibility(View.INVISIBLE);
            contextPlayings.put((int) getItemId(position), holder.playing);
            holder.download.setVisibility(View.INVISIBLE);
            contextDownloads.put((int) getItemId(position), holder.download);
        }
        else
            holder.busy.setVisibility(View.INVISIBLE);
        holder.text1.setTag(position);
        holder.text1.setText(Jsoup.parse(StringEscapeUtils.unescapeHtml4(
                getTitle(position) == null ? Constants.EMPTY : getTitle(position))).text());
        if (getRead(position))
            holder.text1.setTextColor(Color.WHITE);
        else
            holder.text1.setTextColor(Color.GRAY);
        holder.text2.setTag(position);
        holder.text2.setText(Jsoup.parse(StringEscapeUtils.unescapeHtml4(
                mEpisodes.get(position).getSubtitle())).text());
        final String url = getUrl(position) == null ? Constants.EMPTY : getUrl(position);
        try {
            if (map.containsKey(orderPosition) &&
                    map.get(orderPosition).getKey() == getFeedId(position))
                holder.icon.setImageBitmap(map.get(orderPosition).getValue());
            else if (updateImage && cancelPotentialImage(url, holder.icon)) {
                Task task = new Task(holder.icon, false, holder.id,
                        orderPosition, url, -1, -1, -1, -1, TASK_GET_IMAGE,
                        null, getFeedId(position), false, false);
                TempDrawable bitmapDrawable = new TempDrawable(task);
                holder.icon.setImageDrawable(bitmapDrawable);
                try {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                        task.execute();
                    else
                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } catch (RejectedExecutionException e) {}
            }
        } catch (NullPointerException e) {}
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
                        dispatchEpisodesAction(pos);
                    else {
                        view.setBackgroundResource(
                                R.drawable.color_btn_row_checked);
                        contextChecked.put(holder.id, true);
                        contextBusys.put(holder.id, holder.busy);
                        contextPlayings.put(holder.id, holder.playing);
                        contextDownloads.put(holder.id, holder.download);
                    }
                }
                else {
                    contextBusys.delete(holder.id);
                    contextPlayings.delete(holder.id);
                    contextDownloads.delete(holder.id);
                    contextChecked.delete(holder.id);
                    view.setBackgroundResource(
                            R.drawable.color_btn_context_menu);
                }
            }
        });
        holder.button.setBackgroundResource(R.drawable.color_btn_context_menu);
        holder.button.setTag(position);
        holder.button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pos < mEpisodes.size()) {
                    contextIds.add((int) getItemId(pos));
                    contextPosition = pos;
                    contextRead = getRead(pos);
                    contextDownloaded = getDownloaded(pos);
                    contextPlaying = getPlaying(pos);
                    contextBusys.put((int) getItemId(pos), holder.busy);
                    contextPlayings.put((int) getItemId(pos), holder.playing);
                    contextDownloads.put((int) getItemId(pos), holder.download);
                    view.performLongClick();
                }
            }
        });
        holder.playingleft.setBackgroundResource(R.anim.playing_left);
        holder.playingright.setBackgroundResource(R.anim.playing_right);
        if (ApplicationEx.dbHelper.getCurrentPlaylistType() ==
                    Constants.PLAYLIST_BY_DATE
                && ApplicationEx.dbHelper.getCurrentEpisode() == 
                    mEpisodes.get(pos).getId()) {
            leftPlaying = holder.playingleft;
            rightPlaying = holder.playingright;
            holder.playing.setVisibility(View.VISIBLE);
            holder.download.setVisibility(View.INVISIBLE);
            if (ApplicationEx.getIsPlaying()) {
                startLeftPlaying();
                startRightPlaying();
            }
            else {
                stopLeftPlaying();
                stopRightPlaying();
            }
        }
        else {
            ((AnimationDrawable) holder.playingleft.getBackground()).stop();
            ((AnimationDrawable) holder.playingright.getBackground()).stop();
            holder.playing.setVisibility(View.INVISIBLE);
            if (mEpisodes.get(pos).getDownloaded())
                holder.download.setVisibility(View.VISIBLE);
            else
                holder.download.setVisibility(View.INVISIBLE);
        }
        return convertView;
    }
    
    protected int getContextId() {
        return (int) getItemId(contextPosition);
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
    
    @SuppressLint("NewApi")
    public void refreshEpisodes(int readType, int downloaded, int sortType,
            int archive, FragmentEpisodes fragment, boolean down) {
        Task task;
        if (this.readType != readType || this.downloaded != downloaded ||
                this.sortType != sortType || this.archive != archive)
            task = new Task(null, false, -1, -1, null, readType, downloaded,
                    sortType, archive, TASK_GET_PODCASTS, fragment, -1, false,
                    false);
        else
            task = new Task(null, false, -1, -1, null, readType, downloaded, 
                    sortType, archive, TASK_REFRESH_EPISODES, fragment, -1,
                    true, down);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        this.readType = readType;
        this.downloaded = downloaded;
        this.sortType = sortType;
        this.archive = archive;
    }
    
    @SuppressLint("NewApi")
    public void markEpisodeRead(boolean read, int readType, int downloaded, 
            int sortType, int position, int archive) {
        showBusys.add((int) getItemId(position));
        Task task = new Task(null, read, getContextId(), position,
                Constants.EMPTY, readType, downloaded, sortType, archive,
                TASK_MARK_EPISODE_READ, null, -1, false, false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    @SuppressLint("NewApi")
    public void markSelectedRead(boolean read) {
        Task task = new Task(null, read, -1, -1, null, readType, downloaded,
                sortType, archive, TASK_MARK_EPISODES_READ, null, -1, false,
                false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    @SuppressLint("NewApi")
    public void deleteEpisode(int position, int readType, int downloaded,
            int sortType, int archive) {
        showBusys.add((int) getItemId(position));
        int id = (int) getItemId(position);
        Task task = new Task(null, false, id, position, null, readType,
                downloaded, sortType, archive, TASK_DELETE_EPISODE, null, -1,
                false, false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    @SuppressLint("NewApi")
    public void deleteSelected() {
        Task task = new Task(null, false, -1, -1, null, readType, downloaded,
                sortType, archive, TASK_DELETE_EPISODES, null, -1, false,
                false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    @SuppressLint("NewApi")
    public void downloadEpisode(int position) {
        int id = (int) getItemId(position);
        String url = ApplicationEx.dbHelper.getEpisodeUrl(id);
        Task task = new Task(null, false, id, position, url, -1, -1, -1, -1, 
                TASK_DOWNLOAD_EPISODE, null, -1, false, false);
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
            uiContext.sendBroadcast(intent);
        }
        else {
            Intent intent = new Intent(uiContext, DownloadService.class);
            intent.putExtra(Constants.URLS, urlArray);
            uiContext.startService(intent);
        }
        resetViews();
    }
    
    public void dispatchEpisodesAction(int position) {
        final int pos = position;
        Thread t = new Thread(){
            public void run(){
                String epUrl = mEpisodes.get(pos).getUrl();
                int epId = mEpisodes.get(pos).getId();
                if (ApplicationEx.dbHelper.isDownloaded(epId)) {
                    memoryCache.clear();
                    // TODO Add ability to stream episodes here
                    ArrayList<Integer> epIdList = Util.getEpisodeIdList(true,
                            -1, -1);
                    Intent playbackIntent = new Intent(uiContext,
                            PlaybackService.class);
                    playbackIntent.putExtra(Constants.START_EP, epId);
                    playbackIntent.putIntegerArrayListExtra(Constants.EPS, 
                            epIdList);
                    uiContext.startService(playbackIntent);
                    ApplicationEx.dbHelper.setCurrentPlaylistType(
                            Constants.PLAYLIST_BY_DATE);
                    uiContext.sendBroadcast(
                            new Intent(Constants.ACTION_START_PLAYBACK));
                }
                else if (Util.isMyServiceRunning(
                        DownloadService.class.getName())) {
                    Intent intent = new Intent(Constants.ACTION_START_DOWNLOAD);
                    intent.putExtra(Constants.URLS, new String[]{epUrl});
                    uiContext.sendBroadcast(intent);
                }
                else {
                    Intent intent = new Intent(ApplicationEx.getApp(),
                            DownloadService.class);
                    intent.putExtra(Constants.URLS, new String[]{epUrl});
                    uiContext.startService(intent);
                }
            }
        };
        t.start();
    }
    
    public void showDetails(int position) {
        String[] values = ApplicationEx.dbHelper.getEpisode(
                (int) getItemId(position));
        callback.showDetailsDialog(values);
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
        if(getCount() < 1) {
            if (ApplicationEx.dbHelper.getFeedCount() < 1) {
                if (sync)
                    return ApplicationEx.getApp().getString(
                            R.string.emptyFeedsSync);
                else
                    return ApplicationEx.getApp().getString(
                            R.string.emptyFeeds);
            }
            else {
                String text = Constants.EMPTY;
                if (isFeed)
                    text = uiContext.getString(R.string.emptyFeedsFiltered);
                else
                    text = uiContext.getString(R.string.emptyEpisodes);
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
        int position;
        
        private Bitmap bitmap;
        
        private int readType;
        private int downloaded;
        private int sortType;
        private int archive;
        private String emptyText;
        private int task;
        private FragmentEpisodes fragment;
        private int feedId;
        private boolean moving;
        private boolean down;
        
        private Task bitmapImageTask;
        private ImageView imageView;
        private boolean showEmpty = false;

        public Task(ImageView imageView, boolean read, int id, int position,
                String url, int readType, int downloaded, int sortType,
                int archive, int task, FragmentEpisodes fragment, int feedId,
                boolean moving, boolean down) {
            imageViewReference = new WeakReference<ImageView>(imageView);
            this.read = read;
            this.id = id;
            this.position = position;
            this.url = url;
            this.readType = readType;
            this.downloaded = downloaded;
            this.sortType = sortType;
            this.archive = archive;
            this.task = task;
            this.fragment = fragment;
            this.feedId = feedId;
            this.moving = moving;
            this.down = down;
        }
        
        @Override
        protected void onPreExecute() {
            switch(task) {
            case TASK_MARK_EPISODES_READ:
            case TASK_DELETE_EPISODES:
                int tempId;
                for (int i = 0; i < contextChecked.size(); i++) {
                    tempId = contextChecked.keyAt(i);
                    Log.d(Constants.LOG_TAG, "APPLYING: " + tempId);
                    showBusys.add(tempId);
                    contextIds.add(tempId);
                    contextBusys.get(tempId).setVisibility(View.VISIBLE);
                    contextPlayings.get(tempId).setVisibility(View.INVISIBLE);
                    contextDownloads.get(tempId).setVisibility(View.INVISIBLE);
                }
                break;
            case TASK_MARK_EPISODE_READ:
            case TASK_DELETE_EPISODE:
                contextBusys.get(id).setVisibility(View.VISIBLE);
                contextPlayings.get(id).setVisibility(View.INVISIBLE);
                contextDownloads.get(id).setVisibility(View.INVISIBLE);
                break;
            case TASK_GET_PODCASTS:
                callback.setProgressVisible(true);
                callback.setEmptyVisible(false);
                mEpisodes.clear();
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
                bitmap = getBitmap(feedId);
                if (imageViewReference != null) {
                    imageView = imageViewReference.get();
                    bitmapImageTask = getBitmapImageTask(imageView);
                    publishProgress();
                }
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
                refresh();
                break;
            case TASK_DOWNLOAD_EPISODE:
                if (!ApplicationEx.dbHelper.isDownloaded(id)) {
                    if (Util.isMyServiceRunning(
                            DownloadService.class.getName())) {
                        Intent intent = new Intent(
                                Constants.ACTION_START_DOWNLOAD);
                        intent.putExtra(Constants.URLS, new String[]{url});
                        uiContext.sendBroadcast(intent);
                    }
                    else {
                        Intent intent = new Intent(uiContext, 
                                DownloadService.class);
                        intent.putExtra(Constants.URLS, new String[]{url});
                        uiContext.startService(intent);
                    }
                }
                break;
            case TASK_DELETE_EPISODE:
                if (ApplicationEx.dbHelper.isDownloaded(id)) {
                    new File(ApplicationEx.dbHelper.getEpisodeLocation(id))
                            .delete();
                    ApplicationEx.dbHelper.episodeDownloaded(id, false);
                    // Update the current playlist (downloaded episodes)
                    Intent intent = new Intent(
                            Constants.ACTION_NEW_DOWNLOAD);
                    // Set the name sorted playlist, if currently set
                    if (ApplicationEx.dbHelper.getCurrentPlaylistType() ==
                            Constants.PLAYLIST_BY_NAME)
                        intent.putIntegerArrayListExtra(Constants.ID_LIST, 
                                Util.getFeedEpisodeIdList(true));
                    // Send the playlist broadcast
                    ApplicationEx.getApp().sendBroadcast(intent);
                    mDuration = Util.getAllEpisodesDuration();
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
                mDuration = Util.getAllEpisodesDuration();
                refresh();
                break;
            case TASK_REFRESH_EPISODES:
            case TASK_GET_PODCASTS:
                map.clear();
                refresh();
                mDuration = Util.getAllEpisodesDuration();
                break;
            }
            return null;
        }

        protected void onPostExecute(Void nothing) {
            switch(task) {
            case TASK_GET_IMAGE:
            case TASK_DOWNLOAD_EPISODE:
                break;
            case TASK_MARK_EPISODE_READ:
            case TASK_DELETE_EPISODE:
                showBusys.remove((Integer)id);
                contextIds.remove((Integer)id);
                if (contextBusys.get(id) != null) {
                    contextBusys.get(id).setVisibility(View.INVISIBLE);
                    contextBusys.remove((Integer)id);
                }
                contextPlayings.remove((Integer)id);
                contextDownloads.remove((Integer)id);
                if (ApplicationEx.dbHelper.getCurrentPlaylistType() ==
                        Constants.PLAYLIST_BY_DATE) {
                    Intent intent = new Intent(Constants.ACTION_NEW_DOWNLOAD);
                    intent.putIntegerArrayListExtra(Constants.ID_LIST,
                            epIdList);
                    uiContext.sendBroadcast(intent);
                }
                uiContext.sendBroadcast(
                        new Intent(Constants.ACTION_REFRESH_FEEDS));
                uiContext.sendBroadcast(
                        new Intent(Constants.ACTION_REFRESH_DURATION));
                callback.setEmptyVisible(showEmpty);
                break;
            case TASK_MARK_EPISODES_READ:
            case TASK_DELETE_EPISODES:
                int tempId;
                for (int i = 0; i < contextChecked.size(); i++) {
                    tempId = contextChecked.keyAt(i);
                    showBusys.remove((Integer)tempId);
                    contextIds.remove((Integer)tempId);
                    contextBusys.get(tempId).setVisibility(View.INVISIBLE);
                    contextBusys.remove((Integer)tempId);
                    contextPlayings.remove((Integer)tempId);
                    contextDownloads.remove((Integer)tempId);
                }
                if (ApplicationEx.dbHelper.getCurrentPlaylistType() ==
                        Constants.PLAYLIST_BY_DATE) {
                    Intent intent = new Intent(Constants.ACTION_NEW_DOWNLOAD);
                    intent.putIntegerArrayListExtra(Constants.ID_LIST,
                            epIdList);
                    uiContext.sendBroadcast(intent);
                }
                uiContext.sendBroadcast(
                        new Intent(Constants.ACTION_REFRESH_FEEDS));
                uiContext.sendBroadcast(
                        new Intent(Constants.ACTION_REFRESH_DURATION));
                callback.setEmptyVisible(showEmpty);
                resetViews();
                break;
            case TASK_REFRESH_EPISODES:
            case TASK_GET_PODCASTS:
                uiContext.sendBroadcast(
                        new Intent(Constants.ACTION_REFRESH_DURATION));
                callback.setEmptyVisible(true);
                callback.setProgressVisible(false);
                if (ApplicationEx.dbHelper.getCurrentPlaylistType() ==
                        Constants.PLAYLIST_BY_DATE) {
                    Intent intent = new Intent(Constants.ACTION_NEW_DOWNLOAD);
                    intent.putIntegerArrayListExtra(Constants.ID_LIST,
                            epIdList);
                    uiContext.sendBroadcast(intent);
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
                if (fragment != null)
                    fragment.finishPaging(task == TASK_GET_PODCASTS);
                callback.changeEmptyText(emptyText);
                break;
            }
        }
        
        private void refresh() {
            epIdMap = Util.getEpisodeOrder();
            count = ApplicationEx.dbHelper.getEpisodeCount(readType, downloaded,
                    Constants.PLAYLIST_SORT_EPISODE |
                        Constants.PLAYLIST_SORT_DATE, sortType, archive);
            total = Constants.PAGE_SIZE*2;
            if (ApplicationEx.dbHelper.getAppEpisodePosition() >= 0) {
                int newPosition = epIdMap.indexOfValue(
                        ApplicationEx.dbHelper.getAppEpisodePosition());
                Log.e(Constants.LOG_TAG, "newPosition: " + newPosition);
                offset = newPosition - Constants.PAGE_SIZE;
                if (epIdMap.size() - offset < Constants.PAGE_SIZE*2)
                    offset = epIdMap.size() - Constants.PAGE_SIZE*2;
                if (offset < 0)
                    offset = 0;
            }
            else {
                if (moving) {
                    if (!down)
                        offset -= Constants.PAGE_SIZE;
                    else
                        offset += Constants.PAGE_SIZE;
                    if (offset < 0) {
                        offset = 0;
                    }
                }
                if (count - offset < total) {
                    offset = count - Constants.PAGE_SIZE*2;
                }
            }
            if (offset < 0) {
                offset = 0;
                total = count;
            }
            mEpisodes = Util.getAllEpisodes(readType, downloaded, 
                    Constants.PLAYLIST_SORT_EPISODE|
                        Constants.PLAYLIST_SORT_DATE, sortType, archive, true,
                    offset, total);
            for (int i = 0; i < mEpisodes.size(); i++) {
                if (epIdMap.indexOfValue((int)getItemId(i)) > -1) {
                    map.put(epIdMap.indexOfValue(
                            (int)getItemId(i)),
                            new SimpleEntry<Integer,Bitmap>(getFeedId(i),
                                    getBitmap(getFeedId(i))));
                }
            }
            Log.i(Constants.LOG_TAG, "count: " + ApplicationEx.epAdapter.getCount());
            if (!mEpisodes.isEmpty()) {
                if (mEpisodes.get(mEpisodes.size()-1) != null &&
                        getItemId(mEpisodes.size()-1) !=
                        epIdMap.valueAt(epIdMap.size()-1))
                    mEpisodes.add(null);
                if (mEpisodes.get(0) != null && epIdMap.size() > 0 &&
                        mEpisodes.get(0).getId() != epIdMap.get(0))
                    mEpisodes.add(0, null);
            }
            else
                showEmpty = true;
            emptyText = getEmptyText(readType, downloaded, false);
            publishProgress();
            // TODO Add ability to stream episodes here
            epIdList = Util.getEpisodeIdList(true, 0, 0);
        }
    }
    
    private int offset;
    private int total;
    private int count;
    
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
        if (mEpisodes != null && mEpisodes.size() > position)
            return mEpisodes.get(position).getUrl();
        return null;
    }
    
    public long getTotalDuration() {
        return mDuration;
    }
    
    private String getTitle(int position) {
        if (mEpisodes != null && mEpisodes.size() > position)
            return mEpisodes.get(position).getTitle();
        return null;
    }
    
    private boolean getRead(int position) {
        if (mEpisodes != null && mEpisodes.size() > position)
            return mEpisodes.get(position).getUnread();
        return false;
    }
    
    private boolean getDownloaded(int position) {
        if (mEpisodes != null && mEpisodes.size() > position)
            return mEpisodes.get(position).getDownloaded();
        return false;
    }
    
    private boolean getPlaying(int position) {
        if (ApplicationEx.dbHelper.getCurrentEpisode() == 
                    mEpisodes.get(position).getId())
            return true;
        return false;
    }
    
    public void clearContexts() {
        contextIds.clear();
        showBusys.clear();
        contextPlayings.clear();
        contextDownloads.clear();
        contextBusys.clear();
    }
    
    private Bitmap getBitmap(int id) {
        Bitmap bitmap = memoryCache.get(id);
        if (bitmap != null)
            return bitmap;
        String imageLocation = TextUtils.concat(ApplicationEx.cacheLocation,
                Constants.FEEDS_LOCATION, Integer.toString(id), File.separator)
                        .toString();
        if (Util.findFile(imageLocation, TextUtils.concat(Integer.toString(id),
                Constants.SMALL, Constants.PNG).toString())) {
            bitmap = BitmapFactory.decodeFile(TextUtils.concat(imageLocation,
                    Integer.toString(id), Constants.SMALL, Constants.PNG)
                            .toString());
            memoryCache.put(id, bitmap);
        }
        return bitmap;
    }

}
