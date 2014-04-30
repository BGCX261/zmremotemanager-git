package com.zm.epad.plugins.backup.special;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.zm.epad.plugins.backup.Notifier;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 *
 * <pre>
 *{
 * "version" : "zm v0.1"
 *
 * "calllog" : [
 *      {
 *        "type" : "incoming|outgoing|missed",
 *        "number" : "13811728789",
 *        "countryiso" : "cn",
 *        "date" : 1256539465022,
 *        "date_humanity" : ""
 *        "duration" : 13,
 *      },
 *      {
 *        "type" : "incoming|outgoing|missed",
 *        "number" : "13811728789",
 *        "countryiso" : "cn",
 *        "date" : 1256539465022,
 *        "date_humanity" : ""
 *        "duration" : 13,
 *      }
 * ]
 *}
 *</pre>
 */
public class CallLog extends ISpecial {
    private static final String VERSION = "zm v0.1";

    private static final String INCOMING_TYPE = "incoming";
    private static final String OUTGOING_TYPE = "outgoing";
    private static final String MISSED_TYPE = "missed";

    protected CallLog(Context context, String backupPath, Notifier notifier) {
        super(context, backupPath, notifier);
    }

    @Override
    protected void doOneBackup(JSONObject item, Cursor c) {
        int intType = c.getInt(COLUMNINDEX_TYPE);
        String strType = null;
        if (intType == android.provider.CallLog.Calls.INCOMING_TYPE) {
            strType = INCOMING_TYPE;
        } else if (intType == android.provider.CallLog.Calls.OUTGOING_TYPE) {
            strType = OUTGOING_TYPE;
        } else if (intType == android.provider.CallLog.Calls.MISSED_TYPE) {
            strType = MISSED_TYPE;
        }
        String strNumber = c.getString(COLUMNINDEX_NUMBER);
        String strISO = c.getString(COLUMNINDEX_COUNTRY_ISO);
        long intDuration = c.getLong(COLUMNINDEX_DURATION);
        long longDate = c.getLong(COLUMNINDEX_DATE);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date date = new Date(longDate);
        String strDate = dateFormat.format(date);
        try {
            item.put(JSON_KEY_TYPE, strType);
            item.put(JSON_KEY_NUMBER, strNumber);
            item.put(JSON_KEY_COUNTRYISO, strISO);
            item.put(JSON_KEY_DURATION, intDuration);
            item.put(JSON_KEY_DATE, longDate);
            item.put(JSON_KEY_DATE_HUMANITY, strDate);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    protected String getName() {
        return JSON_ARRAY_CALLLOG;
    }

    @Override
    protected String getVersion() {
        return VERSION;
    }

    private static final int COLUMNINDEX_TYPE = 1;
    private static final int COLUMNINDEX_NUMBER = 2;
    private static final int COLUMNINDEX_COUNTRY_ISO = 3;
    private static final int COLUMNINDEX_DATE = 4;
    private static final int COLUMNINDEX_DURATION = 5;

    @Override
    protected String[] getProjection() {
        return new String[] { android.provider.CallLog.Calls._ID,
                              android.provider.CallLog.Calls.TYPE,
                              android.provider.CallLog.Calls.NUMBER,
                              android.provider.CallLog.Calls.COUNTRY_ISO,
                              android.provider.CallLog.Calls.DATE,
                              android.provider.CallLog.Calls.DURATION };
    }

    @Override
    protected Uri getContentUri() {
        return android.provider.CallLog.Calls.CONTENT_URI;
    }

    @Override
    protected String getSortOrder() {
        return android.provider.CallLog.Calls._ID + " desc";
    }
}
