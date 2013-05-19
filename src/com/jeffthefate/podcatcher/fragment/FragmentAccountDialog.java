package com.jeffthefate.podcatcher.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.jeffthefate.podcatcher.ApplicationEx;
import com.jeffthefate.podcatcher.Constants;
import com.jeffthefate.podcatcher.R;

public class FragmentAccountDialog extends DialogFragment {
    
    private boolean clicked = false;
    private String currSelectedAccount = null;
    
    public static FragmentAccountDialog newInstance() {
        return new FragmentAccountDialog();
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View v = inflater.inflate(R.layout.accounts, null);
        ListView listView = (ListView) v.findViewById(android.R.id.list);
        Account[] accounts = AccountManager.get(ApplicationEx.getApp())
                .getAccountsByType(Constants.ACCOUNT_TYPE);
        String[] accountArray = new String[accounts.length];
        for (int i = 0; i < accounts.length; i++) {
            accountArray[i] = accounts[i].name;
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                ApplicationEx.getApp(), 
                android.R.layout.simple_list_item_single_choice, accountArray);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View view,
                    int position, long id) {
                clicked = true;
                currSelectedAccount = (String) listView.getItemAtPosition(
                        position);
            }
        });
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        builder.setView(v).setNeutralButton(R.string.doneButton,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (clicked) {
                            // TODO Create account on Parse if it isn't already there
                            // Set to use this account in the app
                        }
                        else {
                            // TODO Show toast to select an account
                        }
                    }
                });
        builder.setTitle(R.string.AccountTitle);
        // Create the AlertDialog object and return it
        return builder.create();
    }
}