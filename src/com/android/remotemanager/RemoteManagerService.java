package com.android.remotemanager;

import com.android.logmanager.LogManager;
import com.android.remotemanager.plugins.XmppClient;
import com.android.remotemanager.NetCmdDispatcher;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;


public class RemoteManagerService extends Service {
    
    
    private boolean     mbInitialized = false;
    private Bundle      mLoginBundle = new Bundle();
    private XmppClient  mXmppClient = null;
    private LogManager  mLogManager = null;
    private NetworkStatusMonitor mNetworkStatusMonitor = null;
    private NetCmdDispatcher mNetCmdDispatcher = null;
    
    @Override
    public void onCreate() {
        super.onCreate();
        XmppClient.initializeXMPPEnvironment(this);

 
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(mbInitialized == false){
            init(intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }


   
    private void init(Intent intent){
        Bundle data = intent.getExtras();
        
        mLoginBundle.putString("server", data.getString("name"));
        mLoginBundle.putString("username", data.getString("username"));
        mLoginBundle.putString("password", data.getString("password"));
        mLoginBundle.putString("resource", data.getString("resource"));
        
        mbInitialized = true;
        mNetworkStatusMonitor = new NetworkStatusMonitor(this);
        mNetCmdDispatcher = new NetCmdDispatcher();
        mXmppClient = new XmppClient(this,mNetCmdDispatcher);
        mXmppClient.start(mLoginBundle.getString("server"));
        mXmppClient.login(mLoginBundle.getString("username"), 
                mLoginBundle.getString("password"), mLoginBundle.getString("resource"));
        
        mbInitialized = true;
        
        
        
    }
}
