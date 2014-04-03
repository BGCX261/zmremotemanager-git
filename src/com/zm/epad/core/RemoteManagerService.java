package com.zm.epad.core;

import com.zm.epad.IRemoteManager;
import com.zm.xmpp.communication.handler.CommandProcessor;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.zm.epad.core.Config;

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
    private RemoteManagerStub mInterfaceStub = null;
    private final String REMOTE_SERVICE_NAME = "com.zm.epad.IRemoteManager";

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

        mInterfaceStub = new RemoteManagerStub();
        ServiceManager.addService(REMOTE_SERVICE_NAME, mInterfaceStub);
        mXmppClient.addXmppClientCallback(mInterfaceStub);
        /*
         * @todo:How to handle the case if we don't know the login info ??????
         * Answer: if username is null, don't login
         */
        mXmppClient.start();
        // if (mLoginBundle.getString(CoreConstants.CONSTANT_USRNAME) != null) {
        if (mConfig.isAccountInitiated()) {
            mXmppClient.connect(mLoginBundle
                    .getString(CoreConstants.CONSTANT_SERVER));
            mXmppClient.login(
                    mLoginBundle.getString(CoreConstants.CONSTANT_USRNAME),
                    mLoginBundle.getString(CoreConstants.CONSTANT_PASSWORD),
                    mLoginBundle.getString(CoreConstants.CONSTANT_RESOURCE));
        }
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

    private class RemoteManagerStub extends IRemoteManager.Stub implements
            XmppClient.XmppClientCallback {

        private Object mSyncLock = new Object();
        private String mLoginStatus = null;

        @Override
        public boolean login(String userName, String password)
                throws RemoteException {
            LogManager.local(TAG, "login:" + userName + ";" + password);
            boolean bRet = false;
            try {
                String resource = mLoginBundle
                        .getString(CoreConstants.CONSTANT_RESOURCE);
                int status = mXmppClient.getStatus();
                if (mConfig.isAccountInitiated()) {
                    LogManager.local(TAG, "Already initiated:" + status);
                    return false;
                }

                mXmppClient.connect(mLoginBundle
                        .getString(CoreConstants.CONSTANT_SERVER));
                bRet = mXmppClient.login(userName, password, resource);
                if (bRet == false) {
                    return false;
                }
                mLoginStatus = new String("false");
                synchronized (mSyncLock) {
                    mSyncLock.wait(5000);
                    bRet = Boolean.valueOf(mLoginStatus);
                    LogManager.local(TAG, "Login Done:" + mLoginStatus);
                    mLoginStatus = null;
                }

                if (bRet) {
                    // if success, save username and password
                    mConfig.setConfig(Config.USERNAME, userName);
                    mConfig.setConfig(Config.PASSWORD, password);
                    mConfig.saveConfig();

                    mLoginBundle.putString(CoreConstants.CONSTANT_USRNAME,
                            mConfig.getConfig(Config.USERNAME));
                    mLoginBundle.putString(CoreConstants.CONSTANT_PASSWORD,
                            mConfig.getConfig(Config.PASSWORD));
                }
            } catch (Exception e) {
                e.printStackTrace();
                bRet = false;
            }

            return bRet;
        }

        @Override
        public Object reportXMPPClientEvent(int xmppClientEvent, Object... args) {
            LogManager.local(TAG, "XMPPClientEvent:" + xmppClientEvent);
            switch (xmppClientEvent) {
            case XmppClient.XMPPCLIENT_EVENT_LOGIN:
                if (args.length > 0 && mLoginStatus != null) {
                    synchronized (mSyncLock) {
                        boolean login = (Boolean) args[0];
                        LogManager.local(TAG, "login event:" + login);
                        mLoginStatus = String.valueOf(login);
                        mSyncLock.notifyAll();
                    }
                }
                break;
            default:
                break;

            }
            return null;
        }
    }
}
