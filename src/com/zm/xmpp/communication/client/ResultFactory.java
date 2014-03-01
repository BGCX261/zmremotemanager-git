package com.zm.xmpp.communication.client;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.UserInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.text.format.Time;

import com.zm.epad.core.CoreConstants;
import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;
import com.zm.epad.plugins.RemoteDeviceManager;
import com.zm.epad.plugins.RemotePackageManager;
import com.zm.epad.structure.Application;
import com.zm.epad.structure.Configuration;
import com.zm.epad.structure.Device;
import com.zm.epad.structure.Environment;
import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.result.IResult;
import com.zm.xmpp.communication.result.ResultApp;
import com.zm.xmpp.communication.result.ResultDevice;
import com.zm.xmpp.communication.result.ResultEnv;
import com.zm.xmpp.communication.result.ResultNormal;
import com.zm.xmpp.communication.result.ResultRunningApp;

import android.app.ActivityManager.RunningAppProcessInfo;

import java.util.ArrayList;
import java.util.List;

public class ResultFactory {
    public static final String TAG = "ClientResultFactory";

    public static final int RESULT_LENGTH_MAX = 2 * 1024 * 1024;

    public static final int RESULT_NORMAL = 1;
    public static final int RESULT_APP = 2;
    public static final int RESULT_DEVICE = 3;
    public static final int RESULT_ENV = 4;
    public static final int RESULT_RUNNINGAPP = 5;

    private static final int RESULT_APPINFO_LENGTH_MAX = 120;
    private static final int RESULT_APPINFO_LENGTH_TAG = 80;
    private static final int RESULT_EVNINFO_LENGTH = 500;

    /*
     * private RemotePackageManager mPkgsManager = null; private
     * RemoteDeviceManager mDeviceManager = null;
     */
    SubSystemFacade mSubSystemFacade;

    /*public ResultFactory() {

    }*/

    public void setSubSystem(SubSystemFacade subSystemFacade) {
        mSubSystemFacade = subSystemFacade;
    }

    public interface ResultCallback {
        public void handleResult(IResult result);
    }

    public IResult getResult(int type, String id, String status,
            ResultCallback callback) {
        IResult ret = null;

        switch (type) {
        case RESULT_NORMAL:
            ret = new ResultNormal();
            break;
        case RESULT_DEVICE:
            try {
                ret = ConfigResultDevice(id);
            } catch (Exception e) {
                LogManager.local(TAG, "getResult:" + e.toString());
                ret = null;
            }
            break;
        case RESULT_RUNNINGAPP:
            ret = getRunningAppResult();
            break;
        default:
            LogManager.local(TAG, "bad type: " + type);
            ret = null;
            break;
        }

        if (ret != null) {
            if (false/* ret.toXML().length() > RESULT_LENGTH_MAX */) {
                LogManager.local(TAG, "can't be solved by 1 resutl. length: "
                        + ret.toXML().length());
                return null;
            }

            ret.setId(id);
            ret.setStatus(status);
            ret.setDeviceId(CoreConstants.CONSTANT_DEVICEID/* Build.SERIAL */);
            ret.setIssueTime(getCurrentTime());
            ret.setDirection(Constants.XMPP_NAMESPACE_PAD);
        }

        return ret;
    }

    public IResult getResult(int type, String id) {
        return getResult(type, id, null, null);
    }

    public IResult getResult(int type, String id, String status) {
        return getResult(type, id, status, null);
    }

    public List<IResult> getResults(int type, String id) {
        return getResults(type, id, null);
    }

    public List<IResult> getResults(int type, String id, ResultCallback callback) {
        List<IResult> resultList = null;

        switch (type) {
        case RESULT_NORMAL:
        case RESULT_DEVICE:
        case RESULT_RUNNINGAPP:
            IResult ret = getResult(type, id,
                    CoreConstants.CONSTANT_RESULT_DONE_0, callback);
            if (ret != null) {
                resultList = new ArrayList<IResult>();
                resultList.add(ret);
            } else {
                return null;
            }
            break;
        case RESULT_APP:
            resultList = ConfigResultApp();
            break;
        case RESULT_ENV:
            resultList = ConfigResultEnv();
            break;
        default:
            LogManager.local(TAG, "bad type: " + type);
            return null;
        }

        for (IResult r : resultList) {
            r.setId(id);
            r.setDeviceId(CoreConstants.CONSTANT_DEVICEID/* Build.SERIAL */);
            r.setIssueTime(getCurrentTime());
            r.setDirection(Constants.XMPP_NAMESPACE_PAD);
        }
        return resultList;
    }

