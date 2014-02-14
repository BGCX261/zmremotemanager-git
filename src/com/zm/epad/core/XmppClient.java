package com.zm.epad.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
/*import org.jivesoftware.smack.util.Base64.OutputStream;
 import org.jivesoftware.smackx.filetransfer.FileTransferListener;
 import org.jivesoftware.smackx.filetransfer.FileTransferManager;
 import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
 import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
 import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;*/

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Locale;
import java.util.UUID;
import java.net.URL;

public class XmppClient implements NetworkStatusMonitor.NetworkStatusReport {
    private static final String TAG = "XmppClient";

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
    static final int CMD_SEND_OBJECT_ASYNC = 7;

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
    private TransferService mTransferService = null;

    // private FileTransferManager mFTManager = null;

    public interface XmppClientCallback {
        public Object reportXMPPClientEvent(int xmppClientEvent, Object... args);
    }

    public XmppClient(Context context) {
        mContext = context;
        mXmppClientCallbacks = new ArrayList<XmppClient.XmppClientCallback>();
        mConnectionInfo = new Bundle();
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
            }

            return;
        }
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

            /* addFileReceiver(mXmppConnection); */

            transitionToStatusLocked(XMPPCLIENT_STATUS_LOGINED);
            mTransferService = new TransferService();
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
            if (bNetworkConnect)
                transitionToStatusLocked(XMPPCLIENT_STATUS_IDLE);
            else
                transitionToStatusLocked(XMPPCLIENT_STATUS_ERROR);
            mTransferService = null;
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

    /*
     * public void sendFile(File file, final String description) { try { String
     * usrName = mConnectionInfo.getString("username"); String usrResource =
     * mConnectionInfo.getString("resource");
     * 
     * OutgoingFileTransfer fileTransfer = mFTManager
     * .createOutgoingFileTransfer("capture@com.zm.openfire/default");
     * fileTransfer.sendFile(file, description);
     * 
     * } catch (Exception e) { LogManager.local(TAG, e.toString()); } }
     */

    public String sendObject(byte[] png, final String description,
            String requestUrl, Bundle info) {
        if (mTransferService == null)
            return null;
        return mTransferService.uploadObject(png, description, requestUrl, info);
    }

    public File receiveObject(final String requestUrl) {
        if (mTransferService == null) {
            return null;
        }
        return mTransferService.receiveObject(requestUrl);
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

    private class TransferService {
        private static final int TIME_OUT = 10 * 10000000;
        private static final String CHARSET = "utf-8";
        public static final String SUCCESS = "1";
        public static final String FAILURE = "0";
        public static final String PREFIX = "--";
        public static final String LINE_END = "\r\n";
        public static final String CONTENT_TYPE = "multipart/form-data";

        private SimpleDateFormat mSimpleDateFmt = new SimpleDateFormat(
                "yyyy-MM-dd HH:mmZ", Locale.US);

        public TransferService() {
        }

        private HttpURLConnection createUrlConnection(String requestUrl,
                String BOUNDARY) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(requestUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(TIME_OUT);
                conn.setConnectTimeout(TIME_OUT);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                if (BOUNDARY != null) {
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Charset", CHARSET);
                    conn.setRequestProperty("connection", "keep-alive");
                    conn.setRequestProperty("Content-Type", CONTENT_TYPE
                            + ";boundary=" + BOUNDARY);
                    conn.setChunkedStreamingMode(10240);
                }

                return conn;
            } catch (Exception e) {
                LogManager.local(TAG,
                        "createUrlConnection fails " + e.getMessage());
                return null;
            }
        }

        private String getFileName(String desc) {
            return mSimpleDateFmt.format(new Date()) + desc;
        }

        private String getHttpTailInfo(String BOUNDARY) {
            String endString = LINE_END + PREFIX + BOUNDARY + PREFIX + LINE_END;
            return endString;
        }

        public File receiveObject(String requestUrl) {
            if (requestUrl == null)
                return null;
            HttpURLConnection conn = createUrlConnection(requestUrl, null);
            if (conn == null) {
                return null;
            }
            int lastIndexofSlash = requestUrl.lastIndexOf("/");
            String fileName = requestUrl.substring(lastIndexofSlash);
            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            File recvedFile = new File(mContext.getFilesDir().getAbsolutePath()
                    + fileName);
            try {
                inputStream = conn.getInputStream();
                // don't handle the case that the file already exists
                recvedFile.createNewFile();
                outputStream = new FileOutputStream(recvedFile);
                byte[] buff = new byte[1024];
                int readCount = 0;
                while ((readCount = inputStream.read(buff)) > 0) {
                    outputStream.write(buff, 0, readCount);
                }
                buff = null;
                inputStream.close();
                inputStream = null;
                outputStream.flush();
                outputStream.close();
                outputStream = null;

                conn.disconnect();
                conn = null;
            } catch (Exception e) {
                LogManager.local(TAG, "recvObject fails " + e.getMessage());
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                    inputStream = null;
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
            try {
                if (outputStream != null) {
                    outputStream.close();
                    outputStream = null;
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
            if (conn != null) {
                conn.disconnect();
                conn = null;
            }
            return recvedFile;

        }

        // example code from :
        // http://blog.csdn.net/qq247890212/article/details/16358581
        // about multi-part/data, see
        // http://blog.csdn.net/five3/article/details/7181521
        public String uploadObject(byte[] data, final String desc,
                String requestUrl, Bundle Info) {
            String BOUNDARY = PREFIX + UUID.randomUUID().toString();
            HttpURLConnection conn = createUrlConnection(requestUrl, BOUNDARY);
            if (conn == null)
                return null;

            OutputStream outputStream = null;
            try {
                outputStream = conn.getOutputStream();
            } catch (Exception e) {
                LogManager
                        .local(TAG, "getOutputStream fails " + e.getMessage());
                conn.disconnect();
                return null;
            }
            DataOutputStream dos = new DataOutputStream(outputStream);

            String fileName = desc;

            int res = 0;
            try {
                // the http form data must be written one by one
                writeHttpFormInfo(dos, fileName, BOUNDARY, Info);
                dos.write(data, 0, data.length);
                dos.write(getHttpTailInfo(BOUNDARY).getBytes());
                dos.flush();
                dos.close();
                dos = null;
                res = conn.getResponseCode();
            } catch (Exception e) {
                // TODO: handle exception
                LogManager.local(TAG, "uploadObject 1 fails " + e.getMessage());
            }
            if (dos != null) {
                try {
                    dos.close();
                } catch (Exception e) {
                    // TODO: handle exception
                }

            }
            conn.disconnect();
            if (res == 200)
                return fileName;
            else
                return null;
        }

        public void writeHttpFormInfo(DataOutputStream dos, String filename,
                String Boundary, Bundle info) {

            if (dos == null || filename == null || Boundary == null
                    || info == null)
                return;

            try {
                StringBuilder sb = new StringBuilder();
                // write user name
                sb.append("--" + Boundary + LINE_END);
                sb.append("Content-Disposition: form-data; name=\"username\"");
                sb.append(LINE_END + LINE_END);
                sb.append(mConnectionInfo.getString("username"));
                dos.write(sb.toString().getBytes(CHARSET));
                dos.write(LINE_END.getBytes(CHARSET));

                // write password
                sb.setLength(0);
                sb.append("--" + Boundary + LINE_END);
                sb.append("Content-Disposition: form-data; name=\"password\"");
                sb.append(LINE_END + LINE_END);
                sb.append(mConnectionInfo.getString("password"));
                dos.write(sb.toString().getBytes(CHARSET));
                dos.write(LINE_END.getBytes(CHARSET));

                // write resource
                sb.setLength(0);
                sb.append("--" + Boundary + LINE_END);
                sb.append("Content-Disposition: form-data; name=\"resource\"");
                sb.append(LINE_END + LINE_END);
                sb.append(mConnectionInfo.getString("resource"));
                dos.write(sb.toString().getBytes(CHARSET));
                dos.write(LINE_END.getBytes(CHARSET));

                // write command id
                sb.setLength(0);
                sb.append("--" + Boundary + LINE_END);
                sb.append("Content-Disposition: form-data; name=\"commandid\"");
                sb.append(LINE_END + LINE_END);
                sb.append(info.getString("commandid"));
                dos.write(sb.toString().getBytes(CHARSET));
                dos.write(LINE_END.getBytes(CHARSET));

                // write type
                sb.setLength(0);
                sb.append("--" + Boundary + LINE_END);
                sb.append("Content-Disposition: form-data; name=\"type\"");
                sb.append(LINE_END + LINE_END);
                sb.append(info.getString("type"));
                dos.write(sb.toString().getBytes(CHARSET));
                dos.write(LINE_END.getBytes(CHARSET));

                // write action
                sb.setLength(0);
                sb.append("--" + Boundary + LINE_END);
                sb.append("Content-Disposition: form-data; name=\"action\"");
                sb.append(LINE_END + LINE_END);
                sb.append(info.getString("action"));
                dos.write(sb.toString().getBytes(CHARSET));
                dos.write(LINE_END.getBytes(CHARSET));

                // write upload file info
                sb.setLength(0);
                sb.append("--" + Boundary + LINE_END);
                sb.append("Content-Disposition: form-data; name=\"upload\"; filename=\""
                        + filename + "\"" + LINE_END);
                sb.append("Content-Type: " + info.getString("mime"));
                sb.append(LINE_END + LINE_END);
                dos.write(sb.toString().getBytes(CHARSET));
            } catch (Exception e) {
                LogManager.local(TAG,
                        "writeHttpFormInfo fails " + e.getMessage());
            }

        }
    }
}
/*
 * private void addFileReceiver(Connection conn){ mFTManager = new
 * FileTransferManager(conn); mFTManager.addFileTransferListener(new
 * XmppFileTransferListener()); }
 */

/*
 * private class XmppFileTransferListener implements FileTransferListener {
 * 
 * @Override public void fileTransferRequest(FileTransferRequest request) {
 * IncomingFileTransfer transfer = request.accept(); try { LogManager.local(TAG,
 * "Begin receive file:"+request.getRequestor()); String fileName =
 * request.getFileName(); String Mime = request.getMimeType();
 * 
 * File temp = new File(mContext.getFilesDir().getAbsolutePath() +
 * "/"+fileName); transfer.recieveFile(temp);
 * 
 * Thread saveThread = new Thread(new SaveRunnable(temp, Mime));
 * 
 * saveThread.start(); } catch (Exception e) { e.printStackTrace(); } }
 * 
 * private boolean isImage(String Mime){ return Mime.equals("image/jpeg")||Mime
 * .equals("image/png")||Mime.equals("image/bmp"); }
 * 
 * private class SaveRunnable implements Runnable{ private File mTempfile =
 * null; private long mFileSize = 0; private String mMime = null;
 * 
 * public SaveRunnable(File file, String Mime) { super(); mTempfile = file;
 * mMime = Mime; mFileSize = file.length(); }
 * 
 * @Override public void run() { do { //wait for download complete try {
 * mFileSize = mTempfile.length(); LogManager.local(TAG, "receiving file S:" +
 * mFileSize); Thread.sleep(500); LogManager.local(TAG, "receiving file E:" +
 * mTempfile.length()); } catch (Exception e) { e.printStackTrace(); } } while
 * (mTempfile.length() != mFileSize);
 * 
 * if (isImage(mMime)) { // remove this //
 * ProminentFeature.saveFileAsImage(mContext, mTempfile); } } } }
 */
