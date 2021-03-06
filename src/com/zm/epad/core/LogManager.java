package com.zm.epad.core;

import com.zm.epad.plugins.RemoteFileManager;
import com.zm.epad.plugins.RemoteFileManager.FileTransferTask;

import java.io.File;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.content.Context;
import android.os.Bundle;

public class LogManager {
    private Context mContext;
    private String mRootDir = null;

    private RandomAccessFile[] mDefaultLogFiles = null;
    private String[] mDefaultLogDirs = null;
    private static final String TAG = "LogManager";
    private final String LINE_END = "\r\n";
    private final String CHARSET = "utf-8";
    private final TimeZone mTimeZone = SubSystemFacade.getInstance()
            .getDefaultTimeZone();

    private static LogManager gLogManager = null;

    public interface LogFileTransferInterface {
        public String[] getLogFiles();

        public boolean uploadLogFiles(String filePath);

        public boolean uploadAllLogFiles();
    }

    static public LogManager createLogManager(Context context) {
        if (gLogManager != null)
            return gLogManager;
        gLogManager = new LogManager(context);
        return gLogManager;
    }

    static public LogManager getInstance() {
        return gLogManager;
    }

    public static String local(String tag, String msg) {
        String tagWrapper = CoreConstants.CONSTANT_LOGTAG_HEADER + tag;
        String content = getCurrentTimeAsFormat() + " : " +msg;
        android.util.Log.e(tagWrapper, content);
        tagWrapper = "<" + tagWrapper + ">  ";
        return tagWrapper;
    }

    public static void server(String tag, String msg) {
        String tagWrapper = local(tag, msg);
        // right now, do nothing here.
        String content = tagWrapper + getCurrentTimeAsFormat() + " : " + msg;
        if (gLogManager != null) {
            gLogManager.addLog(CoreConstants.CONSTANT_INT_LOGTYPE_RUNTIME,
                    content);
        }
    }

    private LogManager(Context context) {
        mContext = context;
        final int count = CoreConstants.CONSTANT_INT_LOGTYPE_ARRAYS.length;
        mDefaultLogFiles = new RandomAccessFile[count];
        mDefaultLogDirs = new String[count];

    }

    public void registerDefaultLogType() {
        String today = getTodayDateString(null);
        mRootDir = mContext.getFilesDir().getAbsolutePath();
        for (int type : CoreConstants.CONSTANT_INT_LOGTYPE_ARRAYS) {
            String logType = CoreConstants.CONSTANT_LOGTYPE_ARRAYS[type];
            String logTypeDir = mRootDir + "/logs/" + logType;
            mDefaultLogDirs[type] = logTypeDir;
            ensureLogDirExists(logTypeDir, today, type);
        }
    }

    public void start() {
        registerDefaultLogType();
    }

    private void ensureLogDirExists(String logTypeDir, String today, int index) {
        File logFileDir = new File(logTypeDir);
        logFileDir.mkdirs();
        logFileDir = null;
        // log file name is: logTypeDir/<deviceId>.<date>.log
        String logToday = logTypeDir + "/" + getLogFileName(today);
        File logFile = new File(logToday);
        if (logFile.exists() == false) {
            try {
                logFile.createNewFile();
            } catch (Exception e) {
                local(TAG, " ensureLogDirExists failed:" + e.getMessage());
            }
        }
        closeLogFile(index);
        try {
            mDefaultLogFiles[index] = new RandomAccessFile(logFile, "rw");
            mDefaultLogFiles[index].seek(logFile.length());
            mDefaultLogDirs[index] = logTypeDir;
        } catch (Exception e) {
            LogManager.local(TAG, " openFile failed:" + e.getMessage());
        }

    }

    // if type = -1, then close all log files
    public void stop() {
        closeLogFile(-1);
    }

    public void closeLogFile(int type) {
        if (type == -1) {
            for (int index : CoreConstants.CONSTANT_INT_LOGTYPE_ARRAYS) {
                closeLogFile(index);
            }
            return;
        }

        if (mDefaultLogFiles[type] != null) {
            try {
                mDefaultLogFiles[type].close();
            } catch (Exception e) {
                // TODO: handle exception
            }
            mDefaultLogFiles[type] = null;
            mDefaultLogDirs[type] = null;
        }
        return;
    }

    private String getTodayDateString(Calendar day) {
        Calendar today = null;
        if (day == null)
            today = Calendar.getInstance(mTimeZone);
        else
            today = day;
        StringBuffer sb = new StringBuffer();
        sb.append(today.get(Calendar.YEAR));
        sb.append("-");
        sb.append(today.get(Calendar.MONTH) + 1); // JANUARY = 0, ...
        sb.append("-");
        sb.append(today.get(Calendar.DATE));
        return sb.toString();
    }

    // synchronized to avoid call from multi-thread
    public void addLog(int type, String logLine) {
        synchronized (mDefaultLogFiles) {
            if (checkIfCreateLogFile(type)) {
                // when turn to another day or other reason which cause no right
                // log file exists, create a new log file
                ensureLogDirExists(mDefaultLogDirs[type],
                        getTodayDateString(null), type);
            }
            try {
                RandomAccessFile logFile = mDefaultLogFiles[type];
                logFile.write(logLine.getBytes(CHARSET));
                logFile.writeBytes(LINE_END);
            } catch (Exception e) {
                local(TAG, "addLog failed:" + e.getMessage());
                local(TAG, "addLog failed:" + logLine);
            }
        }
    }

