package com.zm.epad.core;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class XmppClient implements NetworkStatusMonitor.NetworkStatusReport {
    private static final String TAG = "XmppClient";

    public static final int XMPPCLIENT_EVENT_CONNECT = 1;
    public static final int XMPPCLIENT_EVENT_LOGIN = 2;
    public static final int XMPPCLIENT_EVENT_CONNECTION_UPDATE_STATUS = 3;
    public static final int XMPPCLIENT_EVENT_LOGOUT = 4;
    public static final int XMPPCLIENT_EVENT_SENDPACKET_RESULT = 5;

    public static final int XMPPCLIENT_STATUS_IDLE = 0;
    public static final int XMPPCLIENT_STATUS_STARTING = 1;
    public static final int XMPPCLIENT_STATUS_STARTED = 2;
    public static final int XMPPCLIENT_STATUS_LOGINING = 3;
    public static final int XMPPCLIENT_STATUS_LOGINED = 4;
    public static final int XMPPCLIENT_STATUS_ERROR = 5;

    static final int CMD_START = 0;
    static final int CMD_CONNECTION_STATUS_UPDATE = 1;
    static final int CMD_NETWORK_STATUS_UPDATE = 2;
    static final int CMD_LOGIN = 3;
    static final int CMD_LOGOUT = 4;
    static final int CMD_QUIT = 5;
    static final int CMD_SEND_PACKET_ASYNC = 6;
    static final int CMD_SEND_OBJECT_ASYNC = 7;
    static final int CMD_RECONNECT_BY_ERROR = 8;

    private int mCurrentStatus = XMPPCLIENT_STATUS_IDLE;
    private int mPrevStatus = mCurrentStatus;
    static private SmackAndroid mSmackAndroid = null;

    static final String[] XMPPCLIENT_STATUS_STRINGS = { " idle ", " starting ",
            " started ", " logining ", " logined ", " error " };

    private ReentrantLock mStatusLock = new ReentrantLock();

    private XmppClientThreadHandler mXmppClientHandler = null;
    private HandlerThread mXmppHandlerThread = null;
    private Context mContext;
    private ArrayList<XmppClientCallback> mXmppClientCallbacks;
    private Connection mXmppConnection = null;
    private Bundle mConnectionInfo = null;
    private XMPPConnectionListener mXmppConnectionListener = null;

    public interface XmppClientCallback {
        public Object reportXMPPClientEvent(int xmppClientEvent, Object... args);
    }

    public XmppClient(Context context) {
        initializeXMPPEnvironment(context);
        mContext = context;
        mXmppClientCallbacks = new ArrayList<XmppClient.XmppClientCallback>();
        mConnectionInfo = new Bundle();
    }

    void initializeXMPPEnvironment(Context context) {
        if (mSmackAndroid == null) {
            mSmackAndroid = SmackAndroid.init(context);
        }
    }

    void destroyXMPPEnvironment() {
        if (mSmackAndroid != null) {
            mSmackAndroid.onDestroy();
            mSmackAndroid = null;
        }
    }

    @Override
    public void reportNetworkStatus(boolean bConnected) {
        try {
            mStatusLock.lock();
            if (mXmppClientHandler == null)
                return;
            Message msg = mXmppClientHandler
                    .obtainMessage(CMD_NETWORK_STATUS_UPDATE);
            msg.arg1 = bConnected ? 1 : 0;
            mXmppClientHandler.sendMessage(msg);
            LogManager.local(TAG, "reportNetworkStatus : " + bConnected);
        } finally {
            mStatusLock.unlock();
        }
    }

    private class XMPPConnectionListener implements ConnectionListener {

        @Override
        public void connectionClosed() {
            try {
                mStatusLock.lock();
                if (mXmppClientHandler == null)
                    return;
                LogManager.local(TAG, "connectionClosed ");
                Message msg = mXmppClientHandler
                        .obtainMessage(CMD_CONNECTION_STATUS_UPDATE);
                msg.arg1 = 0;
                mXmppClientHandler.sendMessage(msg);
            } finally {
                mStatusLock.unlock();
            }
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            try {
                mStatusLock.lock();
                if (mXmppClientHandler == null)
                    return;
                LogManager.local(TAG,
                        "connectionClosedOnError " + e.getMessage());
                Message msg = mXmppClientHandler
                        .obtainMessage(CMD_CONNECTION_STATUS_UPDATE);
                msg.arg1 = 0;
                msg.obj = e;
                mXmppClientHandler.sendMessage(msg);
            } finally {
                mStatusLock.unlock();
            }
        }

        @Override
        public void reconnectingIn(int seconds) {
            LogManager.local(TAG, "reconnectingIn " + seconds + "s");
        }

        @Override
        public void reconnectionSuccessful() {
            try {
                mStatusLock.lock();
                if (mXmppClientHandler == null)
                    return;
                LogManager.local(TAG, "reconnectionSuccessful ");
                Message msg = mXmppClientHandler
                        .obtainMessage(CMD_CONNECTION_STATUS_UPDATE);
                msg.arg1 = 1;
                mXmppClientHandler.sendMessage(msg);
            } finally {
                mStatusLock.unlock();
            }
        }

        @Override
        public void reconnectionFailed(Exception e) {
            LogManager.local(TAG, "reconnectionFailed : " + e.getMessage());
        }
    }

    private class XmppClientThreadHandler extends Handler {
        public XmppClientThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int cmd = msg.what;
            LogManager.local(TAG, "handleMessage cmd:" + cmd);
            if (cmd == CMD_START) {
                handleStartCmd();
            } else if (cmd == CMD_LOGIN) {
                handleLoginCmd();
            } else if (cmd == CMD_LOGOUT) {
                handleLogoutCmd(true);
            } else if (cmd == CMD_SEND_PACKET_ASYNC) {
                handleSendPacketCmd(msg);
            } else if (cmd == CMD_CONNECTION_STATUS_UPDATE) {
                handleConnectionStatus(msg);
            } else if (cmd == CMD_NETWORK_STATUS_UPDATE) {
                handleNetworkAvailable(msg.arg1);
            } else if (cmd == CMD_RECONNECT_BY_ERROR) {
                reconnectByError();
            }else if (cmd == CMD_QUIT) {
                handleQuitCmd();
            }

            return;
        }
    }

    private void handleStartCmd() {
        try {
            mStatusLock.lock();
            handleStartCmdLocked();
        } catch (Exception e) {
            sendReconnectByError(e);
        } finally {
            mStatusLock.unlock();
        }
    }

    private void dispatchXmppClientEvent(int xmppClientEvent, Object... args) {
        for (XmppClientCallback callback : mXmppClientCallbacks) {
            callback.reportXMPPClientEvent(xmppClientEvent, args);
        }
    }

    private void handleStartCmdLocked() throws Exception {
        try {
            String serverName = mConnectionInfo
                    .getString(CoreConstants.CONSTANT_SERVER);
            LogManager.local(TAG, "connect to server:" + serverName);

            ConnectionConfiguration config = new ConnectionConfiguration(
                    serverName);
            config.setCompressionEnabled(true);
            config.setDebuggerEnabled(true);
            // disable ReconnectionAllowed to avoid double connect
            config.setReconnectionAllowed(false);

            mXmppConnection = new XMPPConnection(config);
            mXmppConnection.connect();

            mXmppConnectionListener = new XMPPConnectionListener();
            mXmppConnection.addConnectionListener(mXmppConnectionListener);

            transitionToStatusLocked(XMPPCLIENT_STATUS_STARTED);

            dispatchXmppClientEvent(XMPPCLIENT_EVENT_CONNECT, 1,
                    mXmppConnection, ProviderManager.getInstance());

        } catch (Exception e) {
            LogManager.local(TAG, "handleStartCmd ERR: " + e.toString());
            e.printStackTrace();
            transitionToStatusLocked(XMPPCLIENT_STATUS_ERROR);
            dispatchXmppClientEvent(XMPPCLIENT_EVENT_CONNECT, 0);
            throw e;
        }
    }

    private void handleLoginCmd() {
        try {
            mStatusLock.lock();
            handleLoginCmdLocked();
        } catch (Exception e) {
            sendReconnectByError(e);
        } finally {
            mStatusLock.unlock();
        }
    }

    private void handleLoginCmdLocked() throws Exception {
        try {
            String usrName = mConnectionInfo
                    .getString(CoreConstants.CONSTANT_USRNAME);
            String usrPwd = mConnectionInfo
                    .getString(CoreConstants.CONSTANT_PASSWORD);
            String usrResource = mConnectionInfo
                    .getString(CoreConstants.CONSTANT_RESOURCE);

            mXmppConnection.login(usrName, usrPwd, usrResource);

            transitionToStatusLocked(XMPPCLIENT_STATUS_LOGINED);
            dispatchXmppClientEvent(XMPPCLIENT_EVENT_LOGIN, true);
        } catch (Exception e) {
            LogManager.local(TAG, "handleLoginCmd ERR: " + e.toString());
            e.printStackTrace();
            transitionToStatusLocked(XMPPCLIENT_STATUS_ERROR);
            dispatchXmppClientEvent(XMPPCLIENT_EVENT_LOGIN, false);
            throw e;
        }
    }

    private void handleLogoutCmd(boolean bNetworkConnect) {
        try {
            mStatusLock.lock();
            mXmppConnection.disconnect();
            if (bNetworkConnect)
                transitionToStatusLocked(XMPPCLIENT_STATUS_IDLE);
            else
                transitionToStatusLocked(XMPPCLIENT_STATUS_ERROR);
            dispatchXmppClientEvent(XMPPCLIENT_EVENT_LOGOUT, true);
        } finally {
            mStatusLock.unlock();
        }
    }

    private void handleQuitCmd() {
        try {
            mStatusLock.lock();
            mXmppConnection.disconnect();
            mPrevStatus = mCurrentStatus = XMPPCLIENT_STATUS_IDLE;
            mXmppConnection = null;
            mXmppClientHandler = null;
        } finally {
            mStatusLock.unlock();
        }
    }

    private void handleNetworkAvailable(int networkAvailable) {
        if (networkAvailable == 0) {
            LogManager.local(TAG,
                    "handleNetworkAvailable : network is down, need logout");
            handleLogoutCmd(false);
        } else if (networkAvailable == 1) {
            try {
                mStatusLock.lock();
                LogManager.local(TAG, "handleNetworkAvailable current status:"
                        + mCurrentStatus);
                if (mCurrentStatus == XMPPCLIENT_STATUS_IDLE
                        || mCurrentStatus == XMPPCLIENT_STATUS_ERROR) {
                    // if idle or error, reconnect
                    if (mPrevStatus == XMPPCLIENT_STATUS_STARTING
                            || mPrevStatus == XMPPCLIENT_STATUS_STARTED) {
                        LogManager.local(TAG, "\t xmppclient re-start");
                        handleStartCmdLocked();
                    } else if (mPrevStatus == XMPPCLIENT_STATUS_LOGINING
                            || mPrevStatus == XMPPCLIENT_STATUS_LOGINED) {
                        LogManager.local(TAG, "\t xmppclient re-login");
                        handleStartCmdLocked();
                        handleLoginCmdLocked();
                    }
                }
            } catch (Exception e) {
                sendReconnectByError(e);
            } finally {
                mStatusLock.unlock();
            }
        }
        return;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info == null ? false: info.isConnected();
    }

    private void handleConnectionStatus(Message msg) {
        try {
            mStatusLock.lock();
            int connected = msg.arg1;
            if (connected == 0) {
                dispatchXmppClientEvent(
                        XMPPCLIENT_EVENT_CONNECTION_UPDATE_STATUS, 0, msg.obj);

                if (isNetworkConnected()) {
                    // network is on but connect closed, set status to error
                    transitionToStatusLocked(XMPPCLIENT_STATUS_ERROR);
                    if (msg.obj == null) {
                        // simply closed, reconnect directly
                        reconnectByError();
                    } else {
                        // closed by error, reconnect after a few seconds
                        sendReconnectByError((Exception) msg.obj);
                    }
                }
            } else {
                dispatchXmppClientEvent(
                        XMPPCLIENT_EVENT_CONNECTION_UPDATE_STATUS, 1);
            }
        } finally {
            mStatusLock.unlock();
        }
    }

    private void handleSendPacketCmd(Message msg) {
        try {
            mStatusLock.lock();
            mXmppConnection.sendPacket((Packet) msg.obj);
            dispatchXmppClientEvent(XMPPCLIENT_EVENT_SENDPACKET_RESULT, true,
                    msg.obj);
        } catch (Exception e) {
            dispatchXmppClientEvent(XMPPCLIENT_EVENT_SENDPACKET_RESULT, false,
                    msg.obj);
        } finally {
            mStatusLock.unlock();
        }
    }

    public void addXmppClientCallback(XmppClientCallback[] callbacks) {
        try {
            mStatusLock.lock();
            for (XmppClientCallback xmppClientCallback : callbacks) {
                mXmppClientCallbacks.add(xmppClientCallback);
            }
        } finally {
            mStatusLock.unlock();
        }
    }

    public void addXmppClientCallback(XmppClientCallback callback) {
        try {
            mStatusLock.lock();
            mXmppClientCallbacks.add(callback);
        } finally {
            mStatusLock.unlock();
        }
    }

    public void removeXmppClientCallback(XmppClientCallback callback) {
        try {
            mStatusLock.lock();
            mXmppClientCallbacks.remove(callback);
        } finally {
            mStatusLock.unlock();
        }
    }

    private void transitionToStatusLocked(int newStatus) {
        switch (newStatus) {
        case XMPPCLIENT_STATUS_STARTED: {
            if (mCurrentStatus == XMPPCLIENT_STATUS_LOGINING) {
                mPrevStatus = XMPPCLIENT_STATUS_STARTED;
                mCurrentStatus = XMPPCLIENT_STATUS_LOGINING;
            } else if (mCurrentStatus == XMPPCLIENT_STATUS_STARTING) {
                mPrevStatus = XMPPCLIENT_STATUS_STARTING;
                mCurrentStatus = XMPPCLIENT_STATUS_STARTED;
            }
        }
            break;
        default: {
            mPrevStatus = mCurrentStatus;
            mCurrentStatus = newStatus;
        }
            break;
        }
    }

    public boolean start(String serverName) {
        if (serverName == null) {
            LogManager.local(TAG, "FAILED: start without serverName");
            return false;
        }
        try {
            mStatusLock.lock();
            if (mCurrentStatus != XMPPCLIENT_STATUS_IDLE
                    && mCurrentStatus != XMPPCLIENT_STATUS_ERROR) {
                LogManager.local(TAG, "Cannot start because currentstatus is "
                        + XMPPCLIENT_STATUS_STRINGS[mCurrentStatus]);
                return false;
            }

            LogManager.local(TAG, "xmppclient start with servername :"
                    + serverName);
            mConnectionInfo
                    .putString(CoreConstants.CONSTANT_SERVER, serverName);
            mXmppHandlerThread = new HandlerThread(TAG);
            mXmppHandlerThread.start();
            mXmppClientHandler = new XmppClientThreadHandler(
                    mXmppHandlerThread.getLooper());
            transitionToStatusLocked(XMPPCLIENT_STATUS_STARTING);
            mXmppClientHandler.sendEmptyMessage(CMD_START);

        } finally {
            mStatusLock.unlock();
        }
        return true;
    }

    public void stop() {
        LogManager.local(TAG, "stop xmppclient");
        try {
            if (mXmppClientHandler == null) {
                LogManager.local(TAG, "xmppclient already stopped");
                return;
            }
            mXmppClientHandler.sendEmptyMessage(CMD_QUIT);
            try {
                mStatusLock.wait();
            } catch (Exception e) {
                // TODO: handle exception
            }

            try {
                mXmppHandlerThread.quit();
                mXmppHandlerThread.join();
                mXmppHandlerThread = null;
            } catch (Exception e) {
                LogManager.local(TAG, e.toString());
            }
        } finally {
        }
        destroyXMPPEnvironment();
        LogManager.local(TAG, "xmppclient stopped");
    }

    public boolean login(String usrName, String password, String resource) {
        if (usrName == null || password == null) {
            LogManager
                    .local(TAG,
                            "xmppclient login failed,either username or password is null");
            return false;
        }
        if (resource == null && CoreConstants.CONSTANT_DEVICEID == null) {
            if (CoreConstants.CONSTANT_DEVICEID == null) {
                LogManager.local(TAG,
                        "xmppclient login failed due to resource is null");
                return false;
            }
            resource = CoreConstants.CONSTANT_DEVICEID;

        }
        try {
            mStatusLock.lock();
            if (mXmppClientHandler == null) {
                return false;
            }

            if (mCurrentStatus != XMPPCLIENT_STATUS_STARTING
                    && mCurrentStatus != XMPPCLIENT_STATUS_STARTED) {
                LogManager.local(TAG, "Cannot login because currentstatus is "
                        + XMPPCLIENT_STATUS_STRINGS[mCurrentStatus]);
                return false;
            }
            transitionToStatusLocked(XMPPCLIENT_STATUS_LOGINING);
            LogManager.local(TAG, "xmppclient login username " + usrName
                    + " password " + password + " resource " + resource);
            mConnectionInfo.putString(CoreConstants.CONSTANT_USRNAME, usrName);
            mConnectionInfo
                    .putString(CoreConstants.CONSTANT_PASSWORD, password);
            mConnectionInfo
                    .putString(CoreConstants.CONSTANT_RESOURCE, resource);
            Message msg = mXmppClientHandler.obtainMessage(CMD_LOGIN);
            msg.setData(mConnectionInfo);
            mXmppClientHandler.sendMessage(msg);
        } finally {
            mStatusLock.unlock();
        }
        return true;
    }

    public void logout() {
        try {
            mStatusLock.lock();
            if (mXmppClientHandler == null)
                return;
            mXmppClientHandler.sendEmptyMessage(CMD_LOGOUT);
        } finally {
            mStatusLock.unlock();
        }
        return;
    }

    public void sendLogPacket(String msg) {
        // lalalalala
    }

    public boolean sendPacket(Packet packet) {
        try {
            mStatusLock.lock();
            if (mXmppConnection == null)
                return false;
            mXmppConnection.sendPacket(packet);
        } finally {
            mStatusLock.unlock();
        }
        return true;
    }

    public boolean sendPacketAsync(Packet packet) {
        try {
            mStatusLock.lock();
            if (mXmppConnection == null)
                return false;
            Message msg = mXmppClientHandler.obtainMessage(
                    CMD_SEND_PACKET_ASYNC, packet);
            mXmppClientHandler.sendMessage(msg);
        } finally {
            mStatusLock.unlock();
        }
        return true;
    }

    public boolean sendPacketAsync(Packet packet, long delayMillis) {
        Message msg = mXmppClientHandler.obtainMessage(CMD_SEND_PACKET_ASYNC,
                packet);
        return mXmppClientHandler.sendMessageDelayed(msg, delayMillis);
    }

    public boolean sendObjectAsync(Object targetObject,
            final String description, String requestUrl) {
        try {
            mStatusLock.lock();
            if (mXmppConnection == null)
                return false;
            Message msg = mXmppClientHandler.obtainMessage(
                    CMD_SEND_OBJECT_ASYNC, null);
            mXmppClientHandler.sendMessage(msg);
        } finally {
            mStatusLock.unlock();
        }
        return true;
    }

    public int getStatus() {
        return mCurrentStatus;
    }

    private void sendReconnectByError(Exception e){
        LogManager.local(TAG, "sendReconnectByError:" + e.toString());
        Message msg = mXmppClientHandler.obtainMessage(
                CMD_RECONNECT_BY_ERROR, null);
        // if failed, reconnect after 10 seconds.
        mXmppClientHandler.sendMessageDelayed(msg, 10000);
    }

    private void reconnectByError() {
        if (mCurrentStatus != XMPPCLIENT_STATUS_ERROR) {
            return;
        }
        try {
            mStatusLock.lock();
            String serverName = mConnectionInfo
                    .getString(CoreConstants.CONSTANT_SERVER);
            LogManager.local(TAG, "connect to server:" + serverName);

            ConnectionConfiguration config = new ConnectionConfiguration(
                    serverName);
            config.setCompressionEnabled(true);
            config.setDebuggerEnabled(true);
            // disable ReconnectionAllowed to avoid double connect
            config.setReconnectionAllowed(false);

            mXmppConnection = new XMPPConnection(config);
            mXmppConnection.connect();

            mXmppConnectionListener = new XMPPConnectionListener();
            mXmppConnection.addConnectionListener(mXmppConnectionListener);
            dispatchXmppClientEvent(XMPPCLIENT_EVENT_CONNECT, 1,
                    mXmppConnection, ProviderManager.getInstance());

            String usrName = mConnectionInfo
                    .getString(CoreConstants.CONSTANT_USRNAME);
            String usrPwd = mConnectionInfo
                    .getString(CoreConstants.CONSTANT_PASSWORD);
            String usrResource = mConnectionInfo
                    .getString(CoreConstants.CONSTANT_RESOURCE);

            mXmppConnection.login(usrName, usrPwd, usrResource);

            transitionToStatusLocked(XMPPCLIENT_STATUS_LOGINED);
            dispatchXmppClientEvent(XMPPCLIENT_EVENT_LOGIN, true);

        } catch (Exception e) {
            if (mXmppConnection == null)
                return;
            // clear the status;
            transitionToStatusLocked(XMPPCLIENT_STATUS_ERROR);
            sendReconnectByError(e);
        } finally {
            mStatusLock.unlock();
        }
    }
}
