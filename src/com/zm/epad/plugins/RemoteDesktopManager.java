package com.zm.epad.plugins;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.apache.http.conn.util.InetAddressUtils;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
//import android.media.RemoteDesktop;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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

    private Object mRemoteDesktop;
    private VirtualDisplay mDisplay;
    private final static String mDisplayName = "zm_display";
    private final DisplayParam mDisplayParam[] = new DisplayParam[3];
    private String mIface;

    private static final int DEFAULT_CONTROL_PORT = 57236;

    private Listener mListener;

    private final DisplayManager.DisplayListener mDisplayListener =
            new DisplayManager.DisplayListener() {
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

    private static class DisplayParam {
        public int mWidth;
        public int mHeight;
        public int mDpi;
        DisplayParam(int w, int h, int dpi) {
            mWidth = w;
            mHeight = h;
            mDpi = dpi;
        }
    }

    public final static int RD_NOT_SUPPORT = 1;
    public final static int RD_IN_USE = 2;
    public final static int RD_NO_NETWORK = 3;
    public final static int RD_SERVER_CREATE_FAILED = 4;
    public final static int RD_DISPLAY_CREATE_FAILED = 5;

    /**
     * Listener invoked when the remote display connection changes state.
     */
    public interface Listener {
        void onServerCreated(String iface);
        void onServerError(int err);
        void onServerStarted();
        void onServerStopped();
    }

    private static Class<?> sRemoteDesktop;
    private static Class<?> sRemoteDesktopListener;
    private static Method sListen;
    private static Method sDispose;
    private static Class<?>[] sRemoteDesktopListenerArray;

    static {
        try {
            sRemoteDesktop = Class.forName("android.media.RemoteDesktop");
            sRemoteDesktopListener = Class.forName("android.media.RemoteDesktop$Listener");
            sRemoteDesktopListenerArray = new Class<?>[1];
            sRemoteDesktopListenerArray[0] = sRemoteDesktopListener;
            sListen = sRemoteDesktop.getMethod("listen", String.class,
                    sRemoteDesktopListener, Handler.class);
            sDispose = sRemoteDesktop.getMethod("dispose");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private static Object listenRtsp(String iface, Object listener, Handler mHandler) {
        try {
            return sListen.invoke(sRemoteDesktop, iface, listener, mHandler);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void disposeRtsp() {
        try {
            sDispose.invoke(mRemoteDesktop);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static boolean support() {
        return sRemoteDesktop != null;
    }

    public RemoteDesktopManager(Context context) {
        mContext = context;
        mHandler = new Handler();
        mDisplayManager = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        if (sRemoteDesktop != null) {
            Log.i(TAG, "This Android version support Remote Desktop");
            mDisplayManager.registerDisplayListener(mDisplayListener, mHandler);
            updateDisplayParams();
        } else {
            Log.e(TAG, "This Android version don't support Remote Desktop");
        }
    }

    private void updateDisplayParams() {
        // Get Current Display width and size
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager)
                mContext.getSystemService(Context.WINDOW_SERVICE);
        Display d = windowManager.getDefaultDisplay();
        d.getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        final int densityDpi = metrics.densityDpi;
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17) {
            try {
                screenWidth = (Integer)
                        Display.class.getMethod("getRawWidth").invoke(d);
                screenHeight = (Integer)
                        Display.class.getMethod("getRawHeight").invoke(d);
            } catch (Exception ignored) {
            }
        } else if (Build.VERSION.SDK_INT >= 17) {
            try {
                android.graphics.Point realSize = new android.graphics.Point();
                Display.class.getMethod("getRealSize",
                android.graphics.Point.class).invoke(d,realSize);
                screenWidth = realSize.x;
                screenHeight = realSize.y;
            } catch (Exception e) {
            }
        }
        if (screenWidth < screenHeight) {
            int temp = screenWidth;
            screenWidth = screenHeight;
            screenHeight = temp;
        }
        // emun available current display width and size.
        mDisplayParam[0] = new DisplayParam(screenWidth, screenHeight, densityDpi);
        mDisplayParam[1] = new DisplayParam((screenWidth * 2) / 3,
            (screenHeight * 2) / 3, (densityDpi * 2) / 3);
        mDisplayParam[2] = new DisplayParam((screenWidth * 2) / 5,
            (screenHeight * 2) / 5, (densityDpi * 2) / 5);
    }

    public void startRemoteDesktop(Listener listener) {
        if (sRemoteDesktop == null) {
            notityError(listener, RD_NOT_SUPPORT);
            return;
        }
        if (mListener != null) {
            notityError(listener, RD_IN_USE);
            return;
        }
        mListener = listener;
        String iface = getLocalHostIp();
        if (iface != null && !iface.isEmpty()) {
            iface += ":" + DEFAULT_CONTROL_PORT;
            mIface = iface;
        }
        if (iface != null && !iface.isEmpty()) {
            startRtspServer(iface);
        } else {
            if (mListener != null) {
                notityError(RD_NO_NETWORK);
                mListener = null;
            }
        }
    }

    public void stopRemoteDesktop() {
        if (mRemoteDesktop != null) {
            Log.i(TAG, "Release Rtsp Server");
            disposeRtsp();
        }
    }

    private String getUrl() {
        return mIface == null ? null : "rtsp://" + mIface + "/";
    }

    private String getActiveNetworkType() {
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
        //mRemoteDesktop = RemoteDesktop.listen(iface, mRemoteDesktopListener, mHandler);
        mRemoteDesktop = listenRtsp(iface, mRemoteDesktopListener, mHandler);
        if (mRemoteDesktop == null) {
            if (mListener != null) notityError(RD_SERVER_CREATE_FAILED);
            mListener = null;
        } else {
            if (mListener != null) mListener.onServerCreated(getUrl());
        }
    }

    private class RemoteDesktopListener implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            if (method.getName().equals("onDesktopConnected")) {
                final Surface surface = (Surface) args[0];
                int width = (Integer) args[1];
                int height = (Integer) args[2];
                int flags = (Integer) args[3];
                int session = (Integer) args[4];
                Log.i(TAG, "Rtsp Server: recieved a client: width(" + width +
                        ") height(" + height + ") flags(" + flags + ") session("
                        + session + ")" );
                createDisplay(surface, width, height);
            } else if (method.getName().equals("onDesktopDisconnected")) {
                Log.i(TAG, "Rtsp Server: client disconnected");
                release(true);
            } else if (method.getName().equals("onDesktopError")) {
                int error = (Integer) args[0];
                Log.i(TAG, "Rtsp Server: client error: " + error);
            }
            return null;
        }
    }

    private Object mRemoteDesktopListener = Proxy.newProxyInstance(null,
            sRemoteDesktopListenerArray, new RemoteDesktopListener());

//    private final RemoteDesktop.Listener mRemoteDesktopListener =
//        new RemoteDesktop.Listener() {
//        @Override public void onDesktopConnected(final Surface surface,
//                int width, int height, int flags, int session) {
//            Log.i(TAG, "Rtsp Server: recieved a client: width(" + width +
//                    ") height(" + height + ") flags(" + flags + ") session("
//                    + session + ")" );
//            createDisplay(surface, width, height);
//        }
//
//        @Override public void onDesktopDisconnected() {
//            Log.i(TAG, "Rtsp Server: client disconnected");
//            release();
//        }
//
//        @Override public void onDesktopError(int error) {
//            Log.i(TAG, "Rtsp Server: client error: " + error);
//            //release();
//        }
//    };

    private void createDisplay(final Surface surface, int width, int height) {
        mHandler.post(new Runnable() {
            @Override public void run() {
                // a surface can comes from window or media service.
                // It pass to Display, and movie can be draw on it.
                // In our case, it should comes from rtsp server.
                final DisplayParam dp = mDisplayParam[1];
                mDisplay = mDisplayManager.createVirtualDisplay(mDisplayName,
                        dp.mWidth, dp.mHeight, dp.mDpi, surface,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC);
                if (mDisplay == null) {
                    if (mListener != null) mListener.onServerError(RD_DISPLAY_CREATE_FAILED);
                    release(true);
                } else {
                    if (mListener != null) mListener.onServerStarted();
                }
            }
        });
    }

    private void release(boolean aysn) {
        if (mRemoteDesktop == null) return;
        Runnable r = new Runnable() {
            @Override public void run() {
                boolean callbackstop = false;
                if (mRemoteDesktop != null) {
                    Log.i(TAG, "Release Rtsp Server");
                    //mRemoteDesktop.dispose();
                    disposeRtsp();
                    mRemoteDesktop = null;
                    callbackstop = true;
                }
                if (mDisplay != null) {
                    mDisplay.release();
                    mDisplay = null;
                    callbackstop = true;
                }
                if (callbackstop && mListener != null) {
                    final Listener listener = mListener;
                    mHandler.postDelayed(new Runnable() {
                        @Override public void run() {
                            listener.onServerStopped();
                        }
                    }, 200);
                }
                mIface = null;
                mListener = null;
            }
        };
        if (aysn) {
            mHandler.post(r);
        } else {
            r.run();
        }
    }

    private void notityError(final int error) {
        notityError(mListener, error);
    }

    private void notityError(final Listener listener, final int error) {
        mHandler.post(new Runnable() {
            @Override public void run() {
                if (listener != null) listener.onServerError(error);
            }
        });
    }
}
