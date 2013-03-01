package com.jeffthefate.pod_dev;

import java.io.InputStream;
import java.lang.ref.WeakReference;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
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
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class JsonAdapter extends BaseAdapter {
    
    private int count = 0;
    private JSONArray jsonArray;
    private Context context;

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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.row, parent, false);
        if (convertView != null) {
            try {
                ((TextView)convertView.findViewById(R.id.text1))
                        .setText(jsonArray.getJSONObject(position)
                                .getString("trackName"));
                ((TextView)convertView.findViewById(R.id.text2))
                    .setText(jsonArray.getJSONObject(position)
                            .getString("artistName"));
                String url = jsonArray.getJSONObject(position).getString(
                        "artworkUrl100");
                ImageView imageView = (ImageView)convertView.findViewById(
                        R.id.image);
                if (cancelPotentialDownload(url, imageView)) {
                    DownloadImageTask task = new DownloadImageTask(imageView,
                            url);
                    DownloadedDrawable downloadedDrawable = 
                        new DownloadedDrawable(task);
                    imageView.setImageDrawable(downloadedDrawable);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                        task.execute();
                    else
                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            } catch (JSONException e) {
                Log.e(Constants.LOG_TAG, 
                        "Bad JSON object from results array at " + position, e);
            }
        }
        return convertView;
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
        }
    }
    
    static Bitmap downloadBitmap(String url) {
        final AndroidHttpClient client =
            AndroidHttpClient.newInstance("Android");
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
                    final Bitmap bitmap = BitmapFactory.decodeStream(
                            inputStream);
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
