package com.zm.epad.core;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import com.zm.epad.plugins.RemoteCmdProcessor;
import com.zm.epad.plugins.RemoteDeviceManager;
import com.zm.epad.plugins.RemoteFileManager;
import com.zm.epad.plugins.RemotePackageManager;
import com.zm.epad.plugins.policy.RemotePolicyManager;
import com.zm.xmpp.communication.Constants;


/**
 * Core Service.
 */
public class RemoteManagerService extends Service {
    private final static String TAG = "RemoteManagerService";
    private boolean mbInitialized = false;
    private Bundle mLoginBundle = new Bundle();
    
    
    //following are core system components
    private XmppClient mXmppClient = null;
    private LogManager mLogManager = null;
    private NetworkStatusMonitor mNetworkStatusMonitor = null;
    private NetCmdDispatcher mNetCmdDispatcher = null;
    private RemoteCmdProcessor  mRemoteCmdProcessor = null;
    //following are sub-systems
/*    private RemotePackageManager mPackageManager;
    private RemoteDeviceManager mDeviceManager;
    private RemoteFileManager mFileManager;
    private RemotePolicyManager mPolicyManager;*/
    private  SubSystemFacade mSubSystem = null;
   
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        subsystemsStop();
        coreSystemStop();
        mbInitialized = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mbInitialized == false) {
            init(intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }
    
    void prepareLoginData(Intent intent){
        Bundle data = intent.getExtras();
        mLoginBundle.putString(CoreConstants.CONSTANT_SERVER, data.getString(CoreConstants.CONSTANT_SERVER));
        mLoginBundle.putString(CoreConstants.CONSTANT_USRNAME, data.getString(CoreConstants.CONSTANT_USRNAME));
        mLoginBundle.putString(CoreConstants.CONSTANT_PASSWORD, data.getString(CoreConstants.CONSTANT_PASSWORD));
        mLoginBundle.putString(CoreConstants.CONSTANT_RESOURCE, data.getString(CoreConstants.CONSTANT_RESOURCE));
    }
    private void init(Intent intent) {
       
        prepareLoginData(intent);
        coreSystemStart();
        subsystemsStart();
        
        mbInitialized = true;
        
        LogManager.local(TAG, "RemoteManagerService started");
    }
    
    void coreSystemStart(){
        mXmppClient = new XmppClient(this);

        mNetCmdDispatcher = new NetCmdDispatcher();
        
        mRemoteCmdProcessor = new RemoteCmdProcessor(this,mXmppClient);
        
        mNetCmdDispatcher.registerDispacher(mRemoteCmdProcessor);

        mXmppClient.addXmppClientCallback(mNetCmdDispatcher);

        mLogManager = new LogManager(this, mXmppClient);
        mLogManager.start();

        mNetworkStatusMonitor = new NetworkStatusMonitor(this);
        mNetworkStatusMonitor.addReportee(mXmppClient);

        mNetworkStatusMonitor.start(); // we could get network status very
                                       // quickly. so there is a time race....
        mNetCmdDispatcher.start();
        
        /*@todo:How to handle the case if we don't know the login info?
         * */
        mXmppClient.start(mLoginBundle.getString(CoreConstants.CONSTANT_SERVER));

        mXmppClient.login(mLoginBundle.getString(CoreConstants.CONSTANT_USRNAME),
                mLoginBundle.getString(CoreConstants.CONSTANT_PASSWORD), CoreConstants.CONSTANT_DEVICEID);
    }
    void coreSystemStop(){
        mNetworkStatusMonitor.stop();
        mLogManager.stop();
        mXmppClient.stop();
        mNetCmdDispatcher.stop();
    }
    void subsystemsStart(){
        mSubSystem = new SubSystemFacade(this);
        mSubSystem.start(mLoginBundle);
        
        mRemoteCmdProcessor.setSubSystem(mSubSystem);
/*        mPackageManager = RemotePackageManager.getInstance(this);
        mDeviceManager = RemoteDeviceManager.getInstance(this);
        mFileManager = RemoteFileManager.getInstance(this);
        mFileManager.setXmppLoginResource(mLoginBundle);
        mPolicyManager = RemotePolicyManager.getInstance(this);
        mPolicyManager.loadPolicy();*/
    }
    void subsystemsStop(){
        mSubSystem.stop();
        mSubSystem = null;
        /*RemotePackageManager.release();
        RemoteDeviceManager.release();
        RemoteFileManager.release();
        RemotePolicyManager.release();*/
    }
    
}
