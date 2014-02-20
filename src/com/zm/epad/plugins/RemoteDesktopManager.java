package com.zm.epad.plugins;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.apache.http.conn.util.InetAddressUtils;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.RemoteDisplay;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class RemoteDesktopManager {
    public static final String TAG = "RemoteDesktopManager";

    private final Context mContext;
    private final Handler mHandler;
    private final DisplayManager mDisplayManager;

    RemoteDisplay mRemoteDisplay;
    private VirtualDisplay mDisplay;
    private final static String mDisplayName = "zm_display";
    private final DisplayParam mDisplayParam[] = new DisplayParam[3];
    private String mIface;

    private static final int DEFAULT_CONTROL_PORT = 57236;

//    static {
//        System.loadLibrary("zm_jni");
//    }

    private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onDisplayChanged(int displayId) {
            // TODO Auto-generated method stub
        }
    };

    public static class DisplayParam {
        public int mWidth;
        public int mHeight;
        public int mDpi;
        DisplayParam(int w, int h, int dpi) {
        mWidth = w;
        mHeight = h;
        mDpi = dpi;
        }
    }

    public RemoteDesktopManager(Context context) {
        mContext = context;
        mHandler = new Handler();
        mDisplayManager = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        mDisplayManager.registerDisplayListener(mDisplayListener, mHandler);
        updateDisplayParams();
    }

    private void updateDisplayParams() {
    DisplayMetrics metrics = new DisplayMetrics();
    WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    Display d = windowManager.getDefaultDisplay();
    d.getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        final int densityDpi = metrics.densityDpi;
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17) {
            try {
                screenWidth = (Integer) Display.class.getMethod("getRawWidth").invoke(d);
                screenHeight = (Integer) Display.class.getMethod("getRawHeight").invoke(d);
            } catch (Exception ignored) {
            }
        } else if (Build.VERSION.SDK_INT >= 17) {
            try {
                android.graphics.Point realSize = new android.graphics.Point();
                Display.class.getMethod("getRealSize", android.graphics.Point.class).invoke(d,realSize);
                screenWidth = realSize.x;
                screenHeight = realSize.y;
            } catch (Exception ignored) {
            }
        }
        mDisplayParam[0] = new DisplayParam(screenWidth, screenHeight, densityDpi);
        mDisplayParam[1] = new DisplayParam((screenWidth * 2) / 3, (screenHeight * 2) / 3, (densityDpi * 2) / 3);
        mDisplayParam[2] = new DisplayParam((screenWidth * 2) / 5, (screenHeight * 2) / 5, (densityDpi * 2) / 5);
    }

    public DisplayParam[] getDisplayParams() {
    return mDisplayParam;
    }

    public void startRemoteDesktop() {
        String iface = getLocalHostIp();
        if (iface != null && !iface.isEmpty()) {
            iface += ":" + DEFAULT_CONTROL_PORT;
            mIface = iface;
        }
        if (iface != null && !iface.isEmpty()) {
            startRtspServer(iface);
        }
    }

    public void stopRemoteDesktop() {
        if (mRemoteDisplay != null) {
            Log.i(TAG, "Release Rtsp Server");
            mRemoteDisplay = null;
        }
    }

    public String getUrl() {
        return mIface == null ? null : "rtsp://" + mIface + "/";
    }

    public String getActiveNetworkType() {
        ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ani = cm.getActiveNetworkInfo();
        if (ani != null) {
            return ConnectivityManager.getNetworkTypeName(ani.getType());
        }
        return null;
    }

    private String getLocalHostIp() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface nif = en.nextElement();
                if (!nif.supportsMulticast()) continue;
                Enumeration<InetAddress> inet = nif.getInetAddresses();
                while (inet.hasMoreElements()) {
                    InetAddress ip = inet.nextElement();
                    if (!ip.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ip.getHostAddress())) {
                        return ip.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "Get host IP address failed");
            e.printStackTrace();
        }
        return null;
    }

    private void startRtspServer(String iface) {
        Log.i(TAG, "Rtsp Server listen at: " + iface);
        mRemoteDisplay = RemoteDisplay.listen(iface, new RemoteDisplay.Listener() {
            @Override
            public void onDisplayConnected(final Surface surface,
                    int width, int height, int flags, int session) {
                Log.i(TAG, "Rtsp Server: recieved a client");
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // a surface can comes from window or media service.
                        // It pass to Display, and movie can be draw on it.
                        // In out case, it should comes from rtsp server.
                        final DisplayParam dp = mDisplayParam[0];
                        mDisplay = mDisplayManager.createVirtualDisplay(mDisplayName,
                                dp.mWidth, dp.mHeight, dp.mDpi, surface,
                                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC);
                    }
                });
            }

            @Override
            public void onDisplayDisconnected() {
                Log.i(TAG, "Rtsp Server: client disconnected");
            }

            @Override
            public void onDisplayError(int error) {
                Log.i(TAG, "Rtsp Server: client error");
            }
        }, mHandler);
    }
}