    private static String getCurrentTime() {
        Time t = new Time();
        t.setToNow();
        String ret = String.valueOf(t.year) + String.valueOf(t.month)
                + String.valueOf(t.monthDay) + String.valueOf(t.hour)
                + String.valueOf(t.minute) + String.valueOf(t.second);
        return ret;
    }

    private List<IResult> ConfigResultApp() {
        List<IResult> resultList = new ArrayList<IResult>();

        List<UserInfo> userList = mSubSystemFacade.getAllUsers();

        ResultApp result = new ResultApp();
        for (UserInfo ui : userList) {
            Environment env = new Environment();
            env.setId(String.valueOf(ui.id));
            result.addEnv(env);

            List<PackageInfo> pkgList = mSubSystemFacade.getInstalledPackages(
                    0, ui.id);
            if (pkgList.size() == 0)
                continue;

            for (PackageInfo pi : pkgList) {
                // xmpp protocol is a stream, there is no fixed size concept in
                // streaming transfer.
                // we have use compressed data transport.

                if (false/*
                          * result.toXML().length() > RESULT_LENGTH_MAX -
                          * (RESULT_APPINFO_LENGTH_MAX +
                          * RESULT_APPINFO_LENGTH_TAG)
                          */) {

                    LogManager.local(TAG,
                            "no space for another appinfo in current result("
                                    + resultList.size() + ") length:"
                                    + result.toXML().length());
                    result.setStatus(String.valueOf(resultList.size()));
                    resultList.add(result);

                    result = new ResultApp();
                    env = new Environment();
                    env.setId(String.valueOf(ui.id));
                    result.addEnv(env);
                }

                // only give non-system app info to server
                if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    Application zmAppInfo = mSubSystemFacade
                            .getZMApplicationInfo(pi);
                    env.addApp(zmAppInfo);
                }
            }
        }
        result.setStatus(CoreConstants.CONSTANT_RESULT_DONE_ + resultList.size());
        resultList.add(result);

        return resultList;
    }

    private IResult ConfigResultDevice(String id) {

        Device device = mSubSystemFacade.getZMDeviceInfo();
        ResultDevice result = new ResultDevice();
        result.setDevice(device);
        return result;
    }

    private List<IResult> ConfigResultEnv() {
        List<IResult> resultList = new ArrayList<IResult>();

        List<UserInfo> userList = mSubSystemFacade.getAllUsers();

        ResultEnv result = new ResultEnv();
        for (UserInfo ui : userList) {
            if (false/*
                      * result.toXML().length() > RESULT_LENGTH_MAX -
                      * RESULT_EVNINFO_LENGTH
                      */) {
                LogManager.local(TAG,
                        "no space for another envinfo in current result("
                                + resultList.size() + ") length:"
                                + result.toXML().length());
                result.setStatus(String.valueOf(resultList.size()));
                resultList.add(result);

                result = new ResultEnv();
            }
            Configuration cfg = mSubSystemFacade.getZMUserConfigInfo(ui.id);
            if (cfg == null)
                continue;

            Environment env = new Environment();
            env.setId(String.valueOf(ui.id));

            env.setConf(cfg);
            result.addEnv(env);
        }

        result.setStatus(CoreConstants.CONSTANT_RESULT_DONE);
        resultList.add(result);

        return resultList;
    }

    private class ResultCallbackHandler {
        protected String mId = null;
        protected ResultCallback mCallback = null;

        public ResultCallbackHandler(String id, ResultCallback callback) {
            mId = id;
            mCallback = callback;
        }

        public void sendResult(IResult result) {
            if (mCallback != null) {
                result.setId(mId);
                result.setDeviceId(CoreConstants.CONSTANT_DEVICEID);
                result.setIssueTime(getCurrentTime());
                result.setDirection(Constants.XMPP_NAMESPACE_PAD);

                mCallback.handleResult(result);
            }
        }
    }

    private IResult getRunningAppResult() {

        ResultRunningApp result = new ResultRunningApp(
                mSubSystemFacade.getCurrentUserId(), getCurrentTime());

        List<RunningAppProcessInfo> runningList =mSubSystemFacade.getRunningAppProcesses();
        
        String[] outArry = new String[mSubSystemFacade.INDEX_MAX];
        
        for (RunningAppProcessInfo pi : runningList) {

            mSubSystemFacade.getRunningAppProcessInfo(outArry,pi);
            result.addProcess(outArry[mSubSystemFacade.INDEX_NAME], 
                    outArry[mSubSystemFacade.INDEX_DISPLAY], outArry[mSubSystemFacade.INDEX_IMPORTANCE], 
                    outArry[mSubSystemFacade.INDEX_VERSION]);
        }
        return result;
    }
}
