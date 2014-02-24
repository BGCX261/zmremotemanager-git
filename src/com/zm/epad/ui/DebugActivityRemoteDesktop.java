package com.zm.epad.ui;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.apache.http.conn.util.InetAddressUtils;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.zm.epad.R;
import com.zm.epad.plugins.RemoteDesktopManager;

public class DebugActivityRemoteDesktop extends Activity {
    public static final String TAG = "DebugActivityRemoteDesktop";

    private Button mBtnOpenRtsp;
    private Button mBtnCloseRtsp;
    private final TextView mTextIP[] = new TextView[3];
    private final TextView mTextDisplay[] = new TextView[3];

    private RemoteDesktopManager mRdManager;
    private String mIface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_desktop);

        mBtnOpenRtsp = (Button) findViewById(R.id.btn_open_rtsp);
        mBtnCloseRtsp = (Button) findViewById(R.id.btn_close_rtsp);
        mTextIP[0] = (TextView) findViewById(R.id.text_rd_ip1);
        mTextIP[1] = (TextView) findViewById(R.id.text_rd_ip2);
        mTextIP[2] = (TextView) findViewById(R.id.text_rd_ip3);
        mTextDisplay[0] = (TextView) findViewById(R.id.text_rd_display1);
        mTextDisplay[1] = (TextView) findViewById(R.id.text_rd_display2);
        mTextDisplay[2] = (TextView) findViewById(R.id.text_rd_display3);

        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            cm.setUsbTethering(true);
        }

        mRdManager = new RemoteDesktopManager(this);

        mBtnOpenRtsp.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                int i = 0;
                try {
                    Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                    while (en.hasMoreElements() && i < 3) {
                        NetworkInterface nif = en.nextElement();
                        if (!nif.supportsMulticast()) continue;
                        Enumeration<InetAddress> inet = nif.getInetAddresses();
                        while (inet.hasMoreElements() && i < 3) {
                            InetAddress ip = inet.nextElement();
                            if (!ip.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ip.getHostAddress())) {
                                mTextIP[i].setText("IP " + (i+1) + ": " +
                                    ip.getHostAddress() + " " + nif.getName() +
                                    " " + nif.getIndex());
                                    if (mIface == null) mIface = ip.getHostAddress();
                                    i++;
                            }
                        }
                    }
                } catch (SocketException e) {
                    Log.e(TAG, "Get host IP address failed");
                    e.printStackTrace();
                }
                if (mTextIP[0].getText() != null && !mTextIP[0].getText().equals("")) {
                    mTextIP[0].setText(mTextIP[0].getText() + " " + mRdManager.getActiveNetworkType());
                }
                i = 0;
                for (RemoteDesktopManager.DisplayParam dp : mRdManager.getDisplayParams()) {
                    mTextDisplay[i].setText("Display param " + (i+1) + ": w=" +
                        dp.mWidth + " h=" + dp.mHeight + " dpi=" + dp.mDpi);
                    i++;
                    if (i == 3) break;
                }
                // Start Remote Desktop
                if (mIface != null) {
                    mRdManager.startRemoteDesktop();
                    String url = mRdManager.getUrl();
                    if (url != null) {
                        if (mTextIP[1].getText() == null || mTextIP[1].getText().length() == 0) {
                            mTextIP[1].setText("Rtsp server running now: " + url);
                        }
                    }
                } else {
                    if (mTextIP[1].getText() == null || mTextIP[1].getText().length() == 0) {
                        mTextIP[1].setText("Error: No Network available!");
                    }
                }
                //
                mBtnOpenRtsp.setVisibility(View.GONE);
                mBtnCloseRtsp.setVisibility(View.VISIBLE);
            }
        });

        mBtnCloseRtsp.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                for (TextView tv : mTextIP) {
                    tv.setText("");
                }
                for (TextView tv : mTextDisplay) {
                    tv.setText("");
                }
                mIface = null;
                mBtnCloseRtsp.setVisibility(View.GONE);
                mBtnOpenRtsp.setVisibility(View.VISIBLE);
            }
        });
    }
}
