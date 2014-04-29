package com.zm.epad.plugins.backup.special;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zm.epad.plugins.backup.Notifier;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * <pre>
 *{
 * "version" : "zm v0.1"
 *
 * "contact" : [
 *
 *  {
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
 *        "number" = "11111111"
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
    private static final Uri CONTENT_URI = android.provider.ContactsContract.Contacts.CONTENT_URI;
    private static final String[] PHONES_PROJECTION = new String[] { android.provider.ContactsContract.Contacts._ID };
    private static final int COLUMNINDEX_ID = 0;

    private static final Uri PRIV_URI = android.provider.ContactsContract.Data.CONTENT_URI;
    private static final String CONTACT_ID = android.provider.ContactsContract.Data.CONTACT_ID;
    private static final String[] PRIV_PROJECTION = new String[]{
            CONTACT_ID,
            android.provider.ContactsContract.Data.MIMETYPE,
            android.provider.ContactsContract.Data.DATA1
            };
    private static final int PRIV_COLUMNINDEX_MIMETYPE = 1;
    private static final int PRIV_COLUMNINDEX_DATA1 = 2;

    protected Contacts(Context context, String backupPath, Notifier notifier) {
        super(context, backupPath, notifier);
    }

    /**
     * http://blog.csdn.net/wssiqi/article/details/8152630
     * http://www.alnton.com/?p=542
     */
    @Override
    protected void doOneBackup(JSONObject item, Cursor c) {
        String id = c.getString(COLUMNINDEX_ID);
        Cursor contactInfoCursor = mResolver.query(PRIV_URI, PRIV_PROJECTION,
                CONTACT_ID + "=" + id, null, null);

        JSONArray ims = null;
        JSONArray emails = null;
        JSONArray phones = null;
        JSONArray addrs = null;

        while(contactInfoCursor.moveToNext()) {
            String mimetype = contactInfoCursor.getString(PRIV_COLUMNINDEX_MIMETYPE);
            String value = contactInfoCursor.getString(PRIV_COLUMNINDEX_DATA1);
            try {
                if(mimetype.contains("/name")) {
                    item.put(JSON_KEY_NAME, value);
                } else if(mimetype.contains("/im")) {
                    if (ims == null) {
                        ims = new JSONArray();
                        item.put(JSON_ARRAY_IM, ims);
                    }
                    JSONObject newIm = new JSONObject();
                    ims.put(newIm);
                    newIm.put(JSON_KEY_NUMBER, value);
                } else if(mimetype.contains("/email")) {
                    if (emails == null) {
                        emails = new JSONArray();
                        item.put(JSON_ARRAY_EMAIL, emails);
                    }
                    JSONObject newMail = new JSONObject();
                    emails.put(newMail);
                    newMail.put(JSON_KEY_MAIL, value);
                } else if(mimetype.contains("/phone")) {
                    if (phones == null) {
                        phones = new JSONArray();
                        item.put(JSON_ARRAY_PHONE, phones);
                    }
                    JSONObject newPhone = new JSONObject();
                    phones.put(newPhone);
                    newPhone.put(JSON_KEY_NUMBER, value);
                } else if(mimetype.contains("/postal")) {
                    if (addrs == null) {
                        addrs = new JSONArray();
                        item.put(JSON_ARRAY_PHONE, addrs);
                    }
                    JSONObject newAddr = new JSONObject();
                    addrs.put(newAddr);
                    newAddr.put(JSON_KEY_ZIP, value);
                } else if(mimetype.contains("/photo")) {
                    item.put(JSON_KEY_PHOTO, value);
                } else if(mimetype.contains("/group")) {
                    item.put(JSON_KEY_GROUP, value);
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        contactInfoCursor.close();
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
        return PHONES_PROJECTION;
    }

    @Override
    protected Uri getContentUri() {
        return CONTENT_URI;
    }

    @Override
    protected String getSortOrder() {
        return null;
    }
}
