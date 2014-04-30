package com.zm.epad.plugins.backup.special;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.zm.epad.plugins.backup.Notifier;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

/**
 * _id            一个自增字段，从1开始
 * thread_id      序号，同一发信人的id相同
 * address        发件人手机号码
 * person         联系人列表里的序号，陌生人为null
 * date           发件日期
 * protocol       协议，分为： 0 SMS_RPOTO, 1 MMS_PROTO
 * read           是否阅读 0未读， 1已读
 * status         状态 -1接收，0 complete, 64 pending, 128 failed
 * type           ALL(0) INBOX(1) SENT(2) DRAFT(3) OUTBOX(4) FAILED(5) QUEUED(6)
 * body           短信内容
 * service_center 短信服务中心号码编号
 * subject        短信的主题
 * reply_path_present     TP-Reply-Path
 * locked
 *
 * from http://blog.csdn.net/ithomer/article/details/7328321
 *
 * <pre>
 *{
 * "version" : "zm v0.1"
 *
 * "mms_sms" : [
 *      {
 *        "address" : "13811728789",
 *        "person" : "0",
 *        "body" : "Welcome to Beijing",
 *        "date" : 1256539465022,
 *        "type" : 1,
 *        "protocol" : 0
 *        "status" : 0
 *        "read" : 1
 *      },
 *      {
 *        "address" : "13811728789",
 *        "person" : "0",
 *        "body" : "Welcome to Beijing",
 *        "date" : 1256539466022,
 *        "date_humanity" : ""
 *        "type" : 1,
 *        "protocol" : 1
 *        "status" : 0
 *        "read" : 1
 *        "subject" : "Welcome"
 *      }
 * ]
 *}
 *</pre>
 */
public class SMS extends ISpecial {
    private static final String VERSION = "zm v0.1";

    protected SMS(Context context, String backupPath, Notifier notifier) {
        super(context, backupPath, notifier);
    }

    @Override
    protected void doOneBackup(JSONObject item, Cursor c) {
        String strAddress = c.getString(COLUMNINDEX_ADDRESS);
        int intPerson = c.getInt(COLUMNINDEX_PERSON);
        String strbody = c.getString(COLUMNINDEX_BODY);
        int intType = c.getInt(COLUMNINDEX_TYPE);
        long longDate = intType == Telephony.Sms.MESSAGE_TYPE_SENT ?
                c.getLong(COLUMNINDEX_DATE_SENT) : c.getLong(COLUMNINDEX_DATE);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date date = new Date(longDate);
        String strDate = dateFormat.format(date);
        int intProtocol = c.getInt(COLUMNINDEX_PROTOCOL);
        int intRead = c.getInt(COLUMNINDEX_READ);
        int intStatus = c.getInt(COLUMNINDEX_STATUS);
        try {
            item.put(JSON_KEY_ADDRESS, strAddress);
            item.put(JSON_KEY_PERSON, intPerson);
            item.put(JSON_KEY_BODY, strbody);
            item.put(JSON_KEY_TYPE, intType);
            item.put(JSON_KEY_DATE, longDate);
            item.put(JSON_KEY_DATE_HUMANITY, strDate);
            item.put(JSON_KEY_PROTOCOL, intProtocol);
            item.put(JSON_KEY_READ, intRead);
            item.put(JSON_KEY_STATUS, intStatus);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    protected String getName() {
        return JSON_ARRAY_MMS_SMS;
    }

    @Override
    protected String getVersion() {
        return VERSION;
    }

    private static final int COLUMNINDEX_ADDRESS = 1;
    private static final int COLUMNINDEX_PERSON = 2;
    private static final int COLUMNINDEX_BODY = 3;
    private static final int COLUMNINDEX_DATE = 4;
    private static final int COLUMNINDEX_DATE_SENT = 5;
    private static final int COLUMNINDEX_PROTOCOL = 6;
    private static final int COLUMNINDEX_TYPE = 7;
    private static final int COLUMNINDEX_READ = 8;
    private static final int COLUMNINDEX_STATUS = 9;

    @Override
    protected String[] getProjection() {
        return new String[] { Telephony.Sms._ID,
                              Telephony.Sms.ADDRESS,
                              Telephony.Sms.PERSON,
                              Telephony.Sms.BODY,
                              Telephony.Sms.DATE,
                              Telephony.Sms.DATE_SENT,
                              Telephony.Sms.PROTOCOL,
                              Telephony.Sms.TYPE,
                              Telephony.Sms.READ,
                              Telephony.Sms.STATUS};
    }

    @Override
    protected Uri getContentUri() {
        return Telephony.Sms.CONTENT_URI;
    }

    @Override
    protected String getSortOrder() {
        return "date desc";
    }
}
