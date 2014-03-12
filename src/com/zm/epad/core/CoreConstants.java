package com.zm.epad.core;

import android.os.Build;

public interface CoreConstants {
    final static String CONSTANT_SERVER = "server";
    final static String CONSTANT_USRNAME = "username";
    final static String CONSTANT_PASSWORD = "password";
    final static String CONSTANT_RESOURCE = "resource";
    final static String CONSTANT_COMMANDID = "commandid";
    final static String CONSTANT_TYPE = "type";
    final static String CONSTANT_ACTION = "action";
    final static String CONSTANT_UPLOAD = "upload";
    final static String CONSTANT_MIME = "mime";
    final static String CONSTANT_DEVICEID = Build.SERIAL;
    final static String CONSTANT_IMG_PNG = "image/png";
    final static String CONSTANT_POLICY = "policy";
    final static String CONSTANT_RESULT_OK = "OK";
    final static String CONSTANT_RESULT_NG = "NG";
    final static String CONSTANT_RESULT_DONE = "done";
    final static String CONSTANT_RESULT_DONE_ = "done:";
    final static String CONSTANT_RESULT_DONE_0 = "done:0";
    
    
    
    final static int    CONSTANT_INT_LOGTYPE_COMMON = 0;
    final static int    CONSTANT_INT_LOGTYPE_LOCATION = 1;
    final static int    CONSTANT_INT_LOGTYPE_APPRUNTIME = 2;
    final static int    CONSTANT_INT_LOGTYPE_RUNTIME = 3;
    final static int[]    CONSTANT_INT_LOGTYPE_ARRAYS = {
        CONSTANT_INT_LOGTYPE_COMMON,
        CONSTANT_INT_LOGTYPE_LOCATION,
        CONSTANT_INT_LOGTYPE_APPRUNTIME,
        CONSTANT_INT_LOGTYPE_RUNTIME
    };
    
    final static String CONSTANT_LOGTAG_HEADER = "com.zm.epad:";
    
    final static String CONSTANT_LOGTYPE_COMMON = "common-log";
    final static String CONSTANT_LOGTYPE_LOCATION = "location-log";
    final static String CONSTANT_LOGTYPE_APPRUNTIME = "appruntime-log";
    final static String CONSTANT_LOGTYPE_RUNTIME = "runtime-log";
    final static String[] CONSTANT_LOGTYPE_ARRAYS = { CONSTANT_LOGTYPE_COMMON,
            CONSTANT_LOGTYPE_LOCATION, CONSTANT_LOGTYPE_APPRUNTIME,
            CONSTANT_LOGTYPE_RUNTIME };

}