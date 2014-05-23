package com.zm.epad.plugins;

import com.zm.epad.core.CoreConstants;
import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;

import android.content.Context;
import android.os.Bundle;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class RemoteFileManager {
    private static final String TAG = "RemoteFileManager";

    private class HttpTransferHelper {
        private static final int TIME_OUT = 10 * 10000000;
        private static final String CHARSET = "utf-8";
        public static final String SUCCESS = "1";
        public static final String FAILURE = "0";
        public static final String PREFIX = "--";
        public static final String LINE_END = "\r\n";
        public static final String CONTENT_TYPE = "multipart/form-data";

        private SimpleDateFormat mSimpleDateFmt = new SimpleDateFormat(
                "yyyy-MM-dd HH:mmZ", Locale.US);

        public HttpTransferHelper() {
        }

        private HttpURLConnection createUrlConnection(String requestUrl,
                String BOUNDARY) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(requestUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(TIME_OUT);
                conn.setConnectTimeout(TIME_OUT);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                if (BOUNDARY != null) {
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Charset", CHARSET);
                    conn.setRequestProperty("connection", "keep-alive");
                    conn.setRequestProperty("Content-Type", CONTENT_TYPE
                            + ";boundary=" + BOUNDARY);
                    conn.setChunkedStreamingMode(10240);
                }

                return conn;
            } catch (Exception e) {
                LogManager.local(TAG,
                        "createUrlConnection fails " + e.getMessage());
                return null;
            }
        }

        private String getFileName(String desc) {
            return mSimpleDateFmt.format(new Date()) + desc;
        }

        private String getHttpTailInfo(String BOUNDARY) {
            String endString = LINE_END + PREFIX + BOUNDARY + PREFIX + LINE_END;
            return endString;
        }

        public File receiveObject(String requestUrl) {
            if (requestUrl == null)
                return null;
            HttpURLConnection conn = createUrlConnection(requestUrl, null);
            if (conn == null) {
                return null;
            }
            int lastIndexofSlash = requestUrl.lastIndexOf("/");
            String fileName = requestUrl.substring(lastIndexofSlash);
            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            File recvedFile = new File(
                    mContext.getFilesDir().getAbsolutePath(), fileName);

            try {
                inputStream = conn.getInputStream();
                // don't handle the case that the file already exists
                recvedFile.createNewFile();
                outputStream = new FileOutputStream(recvedFile);
                byte[] buff = new byte[1024];
                int readCount = 0;
                while ((readCount = inputStream.read(buff)) > 0) {
                    outputStream.write(buff, 0, readCount);
                }
                buff = null;
                inputStream.close();
                inputStream = null;
                outputStream.flush();
                outputStream.close();
                outputStream = null;

                conn.disconnect();
                conn = null;
            } catch (Exception e) {
                LogManager.local(TAG, "recvObject fails " + e.getMessage());
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                    inputStream = null;
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
            try {
                if (outputStream != null) {
                    outputStream.close();
                    outputStream = null;
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
            if (conn != null) {
                conn.disconnect();
                conn = null;
            }
            return recvedFile;

        }

        // example code from :
        // http://blog.csdn.net/qq247890212/article/details/16358581
        // about multi-part/data, see
        // http://blog.csdn.net/five3/article/details/7181521

        /*
         * Explain: 1 if data is not null, then it will upload data[] to the
         * server. This is used for small data transfer 2 if data is null and
         * inputStream is no null, then we will use inputStream to read data.
         * This is used for large date transfer. Because we can not read the
         * content of a a large file into a single buffer.
         */
        public String uploadObject(byte[] data, final String desc,
                String requestUrl, Bundle Info, InputStream inputStream) {
            String BOUNDARY = PREFIX + UUID.randomUUID().toString();
            HttpURLConnection conn = createUrlConnection(requestUrl, BOUNDARY);
            if (conn == null)
                return null;

            OutputStream outputStream = null;
            try {
                outputStream = conn.getOutputStream();
            } catch (Exception e) {
                LogManager
                        .local(TAG, "getOutputStream fails " + e.getMessage());
                conn.disconnect();
                return null;
            }
            DataOutputStream dos = new DataOutputStream(outputStream);

            String fileName = desc;

            int res = 0;
            try {
                // the http form data must be written one by one
                writeHttpFormInfo(dos, fileName, BOUNDARY, Info);
                if (data != null) {
                    dos.write(data, 0, data.length);
                } else if (inputStream != null) {
                    byte[] inputBuffer = new byte[1 << 10];
                    int readCount = 0;
                    while ((readCount = inputStream.read(inputBuffer)) > 0) {
                        dos.write(inputBuffer, 0, readCount);
                    }
                }
                dos.write(getHttpTailInfo(BOUNDARY).getBytes());
                dos.flush();
                dos.close();
                dos = null;
                res = conn.getResponseCode();
            } catch (Exception e) {
                // TODO: handle exception
                LogManager.local(TAG, "uploadObject 1 fails " + e.getMessage());
            }
            if (dos != null) {
                try {
                    dos.close();
                } catch (Exception e) {
                    // TODO: handle exception
                }

            }
            conn.disconnect();
            if (res == 200)
                return fileName;
            else
                return null;
        }

        void writeHeadInfo(StringBuilder sb, String nameStr, String nameValue,
                String Boundary, DataOutputStream dos) throws IOException {
            if (nameStr != null && nameValue != null) {
                sb.setLength(0);
                sb.append("--" + Boundary + LINE_END);
                sb.append("Content-Disposition: form-data; name=\"");
                sb.append(nameStr + "\"");
                sb.append(LINE_END + LINE_END);
                sb.append(nameValue);

                dos.write(sb.toString().getBytes(CHARSET));
                dos.write(LINE_END.getBytes(CHARSET));
            }
        }

        void writeFileHeadInfo(StringBuilder sb, String nameStr,
                String nameValue, String fileName, String Boundary,
                DataOutputStream dos) throws IOException {
            sb.setLength(0);
            sb.append("--" + Boundary + LINE_END);
            sb.append("Content-Disposition: form-data; name=\"");
            sb.append(nameStr + "\"; ");
            sb.append("filename=\"" + fileName + "\"" + LINE_END);
            sb.append("Content-Type: " + nameValue);
            sb.append(LINE_END + LINE_END);
            dos.write(sb.toString().getBytes(CHARSET));
        }

        public void writeHttpFormInfo(DataOutputStream dos, String filename,
                String Boundary, Bundle info) {

            if (dos == null || filename == null || Boundary == null)
                return;

            try {
                StringBuilder sb = new StringBuilder();
                /*
                 * don't need to check authority in form // write user name
                 * writeHeadInfo(sb, CoreConstants.CONSTANT_USRNAME,
                 * mLoginBundle.getString(CoreConstants.CONSTANT_USRNAME),
                 * Boundary, dos);
                 * 
                 * // write password writeHeadInfo( sb,
                 * CoreConstants.CONSTANT_PASSWORD,
                 * mLoginBundle.getString(CoreConstants.CONSTANT_PASSWORD),
                 * Boundary, dos);
                 * 
                 * // write resource writeHeadInfo( sb,
                 * CoreConstants.CONSTANT_RESOURCE,
                 * mLoginBundle.getString(CoreConstants.CONSTANT_RESOURCE),
                 * Boundary, dos);
                 */
                writeHeadInfo(sb, CoreConstants.CONSTANT_CRC,
                        CoreConstants.CONSTANT_CRC_DEFAULT, Boundary, dos);

                if (info != null) {
                    // write command id
                    Set<String> keys = info.keySet();
                    for (String k : keys) {
                        if (k.equals(CoreConstants.CONSTANT_MIME))
                            continue;

                        writeHeadInfo(sb, k, info.getString(k), Boundary, dos);
                    }

                    // write upload file info
                    String mime = info.getString(CoreConstants.CONSTANT_MIME);
                    writeFileHeadInfo(sb, CoreConstants.CONSTANT_UPLOAD,
                            mime != null ? mime
                                    : CoreConstants.CONSTANT_MIME_DEFAULT,
                            filename, Boundary, dos);
                } else {
                    // write upload file info with default mime
                    writeFileHeadInfo(sb, CoreConstants.CONSTANT_UPLOAD,
                            CoreConstants.CONSTANT_MIME_DEFAULT, filename,
                            Boundary, dos);
                }

            } catch (Exception e) {
                LogManager.local(TAG,
                        "writeHttpFormInfo fails " + e.getMessage());
            }

        }
    }

    // private static RemoteFileManager sInstance = null;

    Context mContext;
    HttpTransferHelper mHttpTransferHelper;
    Bundle mLoginBundle;
    private static final int RUNNING_TASK_MAX = 10;
    private List<FileTransferTask> mRunningTask = new ArrayList<FileTransferTask>();
    private List<FileTransferTask> mPendingTask = new ArrayList<FileTransferTask>();
    private ExecutorService mThreadPool;

    public void setThreadPool(ExecutorService threadPool) {
        mThreadPool = threadPool;
    }

    public void setXmppLoginResource(Bundle srcBundle) {
        mLoginBundle = srcBundle;
    }

    public void stop() {
        LogManager.local(TAG, "stop");
        cancelAllPendingTask();
        mThreadPool = null;
    }

    public RemoteFileManager(Context context) {
        mContext = context;
        mHttpTransferHelper = new HttpTransferHelper();
    }

    public void addFileDownloadTask(String url, FileTransferCallback callback) {
        FileTransferTask downloadTask = new FileDownloadTask(url, callback);
        downloadTask.prepare();
        mThreadPool.execute(downloadTask);
    }

    public void addFileUploadTask(String url, String filePath, String fileName,
            Bundle info, FileTransferCallback callback) {
        FileTransferTask fileUploadTask = new FileUploadTask(url, callback,
                filePath, fileName, info);
        fileUploadTask.prepare();
        mThreadPool.execute(fileUploadTask);
    }

    public void zipAndUploadFile(String url, String srcfilePath,
            String zipPath, Bundle info, FileTransferCallback callback) {
        FileUploadTask fileUploadTask = new FileUploadTask(url, callback,
                srcfilePath, zipPath, info);
        fileUploadTask.mbZipTask = true;
        fileUploadTask.prepare();
        mThreadPool.execute(fileUploadTask);
    }

    public void addScreenshotTask(String url, Bundle info,
            FileTransferCallback callback) {
        FileTransferTask screenshotTask = new ScreenshotTask(url, callback,
                info);
        screenshotTask.prepare();
        mThreadPool.execute(screenshotTask);
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
                mThreadPool.execute(newTask);
            }
        }
    }

    private void removePendingTask(FileTransferTask task) {
        synchronized (mPendingTask) {
            mPendingTask.remove(task);
        }
    }

    public boolean zip(String fileOrDir, String targetFile) {
        File target = new File(fileOrDir);
        if (target.exists() == false)
            return false;
        try {
            File zipFile = new File(targetFile);
            if (zipFile.exists() == false)
                zipFile.createNewFile();
            return zip(target, zipFile);

        } catch (Exception e) {
            return false;
        }
    }

    private boolean zip(File inputFile, File outputFile) {
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new FileOutputStream(outputFile));
        } catch (Exception e) {
            LogManager.local(TAG, "create zip file fails " + e.getMessage());
            return false;
        }

        boolean ret = true;
        try {
            zip(out, inputFile, "");
        } catch (Exception ex) {
            ret = false;
        }
        try {
            out.close();
        } catch (Exception e) {
            // TODO: handle exception
        }

        return ret;
    }

    private void zip(ZipOutputStream out, File f, String base) throws Exception {
        if (f.isDirectory()) {
            File[] fl = f.listFiles();
            out.putNextEntry(new ZipEntry(base + "/"));
            base = base.length() == 0 ? "" : base + "/";
            for (int i = 0; i < fl.length; i++) {
                zip(out, fl[i], base + fl[i].getName());
            }
        } else {
            out.putNextEntry(new ZipEntry(base));
            FileInputStream in = new FileInputStream(f);
            int b;
            System.out.println(base);
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            in.close();
        }
    }

    public interface FileTransferCallback {
        void onDone(boolean success, FileTransferTask task);

        void onCancel(FileTransferTask task);
    }

    public abstract class FileTransferTask implements Runnable {
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

        public synchronized void prepare() {
            if (addTask(this) == false) {
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
                mCallback.onDone(mResult == null ? false : true, this);
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

        /*
         * 1 for zip task: mFilePath is src dir mFileName is target zip file 2
         * for nomral task: mFilePath is dir or the file's full path name
         * mFileName: if not null, it is the file name. if null, use mFilePath
         */
        protected String mFilePath;
        protected String mFileName;
        protected Bundle mInfo;
        public boolean mbZipTask;

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

        private boolean compressFile() {
            boolean bret = zip(mFilePath, mFileName);
            if (bret) {
                // now, the compressed file is in mFileName
                // we need to split it the
                mFilePath = mFileName;
                mFileName = null;
                splitPathAndFileName(mFilePath);
            } else {
                LogManager.local(TAG, "compress " + mFilePath + " to "
                        + mFileName + " faild");
            }
            return bret;
        }

        @Override
        protected Object runForResult() {
            String fileName = null;
            if (mbZipTask) {
                boolean ret = compressFile();
                if (ret == false)
                    return null;
            }
            InputStream inputStream = getInputStream();
            try {
                if (inputStream != null) {
                    fileName = mHttpTransferHelper.uploadObject(
                            null/* getDate() */, mFileName, mUrl, mInfo,
                            inputStream);
                } else {
                    fileName = mHttpTransferHelper.uploadObject(getDate(),
                            mFileName, mUrl, mInfo, null);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                // TODO: handle exception
            }

            return fileName;
        }

        @Override
        public String getResult() {
            // TODO Auto-generated method stub
            return (String) super.getResult();
        }

        // this function is used to spit a full path name into path and filename
        // For example: /dirA/dirB/dirC/fileA is splitted into:
        // mFilePath = /dirA/dirB/dirC and
        // mFileName = fileA
        private void splitPathAndFileName(String fullPathFileName) {
            int lastSplash = fullPathFileName.lastIndexOf("/") + 1;
            int totalLength = fullPathFileName.length();
            mFileName = fullPathFileName.subSequence(lastSplash, totalLength)
                    .toString();
            mFilePath = fullPathFileName.subSequence(0, lastSplash).toString();

            return;
        }

        protected InputStream getInputStream() {
            try {
                if (mFilePath == null) {
                    return null;
                }

                File upFile = null;
                if (mFileName == null)// mFilePath contains full path file name
                    splitPathAndFileName(mFilePath);

                upFile = new File(mFilePath, mFileName);

                FileInputStream in = new FileInputStream(upFile);
                return in;
                /*
                 * LogManager.local(TAG, "upload file size:" + upFile.length());
                 * byte[] buf = new byte[(int) upFile.length()]; in.read(buf);
                 * in.close();
                 */
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }

        protected byte[] getDate() {
            try {
                if (mFileName == null)// mFilePath contains full path file name
                    splitPathAndFileName(mFilePath);

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
                file = mHttpTransferHelper.receiveObject(mUrl);
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
            RemoteDeviceManager dm = SubSystemFacade.getInstance()
                    .getRemoteDeviceManager();
            return dm.takeScreenshot(null);
        }
    }

}
