package com.jeffthefate.podcatcher.activity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.view.MenuItem;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.R;

public class ActivityEpisodeDetails extends ActivityBase {
    
    private HashMap<String, String> episode = null;
    private String imageUrl = null;
    
    private ImageView feedImage = null;
    private TextView episodeTitle = null;
    private TextView episodeSummary = null;
    private TextView episodeDuration = null;
    
    @SuppressLint("NewApi")
    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.episode_details);
        getSherlock().getActionBar().setDisplayHomeAsUpEnabled(true);
        feedImage = (ImageView) findViewById(R.id.FeedImage);
        episodeTitle = (TextView) findViewById(R.id.EpisodeTitle);
        episodeSummary = (TextView) findViewById(R.id.EpisodeSummary);
        episodeDuration = (TextView) findViewById(R.id.EpisodeDuration);
        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            if (intent.hasExtra(Constants.EPISODE)) {
                episode = (HashMap<String, String>) intent.getSerializableExtra(
                        Constants.EPISODE);
            }
            if (intent.hasExtra(Constants.IMAGE_URL)) {
                imageUrl = intent.getStringExtra(Constants.IMAGE_URL);
            }
        }
        if (imageUrl != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                new ImageTask(imageUrl).execute();
            else
                new ImageTask(imageUrl).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR);
        }
        if (episode != null) {
            episodeTitle.setText(episode.get(Constants.TITLE));
            episodeDuration.setText(episode.get(Constants.ITUNES_DURATION) !=
                    null ? episode.get(Constants.ITUNES_DURATION) :
                        Constants.EMPTY);
            episodeSummary.setText(episode.get(Constants.ITUNES_SUMMARY) != null
                    ? episode.get(Constants.ITUNES_SUMMARY) :
                        episode.get(Constants.DESCRIPTION));
        }
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
    
}