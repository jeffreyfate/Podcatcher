package com.jeffthefate.podcatcher.activity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.JsonAdapter;
import com.jeffthefate.podcatcher.R;

public class ActivitySearchFeed extends ActivityBase {
    
    private ListView searchList;
    private ProgressBar progress;
    private Context context = this;
    private TextView searchError;
    JSONArray jsonResults = null;
    
    private EditText entryBox = null;
    InputMethodManager imm = null;
    
    SearchTask searchTask = null;
    
    private Pattern pattern;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed_search);
        getSherlock().getActionBar().setDisplayHomeAsUpEnabled(true);
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
        registerForContextMenu(searchList);
        pattern = Pattern.compile(Patterns.WEB_URL.pattern());
        doneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                imm.hideSoftInputFromWindow(entryBox.getWindowToken(), 0);
                if (pattern.matcher(entryBox.getText().toString()).find()) {
                    Intent detailIntent = new Intent(context, 
                            ActivityFeedDetails.class);
                    detailIntent.putExtra(Constants.URL,
                            entryBox.getText().toString());
                    context.startActivity(detailIntent);
                }
                else    
                    startSearch(entryBox.getText().toString());                
            }
        });
        searchError = (TextView) findViewById(R.id.SearchError);
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
            String term = extras.getString(Constants.TERM);
            if (term != null)
                entryBox.setText(term);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        getMenuInflater().inflate(R.menu.search_context, menu);
    }
    
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo info;
        if (item.getMenuInfo().getClass().equals(AdapterContextMenuInfo.class))
            info = (AdapterContextMenuInfo) item.getMenuInfo();
        else
            return false;
        switch(item.getItemId()) {
        case R.id.SubscribeMenu:
            ((JsonAdapter)searchList.getAdapter()).subscribe(info.position);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(
            com.actionbarsherlock.view.MenuItem item) 
    {    
        switch (item.getItemId()) 
        {        
        case android.R.id.home:            
            Intent mainIntent = new Intent(this, ActivityMain.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(mainIntent);
            return true;        
        default:            
            return super.onOptionsItemSelected(item);    
        }
    }
    
    @SuppressLint("NewApi")
    private void startSearch(String searchTerm) {
        if (searchTask != null)
            searchTask.cancel(true);
        searchList.setVisibility(View.INVISIBLE);
        searchError.setVisibility(View.INVISIBLE);
        progress.setVisibility(View.VISIBLE);
        String charSet = Constants.US_ASCII;
        if (searchTerm.length() > 0) {
            String url = Constants.EMPTY;
            try {
                String locale = Locale.getDefault().toString();
                if (!java.nio.charset.Charset.isSupported(charSet))
                    throw new UnsupportedEncodingException();
                url = TextUtils.concat(Constants.SEARCH_URL_1,
                          URLEncoder.encode(searchTerm, Constants.US_ASCII),
                          Constants.SEARCH_URL_2, locale.substring(
                              locale.indexOf(Constants.UNDERSCORE) + 1),
                          Constants.SEARCH_URL_3, Constants.TEN,
                          Constants.SEARCH_URL_4,
                          locale.toLowerCase(Locale.getDefault()),
                          TextUtils.concat(Constants.SEARCH_URL_5,
                                  Constants.YES).toString()).toString();
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
                    jsonResults = jsonObject.getJSONArray(Constants.RESULTS);
                } catch (JSONException j) {
                    Log.e(Constants.LOG_TAG, 
                            "Bad JSON element: results", j);
                }
            }
            return jsonResults;
        }
        
        protected void onPostExecute(JSONArray jsonResults) {
            if (jsonResults == null) {
                searchError.setText(Constants.ERROR_GETTING_FEED);
                progress.setVisibility(View.INVISIBLE);
                searchList.setVisibility(View.INVISIBLE);
                searchError.setVisibility(View.VISIBLE);
            }
            else if (jsonResults.length() == 0) {
                searchError.setText(Constants.NONE_FOUND);
                progress.setVisibility(View.INVISIBLE);
                searchList.setVisibility(View.INVISIBLE);
                searchError.setVisibility(View.VISIBLE);
            }
            else {
                JsonAdapter jsonAdapter = new JsonAdapter(context, jsonResults);
                searchList.setAdapter(jsonAdapter);
                searchError.setVisibility(View.INVISIBLE);
                progress.setVisibility(View.INVISIBLE);
                searchList.setVisibility(View.VISIBLE);
            }
        }
        
    }
    
}