package com.zm.epad.plugins;

import android.content.Context;
import android.os.Looper;
import android.os.Message;

import com.zm.epad.core.SubSystemFacade;

import java.util.concurrent.ExecutorService;

/**
 * RemoteDesktopManager is only SmartShareManager's sub-function.
 * @author tkboy
 *
 */
public class SmartShareManager {
    private final Context mContext;
    private RemoteDesktopManager mRemoteDesktopManager;
    private Message mDesktopShareNotify;
    private ExecutorService mThreadPool;

    public SmartShareManager(Context context) {
        mContext = context;
        mRemoteDesktopManager = new RemoteDesktopManager(mContext);
    }

    private RemoteDesktopManager.Listener mmRemoteDesktopListener = new RemoteDesktopManager.Listener() {

        @Override
        public void onServerCreated(String iface) {
            Message notify = Message.obtain(mDesktopShareNotify);
            notify.arg1 = SubSystemFacade.MSG_DESKTOP_SERVER_CREATED_OK;
            notify.obj = iface;
            notify.sendToTarget();
        }

        @Override
        public void onServerError(int err) {
            Message notify = Message.obtain(mDesktopShareNotify);
            switch(err) {
                case RemoteDesktopManager.RD_NOT_SUPPORT:
                    notify.arg1 = SubSystemFacade.MSG_DESKTOP_NOT_SUPPORT;
                    break;
                case RemoteDesktopManager.RD_IN_USE:
                    notify.arg1 = SubSystemFacade.MSG_DESKTOP_IN_USE;
                    break;
                case RemoteDesktopManager.RD_NO_NETWORK:
                    notify.arg1 = SubSystemFacade.MSG_DESKTOP_NO_NETWORK;
                    break;
                case RemoteDesktopManager.RD_DISPLAY_CREATE_FAILED:
                    notify.arg1 = SubSystemFacade.MSG_DESKTOP_DISPLAY_CREATE_FAILED;
                    break;
                case RemoteDesktopManager.RD_SERVER_CREATE_FAILED:
                    notify.arg1 = SubSystemFacade.MSG_DESKTOP_SERVER_CREATE_FAILED;
                    break;
            }
            if (notify.arg1 > 0) notify.sendToTarget();
        }

        @Override
        public void onServerStarted() {
            Message notify = Message.obtain(mDesktopShareNotify);
            notify.arg1 = SubSystemFacade.MSG_DESKTOP_RUNNING_OK;
            notify.sendToTarget();
        }

        @Override
        public void onServerStopped() {
            Message notify = Message.obtain(mDesktopShareNotify);
            notify.arg1 = SubSystemFacade.MSG_DESKTOP_STOPPED;
            notify.sendToTarget();
        }
    };

    public void startDesktopShare(Message notify) {
        mDesktopShareNotify = notify;
        mThreadPool.execute(mRunDesktop);
    }

    // for future use.
    private Looper mDesktopLooper = null;

    Runnable mRunDesktop = new Runnable() {
        @Override
        public void run() {
            if (mDesktopLooper != null) {
                Message notify = Message.obtain(mDesktopShareNotify);
                notify.arg1 = SubSystemFacade.MSG_DESKTOP_IN_USE;
                notify.sendToTarget();
                return;
            }
            Looper.prepare();
            mDesktopLooper = Looper.myLooper();
            mRemoteDesktopManager.startRemoteDesktop(mmRemoteDesktopListener);
            Looper.loop();
            mDesktopLooper = null;
        }
    };

    public void stopDesktopShare() {
        if (mDesktopLooper == null) return;
        mRemoteDesktopManager.stopRemoteDesktop();
        mDesktopLooper.quitSafely();
        mDesktopLooper = null;
    }

    public void setThreadPool(ExecutorService threadPool) {
        mThreadPool = threadPool;
    }
}
