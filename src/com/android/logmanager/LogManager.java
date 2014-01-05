package com.android.logmanager;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.android.remotemanager.NetworkStatusMonitor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class LogManager implements NetworkStatusMonitor.NetworkStatusReport {

    static public LogManager mLogManager;

    static public LogManager getLogManagerInstance(String serverName,
            Context context) {
        if (mLogManager == null)
            mLogManager = new LogManager(serverName, context);
        return mLogManager;
    }

    @Override
    public void reportNetworkStatus(boolean bConnected) {

    }
    @Override
    public void reportXMPPConnectionStatus(int type, boolean bConnected) {

    }

    private XMPPConnection mXmppConnection = null;
    private String mServerName;
    private static String TAG = "LogManager";
    private static String LOG_RES = "RemoteLog";
    private File mLocalLogFile;
    private BufferedWriter mLocalLogFileWrite;
    private String LOG_DESTINATION = "zm-remote-logserver";
    public LogThread mLogThread = null;
    static private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mmZ");
    static private Date mLogDate = new Date();

    volatile private Boolean mbConnected = false;
    private XmppLogConnectionListener mLogConnectionListener = null;
    private Context mContext = null;

    private LogManager(String serverName, Context context) {
        mServerName = serverName;
        mContext = context;
        mLocalLogFile = mContext.getDir("local.log", Context.MODE_PRIVATE);
    }

    class XmppLogConnectionListener implements ConnectionListener {

        @Override
        public void connectionClosed() {
            // TODO Auto-generated method stub
            mbConnected = false;
            Log.e(TAG, "connection closed");

        }

        @Override
        public void connectionClosedOnError(Exception e) {
            // TODO Auto-generated method stub
            Log.e(TAG, "connection closed on error");
            mbConnected = false;

        }

        @Override
        public void reconnectingIn(int seconds) {
            // TODO Auto-generated method stub
            Log.e(TAG, "reconnectingIn " + seconds + " seconds");

        }

        @Override
        public void reconnectionSuccessful() {
            // TODO Auto-generated method stub
            Log.e(TAG, "reconnectionSuccessful");
            mbConnected = true;
        }

        @Override
        public void reconnectionFailed(Exception e) {
            // TODO Auto-generated method stub
            Log.e(TAG, "reconnectionFailed " + e.getMessage());
        }

    }

    public boolean start() {
        mXmppConnection = new XMPPConnection(mServerName);
        mLogConnectionListener = new XmppLogConnectionListener();
        mXmppConnection.addConnectionListener(mLogConnectionListener);

        try {
            mXmppConnection.connect();
        } catch (Exception e) {
            mXmppConnection = null;
            Log.e(TAG, "error in start " + e.getMessage());
            return false;
        }

        try {
            mXmppConnection.login("username", "password", LOG_RES);
        } catch (Exception e) {
            Log.e(TAG, "error in login " + e.getMessage());
            mXmppConnection = null;
            return false;
        }
        mbConnected = true;
        transferLeftOverLogs();// left over log must be sent synchronously
        mLogThread = new LogThread();
        mLogThread.start();
        return true;

    }

    private class LogThread extends Thread {
        LinkedList<String> mlogsInQueue = new LinkedList<String>();
        volatile private int mMaxiumLogsInQueu = 10000;
        volatile private boolean mQuitTask;

        public int setInQueueLogsCount(int newValue) {
            int ret = mMaxiumLogsInQueu;
            mMaxiumLogsInQueu = newValue;
            return ret;
        }

        @Override
        public void run() {
            synchronized (mlogsInQueue) {
                while (mQuitTask == false) {
                    try {
                        String logTxt = mlogsInQueue.poll();
                        if (logTxt == null) {
                            mlogsInQueue.wait();
                            continue;
                        }
                        if (mbConnected && sendLogLine(logTxt))
                            continue;
                        else {
                            writeLogLine(logTxt);
                        }
                    } catch (Exception e) {
                        mQuitTask = true;
                    }

                }
            }

        }

        @Override
        public synchronized void start() {
            mQuitTask = false;
            mlogsInQueue.clear();
            super.start();
        }

        public void quit() {
            synchronized (mlogsInQueue) {
                try {
                    mQuitTask = true;
                    mlogsInQueue.notifyAll();
                    wait();
                    Log.e(TAG, "LogThread quit");
                    mlogsInQueue.clear();
                } catch (Exception e) {
                    // TODO: handle exception
                }

            }
        }

        public void addLog(String logTxt) {
            synchronized (mlogsInQueue) {
                int currentIndex = mlogsInQueue.size();
                if (currentIndex >= mMaxiumLogsInQueu) {
                    mlogsInQueue.poll();
                }
                mlogsInQueue.offer(logTxt);
                mlogsInQueue.notifyAll();
            }
            return;

        }

    }

    public void stop() {
        mLogThread.quit();
        mLogThread = null;
        mXmppConnection.disconnect();
        mXmppConnection = null;
        try {
            if (mLocalLogFileWrite != null) {
                mLocalLogFileWrite.flush();
                mLocalLogFileWrite.close();

            }

        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            mLocalLogFileWrite = null;
            mLocalLogFile = null;
        }

    }

    private void transferLeftOverLogs() {
        try {
            long leftSize = mLocalLogFile.length();
            if (mLocalLogFile.length() != 0) {
                Log.e(TAG, "transfer leftover logs to server, size :"
                        + leftSize);
                BufferedReader reader = new BufferedReader(new FileReader(
                        mLocalLogFile));
                boolean bInterrupted = false;
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    if (sendLogLine(line) == false) {
                        bInterrupted = true;
                        break;
                    }

                }
                reader.close();
                if (bInterrupted) {
                    Log.e(TAG,
                            "transfer leftover logs to server is interrupted");
                } else {
                    mLocalLogFile.delete();// delete the old file as leftover
                                           // logs have been sent to server
                    mLocalLogFile = mContext.getDir("local.log",
                            Context.MODE_PRIVATE);
                    mLocalLogFileWrite = new BufferedWriter(new FileWriter(
                            mLocalLogFile));
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }

        return;
    }

    private boolean writeLogLine(String logTxt) {
        if (mLocalLogFile == null || mLocalLogFileWrite == null)
            return false;
        try {
            mLocalLogFileWrite.write(logTxt);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "writeLogLine fails: " + e.getMessage());
            return false;
        }
    }

    private boolean sendLogLine(String logTxt) {
        if (mbConnected == false) {
            return false;
        }
        try {

            Message logMsg = new Message();
            logMsg.setTo(LOG_DESTINATION);
            logMsg.setBody(logTxt);
            mXmppConnection.sendPacket(logMsg);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "sendLogLine fails: " + e.getMessage());
            mbConnected = false;
            return false;
        }
    }

    public static String getComposedLogText(String tag, String msg) {
        mLogDate.setTime(System.currentTimeMillis());
        String timeInfo = mSimpleDateFormat.format(mLogDate);
        return new String(tag + " :" + timeInfo + " " + msg);
    }

    public static void v(String tag, String msg) {
        Log.v(tag, msg);
        mLogManager.mLogThread.addLog(getComposedLogText("V/" + tag, msg));
    }

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
        mLogManager.mLogThread.addLog(getComposedLogText("D/" + tag, msg));
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
        mLogManager.mLogThread.addLog(getComposedLogText("I/" + tag, msg));
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
        mLogManager.mLogThread.addLog(getComposedLogText("W/" + tag, msg));
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
        mLogManager.mLogThread.addLog(getComposedLogText("E/" + tag, msg));
    }
}
