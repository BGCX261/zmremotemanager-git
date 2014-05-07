package com.zm.epad.ui;

import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;

import com.zm.epad.core.Config;
import com.zm.epad.core.CoreConstants;
import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;
import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.client.ZMStringCommand;
import com.zm.xmpp.communication.command.Command4App;
import com.zm.xmpp.communication.command.Command4Query;
import com.zm.xmpp.communication.command.Command4Report;
import com.zm.xmpp.communication.command.ICommand;

import org.jivesoftware.smack.XMPPConnection;

import com.zm.epad.R;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class DebugActivitySender extends Activity {
    public static final String TAG = "DebugActivitySender";

    public final String LOG_UPLOAD_ADDRESS = "http://192.168.0.102:8080/LogUpload/fileupload";
    private Button mDisableBtn = null;
    private Button mEnableBtn = null;
    private Button mRemoveBtn = null;
    private Button mInstallBtn = null;

    private Button mAppInfoBtn = null;
    private Button mDeviceInfoBtn = null;
    private Button mUserInfoBtn = null;

    private Button mAppTraceBtn = null;
    private Button mAppUntraceBtn = null;
    private Button mPosTraceBtn = null;
    private Button mPosUntraceBtn = null;

    private Button mPolicyBtn = null;
    private Button mLogsBtn = null;

    private Button mDeviceAdmin = null;

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

        // IP = getIntent().getExtras().getString("ServerIP");
        IP = Config.getInstance().getConfig(Config.SERVER_ADDRESS);

        testConnectionOn();
        mDeviceAdmin = (Button) findViewById(R.id.button0);

        mDisableBtn = (Button) findViewById(R.id.button1);
        mEnableBtn = (Button) findViewById(R.id.button2);
        mRemoveBtn = (Button) findViewById(R.id.button3);
        mInstallBtn = (Button) findViewById(R.id.button4);

        mAppInfoBtn = (Button) findViewById(R.id.button5);
        mDeviceInfoBtn = (Button) findViewById(R.id.button6);
        mUserInfoBtn = (Button) findViewById(R.id.button7);

        mAppTraceBtn = (Button) findViewById(R.id.button8);
        mPosTraceBtn = (Button) findViewById(R.id.button11);
        mPosUntraceBtn = (Button) findViewById(R.id.button12);

        mPolicyBtn = (Button) findViewById(R.id.button10);

        mNameText = (EditText) findViewById(R.id.editText1);
        mUserIdText = (EditText) findViewById(R.id.editText2);

        mLogsBtn = (Button) findViewById(R.id.button13);

        mDeviceAdmin.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                android.content.Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.zm.epad",
                        "com.zm.epad.ui.RemoteDeviceAdmin"));
                startActivity(intent);
            }
        });

        mDisableBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                try {
                    SendTestCommand4App(Constants.XMPP_APP_DISABLE);
                } catch (Exception e) {
                    LogManager.local(TAG, e.getMessage());
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
                    // cmdIQ.setTo("dengfanping@com.zm.communication/Smack");
                    // cmdIQ.setFrom("test@com.zm.communication/Smack");
                    // cmdIQ.setPacketID("xyzzd");
                    // cmdIQ.setCmdType("enable");
                    // cmdIQ.setCmdArgs(name);
                    // LogManager.e("XmppClient", "test send msg "
                    // +cmdIQ.toString());
                    // testConnection.sendPacket(cmdIQ);
                    SendTestCommand4App(Constants.XMPP_APP_ENABLE);
                } catch (Exception e) {
                    LogManager.local(TAG, e.getMessage());
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
                    LogManager.local(TAG, e.getMessage());
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
                    LogManager.local(TAG, e.getMessage());
                }

            }

        });

        mAppInfoBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                try {
                    sendTestCommand4Query(Constants.XMPP_QUERY_APP);
                } catch (Exception e) {
                    LogManager.local(TAG, e.getMessage());
                }
            }

        });

        mDeviceInfoBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                try {
                    sendTestCommand4Query(Constants.XMPP_QUERY_DEVICE);
                } catch (Exception e) {
                    LogManager.local(TAG, e.getMessage());
                }
            }

        });

        mUserInfoBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                try {
                    sendTestCommand4Query(Constants.XMPP_QUERY_ENV);
                } catch (Exception e) {
                    LogManager.local(TAG, e.getMessage());
                }
            }

        });

        mAppTraceBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                try {
                    sendTestCommand4Query(Constants.XMPP_QUERY_RUNNING);
                } catch (Exception e) {
                    LogManager.local(TAG, e.getMessage());
                }
            }

        });

        mPosTraceBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // Debug.waitForDebugger();
                // TODO Auto-generated method stub
                try {
                    sendTestCommand4Report(null, Constants.XMPP_REPORT_LOCATE);
                } catch (Exception e) {
                    LogManager.local(TAG, e.getMessage());
                }
            }

        });

        mPosUntraceBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                try {
                    sendTestCommand4Report(null, Constants.XMPP_REPORT_UNLOCATE);
                } catch (Exception e) {
                    LogManager.local(TAG, e.getMessage());
                }
            }

        });

        mPolicyBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                try {
                    sendTestCommand4Policy();
                } catch (Exception e) {
                    LogManager.local(TAG, e.getMessage());
                }
            }

        });
        mLogsBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                try {
                    LogManager.getInstance().uploadLog(LOG_UPLOAD_ADDRESS,
                            CoreConstants.CONSTANT_INT_LOGTYPE_COMMON,
                            Calendar.getInstance());
                } catch (Exception e) {
                    LogManager.local(TAG, e.getMessage());
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
                    testConnection.login("test", "test", "default");
                } catch (Exception e) {
                    LogManager.local(TAG, "testConnection" + e.toString());
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
                    LogManager.local(TAG, "testConnection" + e.toString());
                }
            }

        });
        testThread.start();
    }

    void SendTestCommand4App(String action) {
        String name = mNameText.getText().toString();
        String userId = mUserIdText.getText().toString();
        LogManager.local(TAG, "action:" + action + ", name:" + name
                + ", userId:" + userId);

        int IntUid = 0;
        try {
            IntUid = Integer.valueOf(userId);
        } catch (Exception e) {
            LogManager.local(TAG, "userId:" + userId + ";" + e.toString());
            IntUid = 0;
        }

        Command4App Command = new Command4App(Constants.XMPP_NAMESPACE_CENTER,
                "9527", action, "time2014", name, "ver1.1.1", name, IntUid,
                null);
        LogManager.local(TAG, "##Command4App##\n" + Command.toXML());

        testConnection.sendPacket(getIQCommand(Command));
        LogManager.local(TAG, "##IQ##\n" + Command.toXML());
    }

    void sendTestCommand4Query(String action) {
        Command4Query Command = new Command4Query(
                Constants.XMPP_NAMESPACE_CENTER, "9528", action, "time2014");
        LogManager.local(TAG, "##Command4Query##\n" + Command.toXML());

        testConnection.sendPacket(getIQCommand(Command));
        LogManager.local(TAG, "##IQ##\n" + Command.toXML());
    }

    void sendTestCommand4Report(String report, String action) {
        Command4Report Command = new Command4Report(
                Constants.XMPP_NAMESPACE_CENTER, "9529", report, action, "2014");

        testConnection.sendPacket(getIQCommand(Command));
        LogManager.local(TAG, "##IQ##\n" + Command.toXML());
    }

    void sendTestCommand4Policy() {
        try {
            ZMStringCommand Command = new ZMStringCommand();
            File file = new File(mContext.getFilesDir().getAbsolutePath(),
                    "testpolicy.xml");
            if (!file.exists()) {
                return;
            }
            FileInputStream in = new FileInputStream(file);
            byte[] policyForm = new byte[(int) file.length()];
            in.read(policyForm);
            in.close();
            String policy = new String(policyForm, "utf-8");
            Command.setType("policy");
            Command.setContent(policy);

            testConnection.sendPacket(getIQCommand(Command));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ZMIQCommand getIQCommand(ICommand command) {
        ZMIQCommand cmdIQ = new ZMIQCommand();
        cmdIQ.setCommand(command);
        String target = Config.getDeviceId() + "@com.zm.communication/"
                + Config.getInstance().getConfig(Config.RESOURCE);
        cmdIQ.setTo(target);
        return cmdIQ;
    }

    @Override
    protected void onDestroy() {
        testConnectionOff();
        super.onDestroy();
    }
}
