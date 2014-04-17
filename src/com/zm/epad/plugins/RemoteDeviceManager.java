package com.zm.epad.plugins;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.NotificationManager;
import android.app.WallpaperManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.widget.TextView;

import com.zm.epad.R;
import com.zm.epad.core.LogManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class RemoteDeviceManager {
    public static final String TAG = "RemoteDeviceManager";

    private Context mContext = null;

    private Screenshot mScreenshot = null;

    private RemoteLocationTrack mLocationTrack = null;

    private View mKeyguard = null;

    private RemotePowerManager mPowerMgr = null;

    private Handler mHandler;

    public void stop() {
        LogManager.local(TAG, "stop");
        mPowerMgr.stop();
    }

    public RemoteDeviceManager(Context context) {
        mContext = context;
        mScreenshot = new Screenshot(mContext);
        mLocationTrack = new RemoteLocationTrack();
        mPowerMgr = new RemotePowerManager();
        mHandler = new Handler(mContext.getMainLooper());
    }

    public boolean changeWallpaper(String wallImage) {
        LogManager.local(TAG, "changeWallPager:" + wallImage);

        boolean ret = false;
        WallpaperManager wm = WallpaperManager.getInstance(mContext);
        try {
            BitmapFactory factory = new BitmapFactory();
            wm.setBitmap(BitmapFactory.decodeFile(wallImage));
            ret = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }

    public boolean changeWallPager(int id) {
        LogManager.local(TAG, "changeWallPager:" + id);

        boolean ret = false;
        WallpaperManager wm = WallpaperManager.getInstance(mContext);
        try {
            wm.setResource(id);
            ret = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }

    private class Screenshot {
        final float REMOTE_LONG_EDGE_F = 640f;
        WindowManager mWindowManager;
        Display mDisplay;
        DisplayMetrics mDisplayMetrics;
        NotificationManager mNotificationManager;
        private Matrix mDisplayMatrix;

        private Camera mCamera;
        private SurfaceView mSurfaceView;

        public Screenshot(Context context) {
            mWindowManager = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
            mNotificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            mDisplay = mWindowManager.getDefaultDisplay();
            mDisplayMetrics = new DisplayMetrics();
            mDisplay.getRealMetrics(mDisplayMetrics);
            mDisplayMatrix = new Matrix();
        }

        public byte[] takeScreenshot() {

            mDisplay.getRealMetrics(mDisplayMetrics);
            float[] dims = { mDisplayMetrics.widthPixels,
                    mDisplayMetrics.heightPixels };
            float degrees = getDegreesForRotation(mDisplay.getRotation());
            boolean requiresRotation = (degrees > 0);
            if (requiresRotation) {
                // Get the dimensions of the device in its native orientation
                mDisplayMatrix.reset();
                mDisplayMatrix.preRotate(-degrees);
                mDisplayMatrix.mapPoints(dims);
                dims[0] = Math.abs(dims[0]);
                dims[1] = Math.abs(dims[1]);
            }

            // Take the screenshot
            Bitmap screenBitmap = SurfaceControl.screenshot((int) dims[0],
                    (int) dims[1]);
            if (screenBitmap == null) {
                LogManager.local(TAG, "takeScreenshot fails");
                return null;
            }

            if (requiresRotation) {
                // Rotate the screenshot to the current orientation
                Bitmap ss = Bitmap.createBitmap(mDisplayMetrics.widthPixels,
                        mDisplayMetrics.heightPixels, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(ss);
                c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
                c.rotate(degrees);
                c.translate(-dims[0] / 2, -dims[1] / 2);
                c.drawBitmap(screenBitmap, 0, 0, null);
                c.setBitmap(null);
                // Recycle the previous bitmap
                screenBitmap.recycle();
                screenBitmap = ss;
            }

            // resize screen bitmap to make it small to upload
            Matrix matrix = new Matrix();
            int longEdge = mDisplayMetrics.widthPixels > mDisplayMetrics.heightPixels ? mDisplayMetrics.widthPixels
                    : mDisplayMetrics.heightPixels;

            float scale = REMOTE_LONG_EDGE_F / longEdge;
            LogManager.local(TAG, "resize scale:" + scale);
            matrix.postScale(scale, scale);
            Bitmap resize = Bitmap.createBitmap(screenBitmap, 0, 0,
                    screenBitmap.getWidth(), screenBitmap.getHeight(), matrix,
                    true);

            // change color depth rgb565 to reduce bitmap size
            Bitmap bm565 = Bitmap.createBitmap(resize.getWidth(),
                    resize.getHeight(), Bitmap.Config.RGB_565);
            Canvas c = new Canvas(bm565);
            c.drawBitmap(resize, 0, 0, null);
            c.setBitmap(null);

            // recycle useless bitmaps
            resize.recycle();
            screenBitmap.recycle();
            screenBitmap = bm565;

            byte[] res = null;
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream(
                        screenBitmap.getByteCount());
                screenBitmap.compress(Bitmap.CompressFormat.PNG, 0, out);
                res = out.toByteArray();
                out.close();
            } catch (Exception e) {
                // TODO: handle exception
            }
            return res;
        }

        private float getDegreesForRotation(int value) {
            switch (value) {
            case Surface.ROTATION_90:
                return 360f - 90f;
            case Surface.ROTATION_180:
                return 360f - 180f;
            case Surface.ROTATION_270:
                return 360f - 270f;
            }
            return 0f;
        }

        public void takePhotoNG() {
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
                            final OutputStream os = new FileOutputStream(
                                    file_name);
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
    }

    public static final int LOCATION_TRACK_OFF = 0;
    public static final int LOCATION_TRACK_SENSORS_ONLY = 1;
    public static final int LOCATION_TRACK_BATTERY_SAVING = 2;
    public static final int LOCATION_TRACK_HIGH_ACCURACY = 3;

    public interface LocationReportCallback {
        public void reportLocation(RemoteLocation loc);

        public void reportLocationTrackStatus(boolean bRunning);
    }

    public class RemoteLocation {
        public double mLatitude;
        public double mLongitude;
        public long mTime;
        public float mSpeed;

        public RemoteLocation(Location loc) {
            mLatitude = loc.getLatitude();
            mLongitude = loc.getLongitude();
            mTime = loc.getTime();
            mSpeed = loc.getSpeed();
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("<location>\n");
            sb.append("<longitude>\n");
            sb.append("" + mLatitude);
            sb.append("\n<longitude/>\n");

            sb.append("<longitude>\n");
            sb.append("" + mLongitude);
            sb.append("\n<longitude/>\n");

            sb.append("<latitude>\n");
            sb.append("" + mLatitude);
            sb.append("\n<latitude/>\n");

            sb.append("<time>\n");
            sb.append("" + mTime);
            sb.append("\n<time/>\n");

            sb.append("<speed>\n");
            sb.append("" + mSpeed);
            sb.append("\n<speed/>\n");

            sb.append("<location/>\n");

            return sb.toString();
        }
    }

    public boolean startTrackLocation(int mode, long minTime, int minDistance,
            LocationReportCallback callback) {
        return mLocationTrack.startTrackLocation(mode, minTime, minDistance,
                callback);
    }

    public void stopTrackLocation() {
        mLocationTrack.stopTrackLocation();
    }

    public RemoteLocation[] getHistoryLocations() {
        return mLocationTrack.getHistoryLocs();
    }

    // @todo: 4.4 has changed a lot.
    public void start() {
    }

    private class RemoteLocationTrack implements LocationListener {
        int mMode;
        long mMinTime;
        int mMinDistance;
        LocationReportCallback mCallback;
        LocationManager mLocationManager = null;

        private final static int LOC_HISTORYSIZE = 10000;

        private LinkedList<RemoteLocation> mRemoteLocs;

        private boolean setLocationTrackMode(int mode) {
            int defMode = Settings.Secure.LOCATION_MODE_OFF;
            ContentResolver resolver = mContext.getContentResolver();
            mMode = Settings.Secure.getInt(resolver,
                    Settings.Secure.LOCATION_MODE, defMode);
            if (defMode == mode)
                return true;
            boolean bSuc = Settings.Secure.putInt(resolver,
                    Settings.Secure.LOCATION_MODE, mode);
            LogManager.local(TAG, "set location mode to " + mode + " " + bSuc);
            if (bSuc)
                mMode = mode;
            return bSuc;
        }

        public boolean startTrackLocation(int mode, long minTime,
                int minDistance, LocationReportCallback callback) {
            if (mLocationManager == null) {
                mLocationManager = (LocationManager) mContext
                        .getSystemService(Context.LOCATION_SERVICE);
            }
            if (mRemoteLocs == null)
                mRemoteLocs = new LinkedList<RemoteLocation>();
            if (setLocationTrackMode(mode) == false)
                return false;
            LogManager.local(TAG, "startLocationTrack using mode " + mode);

            mCallback = callback;
            mMinTime = minTime;
            mMinDistance = minDistance;
            try {

                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                criteria.setAltitudeRequired(false);
                criteria.setBearingRequired(false);
                criteria.setCostAllowed(true);
                criteria.setPowerRequirement(Criteria.POWER_LOW);

                mLocationManager.requestLocationUpdates(minTime, minDistance,
                        criteria, this, null);
                LogManager.local(TAG, "requestLocationUpdates succeed ");
                return true;
            } catch (Exception e) {
                LogManager.local(TAG,
                        "requestLocationUpdates fail " + e.getMessage());
                return false;
            }
        }

        public void stopTrackLocation() {
            if (mLocationManager != null) {
                mLocationManager.removeUpdates(this);
            }
            setLocationTrackMode(LOCATION_TRACK_OFF);
            LogManager.local(TAG, "stopLocationTrack");
            mLocationManager = null;
        }

        // each Object is a RemoteLocation
        public RemoteLocation[] getHistoryLocs() {
            synchronized (this) {
                if (mRemoteLocs == null || mRemoteLocs.size() == 0)
                    return null;
                RemoteLocation[] ret = new RemoteLocation[mRemoteLocs.size()];
                mRemoteLocs.toArray(ret);
                return ret;
            }

        }

        private void addHistoryLoc(RemoteLocation newLoc) {
            synchronized (this) {
                if (mRemoteLocs.size() >= LOC_HISTORYSIZE) {
                    mRemoteLocs.pollFirst();
                }
                mRemoteLocs.addLast(newLoc);
            }
        }

        @Override
        public void onLocationChanged(Location location) {
            RemoteLocation remoteLoc = new RemoteLocation(location);
            addHistoryLoc(remoteLoc);
            if (mCallback != null)
                mCallback.reportLocation(remoteLoc);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub

        }

    }

    public byte[] takeScreenshot(final Handler handler) {
        LogManager.local(TAG, "takeScreenshot");
        return mScreenshot.takeScreenshot();

    }

    public void lockScreen() {
        try {
            WindowManagerGlobal.getWindowManagerService().lockNow(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * public void startCamera() { Intent intent = new
     * Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA); try {
     * mContext.startActivity(intent); } catch (Exception e) {
     * e.printStackTrace(); } }
     */

    /*
     * public void sendNotify(String title, String text) { NotificationManager
     * NotifyManager = (NotificationManager) mContext
     * .getSystemService(Context.NOTIFICATION_SERVICE); Notification n = new
     * Notification.Builder(mContext) .setContentTitle(title)
     * .setContentText(text) .setSmallIcon(
     * mContext.getResources().getIdentifier("icon", "drawable",
     * "com.zm.epad")).build();
     * 
     * NotifyManager.notify(0, n); }
     */

    // This 2 functions need to move out from this class to a FileUtil class.
    /*
     * public void saveFileAsImage(File file) { ContentValues values = new
     * ContentValues(); ContentResolver resolver =
     * mContext.getContentResolver();
     * 
     * try { long time = System.currentTimeMillis(); String fileName =
     * String.valueOf(time) + "_" + file.getName(); File dir = new File(
     * Environment
     * .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
     * "Recieved"); String filePath = new File(dir, fileName).getAbsolutePath();
     * LogManager.local(TAG, "file path: " + filePath);
     * 
     * dir.mkdir();
     * 
     * values.put(MediaStore.Images.ImageColumns.DATA, filePath);
     * values.put(MediaStore.Images.ImageColumns.TITLE, fileName);
     * values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, fileName);
     * values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, time);
     * values.put(MediaStore.Images.ImageColumns.DATE_ADDED, time / 1000);
     * values.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, time / 1000);
     * values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpg"); Uri
     * uri = resolver.insert( MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
     * values); LogManager.local(TAG, "file Uri: " + uri.toString());
     * 
     * FileInputStream in = new FileInputStream(file); OutputStream out =
     * resolver.openOutputStream(uri); byte[] buffer = new byte[(int)
     * file.length()]; in.read(buffer); out.write(buffer); out.flush();
     * out.close(); in.close();
     * 
     * values.clear(); long length = new File(filePath).length();
     * values.put(MediaStore.Images.ImageColumns.SIZE, length);
     * resolver.update(uri, values, null, null);
     * 
     * LogManager.local(TAG, "file length: " + String.valueOf(length)); } catch
     * (Exception e) { e.printStackTrace(); } }
     */

    // can't start camera in system server
    /*
     * private void takePhotoNG() {
     * 
     * }
     */

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

        // 1st check wifi
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
        } else {
            // add if support mobile network
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

    public boolean isScreenOn() {
        PowerManager pm = (PowerManager) mContext
                .getSystemService(Context.POWER_SERVICE);
        return pm.isScreenOn();
    }

    public synchronized void toggleScreen(final boolean on) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                WindowManager wm = (WindowManager) mContext
                        .getSystemService(Context.WINDOW_SERVICE);
                if (!on && mKeyguard == null) {
                    // show window to mask all event.
                    final WindowManager.LayoutParams attrs = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG,
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                    | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                            PixelFormat.TRANSLUCENT);
                    if (ActivityManager.isHighEndGfx()) {
                        attrs.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
                        attrs.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_FORCE_HARDWARE_ACCELERATED;
                    }
                    attrs.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR;
                    attrs.gravity = Gravity.CENTER;
                    attrs.packageName = mContext.getPackageName();
                    attrs.setTitle("zm_keyguard");
                    attrs.windowAnimations = com.android.internal.R.style.Animation_Dialog;
                    mKeyguard = View.inflate(mContext, R.layout.keyguard, null);

                    attrs.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;
                    wm.addView(mKeyguard, attrs);
                    setSystemUi();
                    mKeyguard.setFocusableInTouchMode(true);
                    mKeyguard.requestFocus();
                    mKeyguard
                            .setOnFocusChangeListener(new View.OnFocusChangeListener() {
                                @Override
                                public void onFocusChange(View v,
                                        boolean hasFocus) {
                                    Log.v(TAG, "zm_keyguad "
                                            + (hasFocus ? "gain" : "lose")
                                            + " focus");
                                    if (hasFocus)
                                        return;
                                    final ActivityManager am = (ActivityManager) mContext
                                            .getSystemService(Context.ACTIVITY_SERVICE);
                                    final int taskId = getMyTaskId();
                                    if (taskId >= 0) {
                                        am.moveTaskToFront(taskId, 0);
                                    }
                                }
                            });
                    mKeyguard.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            resetSystemUi();
                            return true;
                        }
                    });
                    mKeyguard.setOnKeyListener(new View.OnKeyListener() {
                        @Override
                        public boolean onKey(View v, int keyCode, KeyEvent event) {
                            Log.v(TAG,
                                    KeyEvent.keyCodeToString(keyCode)
                                            + "("
                                            + keyCode
                                            + ") "
                                            + KeyEvent.actionToString(event
                                                    .getAction()));
                            resetSystemUi();
                            if (keyCode == KeyEvent.KEYCODE_SYM) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            ActivityManagerNative.getDefault()
                                                    .closeSystemDialogs(null);
                                        } catch (RemoteException e) {
                                        }
                                    }
                                });
                            } else {
                                // if (keyCode == KeyEvent.KEYCODE_EXPLORER ||
                                // keyCode == KeyEvent.KEYCODE_ENVELOPE ||
                                // keyCode == KeyEvent.KEYCODE_CONTACTS ||
                                // keyCode == KeyEvent.KEYCODE_CALENDAR ||
                                // keyCode == KeyEvent.KEYCODE_MUSIC ||
                                // keyCode == KeyEvent.KEYCODE_CALCULATOR) {}
                                // keyCode == KeyEvent.KEYCODE_HOME
                                final ActivityManager am = (ActivityManager) mContext
                                        .getSystemService(Context.ACTIVITY_SERVICE);
                                final int taskId = getMyTaskId();
                                if (taskId >= 0) {
                                    am.moveTaskToFront(taskId, 0);
                                }
                            }
                            return true;
                        }
                    });
                } else if (on && mKeyguard != null) {
                    wm.removeView(mKeyguard);
                    mKeyguard = null;
                }
            }
        });
    }

    private void setSystemUi() {
        mKeyguard.setSystemUiVisibility(View.STATUS_BAR_DISABLE_HOME
                | View.STATUS_BAR_DISABLE_BACK | View.STATUS_BAR_DISABLE_RECENT
                | View.STATUS_BAR_DISABLE_EXPAND
                | View.STATUS_BAR_DISABLE_SEARCH
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    private Runnable mKeepFullscreen = new Runnable() {
        @Override
        public void run() {
            if (mKeyguard == null)
                return;
            setSystemUi();
        }
    };

    private void resetSystemUi() {
        mHandler.removeCallbacks(mKeepFullscreen);
        mHandler.postDelayed(mKeepFullscreen, 100);
    }

    private int getMyTaskId() {
        ActivityManager am = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(30);
        for (ActivityManager.RunningTaskInfo pi : list) {
            if (pi.baseActivity.getPackageName().equals(
                    mContext.getPackageName())) {
                return pi.id;
            }
        }
        return -1;
    }

    public void acquireWakeLock(int levelAndFlags, String tag) {
        mPowerMgr.acquireWakeLock(levelAndFlags, tag);
    }

    public void releaseWakeLock(String tag) {
        mPowerMgr.releaseWakeLock(tag);
    }

    private class RemotePowerManager {
        PowerManager mPm;
        HashMap<String, PowerManager.WakeLock> mWakeLockMap = new HashMap<String, PowerManager.WakeLock>();

        public RemotePowerManager() {
            mPm = (PowerManager) mContext
                    .getSystemService(Context.POWER_SERVICE);
        }

        public void acquireWakeLock(int levelAndFlags, String tag) {
            LogManager.local(TAG, "acquireWakeLock:" + tag);
            PowerManager.WakeLock wl = mWakeLockMap.get(tag);
            if (wl == null) {
                wl = mPm.newWakeLock(levelAndFlags, tag);
                wl.setReferenceCounted(true);
                mWakeLockMap.put(tag, wl);
            }
            wl.acquire();
        }

        public void releaseWakeLock(String tag) {
            LogManager.local(TAG, "releaseWakeLock:" + tag);
            PowerManager.WakeLock wl = mWakeLockMap.get(tag);
            if (wl != null) {
                wl.release();
                mWakeLockMap.remove(tag);
            }
        }

        public void stop() {
            Set<String> keys = mWakeLockMap.keySet();
            for (String k : keys) {
                PowerManager.WakeLock wl = mWakeLockMap.get(k);
                while (wl.isHeld()) {
                    wl.release();
                }
            }
            mWakeLockMap.clear();
        }
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