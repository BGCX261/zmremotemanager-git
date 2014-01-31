package com.zm.epad.plugins;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.LocationManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Handler;
import android.provider.Settings;

import com.zm.epad.core.LogManager;

import java.io.File;

public class RemoteDeviceManager {
    public static final String TAG = "RemoteDeviceManager";

    private Context mContext = null;
    ProminentFeature mProminent = null;

    public RemoteDeviceManager(Context context) {
        mContext = context;
        mProminent = new ProminentFeature(mContext);
    }
    
    public String getWifiName() {
        WifiInfo info = ((WifiManager) mContext
                .getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
        String ret = null;
        // about wifi. We need to figure out what info we want!
        if (info != null
                && info.getSupplicantState() == SupplicantState.COMPLETED) {
            ret = info.getSSID();
        }
        return ret;
    }
    
    public String getIpAddress() {
        String ret = null;
        
        //1st check wifi
        WifiInfo info = ((WifiManager) mContext
                .getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
        if (info != null
                && info.getSupplicantState() == SupplicantState.COMPLETED) {
            int ipAddr = info.getIpAddress();
            StringBuffer ipBuf = new StringBuffer();
            ipBuf.append(ipAddr & 0xff).append('.')
                    .append((ipAddr >>>= 8) & 0xff).append('.')
                    .append((ipAddr >>>= 8) & 0xff).append('.')
                    .append((ipAddr >>>= 8) & 0xff);
            ret = ipBuf.toString();
        }else{
            //add if support mobile network
        }

        return ret;
    }
    
    public String getBlueToothStatus() {
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt == null) {
            return "unsupported";
        } else {
            return bt.isEnabled() ? "on" : "off";
        }
    }
    
    public String getNfcStatus() {
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(mContext);
        if (nfc == null) {
            return "unsupported";
        } else {
            return nfc.isEnabled() ? "on" : "off";
        }       
    }

    public String getMobileNetwork() {
        // not support mobile network currently
        return null;
    }

    public String getGpsStatus() {

        LocationManager lm = (LocationManager) mContext
                .getSystemService(Context.LOCATION_SERVICE);

        if (lm.getProvider("gps") == null) {
            LogManager.local(TAG, "gps is not available");
            return null;
        } else {
            boolean bGpsEnabled = lm.isProviderEnabled("gps");
            return bGpsEnabled ? "on" : "off";
        }        
    }

    public String getAirplaneMode() {
        return String.valueOf(Settings.Global.getInt(
                    mContext.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0);
    }

    public void takeScreenshot(final Handler handler) {
        mProminent.takeScreenshot(handler);
    }

    public File getLatestScreenshot() {
        return mProminent.getLatestScreenshot();
    }
}
/*
 * private class LocationResultCallbackHandler extends ResultCallbackHandler
 * implements LocationListener{
 * 
 * private Device mDevice; private boolean mbDone; private Timer mTimer;
 * 
 * public LocationResultCallbackHandler(String id, ResultCallback callback) {
 * this(id, null, callback); // TODO Auto-generated constructor stub }
 * 
 * public LocationResultCallbackHandler(String id, Device device, ResultCallback
 * callback) { super(id, callback); mDevice = device; mbDone = false;
 * 
 * mTimer = new Timer(); TimerTask task = new TimerTask(){
 * 
 * @Override public void run() { // TODO Auto-generated method stub
 * mDevice.setGps("null"); LogManager.local(TAG, "GPS time out");
 * 
 * ResultDevice result = new ResultDevice(); result.setDevice(mDevice);
 * 
 * mbDone = true; sendResult(result);
 * 
 * stopGetLocation(); }
 * 
 * }; //if can't get location in 60 seconds, stop and send null
 * mTimer.schedule(task, 60000); }
 * 
 * private void stopGetLocation(){ ((LocationManager)
 * mContext.getSystemService(Context.LOCATION_SERVICE)).removeUpdates(this);
 * mTimer.cancel(); }
 * 
 * @Override public void onLocationChanged(Location location) { // TODO
 * Auto-generated method stub LogManager.local(TAG, "GPS onLocationChanged");
 * if(location != null){ double longitude= location.getLongitude(); double
 * latitude = location.getLatitude(); if(mDevice != null) {
 * mDevice.setGps(String.valueOf(longitude+","+latitude)); LogManager.local(TAG,
 * "GPS:"+mDevice.getGps());
 * 
 * ResultDevice result = new ResultDevice(); result.setDevice(mDevice);
 * result.setStatus("done:0");
 * 
 * sendResult(result); } mbDone = true; } if(mbDone == true){ stopGetLocation();
 * } }
 * 
 * @Override public void onStatusChanged(String provider, int status, Bundle
 * extras) { // TODO Auto-generated method stub //do nothing
 * LogManager.local(TAG, "GPS onStatusChanged:"+status); }
 * 
 * @Override public void onProviderEnabled(String provider) { // TODO
 * Auto-generated method stub // do nothing LogManager.local(TAG,
 * "GPS onProviderEnabled:"+provider); }
 * 
 * @Override public void onProviderDisabled(String provider) { // TODO
 * Auto-generated method stub LogManager.local(TAG,
 * "GPS onProviderDisabled:"+provider); if(mbDone == false){
 * mDevice.setGps("null"); LogManager.local(TAG, "GPS disabled");
 * 
 * ResultDevice result = new ResultDevice(); result.setDevice(mDevice);
 * 
 * mbDone = true; sendResult(result); } if(mbDone == true){ stopGetLocation(); }
 * }
 * 
 * }
 */