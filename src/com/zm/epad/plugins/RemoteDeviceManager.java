package com.zm.epad.plugins;

import com.zm.epad.core.LogManager;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.Criteria;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.provider.Settings;

import java.util.List;

public class RemoteDeviceManager {
    public static final String TAG = "RemoteDeviceManager";

    private Context mContext = null;

    public RemoteDeviceManager(Context context) {
        mContext = context;
    }

    public com.zm.epad.structure.Device getZMDeviceInfo() {
        com.zm.epad.structure.Device device = new com.zm.epad.structure.Device();

        WifiInfo info = ((WifiManager) mContext
                .getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
        // about wifi. We need to figure out what info we want!
        if (info != null
                && info.getSupplicantState() == SupplicantState.COMPLETED) {
            device.setWifi(info.getSSID());

            int ipAddr = info.getIpAddress();
            StringBuffer ipBuf = new StringBuffer();
            ipBuf.append(ipAddr & 0xff).append('.')
                    .append((ipAddr >>>= 8) & 0xff).append('.')
                    .append((ipAddr >>>= 8) & 0xff).append('.')
                    .append((ipAddr >>>= 8) & 0xff);
            device.setIp(ipBuf.toString());
        } else {
            device.setWifi("null");
        }
        LogManager.local(TAG,
                "Wifi:" + device.getWifi() + ";ip:" + device.getIp());

        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt == null) {
            device.setBt("unsupport");
        } else {
            device.setBt(bt.isEnabled() ? "on" : "off");
        }
        LogManager.local(TAG, "BT:" + device.getBt());

        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(mContext);
        if (nfc == null) {
            device.setNfc("unsupport");
        } else {
            device.setNfc(nfc.isEnabled() ? "on" : "off");
        }
        LogManager.local(TAG, "NFC:" + device.getNfc());

        device.setAmode(String.valueOf(Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0));
        LogManager.local(TAG, "Airplane mode:" + device.getAmode());

        device.setMnet("null");
        LogManager.local(TAG, "Mobile Network:" + device.getMnet());

        LocationManager lm = (LocationManager) mContext
                .getSystemService(Context.LOCATION_SERVICE);

        if (lm.getProvider("gps") == null) {
            device.setGps("null");
            LogManager.local(TAG, "gps is not supported");
        } else {
            boolean bGpsEnabled = lm.isProviderEnabled("gps");
            device.setGps(bGpsEnabled ? "on" : "off");
            LogManager.local(TAG, "gps is enabled? " + bGpsEnabled);
        }
        return device;
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