package com.zm.epad.core;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract.Contacts.Data;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer.IncomingFileTransferProgress;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer.NegotiationProgress;
import org.xbill.DNS.MFRecord;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class XmppClient implements NetworkStatusMonitor.NetworkStatusReport {
    private static final String TAG = "XmppClient";

    public static final int XMPPCLIENT_EVENT_CONNECT = 1;
    public static final int XMPPCLIENT_EVENT_LOGIN = 2;
    public static final int XMPPCLIENT_EVENT_CONNECTION_UPDATE_STATUS = 3;
    public static final int XMPPCLIENT_EVENT_LOGOUT = 4;
    public static final int XMPPCLIENT_EVENT_SENDPACKET_RESULT = 5;
    public static final int XMPPCLIENT_EVENT_SEND_FILE_RESULT = 6;
    public static final int XMPPCLIENT_EVENT_RECV_FILE_RESULT = 7;

    static final int CMD_START = 0;
    static final int CMD_CONNECTION_STATUS_UPDATE = 1;
    static final int CMD_NETWORK_STATUS_UPDATE = 2;
    static final int CMD_LOGIN = 3;
    static final int CMD_LOGOUT = 4;
    static final int CMD_QUIT = 5;
    static final int CMD_SEND_PACKET_ASYNC = 6;
    static final int CMD_SEND_FILE = 7;
    static final int CMD_SEND_FILE_RESULT = 8;
    static final int CMD_RECV_FILE_RESULT = 9;

    static final int XMPPCLIENT_STATUS_IDLE = 0;
    static final int XMPPCLIENT_STATUS_STARTING = 1;
    static final int XMPPCLIENT_STATUS_STARTED = 2;
    static final int XMPPCLIENT_STATUS_LOGINING = 3;
    static final int XMPPCLIENT_STATUS_LOGINED = 4;
    static final int XMPPCLIENT_STATUS_ERROR = 5;

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
    private TransferServer mTransferServer = null;

    public interface XmppClientCallback {
        public Object reportXMPPClientEvent(int xmppClientEvent, Object... args);
    }

    public XmppClient(Context context) {
        mContext = context;
        mXmppClientCallbacks = new ArrayList<XmppClient.XmppClientCallback>();
        mConnectionInfo = new Bundle();
        mTransferServer = new TransferServer();
    }

    /*
     * Currently, we use XMPP File Transfer Protocol In the future, we need to
     * use Http Protocol
     */
    private class TransferServer implements FileTransferListener,
            NegotiationProgress, IncomingFileTransferProgress {

        private FileTransferManager mFTManager = null;
        final private static String UPLOAD_FILE_PATH = "capture@com.zm.openfire/default";

        public TransferServer() {

        }

        public void sendFile(String filepath, final String description) {
            try {
                File sentFile = new File(filepath);
                if (sentFile.exists() == false || sentFile.canRead() == false) {
                    LogManager.local(TAG, "Target file " + filepath
                            + " is not existed or not readable");
                    return;
                }
                OutgoingFileTransfer fileTransfer = mFTManager
                        .createOutgoingFileTransfer(UPLOAD_FILE_PATH);
                fileTransfer.sendFile(filepath, sentFile.length(), description,
                        this);
            } catch (Exception e) {
                LogManager.local(TAG, e.toString());
            }
        }

        public void start(Connection conn) {
            if (mFTManager != null)
                return;
            mFTManager = new FileTransferManager(conn);
            mFTManager.addFileTransferListener(this);
        }

        public void stop() {
            if (mFTManager == null)
                return;
            mFTManager.removeFileTransferListener(this);
            mFTManager = null;
        }

        @Override
        public void statusUpdated(Status oldStatus, Status newStatus) {
            int fileSentFinished = 0;
            if (newStatus == Status.complete) {
                fileSentFinished = 1;
            } else if (newStatus == Status.error
                    || newStatus == Status.cancelled
                    || newStatus == Status.refused || newStatus == Status.error) {
                fileSentFinished = 0;
            }
            try {
                mStatusLock.lock();
                if (mXmppClientHandler == null)
                    return;
                Message msg = mXmppClientHandler
                        .obtainMessage(CMD_SEND_FILE_RESULT);
                msg.arg1 = fileSentFinished;
                mXmppClientHandler.sendMessage(msg);
            } finally {
                mStatusLock.unlock();
            }
        }

        @Override
        public void outputStreamEstablished(OutputStream stream) {

        }

        @Override
        public void errorEstablishingStream(Exception e) {
            try {
                mStatusLock.lock();
                if (mXmppClientHandler == null)
                    return;
                Message msg = mXmppClientHandler
                        .obtainMessage(CMD_SEND_FILE_RESULT);
                msg.arg1 = 0;
                mXmppClientHandler.sendMessage(msg);
                LogManager.local(TAG,
                        "report File Upload fails : " + e.getMessage());
            } finally {
                mStatusLock.unlock();
            }
        }

        public void incomingFileTransferStatus(Status oldStatus,
                Status newStatus) {
            int fileReceiveFinished = 0;
            if (newStatus == Status.complete) {
                fileReceiveFinished = 1;
            } else if (newStatus == Status.error
                    || newStatus == Status.cancelled
                    || newStatus == Status.refused || newStatus == Status.error) {
                fileReceiveFinished = 0;
            }
            try {
                mStatusLock.lock();
                if (mXmppClientHandler == null)
                    return;
                Message msg = mXmppClientHandler
                        .obtainMessage(CMD_RECV_FILE_RESULT);
                msg.arg1 = fileReceiveFinished;
                mXmppClientHandler.sendMessage(msg);
            } finally {
                mStatusLock.unlock();
            }
        }

        public void incomingFileTransferError(Exception e) {
            try {
                mStatusLock.lock();
                if (mXmppClientHandler == null)
                    return;
                Message msg = mXmppClientHandler
                        .obtainMessage(CMD_RECV_FILE_RESULT);
                msg.arg1 = 0;
                mXmppClientHandler.sendMessage(msg);
                LogManager.local(TAG,
                        "report File Upload fails : " + e.getMessage());
            } finally {
                mStatusLock.unlock();
            }

        }
        public void fileTransferRequest(FileTransferRequest request) {
            String mimeType = request.getMimeType();
            if (isImage(mimeType) == false) {
                LogManager.local(TAG,
                        "reject a file transfer request due to non-matchable"
                                + "mime type:" + mimeType + " from "
                                + request.getRequestor());
                request.reject();
                return;
            }
            IncomingFileTransfer transfer = request.accept();
            try {
                LogManager.local(TAG,
                        "Begin receive file:" + request.getRequestor());
                String fileName = request.getFileName();

                File temp = new File(mContext.getFilesDir().getAbsolutePath()
                        + "/" + fileName);
                transfer.recieveFile(temp, this);
                // // ProminentFeature.saveFileAsImage(mContext, mTempfile);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private boolean isImage(String Mime) {
            return Mime.equals("image/jpeg") || Mime.equals("image/png")
                    || Mime.equals("image/bmp");
        }
    }

    static public void initializeXMPPEnvironment(Context context) {
        if (mSmackAndroid == null) {
            mSmackAndroid = SmackAndroid.init(context);
        }
    }

    static public void destroyXMPPEnvironment() {
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
            } else if (cmd == CMD_QUIT) {
                handleQuitCmd();
            } else if (cmd == CMD_SEND_FILE) {
                handleSendFileCmd(msg.getData());
            } else if (cmd == CMD_SEND_FILE_RESULT
                    || cmd == CMD_RECV_FILE_RESULT) {
                handleSendReceiveFileResult(msg);
            }

            return;
        }
    }

    private void handleSendReceiveFileResult(Message msg) {
        int result = msg.arg1;
        try {
            mStatusLock.lock();
            if (msg.what == CMD_SEND_FILE_RESULT)
                dispatchXmppClientEvent(XMPPCLIENT_EVENT_SEND_FILE_RESULT,
                        result);
            else
                dispatchXmppClientEvent(XMPPCLIENT_EVENT_RECV_FILE_RESULT,
                        result);
        } finally {
            mStatusLock.unlock();
        }
    }

    private void handleSendFileCmd(Bundle data) {
        String filePath = data.getString("filepath");
        String description = data.getString("description");

        mTransferServer.sendFile(filePath, description);

        return;

    }

    private void handleStartCmd() {
        try {
            mStatusLock.lock();
            handleStartCmdLocked();
        } finally {
            mStatusLock.unlock();
        }
    }

    private void dispatchXmppClientEvent(int xmppClientEvent, Object... args) {
        for (XmppClientCallback callback : mXmppClientCallbacks) {
            callback.reportXMPPClientEvent(xmppClientEvent, args);
        }
    }

    private void handleStartCmdLocked() {
        try {
            String serverName = mConnectionInfo.getString("server");
            LogManager.local(TAG, "connect to server:" + serverName);
            ConnectionConfiguration config = new ConnectionConfiguration(
                    serverName);
            config.setCompressionEnabled(true);
            config.setDebuggerEnabled(true);
            config.setReconnectionAllowed(true);

            mXmppConnection = new XMPPConnection(config);
            mXmppConnection.connect();

            mXmppConnectionListener = new XMPPConnectionListener();
            mXmppConnection.addConnectionListener(mXmppConnectionListener);

            transitionToStatusLocked(XMPPCLIENT_STATUS_STARTED);

            dispatchXmppClientEvent(XMPPCLIENT_EVENT_CONNECT, 1,
                    mXmppConnection, ProviderManager.getInstance());

        } catch (Exception e) {
            LogManager.local(TAG, "handleStartCmd ERR: " + e.toString());
            transitionToStatusLocked(XMPPCLIENT_STATUS_ERROR);
            dispatchXmppClientEvent(XMPPCLIENT_EVENT_CONNECT, 0);
        }
    }

    private void handleLoginCmd() {
        try {
            mStatusLock.lock();
            handleLoginCmdLocked();
        } finally {
            mStatusLock.unlock();
        }
    }

    private void handleLoginCmdLocked() {
        try {
            String usrName = mConnectionInfo.getString("username");
            String usrPwd = mConnectionInfo.getString("password");
            String usrResource = mConnectionInfo.getString("resource");

            mXmppConnection.login(usrName, usrPwd, usrResource);
            mTransferServer.start(mXmppConnection);
            // addFileReceiver(mXmppConnection);

            transitionToStatusLocked(XMPPCLIENT_STATUS_LOGINED);

            dispatchXmppClientEvent(XMPPCLIENT_EVENT_LOGIN, true);
        } catch (Exception e) {
            LogManager.local(TAG, "handleLoginCmd ERR: " + e.toString());
            transitionToStatusLocked(XMPPCLIENT_STATUS_ERROR);
            dispatchXmppClientEvent(XMPPCLIENT_EVENT_LOGIN, false);
        }
    }

    private void handleLogoutCmd(boolean bNetworkConnect) {
        try {
            mStatusLock.lock();
            mXmppConnection.disconnect();
            mTransferServer.stop();
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
            synchronized (mStatusLock) {
                if (mCurrentStatus == XMPPCLIENT_STATUS_IDLE) {
                    mPrevStatus = XMPPCLIENT_STATUS_IDLE;
                    LogManager.local(TAG,
                            "\t xmppclient not working, do nothing");
                } else if (mPrevStatus == XMPPCLIENT_STATUS_STARTING
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
        }
        return;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info.isConnected();
    }

    private void handleConnectionStatus(Message msg) {
        try {
            mStatusLock.lock();
            int connected = msg.arg1;
            if (connected == 0) {
                dispatchXmppClientEvent(
                        XMPPCLIENT_EVENT_CONNECTION_UPDATE_STATUS, 0, msg.obj);

                // when it's not closed by error and network is on
                if (msg.obj == null && isNetworkConnected()) {
                    LogManager.local(TAG, "ready to reconnect");
                    if (mCurrentStatus == XMPPCLIENT_STATUS_IDLE) {
                        mPrevStatus = XMPPCLIENT_STATUS_IDLE;
                        LogManager.local(TAG,
                                "\t xmppclient not working, do nothing");
                    } else if (mPrevStatus == XMPPCLIENT_STATUS_STARTING
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
            mConnectionInfo.putString("server", serverName);
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

        LogManager.local(TAG, "xmppclient stopped");
    }

    public boolean login(String usrName, String password, String resource) {
        if (usrName == null || password == null) {
            LogManager
                    .local(TAG,
                            "xmppclient login failed,either username or password is null");
            return false;
        }
        if (resource == null && Build.SERIAL == null) {
            if (Build.SERIAL == null) {
                LogManager.local(TAG,
                        "xmppclient login failed due to resource is null");
                return false;
            }
            resource = Build.SERIAL;

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
            mConnectionInfo.putString("username", usrName);
            mConnectionInfo.putString("password", password);
            mConnectionInfo.putString("resource", resource);
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

    public void sendFile(String filePath, final String description) {
        try {
            mStatusLock.lock();
            if (mXmppConnection == null)
                return;
            Message msg = mXmppClientHandler.obtainMessage(CMD_SEND_FILE);
            Bundle msgData = new Bundle();
            msgData.putString("filepath", filePath);
            msgData.putString("description", description);
            msg.setData(msgData);
            mXmppClientHandler.sendMessage(msg);
        } finally {
            mStatusLock.unlock();
        }
        return;
    }

}
