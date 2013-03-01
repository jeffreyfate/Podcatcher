package com.jeffthefate.pod_dev.activity;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;

import com.jeffthefate.pod_dev.VersionedActionBar;
import com.jeffthefate.pod_dev.fragment.FragmentDownloads;

public class ActivityDownloads extends FragmentActivity {
    
    ArrayList<String> epList;
    private int currEpId;
    private boolean failed;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VersionedActionBar.newInstance().create(this).setDisplayHomeAsUp();
        FragmentManager fMan = getSupportFragmentManager();
        fMan.beginTransaction().replace(android.R.id.content, 
                new FragmentDownloads(), "fDownloads").commit();
        epList = getIntent().getStringArrayListExtra("epList");
        currEpId = getIntent().getIntExtra("currEpId", -1);
        failed = getIntent().getBooleanExtra("failed", false);
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        epList = intent.getStringArrayListExtra("epList");
        currEpId = intent.getIntExtra("currEpId", -1);
        failed = getIntent().getBooleanExtra("failed", false);
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
    
    public boolean getFailed() {
        return failed;
    }
    
    public void setFailed(boolean failed) {
        this.failed = failed;
    }
    
}