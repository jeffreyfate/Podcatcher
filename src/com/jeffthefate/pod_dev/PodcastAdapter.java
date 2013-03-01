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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jeffthefate.pod_dev.service.DownloadService;
import com.jeffthefate.pod_dev.service.PlaybackService;

public class PodcastAdapter extends BaseAdapter {
    
    private Context mContext;
    private ArrayList<Episode> mEpisodes;
    private long mDuration;
    private String mCaller;
    
    private AdapterChange callback;
    
    boolean mIsFeed = false;
    boolean updateImage = true;
    boolean mSync = false;
    
    private static Bitmap temp;
    private static Resources resources;
    
    private int contextPosition;
    private boolean contextRead;
    private boolean contextDownloaded;
    
    private final int TASK_GET_IMAGE = 0;
    private final int TASK_MARK_EPISODE_READ = 2;
    private final int TASK_REFRESH_EPISODES = 4;
    private final int TASK_DOWNLOAD_EPISODE = 6;
    private final int TASK_DELETE_EPISODE = 7;
    private final int TASK_GET_PODCASTS = 8;
    
    private View mConvertView;
    
    private ImageView leftPlaying;
    private ImageView rightPlaying;
    
    MemoryCache memoryCache = new MemoryCache();
    
    protected static class ViewHolder {
        TextView text1;
        TextView text2;
        ImageView icon;
        RelativeLayoutEx button;
        LinearLayout playing;
        ImageView playingleft;
        ImageView playingright;
        ImageView download;
        int id;
    }
    
    public interface AdapterChange {
        public void onAdapterEmptyChange();
        public void onAdapterProgressChange();
        public void onAdapterEmptyTextChange(String text);
    }
    
    public PodcastAdapter(Context context, int read, int downloaded,
            int sortType, int archive, boolean isFeed, String caller, 
            boolean sync, AdapterChange callback) {
        mContext = context;
        mIsFeed = isFeed;
        mCaller = caller;
        mSync = sync;
        this.callback = callback;
        temp = BitmapFactory.decodeResource(resources, 
                R.drawable.ic_album_loading);
        resources = mContext.getResources();
        Task task = new Task(null, false, -1, -1, null, read, downloaded, 
                sortType, archive, TASK_GET_PODCASTS);
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                task.execute();
            else
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (RejectedExecutionException e) {}
    }
    
    @Override
    public int getCount() {
        if (mEpisodes != null)
            return mEpisodes.size();
        return 0;
    }

    @Override
    public Object getItem(int position) {
        if (mEpisodes != null && mEpisodes.size() > position)
            return mEpisodes.get(position);
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (mEpisodes != null && mEpisodes.size() > position)
            return (long) mEpisodes.get(position).getId();
        return -1;
    }
    
    public void startLeftPlaying() {
        if (leftPlaying != null)
            ((AnimationDrawable) leftPlaying.getBackground()).start();
    }
    
    public void stopLeftPlaying() {
        if (leftPlaying != null)
            ((AnimationDrawable) leftPlaying.getBackground()).stop();
    }
    
    public void startRightPlaying() {
        if (rightPlaying != null)
            ((AnimationDrawable) rightPlaying.getBackground()).start();
    }
    
