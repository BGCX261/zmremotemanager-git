package com.zm.epad.core;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Calendar;

import android.content.Context;
import android.renderscript.Type;
import android.webkit.WebView.FindListener;

public class LogManager {
    private Context mContext;
    private String mRootDir = null;

    private RandomAccessFile[] mDefaultLogFiles = null;
    private String[] mDefaultLogDirs = null;
    private static final String TAG = "LogManager";
    
    private static LogManager gLogManager = null;
    
    public interface LogFileTransferInterface{
        public String[] getLogFiles();
        public boolean uploadLogFiles(String filePath);
        public boolean uploadAllLogFiles();
    }
    static public LogManager createLogManager(Context context){
        if(gLogManager != null)
            return gLogManager;
        gLogManager = new LogManager(context);
        return gLogManager;
    }
    static public LogManager getInstance() {
        return gLogManager;
    }
    public static String local(String tag, String msg) {
        String tagWrapper = CoreConstants.CONSTANT_LOGTAG_HEADER + tag;
        android.util.Log.e(tagWrapper, msg);
        tagWrapper = "<" + tagWrapper + ">  ";
        return tagWrapper;
    }

    public static void server(String tag, String msg) {
        String tagWrapper = local(tag, msg);
        //right now, do nothing here.
        gLogManager.addLog(CoreConstants.CONSTANT_INT_LOGTYPE_RUNTIME, 
                tagWrapper + msg);
    }
    
    
    
    private LogManager(Context context) {
        mContext = context;
        final int count = CoreConstants.CONSTANT_INT_LOGTYPE_ARRAYS.length;
        mDefaultLogFiles = new RandomAccessFile[count];
        mDefaultLogDirs = new String[count];
        
    }
    public void registerDefaultLogType(){
        String today = getTodayDateString();
        mRootDir = mContext.getFilesDir().getAbsolutePath();
        for(int type :CoreConstants.CONSTANT_INT_LOGTYPE_ARRAYS){
            String logType = CoreConstants.CONSTANT_LOGTYPE_ARRAYS[type];
            String logTypeDir = mRootDir + "/" + logType ;
            mDefaultLogDirs[type] = logTypeDir;
            ensureLogDirExists(logTypeDir,today,type);
        }
    }
    public void start(){
        registerDefaultLogType();
    }
    private void ensureLogDirExists(String logTypeDir,String today,int index) {
        File logFileDir = new File(logTypeDir);
        logFileDir.mkdirs();
        logFileDir = null;
        String logToday = logTypeDir + "/" + today;
        File logFile = new File(logToday);
        if(logFile.exists() == false){
            try{
                logFile.createNewFile();
            }catch (Exception e) {
                local(TAG, " ensureLogDirExists failed:" + e.getMessage());
            }
           
        }
        closeLogFile(index);
        try {
            mDefaultLogFiles[index] = new RandomAccessFile(logFile, "rw");
            mDefaultLogFiles[index].seek(logFile.length());
        } catch (Exception e) {
            LogManager.local(TAG, " openFile failed:" + e.getMessage());
        }
        
    }
    // if type = -1, then close all log files
    public void stop(){
        closeLogFile(-1);
    }
    public void closeLogFile(int type){
        if(type == -1){
            for(int index:CoreConstants.CONSTANT_INT_LOGTYPE_ARRAYS){
                closeLogFile(index);
            }
            return;
        }
        
        if(mDefaultLogFiles[type] != null){
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
   
    private String getTodayDateString() {
        Calendar today = Calendar.getInstance();
        StringBuffer sb = new StringBuffer();
        sb.append(today.get(Calendar.YEAR) - 1900);
        sb.append("-");
        sb.append(today.get(Calendar.MONTH));
        sb.append("-");
        sb.append(today.get(Calendar.DATE));
        sb.append("-");
        return sb.toString();
    }

   
    
    public void addLog(int type,String logLine){
        RandomAccessFile logFile = mDefaultLogFiles[type];
        if(logFile == null)
            return;
        try {
            logFile.writeChars(logLine);
            } catch (Exception e) {
                local(TAG, "addLog failed:" + e.getMessage());
                local(TAG, "addLog failed:" + logLine);
         }

    }

    public void uploadLog(int type, Calendar date) {
        // stub
    }

    public void uploadAllLogs() {
        // stub
    }
    public String[] listLogFiles(int type){
        if(mDefaultLogDirs[type] == null)
            return null;
        File logDir = new File(mDefaultLogDirs[type]);
        File[] files = logDir.listFiles();
        if(files.length == 0)
            return null;
        String[] fileName = new String[files.length];
        int index = 0;
        for(File file:files){
            fileName[index++] = file.getName();
        }
        return fileName;
    }
    

}