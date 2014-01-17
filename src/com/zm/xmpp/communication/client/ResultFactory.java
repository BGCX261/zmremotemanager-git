package com.zm.xmpp.communication.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.provider.Settings;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.IUserManager;
import android.os.Looper;
import android.os.ServiceManager;
import android.os.UserManager;
import android.text.format.Time;

import com.zm.epad.core.LogManager;
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
				LogManager.local(TAG, "can't be solved by 1 resutl. length: " + ret.toXML().length());
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
	
	private IResult ConfigResultDevice(String id, ResultCallback callback)
	{		
		Device device = new Device();
		boolean sync = false;
        
        WifiInfo info = ((WifiManager)mContext.
        		getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
        if(info != null){
        	device.setWifi(info.getSSID());
        	
        	int ipAddr = info.getIpAddress();
            StringBuffer ipBuf = new StringBuffer();
            ipBuf.append(ipAddr  & 0xff).append('.').
                append((ipAddr >>>= 8) & 0xff).append('.').
                append((ipAddr >>>= 8) & 0xff).append('.').
                append((ipAddr >>>= 8) & 0xff);            
        	device.setIp(ipBuf.toString());
        }else{
        	device.setWifi("null");
        }
        LogManager.local(TAG, "Wifi:"+device.getWifi()+";ip:"+device.getIp());
        
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if(bt == null){
        	device.setBt("unsupport");
        }else{
        	device.setBt(bt.isEnabled()?"on":"off");
        }
        LogManager.local(TAG, "BT:"+device.getBt());
        
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(mContext);
        if(nfc == null){
        	device.setNfc("unsupport");
        }else{
        	device.setNfc(nfc.isEnabled()?"on":"off");
        }
        LogManager.local(TAG, "NFC:"+device.getNfc());
        
        device.setAmode(String.valueOf(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0));
        LogManager.local(TAG, "Airplane mode:"+device.getAmode());
        
        device.setMnet("null");
        LogManager.local(TAG, "Mobile Network:"+device.getMnet());
        
        LocationManager lm = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        
        lm.requestLocationUpdates(100, 0, criteria,
        		new LocationResultCallbackHandler(id, device, callback), null);   
        
        List<String> providers =  lm.getProviders(true);
        if(providers == null || providers.size() == 0){
        	LogManager.local(TAG, "all location providers are disabled");
        	device.setGps("null");
        	sync = true;
        	LogManager.local(TAG, "GPS:"+device.getGps());
        }

        if(sync == true || callback == null){
            ResultDevice result = new ResultDevice();
            result.setDevice(device);
            
    		return result;       	
        }else{
        	return null;
        }

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
	
	private class ResultCallbackHandler{
		protected String mId = null;
		protected ResultCallback mCallback = null;
		
		public ResultCallbackHandler(String id, ResultCallback callback)
		{
			mId = id;
			mCallback = callback;
		}
		
		public void sendResult(IResult result)
		{
			if(mCallback != null){
				result.setId(mId);
				result.setDeviceId(Build.SERIAL);
				result.setIssueTime(getCurrentTime());
				result.setDirection(Constants.XMPP_NAMESPACE_PAD);
				
				mCallback.handleResult(result);
			}	
		}
	}
	
	private class LocationResultCallbackHandler 
		extends ResultCallbackHandler implements LocationListener{

		private Device mDevice;
		private boolean mbDone;
		private Timer mTimer;

		public LocationResultCallbackHandler(String id, ResultCallback callback) {
			this(id, null, callback);
			// TODO Auto-generated constructor stub
		}

		public LocationResultCallbackHandler(String id, 
				Device device, ResultCallback callback) {
			super(id, callback);
			mDevice = device;
			mbDone = false;
			
			mTimer = new Timer();
			TimerTask task = new TimerTask(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					mDevice.setGps("null");			
					LogManager.local(TAG, "GPS time out");
					
		            ResultDevice result = new ResultDevice();
		            result.setDevice(mDevice);
		            
		            mbDone = true;
					sendResult(result);
					
					stopGetLocation();
				}
				
			};
			//if can't get location in 60 seconds, stop and send null
			mTimer.schedule(task, 60000);	
		}
		
		private void stopGetLocation(){
			((LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE)).removeUpdates(this);
			mTimer.cancel();
		}
		
		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			LogManager.local(TAG, "GPS onLocationChanged");
			if(location != null){
                double longitude= location.getLongitude();
                double  latitude = location.getLatitude();
                if(mDevice != null)
                {
                    mDevice.setGps(String.valueOf(longitude+","+latitude));               
                    LogManager.local(TAG, "GPS:"+mDevice.getGps());
                    
                    ResultDevice result = new ResultDevice();
                    result.setDevice(mDevice);
                    result.setStatus("done:0");
                    
                    sendResult(result);               	
                }
                mbDone = true;
			}
			if(mbDone == true){
				stopGetLocation();
			}
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			//do nothing
			LogManager.local(TAG, "GPS onStatusChanged:"+status);
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			// do nothing
			LogManager.local(TAG, "GPS onProviderEnabled:"+provider);
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			LogManager.local(TAG, "GPS onProviderDisabled:"+provider);
			if(mbDone == false){
				mDevice.setGps("null");			
				LogManager.local(TAG, "GPS disabled");
				
	            ResultDevice result = new ResultDevice();
	            result.setDevice(mDevice);
	            
	            mbDone = true;
				sendResult(result);
			}
			if(mbDone == true){
				stopGetLocation();
			}
		}
		
	}
	
}
