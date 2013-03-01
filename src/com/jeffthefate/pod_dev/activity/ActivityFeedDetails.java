package com.jeffthefate.pod_dev.activity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.Constants;
import com.jeffthefate.pod_dev.DatabaseHelper;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.Util;
import com.jeffthefate.pod_dev.VersionedActionBar;
import com.jeffthefate.pod_dev.XmlDomParser;

public class ActivityFeedDetails extends Activity {
    
    private String url = null;
    
    private ImageView feedImage = null;
    private TextView feedTitle = null;
    private TextView feedAuthor = null;
    private TextView feedDescription = null;
    private TextView feedEpisodeCount = null;
    private ListView episodesList = null;
    private ProgressBar progress = null;
    private Button subscribeButton = null;
    private TextView failedText = null;
    
    private Context mContext = this;
    ArrayList<HashMap<String, String>> feedList;
    private String imageUrl = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed_details);
        VersionedActionBar.newInstance().create(this).setDisplayHomeAsUp();
        feedImage = (ImageView) findViewById(R.id.FeedImage);
        feedTitle = (TextView) findViewById(R.id.FeedTitle);
        feedAuthor = (TextView) findViewById(R.id.FeedAuthor);
        feedDescription = (TextView) findViewById(R.id.FeedDescription);
        feedEpisodeCount = (TextView) findViewById(R.id.FeedEpisodeNumber);
        episodesList = (ListView) findViewById(R.id.FeedDetailsList);
        progress = (ProgressBar) findViewById(R.id.EpisodesProgress);
        subscribeButton = (Button) findViewById(R.id.FeedSubscribeButton);
        failedText = (TextView) findViewById(R.id.FailedText);
        Intent intent = getIntent();
        if (intent.getExtras() != null && intent.hasExtra("url")) {
            url = intent.getStringExtra("url");
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                new FeedTask(url).execute();
            else
                new FeedTask(url).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR);
        }
        episodesList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, 
                    int position, long id) {
                HashMap<String, String> item = feedList.get(position);
                Intent episodeIntent = new Intent(
                        mContext, ActivityEpisodeDetails.class);
                episodeIntent.putExtra("episode", item);
                episodeIntent.putExtra("imageUrl", imageUrl);
                startActivity(episodeIntent);
            }
        });
        subscribeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    new Util.UpdateFeedTask(subscribeButton, url).execute();
                else
                    new Util.UpdateFeedTask(subscribeButton, url)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                finish();
            }
        });
        failedText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    new FeedTask(url).execute();
                else
                    new FeedTask(url).executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {    
        switch (item.getItemId()) 
        {        
        case android.R.id.home:            
            finish();
            return true;        
        default:            
            return super.onOptionsItemSelected(item);    
        }
    }
    
    private class ImageTask extends AsyncTask<Void, Void, Bitmap> {
        
        String imageUrl;
        
        private ImageTask(String imageUrl) {
            this.imageUrl = imageUrl;
        }
        
        @Override
        protected Bitmap doInBackground(Void... nothing) {
            Bitmap bitmap = null;
            try {
                URL urlObject = new URL(imageUrl);
                HttpURLConnection hConn = 
                    (HttpURLConnection)urlObject.openConnection();
                hConn.setDoInput(true);
                hConn.connect();
                InputStream input = hConn.getInputStream();
                bitmap = BitmapFactory.decodeStream(input);
            } catch (MalformedURLException e) {
                Log.e(Constants.LOG_TAG, "Unable to create URL from " +
                        imageUrl, e);
            } catch (FileNotFoundException e) {
                Log.e(Constants.LOG_TAG, "Unable to create file at " +
                        imageUrl, e);
            } catch (SocketTimeoutException e) {
                Log.e(Constants.LOG_TAG, "Unable to open connection " +
                        imageUrl, e);
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, "Unable to open connection " +
                        imageUrl, e);
            }
            return bitmap;
        }
        
        protected void onPostExecute(Bitmap bitmap) {
            feedImage.setImageBitmap(bitmap);
        }
        
    }
    
    protected class FeedTask extends 
            AsyncTask<String, Void, HashMap<String, String>> {
        
        String url;
        
        protected FeedTask(String url) {
            this.url = url;
        }
        
        @Override
        protected void onPreExecute() {
            feedImage.setVisibility(View.INVISIBLE);
            episodesList.setVisibility(View.INVISIBLE);
            subscribeButton.setVisibility(View.INVISIBLE);
            failedText.setVisibility(View.INVISIBLE);
            progress.setVisibility(View.VISIBLE);
        }

        @Override
        protected HashMap<String, String> doInBackground(String... url) {
            XmlDomParser parser = new XmlDomParser(url[0]);
            feedList = parser.parseXml(true);
            if (feedList == null)
                return new HashMap<String, String>();
            if (feedList.size() == 0)
                return new HashMap<String, String>();
            HashMap<String, String> feedInfo = feedList.remove(0);
            feedList.remove(feedList.size()-1);
            imageUrl = feedInfo.get("itunes|image") != null ? 
                    feedInfo.get("itunes|image") : feedInfo.get("image > url");
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                new ImageTask(imageUrl).execute();
            else
                new ImageTask(imageUrl).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR);
            if (ApplicationEx.dbHelper.inDb(
                    new String[] {feedInfo.get("title")}, 
                    DatabaseHelper.FEED_TABLE, 
                    new String[] {DatabaseHelper.COL_FEED_TITLE}))
                publishProgress();
            return feedInfo;
        }
        
        protected void onProgressUpdate(Void... nothing) {
            subscribeButton.setEnabled(false);
        }
        
        protected void onPostExecute(HashMap<String, String> info) {
            if (!info.isEmpty()) {
                feedTitle.setText(info.get("title"));
                feedAuthor.setText(info.get("itunes|author"));
                feedDescription.setText(info.get("description"));
                feedEpisodeCount.setText(feedList.size()-2 + " EPISODES");
                episodesList.setAdapter(new SimpleAdapter(mContext, feedList, 
                        R.layout.row_noimage_result, 
                        new String[] {"title", "pubDate"}, 
                        new int[] {R.id.text1, R.id.text2}));
                feedImage.setVisibility(View.VISIBLE);
                episodesList.setVisibility(View.VISIBLE);
                subscribeButton.setVisibility(View.VISIBLE);
            }
            else {
                failedText.setVisibility(View.VISIBLE);
            }
            progress.setVisibility(View.INVISIBLE);
        }
        
    }
    
}