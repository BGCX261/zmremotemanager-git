package com.android.remotemanager.plugins;

import java.util.ArrayList;
import com.android.remotemanager.NetworkStatusMonitor;
import com.android.remotemanager.plugins.xmpp.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.XmlPullParser;

import android.R.bool;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.VoicemailContract;
import android.provider.ContactsContract.Contacts.Data;
import android.util.Log;

public class XmppClient implements NetworkStatusMonitor.NetworkStatusReport {

    public static final int XMPPCLIENT_EVENT_CONNECT = 1;
    public static final int XMPPCLIENT_EVENT_LOGIN = 2;
    public static final int XMPPCLIENT_EVENT_CONNECTION_UPDATE_STATUS = 3;
    public static final int XMPPCLIENT_EVENT_LOGOUT = 4;
    public static final int XMPPCLIENT_EVENT_SENDPACKET_RESULT = 5;

    static final int CMD_START = 0;
    static final int CMD_CONNECTION_STATUS_UPDATE = 1;
    static final int CMD_NETWORK_STATUS_UPDATE = 2;
    static final int CMD_LOGIN = 3;
    static final int CMD_LOGOUT = 4;
    static final int CMD_QUIT = 5;
    static final int CMD_SEND_PACKET_ASYNC = 6;

    static final int XMPPCLIENT_STATUS_IDLE = 0;
    static final int XMPPCLIENT_STATUS_STARTING = 1;
    static final int XMPPCLIENT_STATUS_STARTED = 2;
    static final int XMPPCLIENT_STATUS_LOGINING = 3;
    static final int XMPPCLIENT_STATUS_LOGINED = 4;
    static final int XMPPCLIENT_STATUS_ERROR = 5;

    static final String[] XMPPCLIENT_STATUS_STRINGS = { " idle ", " starting ",
            " started ", " logining ", " logined ", " error " };

    private Object mStatusLock = new Object();
    private int mCurrentStatus = XMPPCLIENT_STATUS_IDLE;
    private int mPrevStatus = mCurrentStatus;

    public interface XmppClientCallback {
        public Object reportXMPPClientEvent(int xmppClientEvent, Object... args);
    }

    static private String TAG = "XmppClient";

    static private SmackAndroid mSmackAndroid = null;

    @Override
    public void reportNetworkStatus(boolean bConnected) {
        synchronized (mStatusLock) {
            if (mXmppClientHandler == null)
                return;
            Message msg = mXmppClientHandler
                    .obtainMessage(CMD_NETWORK_STATUS_UPDATE);
            msg.arg1 = bConnected ? 1 : 0;
            mXmppClientHandler.sendMessage(msg);
            Log.e(TAG, "reportNetworkStatus : " + bConnected);
        }

    }

    static public void initializeXMPPEnvironment(Context context) {
        if (mSmackAndroid == null) {
            mSmackAndroid = SmackAndroid.init(context);
        }
        return;
    }

    static public void destroyXMPPEnvironment() {
        if (mSmackAndroid != null) {
            mSmackAndroid.onDestroy();
            mSmackAndroid = null;
        }
        return;
    }

    private class XMPPConnectionListener implements ConnectionListener {

        @Override
        public void connectionClosed() {
            synchronized (mStatusLock) {
                if (mXmppClientHandler == null)
                    return;
                Log.d(TAG, "connectionClosed ");
                Message msg = mXmppClientHandler
                        .obtainMessage(CMD_CONNECTION_STATUS_UPDATE);
                msg.arg1 = 0;
                mXmppClientHandler.sendMessage(msg);
            }
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            synchronized (mStatusLock) {
                if (mXmppClientHandler == null)
                    return;
                Log.d(TAG, "connectionClosedOnError " + e.getMessage());
                Message msg = mXmppClientHandler
                        .obtainMessage(CMD_CONNECTION_STATUS_UPDATE);
                msg.arg1 = 0;
                msg.obj = e;
                mXmppClientHandler.sendMessage(msg);
            }
        }

        @Override
        public void reconnectingIn(int seconds) {
            Log.d(TAG, "reconnectingIn " + seconds + "s");
        }

        @Override
        public void reconnectionSuccessful() {
            synchronized (mStatusLock) {
                if (mXmppClientHandler == null)
                    return;
                Log.d(TAG, "reconnectionSuccessful ");
                Message msg = mXmppClientHandler
                        .obtainMessage(CMD_CONNECTION_STATUS_UPDATE);
                msg.arg1 = 1;
                mXmppClientHandler.sendMessage(msg);
            }
        }

