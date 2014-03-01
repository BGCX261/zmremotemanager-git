package com.zm.epad.plugins;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class RemoteAccountService extends Service {
    RemoteAccountAuthenticator mRemoteAccAuth = null;

    private static String RAA_TYPE = "zmtech";
    private static String RAA_NAME = "username";
    private static String RAA_PASSWORD = "password";

    @Override
    public void onCreate() {
        super.onCreate();

        if (mRemoteAccAuth == null)
            mRemoteAccAuth = new RemoteAccountAuthenticator(
                    RemoteAccountService.this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (mRemoteAccAuth == null)
            return null;
        return mRemoteAccAuth.getIBinder();
    }

    class RemoteAccountAuthenticator extends AbstractAccountAuthenticator {

        public RemoteAccountAuthenticator(Context context) {
            super(context);
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse response,
                String accountType) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse response,
                String accountType, String authTokenType,
                String[] requiredFeatures, Bundle options)
                throws NetworkErrorException {
            if (accountType.equals("zmtech") == false)
                return null;

            if (options != null && options.containsKey(RAA_PASSWORD)
                    && options.containsKey(RAA_NAME)) {
                final Account account = new Account(
                        options.getString(RAA_NAME), RAA_TYPE);
                // add my account name and password to the system.
                AccountManager.get(RemoteAccountService.this)
                        .addAccountExplicitly(account,
                                options.getString(RAA_PASSWORD), null);

                Bundle bundle = new Bundle();
                bundle.putString(AccountManager.KEY_ACCOUNT_NAME,
                        options.getString(RAA_NAME));
                bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, RAA_TYPE);
                return bundle;

            } else {
                Bundle bundle = new Bundle();
                Intent inputAccountInfo = new Intent();
                inputAccountInfo.setComponent(new ComponentName("com.zm.epad",
                        "com.zm.epad.ui.DebugActivityHome"));
                inputAccountInfo.putExtra(
                        AccountManager.KEY_ACCOUNT_MANAGER_RESPONSE, response);
                bundle.putParcelable(AccountManager.KEY_INTENT,
                        inputAccountInfo);
                return bundle;
            }
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse response,
                Account account, Bundle options) throws NetworkErrorException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse response,
                Account account, String authTokenType, Bundle options)
                throws NetworkErrorException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getAuthTokenLabel(String authTokenType) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse response,
                Account account, String authTokenType, Bundle options)
                throws NetworkErrorException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse response,
                Account account, String[] features)
                throws NetworkErrorException {
            // TODO Auto-generated method stub
            return null;
        }
    }

}
