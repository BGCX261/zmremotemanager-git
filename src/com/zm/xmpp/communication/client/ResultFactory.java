package com.zm.xmpp.communication.client;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.UserInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.text.format.Time;

import com.zm.epad.core.LogManager;
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

import java.util.ArrayList;
import java.util.List;

public class ResultFactory {
    public static final String TAG = "ClientResultFactory";
    
    public static final int RESULT_LENGTH_MAX = 3500;
    
    public static final int RESULT_NORMAL = 1;
    public static final int RESULT_APP = 2;
    public static final int RESULT_DEVICE = 3;
    public static final int RESULT_ENV = 4;
    
    private static final int RESULT_APPINFO_LENGTH_MAX = 120;
    private static final int RESULT_APPINFO_LENGTH_TAG = 80;
    private static final int RESULT_EVNINFO_LENGTH = 500;    

    private RemotePackageManager mPkgsManager = null;
    private RemoteDeviceManager mDeviceManager = null;
    
    public ResultFactory(RemotePackageManager pkgsManager,
            RemoteDeviceManager deviceManager) {
        mPkgsManager = pkgsManager;
        mDeviceManager = deviceManager;
    }

    public interface ResultCallback{
        public void handleResult(IResult result);
    }
    
    public IResult getResult(int type, String id, String status, ResultCallback callback)
    {
        IResult ret = null;
        
        switch(type){
        case RESULT_NORMAL:
            ret = new ResultNormal();
            break;
        case RESULT_DEVICE:
            try{
                ret = ConfigResultDevice(id, callback);
            }catch(Exception e){
                LogManager.local(TAG, "getResult:"+e.toString());
                ret = null;
            }
            break;
        default:
            LogManager.local(TAG, "bad type: " + type);
            ret = null;
            break;
        }

        if(ret!=null)
        {
            if(ret.toXML().length() > RESULT_LENGTH_MAX){
                LogManager.local(TAG, "can't be solved by 1 resutl. length: " +
                        ret.toXML().length()); 
                return null; 
            }

            ret.setId(id);
            ret.setStatus(status);
            ret.setDeviceId(Build.SERIAL);
            ret.setIssueTime(getCurrentTime());
            ret.setDirection(Constants.XMPP_NAMESPACE_PAD);
        }        

        return ret;
    }
    
    public IResult getResult(int type, String id)
    {
        return getResult(type, id, null, null);
    }
    
    public IResult getResult(int type, String id, String status)
    {
        return getResult(type, id, status, null);
    }
    
    public List<IResult> getResults(int type, String id)
    {
        return getResults(type, id, null);
    }
    
