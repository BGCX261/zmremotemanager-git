package com.zm.xmpp.communication.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.IUserManager;
import android.os.ServiceManager;
import android.os.UserManager;
import android.text.format.Time;

import com.zm.epad.core.LogManager;
import com.zm.epad.structure.Application;
import com.zm.epad.structure.Configuration;
import com.zm.epad.structure.Environment;
import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.result.IResult;
import com.zm.xmpp.communication.result.ResultApp;
import com.zm.xmpp.communication.result.ResultDevice;
import com.zm.xmpp.communication.result.ResultEnv;
import com.zm.xmpp.communication.result.ResultNormal;

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
	
	private Context mContext = null;
	private HashMap<String, ResultCallback> mCbMap = new HashMap<String, ResultCallback>();
	
	public ResultFactory(Context context)
	{
		mContext = context;
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
		default:
			LogManager.local(TAG, "bad type: " + type);
			ret = null;
			break;
		}

		if(ret!=null)
		{
			if(ret.toXML().length() > RESULT_LENGTH_MAX){
				LogManager.local(TAG, "can't be solved by 1 resutl. length: " + ret.toXML().length());
				return null;
			}
			ret.setId(id);
			ret.setStatus(status);
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
		case RESULT_APP:
			resultList = ConfigResultApp();
			break;
		case RESULT_DEVICE:
			resultList = ConfigResultDevice();
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
			r.setIssueTime(getCurrentTime());
			r.setDirection(Constants.XMPP_NAMESPACE_PAD);			
		}
		return resultList;
	}
	
	private String getCurrentTime()
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
		IUserManager iUm = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
		IPackageManager iPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));		

		List<UserInfo>userList = null;
		try{
			userList = iUm.getUsers(true);
		}catch(Exception e){
			LogManager.local(TAG, "ConfigResultApp:"+e.toString());
			return null;
		}
		ResultApp result = new ResultApp();
		for(UserInfo ui: userList)
		{	
			Environment env = new Environment();			
			env.setId(String.valueOf(ui.id));
			result.addEnv(env);
			
			List<PackageInfo> pkgList = null;
			try{
				ParceledListSlice<PackageInfo> slice = iPm.getInstalledPackages(0, ui.id);
				pkgList = slice.getList();
			}catch(Exception e){
				LogManager.local(TAG, "getInstalledApplications:"+e.toString());
				continue;
			}
			
			for(PackageInfo pi:pkgList)
			{
				if(result.toXML().length() > RESULT_LENGTH_MAX -
						(RESULT_APPINFO_LENGTH_MAX + RESULT_APPINFO_LENGTH_TAG))
				{
					LogManager.local(TAG, "no space for another appinfo in current result("
							+resultList.size()+") length:"+result.toXML().length());
					result.setStatus(String.valueOf(resultList.size()));
					resultList.add(result);
					
					result = new ResultApp();
					env = new Environment();			
					env.setId(String.valueOf(ui.id));
					result.addEnv(env);
				}
				
				Application app = new Application();
				
				String name = pi.applicationInfo.loadLabel(mContext.getPackageManager()).toString();
				String pkgname = pi.packageName;
				String enabled = String.valueOf(pi.applicationInfo.enabled);
				String flag = String.valueOf(pi.applicationInfo.flags);
				String version = pi.versionName;
				
				if(name.length() + pkgname.length() + enabled.length() + 
						flag.length() + version.length() > RESULT_APPINFO_LENGTH_MAX)
				{
					//if app info is too long, only save package name, enabled and flag 
					app.setAppName(pkgname);
					app.setEnabled(enabled);
					app.setFlag(flag);
				}else{
					app.setName(name);
					app.setAppName(pkgname);
					app.setEnabled(enabled);
					app.setFlag(flag);
					app.setVersion(version);					
				}

				
				env.addApp(app);
			}			
		}
		result.setStatus("done:"+resultList.size());
		resultList.add(result);
		
		return resultList;
	}
	
	private List<IResult> ConfigResultDevice()
	{
		return null;
	}
	
	private List<IResult> ConfigResultEnv()
	{
		List<IResult> resultList = new ArrayList<IResult>();
		
		IUserManager iUm = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
		
		List<UserInfo>userList = null;
		try{
			userList = iUm.getUsers(true);
		}catch(Exception e){
			LogManager.local(TAG, "ConfigResultEnv:"+e.toString());
			return null;
		}
		
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
			
			Environment env = new Environment();
			Configuration cfg = new Configuration();
			Bundle Restrictions = null;
			
			env.setId(String.valueOf(ui.id));
			
			try{
				Restrictions = iUm.getUserRestrictions(ui.id);
			}catch(Exception e){
				LogManager.local(TAG, "getUserRestrictions:"+e.toString());
				continue;
			}
			
			cfg.setNoModifyAccount(
					String.valueOf(Restrictions.getBoolean(UserManager.DISALLOW_MODIFY_ACCOUNTS)));
			cfg.setNoConfigWifi(
					String.valueOf(Restrictions.getBoolean(UserManager.DISALLOW_CONFIG_WIFI)));
			cfg.setNoInstallApps(
					String.valueOf(Restrictions.getBoolean(UserManager.DISALLOW_INSTALL_APPS)));
			cfg.setNoInstallApps(
					String.valueOf(Restrictions.getBoolean(UserManager.DISALLOW_UNINSTALL_APPS)));
			cfg.setNoShareLocation(
					String.valueOf(Restrictions.getBoolean(UserManager.DISALLOW_SHARE_LOCATION)));
			cfg.setNoInstallUnknownSources(
					String.valueOf(Restrictions.getBoolean(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)));
			cfg.setNoConfigBluetooth(
					String.valueOf(Restrictions.getBoolean(UserManager.DISALLOW_CONFIG_BLUETOOTH)));
			cfg.setNoUsbFileTranster(
					String.valueOf(Restrictions.getBoolean(UserManager.DISALLOW_USB_FILE_TRANSFER)));
			cfg.setNoConfigCredentials(
					String.valueOf(Restrictions.getBoolean(UserManager.DISALLOW_CONFIG_CREDENTIALS)));
			cfg.setNoRemoveUser(
					String.valueOf(Restrictions.getBoolean(UserManager.DISALLOW_REMOVE_USER)));
			
			env.setConf(cfg);
			result.addEnv(env);
		}
		
		result.setStatus("done");
		resultList.add(result);
				
		return resultList;
	}
	
	private void addCallback(String id, ResultCallback callback)
	{
		mCbMap.put(id, callback);
	}
	
	private void sendCallback(String id, IResult result)
	{
		ResultCallback cb = mCbMap.get(id);
		cb.handleResult(result);
		mCbMap.remove(id);
	}
}