    public void stopRightPlaying() {
        if (rightPlaying != null)
            ((AnimationDrawable) rightPlaying.getBackground()).stop();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.row, parent, false);
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
            holder.button = (RelativeLayoutEx)convertView.findViewById(
                    R.id.ButtonLayout);
            holder.id = (int) getItemId(position);
            convertView.setTag(holder);
            updateImage = true;
        }
        else {
            holder = (ViewHolder)convertView.getTag();
            if (holder.id != (int) getItemId(position)) {
                updateImage = true;
                holder.id = (int) getItemId(position);
            }
        }
        holder.text1.setTag(position);
        holder.text1.setText(Jsoup.parse(StringEscapeUtils.unescapeHtml4(
                getTitle(position) == null ? "" : getTitle(position))).text());
        if (getRead(position))
            holder.text1.setTextColor(Color.WHITE);
        else
            holder.text1.setTextColor(Color.GRAY);
        holder.text2.setTag(position);
        holder.text2.setText(Jsoup.parse(StringEscapeUtils.unescapeHtml4(
                mEpisodes.get(position).getSubtitle())).text());
        final String url = getUrl(position) == null ? "" : getUrl(position);
        if (updateImage && cancelPotentialImage(url, holder.icon)) {
            Task task = new Task(holder.icon, false, -1, -1, url, -1, -1, -1, 
                    -1, TASK_GET_IMAGE);
            TempDrawable bitmapDrawable = new TempDrawable(task);
            holder.icon.setImageDrawable(bitmapDrawable);
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    task.execute();
                else
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } catch (RejectedExecutionException e) {}
        }
        convertView.setBackgroundResource(R.drawable.color_btn_context_menu);
        convertView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchEpisodesAction(pos);
                memoryCache.clear();
            }
        });
        final View view = convertView;
        holder.button.setBackgroundResource(R.drawable.color_btn_context_menu);
        holder.button.setTag(position);
        holder.button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pos < mEpisodes.size()) {
                    contextPosition = pos;
                    contextRead = getRead(pos);
                    contextDownloaded = getDownloaded(pos);
                    mConvertView = view;
                    view.performLongClick();
                }
            }
        });
        holder.playingleft.setBackgroundResource(R.anim.playing_left);
        holder.playingright.setBackgroundResource(R.anim.playing_right);
        if (ApplicationEx.dbHelper.getCurrentPlaylistType() == 1 
                && ApplicationEx.dbHelper.getCurrentEpisode() == 
                    mEpisodes.get(pos).getId()) {
            leftPlaying = holder.playingleft;
            rightPlaying = holder.playingright;
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
    
    public void refreshEpisodes(int readType, int downloaded, int sortType,
            int archive) {
        Task task = new Task(null, false, -1, -1, "", readType, downloaded,
                sortType, archive, TASK_REFRESH_EPISODES);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    public void markEpisodeRead(boolean read, int readType, int downloaded, 
            int sortType, int position, int archive) {
        Task task = new Task(null, read, getContextId(), position, "", readType,
                downloaded, sortType, archive, TASK_MARK_EPISODE_READ);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    public void deleteEpisode(int position, int readType, int downloaded,
            int sortType, int archive) {
        int id = (int) getItemId(position);
        Task task = new Task(null, false, id, position, null, readType,
                downloaded, sortType, archive, TASK_DELETE_EPISODE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    public void downloadEpisode(int position) {
        int id = (int) getItemId(position);
        String url = ApplicationEx.dbHelper.getEpisodeUrl(id);
        Task task = new Task(null, false, id, position, url, -1, -1, -1, -1, 
                        TASK_DOWNLOAD_EPISODE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            task.execute();
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    public void dispatchEpisodesAction(int position) {
        final int pos = position;
        Thread t = new Thread(){
            public void run(){
                String epUrl = mEpisodes.get(pos).getUrl();
                int epId = mEpisodes.get(pos).getId();
                if (ApplicationEx.dbHelper.isDownloaded(epId)) {
                    // TODO Add ability to stream episodes here
                    ArrayList<Integer> epIdList = Util.getEpisodeIdList(true);
                    Intent playbackIntent = new Intent(mContext,
                            PlaybackService.class);
                    playbackIntent.putExtra("startEp", epId);
                    playbackIntent.putIntegerArrayListExtra("episodes", 
                            epIdList);
                    mContext.startService(playbackIntent);
                    ApplicationEx.dbHelper.setCurrentPlaylistType(1);
                    mContext.sendBroadcast(
                            new Intent(Constants.ACTION_START_PLAYBACK));
                }
                else if (Util.isMyServiceRunning(
                        DownloadService.class.getName())) {
                    Intent intent = new Intent(Constants.ACTION_START_DOWNLOAD);
                    intent.putExtra("urls", new String[]{epUrl});
                    mContext.sendBroadcast(intent);
                }
                else {
                    Intent intent = new Intent(ApplicationEx.getApp(),
                            DownloadService.class);
                    intent.putExtra("urls", new String[]{epUrl});
                    mContext.startService(intent);
                }
            }
        };
        t.start();
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
                if (mSync)
                    return ApplicationEx.getApp().getString(
                            R.string.emptyFeedsSync);
                else
                    return ApplicationEx.getApp().getString(
                            R.string.emptyFeeds);
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
                return text;
            }
        }
        return "";
    }
    
    private ArrayList<Integer> epIdList;
    
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

        public Task(ImageView imageView, boolean read, int id, int position,
                String url, int readType, int downloaded, int sortType,
                int archive, int task) {
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
        }
        
        @Override
        protected void onPreExecute() {
            callback.onAdapterEmptyChange();
            callback.onAdapterProgressChange();
            switch(task) {
            case TASK_GET_PODCASTS:
            case TASK_REFRESH_EPISODES:
                updateData(new ArrayList<Episode>());
            }
        }
        
        @Override
        protected Void doInBackground(Void... nothing) {
            switch(task) {
            case TASK_GET_IMAGE:
                bitmap = getBitmap(url);
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
            case TASK_DELETE_EPISODE:
                if (ApplicationEx.dbHelper.isDownloaded(id)) {
                    ApplicationEx.dbHelper.markRead(id, true);
                    new File(ApplicationEx.dbHelper.getEpisodeLocation(id))
                            .delete();
                    ApplicationEx.dbHelper.episodeDownloaded(id, false);
                }
                refresh();
                break;
            case TASK_REFRESH_EPISODES:
            case TASK_GET_PODCASTS:
                refresh();
                mDuration = 0;
                for (Episode episode : mEpisodes) {
                    mDuration += episode.getDuration();
                }
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
            case TASK_MARK_EPISODE_READ:
            case TASK_DELETE_EPISODE:
                update();
                mContext.sendBroadcast(
                        new Intent(Constants.ACTION_REFRESH_FEEDS));
            case TASK_REFRESH_EPISODES:
            case TASK_GET_PODCASTS:
                mContext.sendBroadcast(
                        new Intent(Constants.ACTION_REFRESH_DURATION));
                update();
                break;
            }
            updateImage = false;
            callback.onAdapterEmptyChange();
            callback.onAdapterProgressChange();
        }
        
        @Override
        protected void onProgressUpdate(Void... nothing) {
            callback.onAdapterEmptyTextChange(emptyText);
        }
        
        private void refresh() {
            mEpisodes = Util.getAllEpisodes(readType, downloaded, 
                    Constants.PLAYLIST_SORT_EPISODE|
                        Constants.PLAYLIST_SORT_DATE, 
                    sortType, archive);
            emptyText = getEmptyText(readType, downloaded, false);
            publishProgress();
            // TODO Add ability to stream episodes here
            if (ApplicationEx.dbHelper.getCurrentPlaylistType() == 1)
                epIdList = Util.getEpisodeIdList(true);
        }
        
        private void update() {
            notifyDataSetChanged();
            if (ApplicationEx.dbHelper.getCurrentPlaylistType() == 1) {
                Intent intent = new Intent(Constants.ACTION_NEW_DOWNLOAD);
                intent.putIntegerArrayListExtra("idList", epIdList);
                mContext.sendBroadcast(intent);
            }
        }
    }
    
    public void updateData(ArrayList<Episode> episodes) {
        mEpisodes = episodes;
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
    
    private Bitmap getBitmap(String url) {
        Bitmap bitmap = memoryCache.get(url);
        if (bitmap != null)
            return bitmap;
        int feedId = ApplicationEx.dbHelper.getInt(url, 
                mIsFeed ? DatabaseHelper.COL_FEED_ID : 
                    DatabaseHelper.COL_EP_FEED_ID, 
                mIsFeed ? DatabaseHelper.FEED_TABLE : 
                    DatabaseHelper.EPISODE_TABLE, 
                mIsFeed ? DatabaseHelper.COL_FEED_ADDRESS : 
                    DatabaseHelper.COL_EPISODE_URL);
        String imageLocation = ApplicationEx.cacheLocation +
                Constants.FEEDS_LOCATION + feedId + File.separator;
        if (Util.findFile(imageLocation, feedId + "_small" + ".png")) {
            bitmap = BitmapFactory.decodeFile(imageLocation + feedId + "_small" 
                    + ".png");
            memoryCache.put(url, bitmap);
        }
        return bitmap;
    }

}
