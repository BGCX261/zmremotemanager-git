package com.zm.epad.core;

import com.zm.epad.IRemoteManager;
import com.zm.epad.RemoteManager;
import com.zm.xmpp.communication.handler.CommandProcessor;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
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
    private WebServiceClient mWebServiceClient = null;
    private LogManager mLogManager = null;
    private NetworkStatusMonitor mNetworkStatusMonitor = null;
    private NetCmdDispatcher mNetCmdDispatcher = null;
    private CommandProcessor mRemoteCmdProcessor = null;

    private SubSystemFacade mSubSystem = null;
    private RemoteManagerStub mInterfaceStub = null;
    private final String REMOTE_SERVICE_NAME = "com.zm.epad.IRemoteManager";
    private boolean mDebugMode = false;

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
        Bundle data = intent == null ? null : intent.getExtras();
        if (data != null) {
            // if intent with login info, use this info
            mLoginBundle.putString(CoreConstants.CONSTANT_SERVER,
                    data.getString(CoreConstants.CONSTANT_SERVER));
            mLoginBundle.putString(CoreConstants.CONSTANT_USRNAME,
                    data.getString(CoreConstants.CONSTANT_USRNAME));
            mLoginBundle.putString(CoreConstants.CONSTANT_PASSWORD,
                    data.getString(CoreConstants.CONSTANT_PASSWORD));
            mLoginBundle.putString(CoreConstants.CONSTANT_RESOURCE,
                    Config.getDeviceId());
            mDebugMode = true;
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
            mDebugMode = false;
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
        mWebServiceClient = new WebServiceClient(this);
        mWebServiceClient.start();
        mXmppClient.start();
        // if (mLoginBundle.getString(CoreConstants.CONSTANT_USRNAME) != null) {
        if (mConfig.isAccountInitiated() || mDebugMode == true) {
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
        mWebServiceClient.stop();
        mNetCmdDispatcher.stop();
    }

    void subsystemsStop() {
        mSubSystem.stop();
        mSubSystem = null;
    }

    private class RemoteManagerStub extends IRemoteManager.Stub implements
            XmppClient.XmppClientCallback {

        private Object mSyncLock = null;
        private boolean mLoginStatus = false;
        private PendingIntent mLoginIntent = null;

        @Override
        public boolean login(String userName, String password,
                PendingIntent intent) {
            LogManager.local(TAG, "login:" + userName + ";" + password);
            try {
                if (mLoginIntent != null) {
                    LogManager.local(TAG, "double call");
                    return false;
                }

                mLoginIntent = intent;
                mWebServiceClient.login(userName, password,
                        new WebServiceClient.Result<String>() {

                            @Override
                            public void receiveResult(String result,
                                    int errorCode) {
                                try {
                                    if (result == null) {
                                        LogManager.local(TAG, "login failed:"
                                                + errorCode);
                                        sendIntent(
                                                mLoginIntent,
                                                convertErrorCode_WebService(errorCode));
                                        mLoginIntent = null;
                                        return;
                                    }
                                    Xmpplogin(result);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    sendIntent(mLoginIntent,
                                            RemoteManager.RESULT_FAILED);
                                    mLoginIntent = null;
                                }
                            }

                        });
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public Object reportXMPPClientEvent(int xmppClientEvent, Object... args) {
            LogManager.local(TAG, "XMPPClientEvent:" + xmppClientEvent);
            switch (xmppClientEvent) {
            case XmppClient.XMPPCLIENT_EVENT_LOGIN:
                if (args.length > 0 && mLoginIntent != null) {
                    int result = RemoteManager.RESULT_FAILED;
                    try {
                        if ((Boolean) args[0] == true) {
                            result = RemoteManager.RESULT_OK;
                            mConfig.setConfig(Config.PASSWORD, mLoginBundle
                                    .getString(CoreConstants.CONSTANT_PASSWORD));
                            mConfig.saveConfig();
                        } else {
                            mLoginBundle.putString(
                                    CoreConstants.CONSTANT_PASSWORD, null);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        sendIntent(mLoginIntent, result);
                        mLoginIntent = null;
                    }
                }
                break;
            default:
                break;
            }
            return null;
        }

        private void sendIntent(PendingIntent intent, int resultCode) {
            try {
                intent.send(resultCode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void Xmpplogin(String password) throws Exception {
            LogManager.local(TAG, "login: password" + password);

            int status = mXmppClient.getStatus();
            if (status == mXmppClient.XMPPCLIENT_STATUS_LOGINED) {
                LogManager.local(TAG, "Already login");
                mXmppClient.logout();
            }
            String defaultUser = Config.getDeviceId();
            String resource = Config.getInstance().getConfig(Config.RESOURCE);
            LogManager.local(TAG, "XMPP user:" + defaultUser);

            mXmppClient.connect(Config.getInstance().getConfig(
                    Config.SERVER_ADDRESS));
            Boolean bRet = mXmppClient.login(defaultUser, password, resource);
            if (bRet == false) {
                throw new Exception("XMPPClient login failed");
            }

            mLoginBundle.putString(CoreConstants.CONSTANT_PASSWORD, password);
        }
    }

    private int convertErrorCode_WebService(int input) {
        int ret = RemoteManager.RESULT_OK;
        switch (input) {
        case WebServiceClient.ERR_NO:
            ret = RemoteManager.RESULT_OK;
            break;
        case WebServiceClient.ERR_NETUNREACH:
            ret = RemoteManager.RESULT_CONNECT_CLOSE;
            break;
        case WebServiceClient.ERR_LOGIN_CHECK:
            ret = RemoteManager.RESULT_LOGIN_INFO_ERROR;
            break;
        default:
            break;
        }

        return ret;
    }
}
