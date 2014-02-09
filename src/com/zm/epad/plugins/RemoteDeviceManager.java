package com.zm.epad.plugins;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.WallpaperManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.hardware.Camera;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.format.Time;
import android.view.SurfaceView;
import android.hardware.Camera;
import com.zm.epad.core.LogManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RemoteDeviceManager {
    public static final String TAG = "RemoteDeviceManager";

    private Context mContext = null;
    private Camera mCamera = null;
    private SurfaceView mSurfaceView = null;
    public RemoteDeviceManager(Context context) {
        mContext = context;
    }

    public void changeWallPager() {
        LogManager.local(TAG, "changeWallPager");
        WallpaperManager wm = WallpaperManager.getInstance(mContext);
        try {
            int id = mContext.getResources().getIdentifier("wall", "drawable",
                    "com.zm.epad");
            wm.setResource(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void takeScreenshot(final Handler handler) {
        LogManager.local(TAG, "takeScreenshot");
        ComponentName cn = new ComponentName("com.android.systemui",
                "com.android.systemui.screenshot.TakeScreenshotService");
        Intent intent = new Intent();
        intent.setComponent(cn);
        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LogManager.local(TAG, "onServiceConnected");
                Messenger messenger = new Messenger(service);
                Message msg = Message.obtain(null, 1);
                msg.replyTo = new Messenger(handler);

                try {
                    messenger.send(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                LogManager.local(TAG, "onServiceDisconnected");
            }
        };
        mContext.bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }
    
    public File getLatestScreenshot() {
        File ret = null;
        try {
            String[] proj = { MediaStore.Images.Media.TITLE,
                    MediaStore.Images.Media.DATE_MODIFIED,
                    MediaStore.Images.Media.SIZE, MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media._ID };

            ContentResolver resolver = mContext.getContentResolver();
            Cursor cursor = MediaStore.Images.Media.query(resolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj);

            String title = null;
            long time = Long.MIN_VALUE;
            int size = 0;
            String data = null;
            int id = 0;

            cursor.moveToFirst();
            do {
                if (time < cursor.getLong(cursor
                        .getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED))) {
                    time = cursor
                            .getLong(cursor
                                    .getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED));
                    title = cursor.getString(cursor
                            .getColumnIndex(MediaStore.Images.Media.TITLE));
                    size = cursor.getInt(cursor
                            .getColumnIndex(MediaStore.Images.Media.SIZE));
                    data = cursor.getString(cursor
                            .getColumnIndex(MediaStore.Images.Media.DATA));
                    id = cursor.getInt(cursor
                            .getColumnIndex(MediaStore.Images.Media._ID));
                }
            } while (cursor.moveToNext());

            LogManager.local(TAG, "media data:" + data);
            LogManager.local(TAG, "media id:" + id);

            Uri fileUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    .buildUpon().appendPath(String.valueOf(id)).build();
            LogManager.local(TAG, "data Uri:" + fileUri.toString());
            InputStream in = resolver.openInputStream(fileUri);

            ret = new File(mContext.getFilesDir().getAbsolutePath()
                    + "/temp.png");
            ret.createNewFile();
            FileOutputStream out = new FileOutputStream(ret);
            byte[] buffer = new byte[size];
            in.read(buffer);
            out.write(buffer);
            out.flush();
            out.close();
            in.close();

            LogManager.local(TAG, String.valueOf(ret.canRead()));
            LogManager.local(TAG, String.valueOf(ret.canWrite()));
            LogManager.local(TAG, String.valueOf(ret.canExecute()));
            LogManager.local(TAG, String.valueOf(ret.exists()));
            LogManager.local(TAG, ret.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        LogManager.local(TAG, ret == null ? "null"
                : (ret.toString() + "|" + ret.length()));
        return ret;
    }

    public void startCamera() {
        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        try {
            mContext.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendNotify(String title, String text) {
        NotificationManager NotifyManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Notification n = new Notification.Builder(mContext)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(
                        mContext.getResources().getIdentifier("icon",
                                "drawable", "com.zm.epad")).build();

        NotifyManager.notify(0, n);
    }

    // This 2 functions need to move out from this class to a FileUtil class.
    public void saveFileAsImage(File file) {
        ContentValues values = new ContentValues();
        ContentResolver resolver = mContext.getContentResolver();

        try {
            long time = System.currentTimeMillis();
            String fileName = String.valueOf(time) + "_" + file.getName();
            File dir = new File(
                    Environment
                            .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Recieved");
            String filePath = new File(dir, fileName).getAbsolutePath();
            LogManager.local(TAG, "file path: " + filePath);

            dir.mkdir();

            values.put(MediaStore.Images.ImageColumns.DATA, filePath);
            values.put(MediaStore.Images.ImageColumns.TITLE, fileName);
            values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, time);
            values.put(MediaStore.Images.ImageColumns.DATE_ADDED, time / 1000);
            values.put(MediaStore.Images.ImageColumns.DATE_MODIFIED,
                    time / 1000);
            values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpg");
            Uri uri = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            LogManager.local(TAG, "file Uri: " + uri.toString());

            FileInputStream in = new FileInputStream(file);
            OutputStream out = resolver.openOutputStream(uri);
            byte[] buffer = new byte[(int) file.length()];
            in.read(buffer);
            out.write(buffer);
            out.flush();
            out.close();
            in.close();

            values.clear();
            long length = new File(filePath).length();
            values.put(MediaStore.Images.ImageColumns.SIZE, length);
            resolver.update(uri, values, null, null);

            LogManager.local(TAG, "file length: " + String.valueOf(length));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // can't start camera in system server
    private void takePhotoNG() {
        LogManager.local(TAG, "camera open");
        mCamera = Camera.open();

        LogManager.local(TAG, "camera open done");
        mSurfaceView = new SurfaceView(mContext);

        try {
            LogManager.local(TAG, "startPreview");
            mCamera.setPreviewDisplay(mSurfaceView.getHolder());
            mCamera.startPreview();

            LogManager.local(TAG, "takePicture");
            mCamera.takePicture(null, null, new Camera.PictureCallback() {

                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    File file_name = null;
                    LogManager.local(TAG, "onPictureTaken");

                    try {
                        Time time = new Time();
                        time.setToNow();

                        file_name = new File("/sdcard/DCIM/Camera/" + "ZM_"
                                + time.toString());
                        if (!file_name.exists()) {
                            file_name.createNewFile();
                        }
                    } catch (final Exception e1) {
                        e1.printStackTrace();
                    }

                    try {
                        final OutputStream os = new FileOutputStream(file_name);
                        os.write(data);
                        os.close();
                    } catch (final Exception e) {
                        e.printStackTrace();
                    } finally {
                        mCamera.release();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
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