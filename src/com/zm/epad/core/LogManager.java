package com.zm.epad.core;

import android.R.string;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;


import java.util.concurrent.locks.ReentrantLock;

public class LogManager implements XmppClient.XmppClientCallback {
    static private String TAG = "LogManager";

    static public LogManager mLogManager;

    static private String LOGDATABASE_TABLE_NAME = "logdata";
    static private String LOGDATABASE_COLUMN_LOGMSG = "logmsg";

    static private String LOGDATABASE_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS logdata(id INTEGER PRIMARY KEY AutoIncrement,"
            + "logmsg TEXT not null, logtime  TIMESTAMP default (datetime('now', 'localtime')));";

    static private String LOGDATABASE_QUERY_LOG = "SELECT * FROM logdata";

    static private String LOGDATABASE_DELETE_LOG = "DELETE FROM logdata WHERE (id <= ?)";

    static private String LOGDATABASE_NAME = "logdatabase";
    static private int LOGDATABASE_VERSION = 1;

    ReentrantLock mLock = null;
    private boolean mbLogined = false; // if xmppclient is logined.

    private static int CMD_UPDATE_LOGGIN_STATUS = 1;
    private static int CMD_ADD_LOGS = 2;

    private LogDatabase mLogDatabase = null;
    private SQLiteDatabase mSqLiteDatabase = null;

    private XmppClient mXmppClient = null;
    private Context mContext = null;

    private HandlerThread mLogWorkingThread = null;
    private LogWorkingHandler mLogWorkingHandler = null;

    public LogManager(Context context, XmppClient xmppClient) {
        mContext = context;
        mXmppClient = xmppClient;
        mLogDatabase = new LogDatabase(context, LOGDATABASE_NAME,
                LOGDATABASE_VERSION);
        mLock = new ReentrantLock();
        if (mLogManager == null)
            mLogManager = this;
    }

    static public LogManager getLogManager() {
        return mLogManager;
    }
    private class LogDatabase extends SQLiteOpenHelper {
        public LogDatabase(Context context, String name, int version) {
            super(context, name, null, 1);
            local(TAG, "constructor logdatabase with name = " + name
                    + " version = " + version);

        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            local(TAG, "create logdatabase");
            try {
                db.execSQL(LOGDATABASE_CREATE_TABLE);
            } catch (Exception e) {
                local(TAG, "create database fails:" + e.getMessage());
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO Auto-generated method stub

        }

    }

    @Override
    public Object reportXMPPClientEvent(int xmppClientEvent, Object... args) {
        mLock.lock();

        boolean bUpdateLoginStatus = false;
        if (xmppClientEvent == XmppClient.XMPPCLIENT_EVENT_LOGIN) {
            boolean bLogined = ((Boolean) args[0]).booleanValue();
            if (mbLogined != bLogined) {
                bUpdateLoginStatus = true;
                mbLogined = bLogined;
            }

        } else if (xmppClientEvent == XmppClient.XMPPCLIENT_EVENT_LOGOUT) {
            if (mbLogined == true) {
                bUpdateLoginStatus = true;
                mbLogined = false;
            }
        } else if (xmppClientEvent == XmppClient.XMPPCLIENT_EVENT_CONNECTION_UPDATE_STATUS) {
            boolean bNetworkConnected = ((Integer)args[0]).equals(0)?false:true;
            if (bNetworkConnected == false && mbLogined != bNetworkConnected) {
                bUpdateLoginStatus = true;
                mbLogined = false;
            }
        }
        if (bUpdateLoginStatus && mLogWorkingHandler != null) {
            mLogWorkingHandler.sendEmptyMessage(CMD_UPDATE_LOGGIN_STATUS);
        }
        mLock.unlock();
        return null;
    }

    public boolean start() {
        try {
            mSqLiteDatabase = mLogDatabase.getWritableDatabase();
        } catch (Exception e) {
            local(TAG, "getWritableDatabase fails with " + e.getMessage());
            mSqLiteDatabase = null;
        }
        mXmppClient.addXmppClientCallback(this);
        mLogWorkingThread = new HandlerThread("logworkingthread");
        mLogWorkingThread.start();
        mLogWorkingHandler = new LogWorkingHandler(
                mLogWorkingThread.getLooper());
        return true;

    }

    private class LogWorkingHandler extends Handler {
        public LogWorkingHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            int what = msg.what;
            if (what == CMD_UPDATE_LOGGIN_STATUS) {
                if (mSqLiteDatabase == null) {
                    local(TAG, "logdatabase not opened");
                    return;
                }
                while (true) {

                    if (mbLogined == false) {
                        local(TAG, "xmppclient is offline");
                        break;
                    }

                    Cursor logCursor = null;
                    try {
                        logCursor = mSqLiteDatabase.rawQuery(
                                LOGDATABASE_QUERY_LOG, null);
                        if (logCursor != null) {
                            //local(TAG, "we have " + logCursor.getCount()
                            //        + " offline logs");
                            logCursor.moveToFirst();
                            int sentId = -1;
                            while (logCursor.isAfterLast()) {
                                sentId = logCursor.getInt(0);
                                String logTxt = logCursor.getString(1);
                                String logTime = logCursor.getString(2);
                                local(TAG, "<offline-log> " + logTxt + " "
                                        + logTime);
                                logCursor.moveToNext();
                            }
                            // we have sent offline-log which id is less than
                            // sentId, so we can delete them
                            int count = mSqLiteDatabase.delete(
                                    LOGDATABASE_TABLE_NAME, "(id <= ?)",
                                    new String[] { "" + sentId });
                            //local(TAG, "" + count + " logs are deleted");
                        } else {
                            local(TAG, "query with null cursor ");
                            mbLogined = false;
                        }
                    } catch (Exception e) {
                        local(TAG, "query offline log fails with " + e.getMessage());
                        mbLogined = false;
                        if (logCursor != null) {
                            logCursor.close();
                        }
                    }

                }
            }else if(what == CMD_ADD_LOGS){
                mLock.lock();
                String logmsg = (String)msg.obj;
                if(mbLogined == true){
                    mXmppClient.sendLogPacket(logmsg);
                } else {
                    if (mSqLiteDatabase != null) {
                        try {
                            ContentValues cv = new ContentValues();
                            cv.put(LOGDATABASE_COLUMN_LOGMSG, logmsg);
                            mSqLiteDatabase.insert(LOGDATABASE_TABLE_NAME,
                                    null, cv);
                        } catch (Exception e) {
                            local(TAG,
                                    "offline log fail to insert into database "
                                            + e.getMessage());
                            local(TAG, "\t original msg is " + logmsg);
                        }
                    } else {
                        local(TAG,
                                "offline log can not be inserted into database. msg is "
                                        + logmsg);
                    }

                }
                mLock.unlock();
            }
        }

    }

    public void stop() {
        mLock.lock();
        mXmppClient.removeXmppClientCallback(this);
        try {
            mbLogined = false;
            mLogWorkingThread.quit();
        } catch (Exception e) {
            local(TAG, "stop logworkingthread : " + e.getMessage());
        }
        mLogWorkingHandler = null;
        try {
            if (mSqLiteDatabase != null) {
                mSqLiteDatabase.close();
                mSqLiteDatabase = null;
            }
            if (mLogDatabase != null)
                mLogDatabase.close();
        } catch (Exception e) {
            local(TAG,
                    "stop logworkingthread close database fail : "
                            + e.getMessage());
        }
        mLock.unlock();
        try {
            mLogWorkingThread.join();
        } catch (Exception e) {
            // TODO: handle exception
        }

    }

    public void addServerLog(String tag, String txt) {
    	
    	//keep safe when LogManager stop unexpectedly
    	if(mLogWorkingHandler != null)
    	{
            Message msg =  mLogWorkingHandler.obtainMessage();
            msg.what = CMD_ADD_LOGS;
            msg.obj = tag + ": " + txt;

            mLogWorkingHandler.sendMessage(msg);    		
    	}
    }

    public static String local(String tag, String msg) {
        String tagWrapper = "com.zm.epad:" + tag;
        android.util.Log.e(tagWrapper, msg);
        return tagWrapper;
    }

    public static void server(String tag, String msg) {
        String tagWrapper = local(tag, msg);
        mLogManager.addServerLog(tagWrapper, msg);
    }

}
