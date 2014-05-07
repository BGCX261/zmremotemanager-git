package com.zm.epad.core;

import android.os.Build;

public interface CoreConstants {
    final static String CONSTANT_SERVER = "server";
    final static String CONSTANT_SERVER_ADDRESS = "114.215.110.230";
    final static String CONSTANT_REST_CHECKEXIST = "http://114.215.110.230:8080/zmepadconsole/rest/user/qualifyifuserexist";
    final static String CONSTANT_REST_SIGNON = "http://114.215.110.230:8080/zmepadconsole/rest/user/newdevicebyexistuser";
    final static String CONSTANT_REST_REGISTER = "http://114.215.110.230:8080/zmepadconsole/rest/user/createnewphoneuser";
    final static String CONSTANT_ASYNC_SERVER = "http://114.215.110.230/zmepadconsole/asyncommand";
    final static String CONSTANT_USRNAME = "username";
    final static String CONSTANT_PASSWORD = "password";
    final static String CONSTANT_RESOURCE = "resource";
    final static String CONSTANT_DEFALT_RESOURCE = "default";
    final static String CONSTANT_COMMANDID = "commandid";
    final static String CONSTANT_DEFAULTSET = "defaultset";
    final static String CONSTANT_CRC = "crc";
    final static String CONSTANT_CRC_DEFAULT = "0";
    final static String CONSTANT_TYPE = "type";
    final static String CONSTANT_ACTION = "action";
    final static String CONSTANT_UPLOAD = "upload";
    final static String CONSTANT_MIME = "mime";
    final static String CONSTANT_MIME_DEFAULT = "text/plain";
    final static String CONSTANT_BUILDID = Build.SERIAL;
    final static String CONSTANT_IMG_PNG = "image/png";
    final static String CONSTANT_POLICY = "policy";

    final static String CONSTANT_RESULT_DONE = "complete";
    final static String CONSTANT_RESULT_DONE_ = "complete:";
    final static String CONSTANT_RESULT_DONE_0 = "complete:0";

    final static int CONSTANT_INT_LOGTYPE_COMMON = 0;
    final static int CONSTANT_INT_LOGTYPE_LOCATION = 1;
    final static int CONSTANT_INT_LOGTYPE_APPRUNTIME = 2;
    final static int CONSTANT_INT_LOGTYPE_RUNTIME = 3;
    final static int[] CONSTANT_INT_LOGTYPE_ARRAYS = {
            CONSTANT_INT_LOGTYPE_COMMON, CONSTANT_INT_LOGTYPE_LOCATION,
            CONSTANT_INT_LOGTYPE_APPRUNTIME, CONSTANT_INT_LOGTYPE_RUNTIME };

    final static String CONSTANT_LOGTAG_HEADER = "com.zm.epad.";

    final static String CONSTANT_LOGTYPE_COMMON = "common-log";
    final static String CONSTANT_LOGTYPE_LOCATION = "location-log";
    final static String CONSTANT_LOGTYPE_APPRUNTIME = "appruntime-log";
    final static String CONSTANT_LOGTYPE_RUNTIME = "runtime-log";
    final static String[] CONSTANT_LOGTYPE_ARRAYS = { CONSTANT_LOGTYPE_COMMON,
            CONSTANT_LOGTYPE_LOCATION, CONSTANT_LOGTYPE_APPRUNTIME,
            CONSTANT_LOGTYPE_RUNTIME };

    final static String CONSTANT_TIMEZOME_DEFAULT = "Asia/Chongqing";
}