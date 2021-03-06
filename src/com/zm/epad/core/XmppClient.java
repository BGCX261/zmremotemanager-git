package com.zm.epad.core;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.ping.PingManager;

import com.zm.epad.plugins.RemoteAlarmManager;

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
    private final long PING_INTERVAL = 22 * 1000;
    private final String XMPP_DOMAIN = Config.getInstance() != null ? Config
            .getInstance().getConfig(Config.XMPP_DOMAIN)
            : "com.zm.communication";

    private ReentrantLock mStatusLock = new ReentrantLock();

    private XmppClientThreadHandler mXmppClientHandler = null;
    private HandlerThread mXmppHandlerThread = null;
    private Context mContext;
    private ArrayList<XmppClientCallback> mXmppClientCallbacks;
    private Connection mXmppConnection = null;
    private Bundle mConnectionInfo = null;
    private XMPPConnectionListener mXmppConnectionListener = null;
    private PingManager mPingManager = null;
    private int mPingAlarmId = -1;

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
            LogManager.server(TAG, "reportNetworkStatus : " + bConnected);
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
                SubSystemFacade.getInstance().acquireWakeLock(TAG);
                LogManager.server(TAG, "connectionClosed ");
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
                SubSystemFacade.getInstance().acquireWakeLock(TAG);
                LogManager.server(TAG,
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
            LogManager.server(TAG, "reconnectingIn " + seconds + "s");
        }

        @Override
        public void reconnectionSuccessful() {
            try {
                mStatusLock.lock();
                if (mXmppClientHandler == null)
                    return;
                LogManager.server(TAG, "reconnectionSuccessful ");
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
            LogManager.server(TAG, "reconnectionFailed : " + e.getMessage());
        }
    }

    private class XmppClientThreadHandler extends Handler {
        public XmppClientThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int cmd = msg.what;
            LogManager.server(TAG, "handleMessage cmd:" + cmd);
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
            } else if (cmd == CMD_QUIT) {
                handleQuitCmd();
            }

            return;
        }
    }

    private void handleStartCmd() {
        try {
            mStatusLock.lock();
            LogManager.server(TAG, "handleStartCmd status:"
                    + XMPPCLIENT_STATUS_STRINGS[mCurrentStatus]);
            if (mCurrentStatus != XMPPCLIENT_STATUS_STARTED
                    && mCurrentStatus != XMPPCLIENT_STATUS_LOGINED) {
                handleStartCmdLocked();
            }
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
            LogManager.server(TAG, "connect to server:" + serverName);

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

            // use ping manager to handle ping
            mPingManager = PingManager.getInstanceFor(mXmppConnection);
            startPing();

            transitionToStatusLocked(XMPPCLIENT_STATUS_STARTED);

            dispatchXmppClientEvent(XMPPCLIENT_EVENT_CONNECT, 1,
                    mXmppConnection, ProviderManager.getInstance());

        } catch (Exception e) {
            LogManager.server(TAG, "handleStartCmd ERR: " + e.toString());
            e.printStackTrace();
            transitionToStatusLocked(XMPPCLIENT_STATUS_ERROR);
            dispatchXmppClientEvent(XMPPCLIENT_EVENT_CONNECT, 0);
            throw e;
        }
    }

    private void handleLoginCmd() {
        try {
            mStatusLock.lock();
            LogManager.server(TAG, "handleLoginCmd status:"
                    + XMPPCLIENT_STATUS_STRINGS[mCurrentStatus]);
            if (mCurrentStatus != XMPPCLIENT_STATUS_LOGINED) {
                handleLoginCmdLocked();
            }
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
            if (usrName == null) {
                throw new Exception("No User Name");
            }

            mXmppConnection.login(usrName, usrPwd, usrResource);

            transitionToStatusLocked(XMPPCLIENT_STATUS_LOGINED);
            dispatchXmppClientEvent(XMPPCLIENT_EVENT_LOGIN, true);
        } catch (Exception e) {
            LogManager.server(TAG, "handleLoginCmd ERR: " + e.toString());
            e.printStackTrace();

            // disconnect so that user can retry. If doesn't disconnect,
            // authentication will always failed even if login with right info
            // when retry
            handleLogoutCmd(false);

            dispatchXmppClientEvent(XMPPCLIENT_EVENT_LOGIN, false);
            throw e;
        }
    }

    private void handleLogoutCmd(boolean bNetworkConnect) {
        try {
            mStatusLock.lock();
            stopPing();
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
            stopPing();
            mXmppConnection.disconnect();
            LogManager.server(TAG, "disconnect by handleQuitCmd");
            mPrevStatus = mCurrentStatus = XMPPCLIENT_STATUS_IDLE;
            mXmppConnection = null;
            mXmppClientHandler = null;
            LogManager.server(TAG, "thread quit");
            mXmppHandlerThread.quit();
        } finally {
            mStatusLock.unlock();
        }
    }

    private void handleNetworkAvailable(int networkAvailable) {
        if (networkAvailable == 0) {
            LogManager.server(TAG,
                    "handleNetworkAvailable : network is down, need logout");
            if (Config.getInstance().isAccountInitiated()) {
                handleLogoutCmd(false);
            }
        } else if (networkAvailable == 1) {
            try {
                mStatusLock.lock();
                LogManager.server(TAG, "handleNetworkAvailable current status:"
                        + mCurrentStatus);

                if (Config.getInstance().isAccountInitiated()) {
                    if (mCurrentStatus == XMPPCLIENT_STATUS_IDLE
                            || mCurrentStatus == XMPPCLIENT_STATUS_ERROR) {
                        // force to start and login
                        LogManager.server(TAG, "xmppclient re-login:"
                                + mCurrentStatus);
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
        return info == null ? false : info.isConnected();
    }

    private void handleConnectionStatus(Message msg) {
        try {
            mStatusLock.lock();
            int connected = msg.arg1;
            LogManager.server(TAG, "handleConnectionStatus:" + connected);
            if (!Config.getInstance().isAccountInitiated()) {
                LogManager.server(TAG, "No user");
                return;
            }
            if (connected == 0) {
                dispatchXmppClientEvent(
                        XMPPCLIENT_EVENT_CONNECTION_UPDATE_STATUS, 0, msg.obj);
                // logout for reconnect
                handleLogoutCmd(false);

                if (isNetworkConnected()) {
                    // network is on, but connect closed, it means error happens
                    LogManager.server(TAG, "network is on");
                    if (msg.obj == null) {
                        // simply closed, reconnect directly
                        reconnectByError();
                    } else {
                        // closed by error, reconnect after a few seconds
                        sendReconnectByError((Exception) msg.obj);
                    }
                } else {
                    LogManager.server(TAG, "network is off");
                }
            } else {
                dispatchXmppClientEvent(
                        XMPPCLIENT_EVENT_CONNECTION_UPDATE_STATUS, 1);
            }
        } finally {
            mStatusLock.unlock();
            SubSystemFacade.getInstance().releaseWakeLock(TAG);
            LogManager.server(TAG, "handleConnectionStatus return");
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
            } else {
                mPrevStatus = mCurrentStatus;
                mCurrentStatus = newStatus;
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

    public boolean start() {
        try {
            mStatusLock.lock();

            LogManager.server(TAG, "xmppclient start");

            mXmppHandlerThread = new HandlerThread(TAG);
            mXmppHandlerThread.start();
            mXmppClientHandler = new XmppClientThreadHandler(
                    mXmppHandlerThread.getLooper());

        } finally {
            mStatusLock.unlock();
        }
        return true;
    }

    public void stop() {
        LogManager.server(TAG, "stop xmppclient");
        try {
            if (mXmppClientHandler == null) {
                LogManager.server(TAG, "xmppclient already stopped");
                return;
            }
            // thread quit will be called when receive CMD_QUIT
            mXmppClientHandler.sendEmptyMessage(CMD_QUIT);

            LogManager.server(TAG, "XmppHandlerThread join start");
            mXmppHandlerThread.join();
            mXmppHandlerThread = null;
            LogManager.server(TAG, "XmppHandlerThread join end");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
        destroyXMPPEnvironment();
        LogManager.server(TAG, "xmppclient stopped");
    }

    public boolean connect(String serverName) {
        if (serverName == null) {
            LogManager.server(TAG, "FAILED: connect without serverName");
            return false;
        }
        try {
            mStatusLock.lock();
            if (mCurrentStatus != XMPPCLIENT_STATUS_IDLE
                    && mCurrentStatus != XMPPCLIENT_STATUS_ERROR) {
                LogManager.server(TAG, "Cannot start because currentstatus is "
                        + XMPPCLIENT_STATUS_STRINGS[mCurrentStatus]);
                return false;
            }

            LogManager.server(TAG, "xmppclient connect :" + serverName);
            mConnectionInfo
                    .putString(CoreConstants.CONSTANT_SERVER, serverName);

            transitionToStatusLocked(XMPPCLIENT_STATUS_STARTING);
            mXmppClientHandler.sendEmptyMessage(CMD_START);

        } finally {
            mStatusLock.unlock();
        }
        return true;
    }

    public boolean login(String usrName, String password, String resource) {
        if (usrName == null || password == null || resource == null) {
            LogManager.server(TAG, "xmppclient login failed due to null info");
            return false;
        }

        boolean bRet = true;
        try {
            mStatusLock.lock();
            if (mXmppClientHandler == null) {
                return false;
            }

            if (mCurrentStatus != XMPPCLIENT_STATUS_STARTING
                    && mCurrentStatus != XMPPCLIENT_STATUS_STARTED) {
                LogManager.server(TAG, "Cannot login because currentstatus is "
                        + XMPPCLIENT_STATUS_STRINGS[mCurrentStatus]);
                return false;
            }
            transitionToStatusLocked(XMPPCLIENT_STATUS_LOGINING);
            LogManager.server(TAG, "xmppclient login username " + usrName
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
        return bRet;
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

    private void sendReconnectByError(Exception e) {
        if (Config.getInstance().isAccountInitiated()) {
            LogManager.server(TAG, "sendReconnectByError:" + e.toString());
            // if failed by error, reconnect after 10 seconds.
            try {
                SubSystemFacade.getInstance().setAlarm(
                        System.currentTimeMillis() + 10000,
                        new RemoteAlarmManager.AlarmCallback() {

                            @Override
                            public void wakeUp() {
                                LogManager.server(TAG,
                                        "sendReconnectByError send event");
                                Message msg = mXmppClientHandler.obtainMessage(
                                        CMD_RECONNECT_BY_ERROR, null);
                                mXmppClientHandler.sendMessage(msg);
                            }
                        });
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }

    private void reconnectByError() {
        if (mCurrentStatus != XMPPCLIENT_STATUS_ERROR || !isNetworkConnected()) {
            // only reconnect when error and online.
            // if offline, reconnect when network available.
            LogManager.server(TAG, "stop reconnect because status is: "
                    + XMPPCLIENT_STATUS_STRINGS[mCurrentStatus]);
            return;
        }
        try {
            mStatusLock.lock();
            // avoid to sleep
            SubSystemFacade.getInstance().acquireWakeLock(TAG);

            String serverName = mConnectionInfo
                    .getString(CoreConstants.CONSTANT_SERVER);
            LogManager.server(TAG, "connect to server:" + serverName);

            ConnectionConfiguration config = new ConnectionConfiguration(
                    serverName);
            config.setCompressionEnabled(true);
            config.setDebuggerEnabled(true);
            // disable ReconnectionAllowed to avoid double connect
            config.setReconnectionAllowed(false);

            if (mXmppConnection == null) {
                LogManager.server(TAG, "new XMPPConnection");
                mXmppConnection = new XMPPConnection(config);
                mXmppConnectionListener = new XMPPConnectionListener();
                mXmppConnection.addConnectionListener(mXmppConnectionListener);
            }
            mXmppConnection.connect();
            mPingManager = PingManager.getInstanceFor(mXmppConnection);
            startPing();

            dispatchXmppClientEvent(XMPPCLIENT_EVENT_CONNECT, 1,
                    mXmppConnection, ProviderManager.getInstance());

            String usrName = mConnectionInfo
                    .getString(CoreConstants.CONSTANT_USRNAME);
            String usrPwd = mConnectionInfo
                    .getString(CoreConstants.CONSTANT_PASSWORD);
            String usrResource = mConnectionInfo
                    .getString(CoreConstants.CONSTANT_RESOURCE);
            if (usrName != null) {
                mXmppConnection.login(usrName, usrPwd, usrResource);
                transitionToStatusLocked(XMPPCLIENT_STATUS_LOGINED);
                dispatchXmppClientEvent(XMPPCLIENT_EVENT_LOGIN, true);
            } else {
                transitionToStatusLocked(XMPPCLIENT_STATUS_STARTED);
            }
        } catch (Exception e) {
            if (mXmppConnection == null) {
                transitionToStatusLocked(XMPPCLIENT_STATUS_ERROR);
            } else {
                handleLogoutCmd(false);
            }
            sendReconnectByError(e);
        } finally {
            SubSystemFacade.getInstance().releaseWakeLock(TAG);
            mStatusLock.unlock();
        }
    }

    private void startPing() {
        try {
            mPingAlarmId = SubSystemFacade.getInstance().setAlarm(
                    System.currentTimeMillis() + PING_INTERVAL,
                    new RemoteAlarmManager.AlarmCallback() {

                        @Override
                        public void wakeUp() {
                            LogManager.server(TAG, "ping server");
                            mPingManager.ping(XMPP_DOMAIN);
                            startPing();
                        }
                    });
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void stopPing() {
        if (mPingAlarmId >= 0) {
            SubSystemFacade.getInstance().cancelAlarm(mPingAlarmId);
        }
    }
}