    private class LogFileUploadCallback implements
            RemoteFileManager.FileTransferCallback {

        public String mTargetFile = null;
        private int mType = -1;
        private boolean mUpdate = false;

        @Override
        public void onDone(boolean success, FileTransferTask task) {
            // TODO Auto-generated method stub
            LogManager.local(TAG, "upload logfile " + mTargetFile
                    + "; success:" + success);
            cleanup();
            if (success && mUpdate) {
                update();
            }
        }

        @Override
        public void onCancel(FileTransferTask task) {
            LogManager.local(TAG, "upload logfile " + mTargetFile + "canceled");
            cleanup();

        }

        private void cleanup() {

            if (mType == -1) {
                try {
                    File gzFile = new File(mTargetFile);
                    if (gzFile.exists())
                        gzFile.delete();
                } catch (Exception e) {

                }
            }

        }

        private void update() {
            if (mType != -1) {
                try {
                    // delete the log file
                    File logFile = new File(mTargetFile);
                    if (logFile.exists())
                        logFile.delete();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        public void prepareUploadFiles(int type, Calendar date, boolean update) {
            String today = getTodayDateString(date);
            mType = type;
            mUpdate = update;

            if (type == -1) {
                mTargetFile = mRootDir + "/" + today + ".all.zip";
                return;
            } else {
                String rootDir = mDefaultLogDirs[type];
                mTargetFile = rootDir + "/" + getLogFileName(today);
            }
            return;

        }

        public void prepareUploadFiles(int type, String fileName, boolean update) {
            mType = type;
            mUpdate = update;

            if (type == -1) {
                mTargetFile = mRootDir + "/" + getTodayDateString(null)
                        + ".all.zip";
                return;
            } else {
                String rootDir = mDefaultLogDirs[type];
                mTargetFile = rootDir + "/" + fileName;
            }
            return;
        }
    }

    // if type == -1, it means upload all log files.
    // we will first compress these logs and them upload
    // if type != -1, then we will upload the specified log file according to
    // date
    public void uploadLog(String url, int type, Calendar date) {
        uploadLog(url, type, date, null, false);
    }

    public void uploadLog(String url, int type, String filename) {
        uploadLog(url, type, null, filename, true);
    }

    public void uploadLog(String url, int type, Calendar date, String filename,
            boolean update) {
        LogFileUploadCallback logFileUploadCB = new LogFileUploadCallback();

        if (update && isTodayLog(date, filename)) {
            // if upload today's log, update log file to avoid name duplication
            File backup = updateLogFile(type);
            logFileUploadCB.prepareUploadFiles(type, backup.getName(), update);
        } else {
            if (date != null) {
                logFileUploadCB.prepareUploadFiles(type, date, update);
            } else {
                logFileUploadCB.prepareUploadFiles(type, filename, update);
            }
        }

        SubSystemFacade subSystemFacade = SubSystemFacade.getInstance();
        if (type != -1) {
            Bundle bundle = new Bundle();
            Config config = Config.getInstance();
            bundle.putString("deviceid", Config.getDeviceId());
            bundle.putString("password", config.getConfig(Config.PASSWORD));
            String typeString;
            switch (type) {
            case CoreConstants.CONSTANT_INT_LOGTYPE_COMMON:
                typeString = "log";
                break;
            default:
                typeString = "debug";
                break;
            }
            bundle.putString("type", typeString);
            subSystemFacade.uploadFile(url, logFileUploadCB.mTargetFile, null,
                    bundle, logFileUploadCB);
        } else {
            String logDir = mRootDir + "/logs/";
            subSystemFacade.zipAndUploadFile(url, logDir,
                    logFileUploadCB.mTargetFile, null, logFileUploadCB);

        }
    }

    public String[] listLogFiles(int type) {
        if (mDefaultLogDirs[type] == null)
            return null;
        File logDir = new File(mDefaultLogDirs[type]);
        File[] files = logDir.listFiles();
        if (files.length == 0)
            return null;
        String[] fileName = new String[files.length];
        int index = 0;
        for (File file : files) {
            fileName[index++] = file.getName();
        }
        return fileName;
    }

    private String getLogFileName(String date) {
        // defined with server team
        // log file name is: logTypeDir/<deviceId>.<date>.log
        return Config.getDeviceId() + "." + date + ".log";
    }

    private boolean checkIfCreateLogFile(int type) {
        if (type < 0) {
            return false;
        }
        File logFile = new File(mDefaultLogDirs[type],
                getLogFileName(getTodayDateString(null)));
        return !logFile.exists();
    }

    private boolean isTodayLog(Calendar date, String filename) {
        String today = getTodayDateString(null);
        return date != null ? today.equals(getTodayDateString(date))
                : getLogFileName(today).equals(filename);
    }

    private File updateLogFile(int type) {
        synchronized (mDefaultLogFiles) {
            try {
                File backup = null;
                File logfile = new File(mDefaultLogDirs[type],
                        getLogFileName(getTodayDateString(null)));
                if (logfile.exists()) {
                    if (mDefaultLogFiles[type] != null) {
                        mDefaultLogFiles[type].close();
                        mDefaultLogFiles[type] = null;
                    }
                    // back up file name is:
                    // <deviceId>.<date>.<Current UTC>.type.log
                    String typeString;
                    switch (type) {
                    case CoreConstants.CONSTANT_INT_LOGTYPE_COMMON:
                        typeString = "log";
                        break;
                    default:
                        typeString = "debug";
                        break;
                    }
                    String filename = Config.getDeviceId() + "."
                            + getTodayDateString(null) + "_"
                            + System.currentTimeMillis() + "." + typeString
                            + ".log";
                    backup = new File(mDefaultLogDirs[type], filename);
                    logfile.renameTo(backup);
                    // create new log file
                    ensureLogDirExists(mDefaultLogDirs[type],
                            getTodayDateString(null), type);
                }
                return backup;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static String getCurrentTimeAsFormat() {
        long time = System.currentTimeMillis();
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ").format(new Date(time));
    }
}