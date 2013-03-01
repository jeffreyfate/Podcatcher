package com.jeffthefate.pod_dev;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.os.Build;
import android.os.Bundle;

public abstract class VersionedGetAuthToken {
    
    public abstract VersionedGetAuthToken create(Account account,
            AccountManagerCallback callback);
    public abstract AccountManagerFuture<Bundle> getAuthToken();
    
    public static VersionedGetAuthToken newInstance() {
        final int sdkVersion = Build.VERSION.SDK_INT;
        VersionedGetAuthToken callback = null;
        if (sdkVersion < Build.VERSION_CODES.HONEYCOMB)
            callback = new GingerbreadGetAuthToken();
        else
            callback = new HoneycombGetAuthToken();

        return callback;
    }
    
    private static class GingerbreadGetAuthToken extends VersionedGetAuthToken {
        Account account;
        AccountManagerCallback callback;
        @Override
        public VersionedGetAuthToken create(Account account,
                AccountManagerCallback callback) {
            this.account = account;
            this.callback = callback;
            return this;
        }
        
        @Override
        public AccountManagerFuture<Bundle> getAuthToken() {
            return AccountManager.get(ApplicationEx.getApp()).getAuthToken(
                    account, "reader", false, callback, null);
        }
    }
    
    private static class HoneycombGetAuthToken extends GingerbreadGetAuthToken {
        @Override
        public AccountManagerFuture<Bundle> getAuthToken() {
            return AccountManager.get(ApplicationEx.getApp()).getAuthToken(
                    account, "reader", null, false, callback, null);
        }
    }
    
}
