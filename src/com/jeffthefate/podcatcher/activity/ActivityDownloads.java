package com.jeffthefate.podcatcher.activity;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.widget.ListView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.R;
import com.jeffthefate.podcatcher.fragment.FragmentDownloads;

public class ActivityDownloads extends ActivityBase {
    
    ArrayList<String> epList;
    private int currEpId;
    private FragmentDownloads fDownloads;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSherlock().getActionBar().setDisplayHomeAsUpEnabled(true);
        FragmentManager fMan = getSupportFragmentManager();
        fDownloads = new FragmentDownloads();
        fMan.beginTransaction().replace(android.R.id.content, fDownloads,
                Constants.FRAGMENT_DOWNLOADS).commit();
        epList = getIntent().getStringArrayListExtra(Constants.EP_LIST);
        currEpId = getIntent().getIntExtra(Constants.CURR_EP_ID, -1);
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        epList = intent.getStringArrayListExtra(Constants.EP_LIST);
        currEpId = intent.getIntExtra(Constants.CURR_EP_ID, -1);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.download_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {    
        switch (item.getItemId()) 
        {        
        case android.R.id.home:            
            Intent mainIntent = new Intent(this, ActivityMain.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(mainIntent);
            return true;
        case R.id.CancelDownloads:
            ApplicationEx.getApp().sendBroadcast(
                    new Intent(Constants.ACTION_CANCEL_ALL_DOWNLOADS));
            return true;
        case R.id.MultiSelect:
            if (fDownloads.getChoiceMode() != ListView.CHOICE_MODE_MULTIPLE)
                fDownloads.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            else
                fDownloads.setChoiceMode(ListView.CHOICE_MODE_NONE);
            return true;
        default:            
            return super.onOptionsItemSelected(item);    
        }
    }
    
    public ArrayList<String> getEpList() {
        return epList;
    }
    
    public void setEpList(ArrayList<String> epList) {
        this.epList = epList;
    }
    
    public int getCurrEpId() {
        return currEpId;
    }
    
    public void setCurrEpId(int currEpId) {
        this.currEpId = currEpId;
    }
    
}