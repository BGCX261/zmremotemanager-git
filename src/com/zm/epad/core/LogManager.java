package com.zm.epad.core;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class LogManager implements XmppClient.XmppClientCallback {

    static public LogManager mLogManager;



    private XmppClient mXmppClient = null;
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

    private Context mContext = null;
    private static boolean mRunning = false;

    public LogManager(Context context, XmppClient xmppClient) {
        mContext = context;
        mLocalLogFile = mContext.getDir("local.log", Context.MODE_PRIVATE);
        mXmppClient = xmppClient;
    }



    @Override
    public Object reportXMPPClientEvent(int xmppClientEvent, Object... args) {
        return null;
    }



    public boolean start() {
        mbConnected = true;
        transferLeftOverLogs();// left over log must be sent synchronously
        mLogThread = new LogThread();
        mLogThread.start();

        mRunning = true;

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

        mRunning = false;

        mLogThread.quit();
        mLogThread = null;
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
            // mXmppConnection.sendPacket(logMsg);
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
        if (!mRunning)
            return;

        mLogManager.mLogThread.addLog(getComposedLogText("V/" + tag, msg));
    }

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
        if (!mRunning)
            return;

        mLogManager.mLogThread.addLog(getComposedLogText("D/" + tag, msg));
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
        if (!mRunning)
            return;

        mLogManager.mLogThread.addLog(getComposedLogText("I/" + tag, msg));
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
        if (!mRunning)
            return;

        mLogManager.mLogThread.addLog(getComposedLogText("W/" + tag, msg));
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
        if (!mRunning)
            return;

        mLogManager.mLogThread.addLog(getComposedLogText("E/" + tag, msg));
    }
}
