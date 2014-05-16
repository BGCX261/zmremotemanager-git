package com.zm.epad.plugins.policy;

import com.zm.epad.core.LogManager;

public class LogUploadRunnable implements Runnable {

    private String mUrl;
    private int mType;

    public LogUploadRunnable(String url, int type) {
        mUrl = url;
        mType = type;
    }

    @Override
    public void run() {
        LogManager logMgr = LogManager.getInstance();
        String[] logs = logMgr.listLogFiles(mType);
        for (String filename : logs) {
            logMgr.uploadLog(mUrl, mType, filename);
        }
    }

}