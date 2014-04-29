package com.zm.epad.plugins.backup.special;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.util.Log;

import com.zm.epad.plugins.backup.Notifier;

import org.jivesoftware.smack.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <pre>
 *{
 * "version" : "zm v0.1"
 *
 * "contact" : [
 *
 *  {
 *    "id" : 1
 *    "name" : "tkboy",
 *    "nickname" : "arvin"
 *    "email" : [
 *      {
 *        "type" : "work",
 *        "mail" : "tkboy@xx.com"
 *      }
 *    ],
 *    "address" : [
 *      {
 *        "full" : "xxxx, Xincheng, Beijing, China",
 *        "country" : "China",
 *        "province" : "Beijing",
 *        "city" : "Beijing",
 *        "street" : "xxxx",
 *        "zip" : "100001"
 *      }
 *    ],
 *    "phone" : [
 *      {
 *        "type" = "mobile",
 *        "number" = "138111111111"
 *      },
 *      {
 *        "type" = "home",
 *        "number" = "138111111112"
 *      }
 *    ],
 *    "im" : [
 *      {
 *        "type" = "qq",
 *        "account" = "11111111"
 *      }
 *    ],
 *    "organization" : [
 *      {
 *        "company" : "zm",
 *        "title" : "developer"
 *      }
 *    ],
 *    "event" : [
 *      {
 *        "type" = "birthday",
 *        "date" = "1985-02-02"
 *      }
 *    ],
 *    "website" : "http://weixin.com/tkboy",
 *    "notes" : "This guy is lazy."
 *    "photo" : "zfsjdlfnsdjflsensdjfr9sdf7djf9..."
 *    "group" : "friends"
 *  },
 *
 *  {
 *    "name" : "fanping",
 *    "nickname" : "innost"
 *    "email" : [
 *      {
 *        "type" : "work",
 *        "mail" : "fanping@xx.com"
 *      }
 *    ],
 *  }
 *
 * ]
 *
 *}
 *</pre>
 */
class Contacts extends ISpecial {

    private static final String VERSION = "zm v0.1";

    protected Contacts(Context context, String backupPath, Notifier notifier) {
        super(context, backupPath, notifier);
    }

    private static final int COLUMNINDEX_ID = 0;
    private static final int COLUMNINDEX_NAME = 1;

