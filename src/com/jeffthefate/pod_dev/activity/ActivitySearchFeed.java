package com.jeffthefate.pod_dev.activity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.Locale;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.jeffthefate.pod_dev.Constants;
import com.jeffthefate.pod_dev.JsonAdapter;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.VersionedActionBar;

public class ActivitySearchFeed extends Activity {
    
    private ListView searchList;
    private ProgressBar progress;
    private Context context = this;
    JSONArray jsonResults = null;
    
    private EditText entryBox = null;
    InputMethodManager imm = null;
    
    SearchTask searchTask = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed_search);
        VersionedActionBar.newInstance().create(this).setDisplayHomeAsUp();
        progress = (ProgressBar) findViewById(R.id.SearchProgress);
        entryBox = (EditText) findViewById(R.id.SearchEntry);
        entryBox.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId,
                    KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && 
                                event.getAction() == KeyEvent.ACTION_DOWN &&
                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    startSearch(entryBox.getText().toString());
                }
                return false;
            }
        });
        entryBox.setFocusableInTouchMode(true);
        entryBox.requestFocus();
        imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        Button doneButton = (Button) findViewById(R.id.SearchButton);
        searchList = (ListView) findViewById(R.id.SearchList);
        doneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                imm.hideSoftInputFromWindow(entryBox.getWindowToken(), 0);
                startSearch(entryBox.getText().toString());                
            }
        });
        searchList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, 
                    int position, long id) {
                String feedUrl = "";
                try {
                    feedUrl = jsonResults.getJSONObject(position).optString(
                            "feedUrl");
                } catch (JSONException e) {
                    Log.e(Constants.LOG_TAG, 
                            "Bad JSON object from results array at " + position, 
                            e);
                }
                Intent detailIntent = new Intent(getApplicationContext(), 
                        ActivityFeedDetails.class);
                detailIntent.putExtra("url", feedUrl);
                startActivity(detailIntent);
            }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (searchList.getAdapter() == null)
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        else
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String term = extras.getString("term");
            if (term != null)
                entryBox.setText(term);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {    
        switch (item.getItemId()) 
        {        
        case android.R.id.home:            
            Intent mainIntent = new Intent(this, ActivitySplash.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(mainIntent);
            return true;        
        default:            
            return super.onOptionsItemSelected(item);    
        }
    }
    
    private void startSearch(String searchTerm) {
        if (searchTask != null)
            searchTask.cancel(true);
        searchList.setVisibility(View.INVISIBLE);
        progress.setVisibility(View.VISIBLE);
        String charSet = "US-ASCII";
        if (searchTerm.length() > 0) {
            String url = "";
            try {
                String locale = Locale.getDefault().toString();
                if (!java.nio.charset.Charset.isSupported(charSet))
                    throw new UnsupportedEncodingException();
                url = Constants.SEARCH_URL_1 + 
                                    URLEncoder.encode(searchTerm, "US-ASCII") + 
                             Constants.SEARCH_URL_2 + 
                                 locale.substring(locale.indexOf("_")+1) +
                             Constants.SEARCH_URL_3 + "10" + 
                             Constants.SEARCH_URL_4 + locale.toLowerCase() +
                             Constants.SEARCH_URL_5 + "Yes";
            } catch (UnsupportedEncodingException e) {
                Log.e(Constants.LOG_TAG, "Unsupported encoding", e);
            }
            searchTask = new SearchTask(url);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                searchTask.execute();
            else
                searchTask.executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
    
    class SearchTask extends AsyncTask<Void, Void, JSONArray> {
        
        private String url;
        
        private SearchTask(String url) {
            this.url = url;
        }

        @Override
        protected JSONArray doInBackground(Void... nothing) {
            Document doc = null;
            Connection mConnection = Jsoup.connect(url).timeout(15000);
            try {
                doc = mConnection.get();
            } catch (MalformedURLException e) {
                Log.e(Constants.LOG_TAG, "Unable to create URL from " +
                        url, e);
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, url + 
                        " failed to respond with code " + 
                        mConnection.response().statusCode(), e);
            }
            if (doc != null) {
                String jsonString = StringEscapeUtils.unescapeHtml4(
                        doc.body().html());
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(jsonString);
                } catch (JSONException e) {
                    Log.e(Constants.LOG_TAG, 
                            "Bad JSON object from string: " + jsonString, e);
                }
                try {
                    jsonResults = jsonObject.getJSONArray("results");
                } catch (JSONException j) {
                    Log.e(Constants.LOG_TAG, 
                            "Bad JSON element: results", j);
                }
            }
            return jsonResults;
        }
        
        protected void onPostExecute(JSONArray jsonResults) {
            JsonAdapter jsonAdapter = new JsonAdapter(
                    context, jsonResults);
            searchList.setAdapter(jsonAdapter);
            progress.setVisibility(View.INVISIBLE);
            searchList.setVisibility(View.VISIBLE);
        }
        
    }
    
}