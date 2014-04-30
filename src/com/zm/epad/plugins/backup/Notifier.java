package com.zm.epad.plugins.backup;

import java.util.concurrent.CountDownLatch;

import android.os.Handler;
import android.os.RemoteException;
import android.util.ArrayMap;

public class Notifier {
    private final Handler mMainHandler;
    private final IZmObserver mObserver;
    private final CountDownLatch mSignal;

    private final ArrayMap<String, Integer> mStats;

    Notifier(Handler handler, IZmObserver observer) {
        mMainHandler = handler;
        mObserver = observer;
        mSignal = null;
        mStats = null;
    }

    Notifier(Handler handler, IZmObserver observer, int count) {
        mMainHandler = handler;
        mObserver = observer;
        mSignal = new CountDownLatch(count);
        mStats = new ArrayMap<String, Integer>();
    }

    public void setStat(String name, int count) {
        mStats.put(name, count);
    }

    public void notifyStart(final String path) {
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

    public void notifyRecordStart(final String name) {
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

    public void notifyRecordProgress(final String name) {
        if (mObserver == null) return;
        mMainHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    mObserver.onRecordProgress(name, -1);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void notifyRecordProgress(final String name, final int index) {
        if (mObserver == null) return;
        mMainHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    mObserver.onRecordProgress(name, index);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void notifyRecordEnd(final String name) {
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

    public void notifyRecordTimeout(final String name) {
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

    public void notifyEnd(final String path, final int system, final int installed,
            final int file) {
        if (mObserver == null) return;
        mMainHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    String[] keyset = new String[] {"system", "installed", "files" };
                    int[] valset = new int[] { system, installed, file };
                    mObserver.onEnd(path, keyset, valset, false);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void notifyEnd(final String path, final int count) {
        if (mObserver == null) return;
        if (mSignal != null) {
            setStat(path, count);
            mSignal.countDown();
        }
        if (mSignal.getCount() > 0) return;
        mMainHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    Integer[] counts = mStats.values().toArray(new Integer[0]);
                    int[] outCounts = new int[counts.length];
                    for (int i = 0; i < outCounts.length; i++) {
                        outCounts[i] = counts[i];
                    }
                    mObserver.onEnd(path, mStats.keySet().toArray(new String[4]), outCounts, true);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
