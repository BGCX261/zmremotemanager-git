package com.zm.epad.plugins;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.Browser;

public class RemoteWebManager {

    private static final String TAG = "RemoteWebManager";

    private Context mContext;

    public RemoteWebManager(Context context) {
        mContext = context;
    }

    public class WebVisitInfo {
        public String url;
        public String title;
        public long lastDate;
        public int visits;
    }

    public List<WebVisitInfo> getBrowerHistory() {
        List<WebVisitInfo> history = new ArrayList<WebVisitInfo>();

        ContentResolver contentResolver = mContext.getContentResolver();
        String[] projection = { Browser.BookmarkColumns.BOOKMARK,
                Browser.BookmarkColumns.URL, Browser.BookmarkColumns.TITLE,
                Browser.BookmarkColumns.DATE, Browser.BookmarkColumns.VISITS };

        Cursor cursor = contentResolver.query(Browser.BOOKMARKS_URI,
                projection, null, null, null);
        while (cursor != null && cursor.moveToNext()) {
            if (cursor.getInt(cursor
                    .getColumnIndex(Browser.BookmarkColumns.BOOKMARK)) == 0) {
                WebVisitInfo wi = new WebVisitInfo();
                wi.url = cursor.getString(cursor
                        .getColumnIndex(Browser.BookmarkColumns.URL));
                wi.title = cursor.getString(cursor
                        .getColumnIndex(Browser.BookmarkColumns.TITLE));
                wi.lastDate = cursor.getLong(cursor
                        .getColumnIndex(Browser.BookmarkColumns.DATE));
                wi.visits = cursor.getInt(cursor
                        .getColumnIndex(Browser.BookmarkColumns.VISITS));
                history.add(wi);
            }
        }

        return history;
    }
}