        @Override
        public void reconnectionFailed(Exception e) {
            Log.d(TAG, "reconnectionFailed : " + e.getMessage());
        }
    }

    private class XmppClientThreadHandler extends Handler {
        public XmppClientThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int cmd = msg.what;
            Log.d(TAG, "handleMessage cmd:" + cmd);
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
            } else if (cmd == CMD_QUIT) {
                handleCmdQuit();
            }

            return;
        }
    }

    private void handleStartCmd() {
        synchronized (mStatusLock) {
            handleStartCmdLocked();
        }

    }

    private void handleStartCmdLocked() {
        try {
            String serverName = mConnectionInfo.getString("server");
            Log.d(TAG, "connect to server:" + serverName);
            mXmppConnection = new XMPPConnection(serverName);
            mXmppConnection.connect();

            mXmppConnectionListener = new XMPPConnectionListener();
            mXmppConnection.addConnectionListener(mXmppConnectionListener);

            transitionToStatusLocked(XMPPCLIENT_STATUS_STARTED);

            mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_CONNECT,
                    1, mXmppConnection, ProviderManager.getInstance());

        } catch (Exception e) {
            Log.e(TAG, "handleStartCmd ERR: " + e.toString());
            transitionToStatusLocked(XMPPCLIENT_STATUS_ERROR);
            mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_CONNECT,
                    0);

        }
    }

    private void handleLoginCmd() {
        synchronized (mStatusLock) {
            handleLoginCmdLocked();
        }
    }

    private void handleLoginCmdLocked() {
        try {
            String usrName = mConnectionInfo.getString("username");
            String usrPwd = mConnectionInfo.getString("password");
            String usrResource = mConnectionInfo.getString("resource");
            mXmppConnection.login(usrName, usrPwd, usrResource);

            transitionToStatusLocked(XMPPCLIENT_STATUS_LOGINED);
            mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_LOGIN,
                    true);
        } catch (Exception e) {
            Log.e(TAG, "handleLoginCmd ERR: " + e.toString());
            transitionToStatusLocked(XMPPCLIENT_STATUS_ERROR);
            mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_LOGIN,
                    false);
        }

    }

    private void handleLogoutCmd(boolean bNetworkConnect) {
        synchronized (mStatusLock) {
            mXmppConnection.disconnect();
            if (bNetworkConnect)
                transitionToStatusLocked(XMPPCLIENT_STATUS_IDLE);
            else
                transitionToStatusLocked(XMPPCLIENT_STATUS_ERROR);
            mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_LOGOUT,
                    true);
        }
    }

    private void handleCmdQuit() {
        synchronized (mStatusLock) {
            mXmppConnection.disconnect();
            mPrevStatus = mCurrentStatus = XMPPCLIENT_STATUS_IDLE;
            mXmppConnection = null;
            mXmppClientHandler = null;
            mStatusLock.notifyAll();
        }
    }

    private void handleNetworkAvailable(int networkAvailable) {
        if (networkAvailable == 0) {
            Log.e(TAG, "handleNetworkAvailable : network is down, need logout");
            handleLogoutCmd(false);
        } else if (networkAvailable == 1) {
            synchronized (mStatusLock) {
                if (mCurrentStatus == XMPPCLIENT_STATUS_IDLE) {
                    mPrevStatus = XMPPCLIENT_STATUS_IDLE;
                    Log.e(TAG, "\t xmppclient not working, do nothing");
                } else if (mPrevStatus == XMPPCLIENT_STATUS_STARTING
                        || mPrevStatus == XMPPCLIENT_STATUS_STARTED) {
                    Log.e(TAG, "\t xmppclient re-start");
                    handleStartCmdLocked();
                } else if (mPrevStatus == XMPPCLIENT_STATUS_LOGINING
                        || mPrevStatus == XMPPCLIENT_STATUS_LOGINED) {
                    Log.e(TAG, "\t xmppclient re-login");
                    handleStartCmdLocked();
                    handleLoginCmdLocked();
                }
            }
        }
        return;
    }

    private void handleConnectionStatus(Message msg) {
        synchronized (mStatusLock) {
            int connected = msg.arg1;
            if (connected == 0) {
                mXmppClientCallback.reportXMPPClientEvent(
                        XMPPCLIENT_EVENT_CONNECTION_UPDATE_STATUS, 0, msg.obj);
            } else {
                mXmppClientCallback.reportXMPPClientEvent(
                        XMPPCLIENT_EVENT_CONNECTION_UPDATE_STATUS, 1);
            }
        }

    }
    private void handleSendPacketCmd(Message msg){
        synchronized (mStatusLock) {
            try {
                mXmppConnection.sendPacket((Packet) msg.obj);
                mXmppClientCallback.reportXMPPClientEvent(
                        XMPPCLIENT_EVENT_SENDPACKET_RESULT, true, msg.obj);
            } catch (Exception e) {
                mXmppClientCallback.reportXMPPClientEvent(
                        XMPPCLIENT_EVENT_SENDPACKET_RESULT, false, msg.obj);
            }
        }
       
    }

    private XmppClientThreadHandler mXmppClientHandler = null;
    private HandlerThread mXmppHandlerThread = null;
    private Context mContext;
    private XmppClientCallback mXmppClientCallback;
    private Connection mXmppConnection = null;
    private Bundle mConnectionInfo = null;
    private XMPPConnectionListener mXmppConnectionListener = null;

    public XmppClient(Context context, XmppClientCallback xmppClientCallback) {
        mContext = context;
        mXmppClientCallback = xmppClientCallback;
        mConnectionInfo = new Bundle();
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
            Log.e(TAG, "FAILED: start without serverName");
            return false;
        }

        synchronized (mStatusLock) {
            if (mCurrentStatus != XMPPCLIENT_STATUS_IDLE
                    && mCurrentStatus != XMPPCLIENT_STATUS_ERROR) {
                Log.e(TAG, "Cannot start because currentstatus is "
                        + XMPPCLIENT_STATUS_STRINGS[mCurrentStatus]);
                return false;
            }

            Log.e(TAG, "xmppclient start with servername :" + serverName);
            mConnectionInfo.putString("server", serverName);
            mXmppHandlerThread = new HandlerThread(TAG);
            mXmppHandlerThread.start();
            mXmppClientHandler = new XmppClientThreadHandler(
                    mXmppHandlerThread.getLooper());
            transitionToStatusLocked(XMPPCLIENT_STATUS_STARTING);
            mXmppClientHandler.sendEmptyMessage(CMD_START);
        }
        return true;
    }

    public void stop() {
        Log.e(TAG, "stop xmppclient");
        synchronized (mStatusLock) {
            if (mXmppClientHandler == null) {
                Log.e(TAG, "xmppclient already stopped");
                return;
            }
            mXmppClientHandler.sendEmptyMessage(CMD_QUIT);

            try {
                mXmppHandlerThread.quit();
                mStatusLock.wait();
            } catch (Exception e) {
                Log.e(TAG, "xmppclient stop thread error " + e.getMessage());
            }
        }
        synchronized (mStatusLock) {
            try {
                mXmppHandlerThread.join();
                mXmppHandlerThread = null;
            } catch (Exception e) {

            }
        }
        Log.e(TAG, "xmppclient stopped");

    }

    public boolean login(String usrName, String password, String resource) {
        if (usrName == null || password == null) {
            Log.e(TAG,
                    "xmppclient login failed,either username or password is null");
            return false;
        }
        synchronized (mStatusLock) {
            if (mXmppClientHandler == null) {
                return false;
            }

            if (mCurrentStatus != XMPPCLIENT_STATUS_STARTING
                    && mCurrentStatus != XMPPCLIENT_STATUS_STARTED) {
                Log.e(TAG, "Cannot login because currentstatus is "
                        + XMPPCLIENT_STATUS_STRINGS[mCurrentStatus]);
                return false;
            }
            transitionToStatusLocked(XMPPCLIENT_STATUS_LOGINING);
            Log.e(TAG, "xmppclient login username " + usrName + " password "
                    + password + " resource " + resource);
            mConnectionInfo.putString("username", usrName);
            mConnectionInfo.putString("password", password);
            mConnectionInfo.putString("resource", resource == null ? "zmtech"
                    : resource);
            Message msg = mXmppClientHandler.obtainMessage(CMD_LOGIN);
            msg.setData(mConnectionInfo);
            mXmppClientHandler.sendMessage(msg);
        }
        return true;
    }

    public void logout() {
        synchronized (mStatusLock) {
            if (mXmppClientHandler == null)
                return;
            mXmppClientHandler.sendEmptyMessage(CMD_LOGOUT);
        }
        return;
    }

    public boolean sendPacket(Packet packet) {
        synchronized (mStatusLock) {
            if (mXmppConnection == null)
                return false;
            mXmppConnection.sendPacket(packet);
        }
        return true;
    }

    public boolean sendPacketAsync(Packet packet) {
        synchronized (mStatusLock) {
            if (mXmppConnection == null)
                return false;
            Message msg = mXmppClientHandler.obtainMessage(
                    CMD_SEND_PACKET_ASYNC, packet);
            mXmppClientHandler.sendMessage(msg);
        }
        return true;
    }

}
