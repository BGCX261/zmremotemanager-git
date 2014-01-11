package com.zm.epad.ui;

import com.zm.epad.R;
import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.command.Command4App;

import org.jivesoftware.smack.XMPPConnection;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class DebugActivitySender extends Activity {
    public static final String TAG = "DebugActivitySender";

    private Button mDisableBtn = null;
    private Button mEnableBtn = null;
    private Button mRemoveBtn = null;
    private Button mInstallBtn = null;

    private EditText mNameText = null;
    private EditText mUserIdText = null;
    private Context mContext = null;
    private XMPPConnection testConnection = null;
    private String IP = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sender);
        mContext = this;

        IP = getIntent().getExtras().getString("ServerIP");

        testConnectionOn();
        mDisableBtn = (Button) findViewById(R.id.button1);
        mEnableBtn = (Button) findViewById(R.id.button2);
        mRemoveBtn = (Button) findViewById(R.id.button3);
        mInstallBtn = (Button) findViewById(R.id.button4);

        mNameText = (EditText) findViewById(R.id.editText1);
        mUserIdText = (EditText) findViewById(R.id.editText2);

        mDisableBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                try {
                    SendTestCommand4App(Constants.XMPP_APP_DISABLE);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }

            }

        });

        mEnableBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                try {
                    // String name = mNameText.getText().toString();
                    // String userId = mUserIdText.getText().toString();
                    // Log.v(TAG, "Enable name: "+ name+", userId: "+ userId);

                    // RemotePackageIQ cmdIQ = new RemotePackageIQ();
                    // cmdIQ.setTo("dengfanping@com.zm.openfire/Smack");
                    // cmdIQ.setFrom("test@com.zm.openfire/Smack");
                    // cmdIQ.setPacketID("xyzzd");
                    // cmdIQ.setCmdType("enable");
                    // cmdIQ.setCmdArgs(name);
                    // LogManager.e("XmppClient", "test send msg "
                    // +cmdIQ.toString());
                    // testConnection.sendPacket(cmdIQ);
                    SendTestCommand4App(Constants.XMPP_APP_ENABLE);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }

        });

        mRemoveBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                try {
                    SendTestCommand4App(Constants.XMPP_APP_REMOVE);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }

            }

        });

        mInstallBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                try {
                    SendTestCommand4App(Constants.XMPP_APP_INSTALL);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }

            }

        });

    }

    void testConnectionOn() {

        Thread testThread = new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub

                try {
                    testConnection = new XMPPConnection(IP, null);
                    testConnection.connect();
                    testConnection.login("test", "test", "zhimotech");
                } catch (Exception e) {
                    Log.e(TAG, "testConnection" + e.toString());
                }

            }

        });
        testThread.start();
    }

    void testConnectionOff() {

        Thread testThread = new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub

                try {
                    if (testConnection != null)
                        testConnection.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "testConnection" + e.toString());
                }
            }

        });
        testThread.start();
    }

    void SendTestCommand4App(String action) {
        String name = mNameText.getText().toString();
        String userId = mUserIdText.getText().toString();
        Log.v(TAG, "action:" + action + ", name:" + name + ", userId:" + userId);

        int IntUid = 0;
        try {
            IntUid = Integer.valueOf(userId);
        } catch (Exception e) {
            Log.v(TAG, "userId:" + userId + ";" + e.toString());
            IntUid = 0;
        }

        Command4App Command = new Command4App(Constants.XMPP_NAMESPACE_CENTER, "9527",
                action, "time2014", name, "ver1.1.1", "/sdcard/testinstall/"
                        + name, IntUid, null);
        Log.v(TAG, "##Command##\n" + Command.toXML());

        ZMIQCommand cmdIQ = new ZMIQCommand();
        cmdIQ.setCommand(Command);
        cmdIQ.setTo("dengfanping@com.zm.openfire/zhimotech");
        cmdIQ.setFrom("test@com.zm.openfire/zhimotech");
        cmdIQ.setPacketID("xyzzd");
        testConnection.sendPacket(cmdIQ);
        Log.v(TAG, "##IQ##\n" + cmdIQ.toXML());
    }

    @Override
    protected void onDestroy() {
        testConnectionOff();
        super.onDestroy();
    }
}
