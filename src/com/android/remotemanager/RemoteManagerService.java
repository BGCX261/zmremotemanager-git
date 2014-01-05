package com.android.remotemanager;

import com.android.logmanager.LogManager;
import com.android.remotemanager.plugins.XmppClient;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.view.LayoutInflater.Filter;

import java.util.ArrayList;

public class RemoteManagerService extends Service {
    
    
    private XmppClient  mXmppClient = null;
    private LogManager  mLogManager = null;
    private NetworkStatusMonitor mNetworkStatusMonitor = null;
    
	@Override
	public IBinder onBind(Intent intent) {
	    // TODO Auto-generated method stub
	    return null;
	}

    @Override
    public void onCreate() {
        super.onCreate();
        init();
 
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        // TODO Auto-generated method stub
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // TODO Auto-generated method stub
        return super.onUnbind(intent);
    }
    
    
    private void init(){
        mNetworkStatusMonitor = new NetworkStatusMonitor(this);
        
        mXmppClient = XmppClient.getXmppClientInstance(this);
        mLogManager = LogManager.getLogManagerInstance(null,this);
        mNetworkStatusMonitor.start(new NetworkStatusMonitor.NetworkStatusReport[]{mXmppClient,mLogManager});
        
        
    }
}