    /**
     * http://blog.csdn.net/wssiqi/article/details/8152630
     * http://www.alnton.com/?p=542
     */
    @Override
    protected void doOneBackup(JSONObject item, Cursor c) {
        long id = c.getLong(COLUMNINDEX_ID);
        try {
            item.put(JSON_KEY_ID, id);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        String name = c.getString(COLUMNINDEX_NAME);
        try {
            item.put(JSON_KEY_NAME, name);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        String nick = getNicknames(id);
        if (nick != null) {
            try {
                item.put(JSON_KEY_NICKNAME, nick);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        JSONArray phones = getPhones(id);
        if (phones != null) {
            try {
                item.put(JSON_ARRAY_PHONE, phones);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        JSONArray emails = getEmails(id);
        if (emails != null) {
            try {
                item.put(JSON_ARRAY_EMAIL, emails);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        JSONArray addrs = getAddress(id);
        if (addrs != null) {
            try {
                item.put(JSON_ARRAY_ADDRESS, addrs);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        JSONArray ims = getIM(id);
        if (ims != null) {
            try {
                item.put(JSON_ARRAY_IM, ims);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        JSONArray organizations = getOrganizations(id);
        if (organizations != null) {
            try {
                item.put(JSON_ARRAY_ORGANIZATION, organizations);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        String photo = getPhoto(id);
        if (photo != null) {
            try {
                item.put(JSON_KEY_PHOTO, photo);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        String site = getWebsites(id);
        if (site != null) {
            try {
                item.put(JSON_KEY_WEBSITE, site);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        String note = getNote(id);
        if (note != null) {
            try {
                item.put(JSON_KEY_NOTES, note);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    @Override
    protected String getName() {
        return JSON_ARRAY_CONTACT;
    }

    @Override
    protected String getVersion() {
        return VERSION;
    }

    @Override
    protected String[] getProjection() {
        return new String[] { ContactsContract.Contacts._ID,
                               ContactsContract.Contacts.DISPLAY_NAME };
    }

    @Override
    protected Uri getContentUri() {
        return ContactsContract.Contacts.CONTENT_URI;
    }

    @Override
    protected String getSortOrder() {
        return null;
    }

    private String getPhoto(long id) {
        /* 联系人头像 */
        final Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        final Uri CONTACT_URI = ContentUris.withAppendedId(CONTENT_URI, id);
        final Uri PHOTO_URI = Uri.withAppendedPath(CONTACT_URI,
                ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        final String[] PROJECTION = new String[] {
                ContactsContract.Contacts.Photo.PHOTO };
        Cursor photoCursor = mResolver.query(PHOTO_URI, PROJECTION, null, null, null);
        if (null != photoCursor && photoCursor.moveToNext()) {
            byte[] data = photoCursor.getBlob(0);
            photoCursor.close();
            return Base64.encodeBytes(data);
        }
        return null;
    }

    private JSONArray getPhones(long id) {
        final Uri CONTENT_URI = CommonDataKinds.Phone.CONTENT_URI;
        final String[] PROJECTION = new String[] {
                CommonDataKinds.Phone._ID,
                CommonDataKinds.Phone.TYPE,
                CommonDataKinds.Phone.NUMBER };
        final String SELECTION = CommonDataKinds.Phone.CONTACT_ID + " = " + id;
        Cursor phoneCursor = mResolver.query(CONTENT_URI, PROJECTION, SELECTION, null, null);
        if (null == phoneCursor) return null;
        JSONArray phones = new JSONArray();
        final int INDEX_TYPE = phoneCursor.getColumnIndex(CommonDataKinds.Phone.TYPE);
        final int INDEX_NUMBER = phoneCursor.getColumnIndex(CommonDataKinds.Phone.NUMBER);
        while (phoneCursor.moveToNext()) {
            String number = phoneCursor.getString(INDEX_NUMBER);
            String type;
            switch(phoneCursor.getInt(INDEX_TYPE)) {
            case CommonDataKinds.Phone.TYPE_HOME:
                type = "home"; break;       // 住宅
            case CommonDataKinds.Phone.TYPE_MOBILE:
                type = "mobile"; break;     // 手机
            case CommonDataKinds.Phone.TYPE_WORK:
                type = "work"; break;       // 单位
            case CommonDataKinds.Phone.TYPE_FAX_WORK:
                type = "fax_work"; break;   // 单位传真
            case CommonDataKinds.Phone.TYPE_FAX_HOME:
                type = "fax_home"; break;   // 住宅传真
            case CommonDataKinds.Phone.TYPE_PAGER:
                type = "pager"; break;      // 寻呼机
            case CommonDataKinds.Phone.TYPE_OTHER:
                type = "other"; break;      // 其他
            case CommonDataKinds.Phone.TYPE_CALLBACK:
                type = "callback"; break;   // 回拨电话
            case CommonDataKinds.Phone.TYPE_CAR:
                type = "car"; break;        // 车载电话
            case CommonDataKinds.Phone.TYPE_COMPANY_MAIN:
                type = "company_main"; break;   // 公司总机
            case CommonDataKinds.Phone.TYPE_ISDN:
                type = "isdn"; break;       // ISDN
            case CommonDataKinds.Phone.TYPE_MAIN:
                type = "main"; break;       // 总机
            case CommonDataKinds.Phone.TYPE_OTHER_FAX:
                type = "other_fax"; break;  // 其他传真
            case CommonDataKinds.Phone.TYPE_RADIO:
                type = "radio"; break;      // 无线装置
            case CommonDataKinds.Phone.TYPE_TELEX:
                type = "telex"; break;      // 电报
            case CommonDataKinds.Phone.TYPE_TTY_TDD:
                type = "tty_tdd"; break;    // TTY TDD
            case CommonDataKinds.Phone.TYPE_WORK_MOBILE:
                type = "work_mobile"; break;  // 单位手机
            case CommonDataKinds.Phone.TYPE_WORK_PAGER:
                type = "work_pager"; break;  // 单位寻呼机
            case CommonDataKinds.Phone.TYPE_ASSISTANT:
                type = "assistant"; break;   // 助理
            case CommonDataKinds.Phone.TYPE_MMS:
                type = "mms"; break;         // 彩信
            default:
                type = "custom"; break; //
            }
            JSONObject newPhone = new JSONObject();
            try {
                newPhone.put(JSON_KEY_MAIL, number);
                newPhone.put(JSON_KEY_TYPE, type);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
                continue;
            }
            phones.put(newPhone);
        }
        phoneCursor.close();
        return phones;
    }

    private JSONArray getEmails(long id) {
        final Uri CONTENT_URI = CommonDataKinds.Email.CONTENT_URI;
        final String[] PROJECTION = new String[] {
                CommonDataKinds.Email._ID,
                CommonDataKinds.Email.TYPE,
                CommonDataKinds.Email.DATA };
        final String SELECTION = CommonDataKinds.Email.CONTACT_ID + " = " + id;
        Cursor emailCursor = mResolver.query(CONTENT_URI, PROJECTION, SELECTION, null, null);
        if (null == emailCursor) return null;
        JSONArray emails = new JSONArray();
        final int INDEX_TYPE = emailCursor.getColumnIndex(CommonDataKinds.Email.TYPE);
        final int INDEX_DATA = emailCursor.getColumnIndex(CommonDataKinds.Email.DATA);
        while (emailCursor.moveToNext()) {
            String emailAccount = emailCursor.getString(INDEX_DATA);
            String emailType;
            switch(emailCursor.getInt(INDEX_TYPE)) {
            case CommonDataKinds.Email.TYPE_HOME:
                emailType = "home"; break;   // 个人邮箱
            case CommonDataKinds.Email.TYPE_WORK:
                emailType = "work"; break;   // 单位邮箱
            case CommonDataKinds.Email.TYPE_OTHER:
                emailType = "other"; break;  // 其他邮箱
            case CommonDataKinds.Email.TYPE_MOBILE:
                emailType = "mobile"; break; // 手机邮箱
            default:
                emailType = "custom"; break; //
            }
            JSONObject newMail = new JSONObject();
            try {
                newMail.put(JSON_KEY_MAIL, emailAccount);
                newMail.put(JSON_KEY_TYPE, emailType);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
                continue;
            }
            emails.put(newMail);
        }
        emailCursor.close();
        return emails;
    }

    private JSONArray getIM(long id) {
        final Uri CONTENT_URI = ContactsContract.Data.CONTENT_URI;
        final String[] PROJECTION = new String[] {
                CommonDataKinds.Im._ID,
                CommonDataKinds.Im.PROTOCOL,
                CommonDataKinds.Im.DATA };
        final String SELECTION = CommonDataKinds.Im.CONTACT_ID + " = " + id
                + " AND " + CommonDataKinds.Im.MIMETYPE + "='"
                + CommonDataKinds.Im.CONTENT_ITEM_TYPE + "'";
        Cursor ImCursor = mResolver.query(CONTENT_URI, PROJECTION, SELECTION, null, null);
        if (null == ImCursor) return null;
        JSONArray ims = new JSONArray();
        final int INDEX_TYPE = ImCursor.getColumnIndex(CommonDataKinds.Im.PROTOCOL);
        final int INDEX_ACCOUNT = ImCursor.getColumnIndex(CommonDataKinds.Im.DATA);
        while (ImCursor.moveToNext()) {
            String IMAccount = ImCursor.getString(INDEX_ACCOUNT);
            String IMType;
            switch(ImCursor.getInt(INDEX_TYPE)) {
            case CommonDataKinds.Im.PROTOCOL_CUSTOM: // 自定义
                IMType = "custom"; break;
            case CommonDataKinds.Im.PROTOCOL_AIM: // AIM
                IMType = "aim"; break;
            case CommonDataKinds.Im.PROTOCOL_MSN: // Windows Live
                IMType = "msn"; break;
            case CommonDataKinds.Im.PROTOCOL_YAHOO: // 雅虎
                IMType = "yahoo"; break;
            case CommonDataKinds.Im.PROTOCOL_SKYPE: // Skype
                IMType = "skype"; break;
            case CommonDataKinds.Im.PROTOCOL_QQ: // QQ
                IMType = "qq"; break;
            case CommonDataKinds.Im.PROTOCOL_GOOGLE_TALK: // Google Talk
                IMType = "google_talk"; break;
            case CommonDataKinds.Im.PROTOCOL_ICQ: // ICQ
                IMType = "icq"; break;
            case CommonDataKinds.Im.PROTOCOL_JABBER: // Jabber
                IMType = "jabber"; break;
            case CommonDataKinds.Im.PROTOCOL_NETMEETING: // Net Meeting
                IMType = "netmeeting"; break;
            default:
                IMType = "custom"; break; //
            }
            JSONObject newIm = new JSONObject();
            try {
                newIm.put(JSON_KEY_ACCOUNT, IMAccount);
                newIm.put(JSON_KEY_TYPE, IMType);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
                continue;
            }
            ims.put(newIm);
        }
        ImCursor.close();
        return ims;
    }

    private JSONArray getAddress(long id) {
        final Uri CONTENT_URI = CommonDataKinds.StructuredPostal.CONTENT_URI;
        final String[] PROJECTION = new String[] {
                CommonDataKinds.StructuredPostal._ID,
                CommonDataKinds.StructuredPostal.STREET,
                CommonDataKinds.StructuredPostal.CITY,
                CommonDataKinds.StructuredPostal.REGION,
                CommonDataKinds.StructuredPostal.POSTCODE,
                CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
                CommonDataKinds.StructuredPostal.COUNTRY };
        final String SELECTION = CommonDataKinds.StructuredPostal.CONTACT_ID + " = " + id;
        Cursor aCursor = mResolver.query(CONTENT_URI, PROJECTION, SELECTION, null, null);
        if (null == aCursor) return null;
        final int INDEX_STREET = aCursor.getColumnIndex(
                CommonDataKinds.StructuredPostal.STREET);
        final int INDEX_CITY = aCursor.getColumnIndex(
                CommonDataKinds.StructuredPostal.CITY);
        final int INDEX_REGION = aCursor.getColumnIndex(
                CommonDataKinds.StructuredPostal.REGION);
        final int INDEX_POSTCODE = aCursor.getColumnIndex(
                CommonDataKinds.StructuredPostal.POSTCODE);
        final int INDEX_FORMATTED_ADDRESS = aCursor.getColumnIndex(
                CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS);
        final int INDEX_COUNTRY = aCursor.getColumnIndex(
                CommonDataKinds.StructuredPostal.COUNTRY);
        JSONArray addrs = new JSONArray();
        while (aCursor.moveToNext()) {
            /* 街道 */
            String street = aCursor.getString(INDEX_STREET);
            /* 城市 */
            String city = aCursor.getString(INDEX_CITY);
            /* 省、直辖市、自治区 */
            String region = aCursor.getString(INDEX_REGION);
            /* 邮政编码 */
            String postCode = aCursor.getString(INDEX_POSTCODE);
            String formatAddress = aCursor.getString(INDEX_FORMATTED_ADDRESS);
            String country = aCursor.getString(INDEX_COUNTRY);
            JSONObject newAddr = new JSONObject();
            try {
                newAddr.put(JSON_KEY_STREET, street);
                newAddr.put(JSON_KEY_CITY, city);
                newAddr.put(JSON_KEY_PROVINCE, region);
                newAddr.put(JSON_KEY_ZIP, postCode);
                newAddr.put(JSON_KEY_COUNTRY, country);
                newAddr.put(JSON_KEY_FULL, formatAddress);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
                continue;
            }
            addrs.put(newAddr);
        }
        aCursor.close();
        return addrs;
    }

    private JSONArray getOrganizations(long id) {
        final Uri CONTENT_URI = ContactsContract.Data.CONTENT_URI;
        final String[] PROJECTION = new String[] {
                CommonDataKinds.Organization._ID,
                CommonDataKinds.Organization.COMPANY,
                CommonDataKinds.Organization.TITLE };
        final String SELECTION = CommonDataKinds.Organization.CONTACT_ID + " = " + id
                + " AND " + CommonDataKinds.Organization.MIMETYPE + "='"
                + CommonDataKinds.Organization.CONTENT_ITEM_TYPE + "'";
        Cursor organizationsCursor = mResolver.query(CONTENT_URI, PROJECTION, SELECTION, null, null);
        if (null == organizationsCursor) return null;
        final int INDEX_COMPANY = organizationsCursor.getColumnIndex(
                CommonDataKinds.Organization.COMPANY);
        final int INDEX_TITLE = organizationsCursor.getColumnIndex(
                CommonDataKinds.Organization.TITLE);
        JSONArray organizations = new JSONArray();
        while (organizationsCursor.moveToNext()) {
            String company = organizationsCursor.getString(INDEX_COMPANY);
            String job = organizationsCursor.getString(INDEX_TITLE);
            JSONObject newOrganization = new JSONObject();
            try {
                newOrganization.put(JSON_KEY_COMPANY, company);
                newOrganization.put(JSON_KEY_TITLE, job);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
                continue;
            }
            organizations.put(newOrganization);
        }
        organizationsCursor.close();
        return organizations;
    }

    private String getNote(long id) {
        final Uri CONTENT_URI = ContactsContract.Data.CONTENT_URI;
        final String[] PROJECTION = new String[] {
                CommonDataKinds.Note._ID,
                CommonDataKinds.Note.NOTE };
        final String SELECTION = CommonDataKinds.Note.CONTACT_ID + " = " + id
                + " AND " + CommonDataKinds.Note.MIMETYPE + "='"
                + CommonDataKinds.Note.CONTENT_ITEM_TYPE + "'";
        Cursor notesCursor = mResolver.query(CONTENT_URI, PROJECTION, SELECTION, null, null);
        if (null == notesCursor) return null;
        String ret = null;
        while (notesCursor.moveToNext()) {
            ret = notesCursor.getString(
                    notesCursor.getColumnIndex(CommonDataKinds.Note.NOTE));
            if (ret != null) break;
        }
        notesCursor.close();
        return ret;
    }

    private String getNicknames(long id) {
        final Uri CONTENT_URI = ContactsContract.Data.CONTENT_URI;
        final String[] PROJECTION = new String[] {
                CommonDataKinds.Nickname._ID,
                CommonDataKinds.Nickname.NAME };
        final String SELECTION = CommonDataKinds.Nickname.CONTACT_ID + " = " + id
                + " AND " + CommonDataKinds.Nickname.MIMETYPE + "='"
                + CommonDataKinds.Nickname.CONTENT_ITEM_TYPE + "'";
        Cursor nicknamesCursor = mResolver.query(CONTENT_URI, PROJECTION, SELECTION, null, null);
        if (null == nicknamesCursor) return null;
        String ret = null;
        while (nicknamesCursor.moveToNext()) {
            ret = nicknamesCursor.getString(
                    nicknamesCursor.getColumnIndex(CommonDataKinds.Nickname.NAME));
            if (ret != null) break;
        }
        nicknamesCursor.close();
        return ret;
    }

    private String getWebsites(long id) {
        final Uri CONTENT_URI = ContactsContract.Data.CONTENT_URI;
        final String[] PROJECTION = new String[] {
                CommonDataKinds.Website._ID,
                CommonDataKinds.Website.URL };
        final String SELECTION = CommonDataKinds.Website.CONTACT_ID + " = " + id
                + " AND " + CommonDataKinds.Website.MIMETYPE + "='"
                + CommonDataKinds.Website.CONTENT_ITEM_TYPE + "'";
        Cursor webSiteCursor = mResolver.query(CONTENT_URI, PROJECTION, SELECTION, null, null);
        if (null == webSiteCursor) return null;
        String ret = null;
        while (webSiteCursor.moveToNext()) {
            ret = webSiteCursor.getString(
                    webSiteCursor.getColumnIndex(CommonDataKinds.Website.URL));
            if (ret != null) break;
        }
        webSiteCursor.close();
        return ret;
    }
}
