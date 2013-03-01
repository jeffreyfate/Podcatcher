package com.jeffthefate.pod_dev.activity;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.jeffthefate.pod_dev.ApplicationEx;
import com.jeffthefate.pod_dev.Constants;
import com.jeffthefate.pod_dev.R;
import com.jeffthefate.pod_dev.Util;
import com.jeffthefate.pod_dev.Util.Magics;
import com.jeffthefate.pod_dev.VersionedGetAuthToken;

public class ActivitySplash extends Activity {
    
    private Account mAccount;
    private String[] mAccountInfo = null;
    private int resultCode = -2;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (resultCode < -1 || resultCode > 0) {
            if (!Util.containsPreference(R.string.sync_key)) {
                    startActivityForResult(new Intent(getApplicationContext(), 
                            ActivityFirstStart.class), 
                        Constants.REQUEST_CODE_FIRST_START);
            }
            else if (Util.readBooleanPreference(R.string.sync_key, false) && 
                    !Util.readBooleanFromFile(Constants.GOOGLE_FILENAME))
                startActivityForResult(new Intent(getApplicationContext(), 
                        ActivityAccount.class), 
                    Constants.REQUEST_CODE_CHOOSE_ACCOUNT);
            else
                startUpdate();
        }
        else if (resultCode == Activity.RESULT_CANCELED)
            finish();
        else
            startUpdate();
    }
    
    private void startUpdate() {
        if (Util.readBooleanFromFile(Constants.CHECKTOKEN_FILENAME)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                new GetApiTokenTask().execute();
            else
                new GetApiTokenTask().executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else {
            resultCode = -2;
            if (Integer.parseInt(Util.readStringPreference(
                    R.string.update_key, "0")) == 1)
                Util.setRepeatingAlarm(Integer.parseInt(
                        Util.readStringPreference(R.string.interval_key, "60")),
                        true);
            else if (Integer.parseInt(Util.readStringPreference(
                    R.string.update_key, "0")) == 2) {
                Magics currMagics = 
                    ApplicationEx.dbHelper.getNextMagicInterval(false);
                Util.setMagicAlarm(currMagics.getTime(), currMagics.getFeeds());
            }
            Intent intent = new Intent(getApplicationContext(), 
                    ActivityMain.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.resultCode = resultCode;
        switch(requestCode) {
        case Constants.REQUEST_CODE_CHOOSE_ACCOUNT:
            if (resultCode != Activity.RESULT_CANCELED) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    new GetApiTokenTask().execute();
                else
                    new GetApiTokenTask().executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR);
            }
            break;
        }
    }
    
    class GetApiTokenTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... nothing) {
            mAccountInfo = ApplicationEx.dbHelper.getAccount();
            if (mAccountInfo == null)
                startActivityForResult(new Intent(getApplicationContext(), 
                        ActivityAccount.class), 
                    Constants.REQUEST_CODE_CHOOSE_ACCOUNT);
            else if (mAccountInfo[0] != null && mAccountInfo[2] != null) {
                getAuthToken();
            }
            return null;
        }
    }
    
    private void getAuthToken() {
        Account[] mAccounts = AccountManager.get(getApplicationContext())
                    .getAccountsByType(mAccountInfo[2]);
        for (Account account : mAccounts) {
            if (account.name.equals(mAccountInfo[0])) {
                mAccount = account;
                break;
            }
        }
        GetAuthTokenCallback callback = new GetAuthTokenCallback();
        VersionedGetAuthToken.newInstance().create(mAccount, callback)
                .getAuthToken();
    }
    
    public class GetAuthTokenCallback implements AccountManagerCallback {
        
        public void run(AccountManagerFuture result) {
            Bundle bundle;
            try {
                bundle = (Bundle) result.getResult();
                Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
                if(intent != null) {
                    startActivity(intent);
                } else {
                    onGetAuthToken(bundle);
                }
            } catch (OperationCanceledException e) {
                Log.e(Constants.LOG_TAG, "Authentication request canceled", e);
            } catch (AuthenticatorException e) {
                Log.e(Constants.LOG_TAG, "Invalid authentication", e);
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, "Authentication server failed to " + 
                        "respond", e);
            }
        }
        
        protected void onGetAuthToken(Bundle bundle) {
            Util.saveAccountInfo(bundle, mAccountInfo);
            resultCode = -2;
            if (Integer.parseInt(Util.readStringPreference(
                    R.string.update_key, "0")) == 1)
                Util.setRepeatingAlarm(Integer.parseInt(
                        Util.readStringPreference(R.string.interval_key, "60")),
                        true);
            Intent intent = new Intent(getApplicationContext(), 
                    ActivityMain.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        }
    };
    
}