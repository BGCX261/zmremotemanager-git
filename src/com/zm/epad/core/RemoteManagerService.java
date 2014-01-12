package com.zm.epad.core;

import com.zm.epad.plugins.IQDispatcherCommand;
import com.zm.xmpp.communication.Constants;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;


public class RemoteManagerService extends Service {
    
    private String TAG = "RemoteManagerService";
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
        
        mNetworkStatusMonitor.stop();
        mLogManager.stop();
        mXmppClient.stop();
        mNetCmdDispatcher.stop();
        XmppClient.destroyXMPPEnvironment();
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
        
        mLoginBundle.putString("server", data.getString("server"));
        mLoginBundle.putString("username", data.getString("username"));
        mLoginBundle.putString("password", data.getString("password"));
        mLoginBundle.putString("resource", data.getString("resource"));
        
        mbInitialized = true;
        
  
        
        mNetCmdDispatcher = new NetCmdDispatcher();        
        mNetCmdDispatcher.registerDispacher(new IQDispatcherCommand(this, 
        		Constants.XMPP_NAMESPACE_CENTER));
        
        mXmppClient = new XmppClient(this);
        mXmppClient.addXmppClientCallback(mNetCmdDispatcher);
        
        mLogManager = new LogManager(this, mXmppClient);
        mLogManager.start();
        
        mNetworkStatusMonitor = new NetworkStatusMonitor(this);
        mNetworkStatusMonitor.addReportee(mXmppClient);
        
        mNetworkStatusMonitor.start(); // we could get network status very
                                       // quickly. so there is a time race....
        mNetCmdDispatcher.start();

        mXmppClient.start(mLoginBundle.getString("server"));

        mXmppClient.login(mLoginBundle.getString("username"), 
                mLoginBundle.getString("password"), Build.SERIAL);


        LogManager.server(TAG, "RemoteManagerService started");
        mbInitialized = true;
        
        
        
    }
}