    public List<IResult> getResults(int type, String id, ResultCallback callback) 
    {
        List<IResult> resultList = null;
        
        switch(type){
        case RESULT_NORMAL:
        case RESULT_DEVICE:
            IResult ret = getResult(type, id, "done:0", callback);
            if(ret != null){
                resultList = new ArrayList<IResult>();
                resultList.add(ret);
            }else{
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
        
        for(IResult r : resultList)
        {
            r.setId(id);
            r.setDeviceId(Build.SERIAL);
            r.setIssueTime(getCurrentTime());
            r.setDirection(Constants.XMPP_NAMESPACE_PAD);        
        }
        return resultList;
    }
    
    private static String getCurrentTime()
    {
        Time t=new Time();
        t.setToNow();
        String ret = String.valueOf(t.year)+String.valueOf(t.month)+String.valueOf(t.monthDay)
                +String.valueOf(t.hour)+String.valueOf(t.minute)+String.valueOf(t.second);
        return ret;
    }
    
    private List<IResult> ConfigResultApp()
    {
        List<IResult> resultList = new ArrayList<IResult>();

        List<UserInfo> userList = mPkgsManager.getAllUsers();
        
        ResultApp result = new ResultApp();
        for(UserInfo ui: userList) {    
            Environment env = new Environment();
            env.setId(String.valueOf(ui.id));
            result.addEnv(env);
            
            List<PackageInfo> pkgList = mPkgsManager.getInstalledPackages(0,
                    ui.id);
            if (pkgList.size() == 0)
                continue;
                        
            for(PackageInfo pi:pkgList){
               //xmpp protocol is a stream, there is no fixed size concept in streaming transfer.
               if(result.toXML().length() > RESULT_LENGTH_MAX -
                        (RESULT_APPINFO_LENGTH_MAX + RESULT_APPINFO_LENGTH_TAG)){
                    
                    LogManager.local(TAG, "no space for another appinfo in current result("
                            +resultList.size()+") length:"+result.toXML().length());
                    result.setStatus(String.valueOf(resultList.size()));
                    resultList.add(result);
                    
                    result = new ResultApp();
                    env = new Environment();            
                    env.setId(String.valueOf(ui.id));
                    result.addEnv(env);
                }
                
               //only give non-system app info to server
                if((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0){
                    Application zmAppInfo = getZMApplicationInfo(pi);                
                    env.addApp(zmAppInfo);                    
                }
            }            
        }
        result.setStatus("done:"+resultList.size());
        resultList.add(result);
        
        return resultList;
    }
    
    private IResult ConfigResultDevice(String id, ResultCallback callback){  

        Device device = getZMDeviceInfo();
        ResultDevice result = new ResultDevice();
        result.setDevice(device);
        return result;           
   }
    
    private List<IResult> ConfigResultEnv()
    {
        List<IResult> resultList = new ArrayList<IResult>();
        
        List<UserInfo> userList = mPkgsManager.getAllUsers();

        ResultEnv result = new ResultEnv();
        for(UserInfo ui: userList)
        {
            if(result.toXML().length() > RESULT_LENGTH_MAX - RESULT_EVNINFO_LENGTH)
            {
                LogManager.local(TAG, "no space for another envinfo in current result("
                        +resultList.size()+") length:"+result.toXML().length());
                result.setStatus(String.valueOf(resultList.size()));
                resultList.add(result);
                
                result = new ResultEnv();
            }
            Configuration cfg = getZMUserConfigInfo(ui.id);
            if(cfg == null)
                continue;
            
            Environment env = new Environment();
            env.setId(String.valueOf(ui.id));

            env.setConf(cfg);
            result.addEnv(env);
        }
        
        result.setStatus("done");
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
            if(mCallback != null){
                result.setId(mId);
                result.setDeviceId(Build.SERIAL);
                result.setIssueTime(getCurrentTime());
                result.setDirection(Constants.XMPP_NAMESPACE_PAD);
                
                mCallback.handleResult(result);
            }    
        }
    }
    
    private Application getZMApplicationInfo(PackageInfo pi) {
        String name = mPkgsManager.getApplicationName(pi);
        String pkgname = pi.packageName;
        String enabled = String.valueOf(pi.applicationInfo.enabled);
        String flag = String.valueOf(pi.applicationInfo.flags);
        String version = pi.versionName;
        
        Application zmAppInfo = new  Application();
        zmAppInfo.setName(name);
        zmAppInfo.setAppName(pkgname);
        zmAppInfo.setEnabled(enabled);
        zmAppInfo.setFlag(flag);
        zmAppInfo.setVersion(version);
        
        return zmAppInfo;
    }
    
    private Device getZMDeviceInfo() {
        Device device = new Device();

        device.setWifi(mDeviceManager.getWifiName());
        LogManager.local(TAG,"Wifi:" + device.getWifi());

        device.setBt(mDeviceManager.getBlueToothStatus());
        LogManager.local(TAG, "BT:" + device.getBt());

        device.setNfc(mDeviceManager.getNfcStatus());
        LogManager.local(TAG, "NFC:" + device.getNfc());
        
        device.setIp(mDeviceManager.getIpAddress());
        LogManager.local(TAG, "IP:" + device.getIp());

        device.setGps(mDeviceManager.getGpsStatus());
        LogManager.local(TAG, "GPS:" + device.getGps());
        
        device.setAmode(mDeviceManager.getAirplaneMode());
        LogManager.local(TAG, "Airplane mode:" + device.getAmode());

        device.setMnet(mDeviceManager.getMobileNetwork());
        LogManager.local(TAG, "Mobile Network:" + device.getMnet());
        
        return device;
    }
    
    private Configuration getZMUserConfigInfo(int uid) {
        Bundle userRestrictionInfo = mPkgsManager.getUserRestrictions(uid);
        
        if(userRestrictionInfo == null){
            return null;
        }

        Configuration cfg = new Configuration();
        cfg.setNoModifyAccount(
                String.valueOf(userRestrictionInfo.getBoolean(UserManager.DISALLOW_MODIFY_ACCOUNTS)));
        cfg.setNoConfigWifi(
                String.valueOf(userRestrictionInfo.getBoolean(UserManager.DISALLOW_CONFIG_WIFI)));
        cfg.setNoInstallApps(
                String.valueOf(userRestrictionInfo.getBoolean(UserManager.DISALLOW_INSTALL_APPS)));
        cfg.setNoInstallApps(
                String.valueOf(userRestrictionInfo.getBoolean(UserManager.DISALLOW_UNINSTALL_APPS)));
        cfg.setNoShareLocation(
                String.valueOf(userRestrictionInfo.getBoolean(UserManager.DISALLOW_SHARE_LOCATION)));
        cfg.setNoInstallUnknownSources(
                String.valueOf(userRestrictionInfo.getBoolean(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)));
        cfg.setNoConfigBluetooth(
                String.valueOf(userRestrictionInfo.getBoolean(UserManager.DISALLOW_CONFIG_BLUETOOTH)));
        cfg.setNoUsbFileTranster(
                String.valueOf(userRestrictionInfo.getBoolean(UserManager.DISALLOW_USB_FILE_TRANSFER)));
        cfg.setNoConfigCredentials(
                String.valueOf(userRestrictionInfo.getBoolean(UserManager.DISALLOW_CONFIG_CREDENTIALS)));
        cfg.setNoRemoveUser(
                String.valueOf(userRestrictionInfo.getBoolean(UserManager.DISALLOW_REMOVE_USER)));
        
        return cfg;
    }
    
}
