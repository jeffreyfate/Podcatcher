package com.jeffthefate.pod_dev.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.Constants;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.Util;

public class ActivityAccount extends ListActivity {
    
    String currSelectedAccount;
    
    private boolean clicked = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accounts);
        Account[] accounts = AccountManager.get(getApplicationContext())
                .getAccountsByType(Constants.ACCOUNT_TYPE);
        String[] accountArray = new String[accounts.length];
        for (int i = 0; i < accounts.length; i++) {
            accountArray[i] = accounts[i].name;
        }
        String[] accountInfo = ApplicationEx.dbHelper.getAccount();
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, 
                android.R.layout.simple_list_item_single_choice, accountArray);
        setListAdapter(arrayAdapter);
        int currAccountPos = 0;
        if (accountInfo != null)
            currAccountPos = arrayAdapter.getPosition(accountInfo[0]);
        Button doneButton = (Button) findViewById(R.id.AccountButton);
        doneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clicked) {
                    Util.writeBufferToFile(Boolean.toString(true).getBytes(),
                            Constants.GOOGLE_FILENAME);
                    ApplicationEx.dbHelper.addAccount(currSelectedAccount);
                    setResult(Activity.RESULT_OK, null);
                    finish();
                }
            }
        });
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }
    
    @Override
    protected void onListItemClick(ListView listView, View view, int position, 
            long id) {
        clicked = true;
        currSelectedAccount = (String) getListView().getItemAtPosition(
                position);
    }
    
}