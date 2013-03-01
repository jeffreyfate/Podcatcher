package com.jeffthefate.pod_dev.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.Constants;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.Util;
import com.jeffthefate.pod_dev.Util.Magics;

public class FragmentAccount extends ListFragment {
    
    String currSelectedAccount;
    
    private boolean clicked = false;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.accounts, container, false);
        Account[] accounts = AccountManager.get(ApplicationEx.getApp())
                .getAccountsByType(Constants.ACCOUNT_TYPE);
        String[] accountArray = new String[accounts.length];
        for (int i = 0; i < accounts.length; i++) {
            accountArray[i] = accounts[i].name;
        }
        String[] accountInfo = ApplicationEx.dbHelper.getAccount();
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                getActivity(), 
                android.R.layout.simple_list_item_single_choice, accountArray);
        setListAdapter(arrayAdapter);
        int currAccountPos = 0;
        if (accountInfo != null)
            currAccountPos = arrayAdapter.getPosition(accountInfo[0]);
        Button doneButton = (Button) v.findViewById(R.id.AccountButton);
        doneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clicked) {
                    Util.writeBufferToFile(Boolean.toString(true).getBytes(),
                            Constants.GOOGLE_FILENAME);
                    ApplicationEx.dbHelper.addAccount(currSelectedAccount);
                    if (Integer.parseInt(
                            Util.readStringPreference(R.string.update_key, "0"))
                                == 1) {
                        Util.setRepeatingAlarm(Integer.parseInt(
                                Util.readStringPreference(R.string.interval_key,
                                        "60")), false);
                    }
                    else if (Integer.parseInt(
                            Util.readStringPreference(R.string.update_key, "0"))
                                == 2) {
                        Magics currMagics = 
                            ApplicationEx.dbHelper.getNextMagicInterval(false);
                        Util.setMagicAlarm(currMagics.getTime(),
                                currMagics.getFeeds());
                    }
                    getActivity().setResult(Activity.RESULT_OK, null);
                    getActivity().finish();
                }
            }
        });
        return v;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }
    
    @Override
    public void onListItemClick(ListView listView, View view, int position, 
            long id) {
        clicked = true;
        currSelectedAccount = (String) getListView().getItemAtPosition(
                position);
    }
    
}