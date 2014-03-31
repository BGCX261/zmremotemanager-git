package com.zm.epad.core;

import com.zm.xmpp.communication.handler.CommandProcessor;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

/**
 * Core Service.
 */
public class RemoteManagerService extends Service {
    private final static String TAG = "RemoteManagerService";
    private boolean mbInitialized = false;
    private Bundle mLoginBundle = new Bundle();
    private Config mConfig = null;

    // following are core system components
    private XmppClient mXmppClient = null;
    private LogManager mLogManager = null;
    private NetworkStatusMonitor mNetworkStatusMonitor = null;
    private NetCmdDispatcher mNetCmdDispatcher = null;
    private CommandProcessor mRemoteCmdProcessor = null;

    private SubSystemFacade mSubSystem = null;

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
        coreSystemStop();
        subsystemsStop();
        Config.closeAndSaveConfig();
        mbInitialized = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mbInitialized == false) {
            init(intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    void prepareLoginData(Intent intent) {
        mConfig = Config.loadConfig(this);
        Bundle data = intent.getExtras();
        if (data != null) {
            // if intent with login info, use this info
            mLoginBundle.putString(CoreConstants.CONSTANT_SERVER,
                    data.getString(CoreConstants.CONSTANT_SERVER));
            mLoginBundle.putString(CoreConstants.CONSTANT_USRNAME,
                    data.getString(CoreConstants.CONSTANT_USRNAME));
            mLoginBundle.putString(CoreConstants.CONSTANT_PASSWORD,
                    data.getString(CoreConstants.CONSTANT_PASSWORD));
            mLoginBundle.putString(CoreConstants.CONSTANT_RESOURCE,
                    CoreConstants.CONSTANT_DEVICEID);
        } else {
            // if no info, use the info in config
            mLoginBundle.putString(CoreConstants.CONSTANT_SERVER,
                    mConfig.getConfig(Config.SERVER_ADDRESS));
            mLoginBundle.putString(CoreConstants.CONSTANT_USRNAME,
                    mConfig.getConfig(Config.USERNAME));
            mLoginBundle.putString(CoreConstants.CONSTANT_PASSWORD,
                    mConfig.getConfig(Config.PASSWORD));
            mLoginBundle.putString(CoreConstants.CONSTANT_RESOURCE,
                    mConfig.getConfig(Config.RESOURCE));
        }
    }

    private void init(Intent intent) {

        prepareLoginData(intent);
        subsystemsStart();
        coreSystemStart();

        mbInitialized = true;

        LogManager.local(TAG, "RemoteManagerService started");
    }

    void subsystemsStart() {
        mSubSystem = new SubSystemFacade(this);
        mSubSystem.start(mLoginBundle);
    }

    void coreSystemStart() {
        mXmppClient = new XmppClient(this);

        mNetCmdDispatcher = new NetCmdDispatcher();

        mRemoteCmdProcessor = new CommandProcessor(this, mXmppClient);
        mRemoteCmdProcessor.setSubSystem(mSubSystem);

        mNetCmdDispatcher.registerDispacher(mRemoteCmdProcessor);

        mXmppClient.addXmppClientCallback(mNetCmdDispatcher);

        mLogManager = LogManager.createLogManager(this);
        mLogManager.start();

        mNetworkStatusMonitor = new NetworkStatusMonitor(this);
        mNetworkStatusMonitor.addReportee(mXmppClient);

        mNetworkStatusMonitor.start(); // we could get network status very
                                       // quickly. so there is a time race....
        mNetCmdDispatcher.start();

        /*
         * @todo:How to handle the case if we don't know the login info?
         */
        mXmppClient
                .start(mLoginBundle.getString(CoreConstants.CONSTANT_SERVER));

        mXmppClient.login(
                mLoginBundle.getString(CoreConstants.CONSTANT_USRNAME),
                mLoginBundle.getString(CoreConstants.CONSTANT_PASSWORD),
                mLoginBundle.getString(CoreConstants.CONSTANT_RESOURCE));
    }

    void coreSystemStop() {
        mNetworkStatusMonitor.stop();
        mLogManager.stop();
        mXmppClient.stop();
        mNetCmdDispatcher.stop();
    }

    void subsystemsStop() {
        mSubSystem.stop();
        mSubSystem = null;
    }

}
