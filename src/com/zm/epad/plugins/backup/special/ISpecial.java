package com.zm.epad.plugins.backup.special;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zm.epad.plugins.backup.Notifier;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public abstract class ISpecial {
    protected final static String TAG = "ZMBackup:Special";
    private final static String JSON_KEY_VERSION = "version";
    protected final static String JSON_ARRAY_CONTACT = "contact";
    protected final static String JSON_ARRAY_CALLLOG = "calllog";
    protected final static String JSON_ARRAY_MMS_SMS = "mms_sms";
    protected final static String JSON_ARRAY_IM = "im";
    protected final static String JSON_ARRAY_EMAIL = "email";
    protected final static String JSON_ARRAY_ADDRESS = "address";
    protected final static String JSON_ARRAY_PHONE = "phone";
    protected final static String JSON_ARRAY_EVENT = "event";
    protected final static String JSON_ARRAY_ORGANIZATION = "organization";
    protected final static String JSON_KEY_MAIL = "mail";
    protected final static String JSON_KEY_TYPE = "type";
    protected final static String JSON_KEY_NAME = "name";
    protected final static String JSON_KEY_FULL = "full";
    protected final static String JSON_KEY_COUNTRY = "country";
    protected final static String JSON_KEY_PROVINCE = "province";
    protected final static String JSON_KEY_CITY = "city";
    protected final static String JSON_KEY_STREET = "street";
    protected final static String JSON_KEY_ZIP = "zip";
    protected final static String JSON_KEY_NUMBER = "number";
    protected final static String JSON_KEY_COMPANY = "company";
    protected final static String JSON_KEY_TITLE = "title";
    protected final static String JSON_KEY_DATE = "date";
    protected final static String JSON_KEY_WEBSITE = "website";
    protected final static String JSON_KEY_NOTES = "notes";
    protected final static String JSON_KEY_PHOTO = "photo";
    protected final static String JSON_KEY_GROUP = "group";
    protected final static String JSON_KEY_PERSON = "person";
    protected final static String JSON_KEY_BODY = "body";
    protected final static String JSON_KEY_PROTOCOL = "protocol";
    protected final static String JSON_KEY_DATE_HUMANITY = "date_humanity";
    protected final static String JSON_KEY_STATUS = "status";
    protected final static String JSON_KEY_READ = "read";
    protected final static String JSON_KEY_SUBJECT = "subject";
    protected final static String JSON_KEY_ADDRESS = "address";
    protected final static String JSON_KEY_COUNTRYISO = "countryiso";
    protected final static String JSON_KEY_DURATION = "duration";

    private JSONObject mRoot;
    private JSONArray mArray;
    private Cursor mCursor;
    private final HandlerThread mHandlerThread;
    private final Handler mBackupOrRestoreHandler;
    private final String mBackupPath;
    protected final ContentResolver mResolver;
    protected final Notifier mNotifier;

    // Status
    private final int STATUS_NONE = 0;
    private final int STATUS_BACKUP_CREATE = 1;
    private final int STATUS_BACKUP_QUERY = 2;
    private final int STATUS_BACKUP_ONE = 3;
    private final int STATUS_BACKUP_FLUSH = 4;
    private final int STATUS_BACKUP_END = 5;
    private int mStatus;
    private int mCountIndex;

    protected ISpecial(Context context, String backupPath, Notifier notifier) {
        // backupOrRestore thread and handler
        mHandlerThread = new HandlerThread("backupOrRestore_Special");
        mHandlerThread.start();
        mBackupOrRestoreHandler = new Handler(mHandlerThread.getLooper());
        mResolver = context.getContentResolver();
        mBackupPath = backupPath;
        mNotifier = notifier;
    }

    public final static int CONTACT = 1;
    public final static int CALLLOG = 2;
    public final static int MMS_SMS = 3;
    public static ISpecial create(Context context, String backupPath, Notifier notifier, int type) {
        switch(type) {
        case CONTACT:
            return new Contacts(context, backupPath, notifier);
        case CALLLOG:
            return new CallLog(context, backupPath, notifier);
        case MMS_SMS:
            return new SMS(context, backupPath, notifier);
        default:
            return null;
        }
    }

    public void fullBackup() {
        doBackup(STATUS_BACKUP_CREATE);
    }

    public String getPath() {
        return mBackupPath + File.separatorChar + getName();
    }

    public int getCount() {
        return mCountIndex;
    }

    private void doBackup(int nextStatus) {
        mStatus = nextStatus;
        mBackupOrRestoreHandler.post(mBackupRunnable);
    }

    private final Runnable mBackupRunnable = new Runnable() {
        @Override
        public void run() {
            switch (mStatus) {
                case STATUS_BACKUP_CREATE:
                    mNotifier.notifyStart(getName());
                    doBackup(create() ? STATUS_BACKUP_QUERY : STATUS_BACKUP_END);
                    break;
                case STATUS_BACKUP_QUERY:
                    doBackup(query() ? STATUS_BACKUP_ONE : STATUS_BACKUP_END);
                    break;
                case STATUS_BACKUP_ONE:
                    mNotifier.notifyRecordProgress(getName(), ++mCountIndex);
                    doBackup(backupOne() ? STATUS_BACKUP_ONE : STATUS_BACKUP_FLUSH);
                    break;
                case STATUS_BACKUP_FLUSH:
                    flush();
                    doBackup(STATUS_BACKUP_END);
                    break;
                case STATUS_BACKUP_END:
                    finish();
                    mNotifier.notifyEnd(getName(), mCountIndex);
                    doBackup(STATUS_NONE);
                    break;
                default:
                    mStatus = STATUS_NONE;
                    break;
            }
        }
    };

    private boolean create() {
        mCountIndex = 0;
        mRoot = new JSONObject();
        try {
            mRoot.put(JSON_KEY_VERSION, getVersion());
            mArray = new JSONArray();
            mRoot.put(getName(), mArray);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
        return true;
    }

    private boolean query() {
        mCursor = mResolver.query(getContentUri(), getProjection(), null, null, getSortOrder());
        if (mCursor == null || !mCursor.moveToFirst()) return false;
        return true;
    }

    private boolean backupOne() {
        JSONObject item = new JSONObject();
        doOneBackup(item, mCursor);
        mArray.put(item);
        if (!mCursor.moveToNext()) return false;
        return true;
    }

    private boolean flush() {
        File file = new File(getPath());
        if (file.exists()) file.delete();
        FileOutputStream fileOutStream = null;
        try {
            fileOutStream = new FileOutputStream(file);
            fileOutStream.write(mRoot.toString(2).getBytes());
            fileOutStream.getFD().sync();
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        } finally {
            try {
                if (fileOutStream != null)
                    fileOutStream.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return true;
    }

    private boolean finish() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        mArray = null;
        mRoot = null;
        return true;
    }

    /**
     * Should not modify Cursor in this function.
     * @param item
     * @param c
     */
    protected abstract void doOneBackup(JSONObject item, Cursor c);
    protected abstract String getName();
    protected abstract String getVersion();
    protected abstract String[] getProjection();
    protected abstract String getSortOrder();
    protected abstract Uri getContentUri();
}
