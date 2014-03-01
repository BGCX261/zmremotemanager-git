package com.zm.epad.ui;

import com.zm.epad.core.LogManager;

import org.jivesoftware.smack.XMPPConnection;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.zm.epad.R;

public class DebugActivity extends ListActivity {

    public static final String TAG = "DebugActivity";
    List<Map<String, String>> mTestFunctions = new ArrayList<Map<String, String>>();
    private static String TEST_XMPP = "test xmpp";
    private static String TEST_DEVICE_ADMIN = "test device admin";
    private String ip = null;
    private String username = null;
    private String password = null;
    private Context mContext = null;

    LogManager mLogManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        ip = getIntent().getExtras().getString("ServerIP");
        username = getIntent().getExtras().getString("UserName");
        password = getIntent().getExtras().getString("Password");
        LogManager.local(TAG, ip);

        connectLogManager(ip);
        mContext = this;

        Map<String, String> test1 = new HashMap<String, String>();
        test1.put("title", TEST_XMPP);
        test1.put("value", TEST_XMPP);
        mTestFunctions.add(test1);

        Map<String, String> test2 = new HashMap<String, String>();
        test2.put("title", TEST_DEVICE_ADMIN);
        test2.put("value", TEST_DEVICE_ADMIN);
        mTestFunctions.add(test2);

        setListAdapter(new SimpleAdapter(this, mTestFunctions,
                android.R.layout.simple_list_item_1, new String[] { "title" },
                new int[] { android.R.id.text1 }));
        getListView().setTextFilterEnabled(true);

        testXMPP();

    }

    private void connectLogManager(String ip) {
        try {
            /*
             * mLogManager = LogManager.getLogManagerInstance(ip, this);
             * mLogManager.start();
             */
        } catch (Exception e) {
            LogManager.local(TAG, "LogManager" + e.toString());
        }

    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Map<String, String> map = (Map<String, String>) l
                .getItemAtPosition(position);
        String clickedItem = map.get("value");
        if (clickedItem.equals(TEST_XMPP)) {
            // testXMPP();
            Intent intent = new Intent();
            intent.setClass(mContext, DebugActivitySender.class);
            intent.putExtra("ServerIP", ip);
            startActivity(intent);
        } else if (clickedItem.equals(TEST_DEVICE_ADMIN)) {
            android.content.Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.zm.epad",
                    "com.zm.epad.plugins.RemoteDeviceAdmin"));
            startActivity(intent);
        }

    }

    void testXMPP() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.zm.epad",
                "com.zm.epad.core.RemoteManagerService"));

        Bundle args = new Bundle();
        args.putString("server", ip);
        args.putString("username", username);
        args.putString("password", password);
        args.putString("resource", "zhimotech");

        intent.putExtras(args);

        try {
            if (startService(intent) != null) {
                LogManager.local(TAG, "start service succeed");

                /*
                 * don't start the feature now WallpaperManager wm =
                 * WallpaperManager.getInstance(mContext); try { int id =
                 * getResources(). getIdentifier("wall", "drawable",
                 * "com.zm.epad"); wm.setResource(id); } catch (Exception e) {
                 * e.printStackTrace(); }
                 */

            } else
                LogManager.local(TAG, "start service failed");
        } catch (Exception e) {
            LogManager.local(TAG, "start service failed " + e.getMessage());
        }

        /*
         * Thread testThread = new Thread(new Runnable(){
         * 
         * @Override public void run() { try { Thread.sleep(5000); //wait
         * 5seconds to RemoteManagerService to get ready! //testXmppClient(); }
         * catch (Exception e) { // TODO: handle exception }
         * 
         * 
         * }
         * 
         * }); testThread.start();
         */

    }

    private void testXmppClient() {
        try {
            XMPPConnection testConnection = new XMPPConnection(ip, null);
            testConnection.connect();
            testConnection.login("test", "test");
            Thread.sleep(5000);
            /*
             * RemotePackageIQ cmdIQ = new RemotePackageIQ();
             * cmdIQ.setTo("dengfanping@com.zm.openfire/Smack");
             * cmdIQ.setFrom("test@com.zm.openfire/Smack");
             * cmdIQ.setPacketID("xyzzd"); cmdIQ.setCmdType("enable");
             * cmdIQ.setCmdArgs("com.android.browser");
             * LogManager.e("XmppClient", "test send msg " +cmdIQ.toString());
             * testConnection.sendPacket(cmdIQ);
             */
        } catch (Exception e) {
            LogManager.local("XmppClient", "testXmppClient " + e.getMessage());
        }

    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();

    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub

        super.onStop();
    }

    @Override
    protected void onDestroy() {

        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.zm.epad",
                "com.zm.epad.core.RemoteManagerService"));
        try {
            stopService(intent);

        } catch (Exception e) {
            LogManager.local(TAG, "stop service failed " + e.getMessage());
        }
        // TODO Auto-generated method stub
        super.onDestroy();

    }

}
