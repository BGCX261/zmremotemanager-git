package com.zm.epad.plugins.policy;

import com.zm.epad.core.CoreConstants;
import com.zm.epad.core.LogManager;

public class LogUploadRunnable implements Runnable {

    private String mUrl;

    public LogUploadRunnable(String url) {
        mUrl = url;
    }

    @Override
    public void run() {
        LogManager logMgr = LogManager.getInstance();
        int type = CoreConstants.CONSTANT_INT_LOGTYPE_COMMON;
        String[] logs = logMgr.listLogFiles(type);
        for (String filename : logs) {
            logMgr.uploadLog(mUrl, type, filename);
        }
    }

}