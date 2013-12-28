package com.android.logmanager;

public class LogManager {
    
    static private LogManager mLogManager;
    static public LogManager getLogManagerInstance(){
        if(mLogManager == null)
            mLogManager = new LogManager();
        return mLogManager;
    }
    private LogManager(){
        
    }
    
}
