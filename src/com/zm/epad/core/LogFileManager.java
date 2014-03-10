package com.zm.epad.core;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Calendar;

import android.content.Context;
import android.provider.MediaStore.Files;

public class LogFilesManager {
    private Context mContext;
    private String mRootDir = null;
    private String mLogToday = null;
    private RandomAccessFile mLogFileToday = null;


    public LogFilesManager(Context context,String logType) {
        mContext = context;
        String today = getTodayDateString();
        mRootDir = mContext.getFilesDir().getAbsolutePath();
        mRootDir = getLogPathByType(logType);
        mLogToday = mRootDir + "/" + today;
        ensureLogDirExists();

    }

    private void ensureLogDirExists() {
        File logFileDir = new File(mRootDir);
        logFileDir.mkdirs();
        logFileDir = null;

        File logFile = new File(mLogToday);
        if(logFile.exists() == false){
            logFile.createNewFile();
        }
        mLogFileToday = new RandomAccessFile(logFile, "rw");
        mLogFileToday.seek(logFile.length());
    }

    public void closeLogFiles(){
      if(mLogFileToday != null){
          try {
              mLogFileToday.close();
        } catch (Exception e) {
            // TODO: handle exception
        }
      }
      mLogToday = null;
    }

    private String getTodayDateString() {
        Calendar today = Calendar.getInstance();
        StringBuffer sb = new StringBuffer();
        sb.append(today.get(Calendar.YEAR) - 1900);
        sb.append("-");
        sb.append(today.get(Calendar.MONTH));
        sb.append("-");
        sb.append(today.get(Calendar.DAY));
        sb.append("-");
        return sb.toString();
    }

    private String getLogPathByType(String logType) {
        if (logType.equals(CoreConstants.CONSTANT_LOGTYPE_LOCATION))
            return mRootDir + "/" + logType ;
        if (logType.equals(CoreConstants.CONSTANT_LOGTYPE_APPRUNTIME))
            return mRootDir + "/" + logType;
        if (logType.equals(CoreConstants.CONSTANT_LOGTYPE_RUNTIME))
            return mRootDir + "/" + logType ;
        return null;
    }
    
    public void addLog(String logLine){
        if(logLine.endsWith("\n") == false)
            logLine += "\n";
        if(mLogFileToday != null){
            try {
                mLogFileToday.writeChars(logLine);
            } catch (Exception e) {
                LogManager.local(TAG, "addLog failed:" + e.getMessage());
                LogManager.local(TAG, "addLog failed:" + logLine);
            }
        }
    }
    public String[] listLogFiles(){
        File logDir = new File(mRootDir);
        Files[] files = logDir.listFiles();
        if(files.length == 0)
            return null;
        String[] fileName = new String[files.length];
        int index = 0;
        for(File file:files){
            fileName[index++] = file.getName();
        }
    }
    

}