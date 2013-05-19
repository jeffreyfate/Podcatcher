package com.jeffthefate.podcatcher;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.RejectedExecutionException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jeffthefate.podcatcher.activity.ActivityFeedDetails;

public class JsonAdapter extends BaseAdapter {
    
    private int count = 0;
    private JSONArray jsonArray;
    private Context context;
    private boolean updateImage = true;
    
    protected static class ViewHolder {
        TextView text1;
        TextView text2;
        ImageView icon;
        RelativeLayoutEx button;
        int id = -1;
    }
    
    private MemoryCacheString memoryCache = new MemoryCacheString();

    public JsonAdapter(Context context, JSONArray jsonArray) {
        this.context = context;
        this.jsonArray = jsonArray;
        count = jsonArray.length();
    }
    
    @Override
    public int getCount() {
        return count;
    }

    @Override
    public Object getItem(int index) {
        return jsonArray.opt(index);
    }

    @Override
    public long getItemId(int index) {
        return (long)index;
    }

    @SuppressLint("NewApi")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        final ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.row, parent, false);
            holder = new ViewHolder();
            holder.text1 = (TextView) convertView.findViewById(R.id.text1);
            holder.text2 = (TextView) convertView.findViewById(R.id.text2);
            holder.icon = (ImageView) convertView.findViewById(R.id.image);
            holder.button = (RelativeLayoutEx)convertView.findViewById(
                    R.id.ButtonLayout);
            if (getItem(position) != null)
                holder.id = (int) getItemId(position);
            convertView.setTag(holder);
            updateImage = true;
        }
        else {
            holder = (ViewHolder) convertView.getTag();
            if (getItem(position) != null) {
                holder.id = (int) getItemId(position);
                updateImage = true;
            }
        }
        holder.text1.setTag(position);
        holder.text2.setTag(position);
        try {
            holder.text1.setText(jsonArray.getJSONObject(position)
                            .getString(Constants.TRACK_NAME));
            holder.text2.setText(jsonArray.getJSONObject(position)
                        .getString(Constants.ARTIST_NAME));
            String url = jsonArray.getJSONObject(position).getString(
                    Constants.ARTWORK_URL_100);
            if (updateImage && cancelPotentialDownload(url, holder.icon)) {
                DownloadImageTask task = new DownloadImageTask(holder.icon,
                        url);
                DownloadedDrawable downloadedDrawable = 
                    new DownloadedDrawable(task);
                holder.icon.setImageDrawable(downloadedDrawable);
                try {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                        task.execute();
                    else
                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } catch (RejectedExecutionException e) {}
            }
        } catch (JSONException e) {
            Log.e(Constants.LOG_TAG, 
                    "Bad JSON object from results array at " + position, e);
        }
        convertView.setBackgroundResource(R.drawable.color_btn_context_menu);
        convertView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String feedUrl = Constants.EMPTY;
                try {
                    feedUrl = jsonArray.getJSONObject(pos).optString(Constants.FEED_URL);
                } catch (JSONException e) {
                    Log.e(Constants.LOG_TAG, 
                            "Bad JSON object from results array at " + pos, e);
                }
                Intent detailIntent = new Intent(context, 
                        ActivityFeedDetails.class);
                detailIntent.putExtra(Constants.URL, feedUrl);
                context.startActivity(detailIntent);
            }
        });
        holder.button.setBackgroundResource(R.drawable.color_btn_context_menu);
        holder.button.setTag(position);
        holder.button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                view.performLongClick();
            }
        });
        return convertView;
    }
    
    @SuppressLint("NewApi")
    public void subscribe(int position) {
        String feedUrl = Constants.EMPTY;
        try {
            feedUrl = jsonArray.getJSONObject(position).optString(Constants.FEED_URL);
        } catch (JSONException e) {
            Log.e(Constants.LOG_TAG, 
                    "Bad JSON object from results array at " + position, e);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            new Util.UpdateFeedTask(null, feedUrl).execute();
        else
            new Util.UpdateFeedTask(null, feedUrl)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    private static boolean cancelPotentialDownload(String url, 
            ImageView imageView) {
        DownloadImageTask bitmapDownloaderTask = 
            getBitmapDownloaderTask(imageView);

        if (bitmapDownloaderTask != null) {
            String bitmapUrl = bitmapDownloaderTask.url;
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
                bitmapDownloaderTask.cancel(true);
            } else {
                // The same URL is already being downloaded.
                return false;
            }
        }
        return true;
    }
    
    private static DownloadImageTask getBitmapDownloaderTask(
            ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = 
                    (DownloadedDrawable)drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }
    
    class DownloadImageTask extends AsyncTask<Void, Void, Bitmap> {
        
        private String url;
        private final WeakReference<ImageView> imageViewReference;
        
        public DownloadImageTask(ImageView imageView, String url) {
            imageViewReference = new WeakReference<ImageView>(imageView);
            this.url = url;
        }
        @Override
        protected Bitmap doInBackground(Void... nothing) {
            return downloadBitmap(url);
        }
        
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null) {
                ImageView imageView = imageViewReference.get();
                DownloadImageTask bitmapDownloaderTask = 
                    getBitmapDownloaderTask(imageView);
                // Change bitmap only if this process is still associated with it
                if (this == bitmapDownloaderTask) {
                    imageView.setImageBitmap(bitmap);
                }
            }
            updateImage = false;
        }
    }
    
    private Bitmap downloadBitmap(String url) {
        Bitmap bitmap = memoryCache.get(url);
        if (bitmap != null)
            return bitmap;
        final AndroidHttpClient client =
            AndroidHttpClient.newInstance(Constants.ANDROID);
        final HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {  
                return null;
            }
            
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent(); 
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    memoryCache.put(url, bitmap);
                    return bitmap;
                } finally {
                    if (inputStream != null) {
                        inputStream.close();  
                    }
                    entity.consumeContent();
                }
            }
        } catch (Exception e) {
            // Could provide a more explicit error message for IOException or IllegalStateException
            getRequest.abort();
        } finally {
            if (client != null) {
                client.close();
            }
        }
        return null;
    }
    
    static class DownloadedDrawable extends ColorDrawable {
        private final WeakReference<DownloadImageTask> 
                bitmapDownloaderTaskReference;

        public DownloadedDrawable(DownloadImageTask bitmapDownloaderTask) {
            super(Color.BLACK);
            bitmapDownloaderTaskReference =
                new WeakReference<DownloadImageTask>(bitmapDownloaderTask);
        }

        public DownloadImageTask getBitmapDownloaderTask() {
            return bitmapDownloaderTaskReference.get();
        }
    }

}
