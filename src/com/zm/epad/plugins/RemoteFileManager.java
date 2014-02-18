package com.zm.epad.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import com.zm.epad.core.LogManager;
import com.zm.epad.core.XmppClient;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

public class RemoteFileManager {
    private static final String TAG = "RemoteFileManager";

    private static RemoteFileManager sInstance = null;

    Context mContext;
    XmppClient mXmppClient;

    private static final int RUNNING_TASK_MAX = 10;
    private List<FileTransferTask> mRunningTask = new ArrayList<FileTransferTask>();
    private List<FileTransferTask> mPendingTask = new ArrayList<FileTransferTask>();

    public static RemoteFileManager getInstance(Context context, XmppClient Xmpp) {
        if (sInstance == null) {
            sInstance = new RemoteFileManager(context, Xmpp);
        }

        return sInstance;
    }

    public static RemoteFileManager getInstance() {
        LogManager.local(TAG, "getInstance:" + sInstance == null ? "null"
                : "OK");
        return sInstance;
    }

    public static void release() {
        LogManager.local(TAG, "release");
        if(sInstance != null){
            sInstance.cancelAllPendingTask();
        }
        sInstance = null;
    }

    private RemoteFileManager(Context context, XmppClient Xmpp) {
        mContext = context;
        mXmppClient = Xmpp;
    }

    public FileDownloadTask getFileDownloadTask(String url,
            FileTransferCallback callback) {
        return new FileDownloadTask(url, callback);
    }

    public FileUploadTask getFileUploadTask(String url, String filePath,
            String fileName, Bundle info, FileTransferCallback callback) {
        return new FileUploadTask(url, callback, filePath, fileName, info);
    }

    public ScreenshotTask getScreenshotTask(String url, Bundle info,
            FileTransferCallback callback) {
        return new ScreenshotTask(url, callback, info);
    }

    public List<FileTransferTask> getRunningTask() {
        return mRunningTask;
    }

    public List<FileTransferTask> getPendingTask() {
        return mPendingTask;
    }

    public void cancelAllPendingTask() {
        synchronized (mPendingTask) {
            for (FileTransferTask t : mPendingTask) {
                t.cancel();
            }
        }
    }

    private boolean addTask(FileTransferTask task) {
        boolean ret = true;

        if (mRunningTask.size() < RUNNING_TASK_MAX) {
            synchronized (mRunningTask) {
                mRunningTask.add(task);
            }
        } else {
            synchronized (mPendingTask) {
                mPendingTask.add(task);
            }
            ret = false;
        }

        return ret;
    }

    private void removeTask(FileTransferTask task) {

        synchronized (mRunningTask) {
            mRunningTask.remove(task);
        }

        synchronized (mPendingTask) {
            if (mPendingTask.size() > 0) {
                FileTransferTask newTask = mPendingTask.get(0);
                mPendingTask.remove(newTask);
                newTask.start();
            }
        }
    }

    private void removePendingTask(FileTransferTask task) {
        synchronized (mPendingTask) {
            mPendingTask.remove(task);
        }
    }

    public interface FileTransferCallback {
        void onDone(FileTransferTask task);

        void onCancel(FileTransferTask task);
    }

    public abstract class FileTransferTask extends Thread {
        public static final int IDLE = 0;
        public static final int RUNNING = 1;
        public static final int PENDING = 2;
        public static final int DONE = 3;

        protected Object mResult = null;
        protected int mStatus = IDLE;
        protected String mUrl;
        protected FileTransferCallback mCallback;

        public FileTransferTask() {

        }

        public FileTransferTask(String url, FileTransferCallback callback) {
            mUrl = url;
            mCallback = callback;
        }

        public void setUrl(String url) {
            mUrl = url;
        }

        public void setCallback(FileTransferCallback cb) {
            mCallback = cb;
        }

        @Override
        public synchronized void start() {
            if (addTask(this)) {
                super.start();
            } else {
                mStatus = PENDING;
            }
        }

        public boolean cancel() {
            boolean ret = true;

            switch (mStatus) {
            case PENDING:
                removePendingTask(this);
                if (mCallback != null) {
                    mCallback.onCancel(this);
                }
                mStatus = IDLE;
                break;
            case IDLE:
            case RUNNING:
            case DONE:
            default:
                ret = false;
            }
            return ret;
        }

        @Override
        public void run() {
            mStatus = RUNNING;
            mResult = runForResult();
            mStatus = DONE;

            removeTask(this);

            if (mCallback != null) {
                mCallback.onDone(this);
            }
        }

        protected abstract Object runForResult();

        public Object getResult() {
            if (mStatus == DONE) {
                return mResult;
            }
            return null;
        }

        public int getStatus() {
            return mStatus;
        }
    }

    public class FileUploadTask extends FileTransferTask {

        protected String mFilePath;
        protected String mFileName;
        protected Bundle mInfo;

        public FileUploadTask() {
            super();
        }

        public FileUploadTask(String url, FileTransferCallback callback) {
            super(url, callback);
        }

        public FileUploadTask(String url, FileTransferCallback callback,
                String filePath, String fileName, Bundle info) {
            this(url, callback);
            mFilePath = filePath;
            mFileName = fileName;
            mInfo = info;
        }

        public void setFile(String filePath, String fileName) {
            mFilePath = filePath;
            mFileName = fileName;
        }

        public void setFileInfo(Bundle info) {
            mInfo = info;
        }

        @Override
        protected Object runForResult() {
            String fileName = null;
            try {
                fileName = mXmppClient.sendObject(getDate(), mFileName, mUrl,
                        mInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return fileName;
        }

        @Override
        public String getResult() {
            // TODO Auto-generated method stub
            return (String) super.getResult();
        }

        protected byte[] getDate() {
            try {
                File upFile = new File(mFilePath, mFileName);
                FileInputStream in = new FileInputStream(upFile);
                LogManager.local(TAG, "upload file size:" + upFile.length());
                byte[] buf = new byte[(int) upFile.length()];
                in.read(buf);
                in.close();
                return buf;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

    }

    public class FileDownloadTask extends FileTransferTask {

        public FileDownloadTask() {
            super();
        }

        public FileDownloadTask(String url, FileTransferCallback callback) {
            super(url, callback);
        }

        @Override
        protected Object runForResult() {
            File file = null;
            try {
                file = mXmppClient.receiveObject(mUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return file;
        }

        @Override
        public File getResult() {
            // TODO Auto-generated method stub
            return (File) super.getResult();
        }

    }

    public class ScreenshotTask extends FileUploadTask {

        public ScreenshotTask(String url, FileTransferCallback callback,
                Bundle info) {
            super(url, callback, null, "screenshot.png", info);
        }

        @Override
        protected byte[] getDate() {
            // TODO Auto-generated method stub
            RemoteDeviceManager dm = RemoteDeviceManager.getInstance(mContext);
            Handler handler = new Handler();
            return dm.takeScreenshot(handler);
        }
    }

}
