package com.zm.epad.plugins.backup;

import android.os.Handler;
import android.os.RemoteException;

class Notifier {
    private final Handler mMainHandler;
    private final IZmObserver mObserver;

    Notifier(Handler handler, IZmObserver observer) {
        mMainHandler = handler;
        mObserver = observer;
    }

    void notifyStart(final String path) {
        if (mObserver == null) return;
        mMainHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    mObserver.onStart(path);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void notifyRecordStart(final String name) {
        if (mObserver == null) return;
        mMainHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    mObserver.onRecordStart(name);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void notifyRecordProgress(final String name) {
        if (mObserver == null) return;
        mMainHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    mObserver.onRecordProgress(name);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void notifyRecordEnd(final String name) {
        if (mObserver == null) return;
        mMainHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    mObserver.onRecordEnd(name);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void notifyRecordTimeout(final String name) {
        if (mObserver == null) return;
        mMainHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    mObserver.onRecordTimeout(name);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void notifyEnd(final String path, final int system, final int installed,
		final int file) {
        if (mObserver == null) return;
        mMainHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    mObserver.onEnd(path, system, installed, file);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
