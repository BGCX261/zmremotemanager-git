package com.android.remotemanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;

import org.jivesoftware.smack.ConnectionListener;
import java.util.ArrayList;

public class NetworkStatusMonitor extends BroadcastReceiver {

    public interface NetworkStatusReport {
        public void reportNetworkStatus(boolean bConnected);

        public void reportXMPPConnectionStatus(int type, boolean bConnected);
    }

    public class XMPPConnectionListener implements ConnectionListener {

        int mType;

        public XMPPConnectionListener(int type) {
            mType = type;
        }

        @Override
        public void connectionClosed() {
            // TODO Auto-generated method stub
            Log.e(TAG, "connection closed");

        }

        @Override
        public void connectionClosedOnError(Exception e) {
            // TODO Auto-generated method stub
            Log.e(TAG, "connection closed on error");

        }

        @Override
        public void reconnectingIn(int seconds) {
            // TODO Auto-generated method stub
            Log.e(TAG, "reconnectingIn " + seconds + " seconds");

        }

        @Override
        public void reconnectionSuccessful() {
            // TODO Auto-generated method stub
            Log.e(TAG, "reconnectionSuccessful");
        }

        @Override
        public void reconnectionFailed(Exception e) {
            // TODO Auto-generated method stub
            Log.e(TAG, "reconnectionFailed " + e.getMessage());
        }
    }

    public static int XMPPCONLISTENER_TYPE_WORK = 0;
    public static int XMPPCONLISTENER_TYPE_LOG = 1;
    private static String TAG = "NetworkStatusMonitor";

    private XMPPConnectionListener[] mXmppConListeners = null;

    public NetworkStatusMonitor(Context context) {
        mContext = context;
        mXmppConListeners = new XMPPConnectionListener[] {
                new XMPPConnectionListener(XMPPCONLISTENER_TYPE_WORK),
                new XMPPConnectionListener(XMPPCONLISTENER_TYPE_LOG) };
    }

    private boolean mbNetworkConnected = false;
    private Context mContext = null;
    private ArrayList<NetworkStatusReport> mReportees = new ArrayList<NetworkStatusReport>();

    public boolean isNetworkConnected() {
        return mbNetworkConnected;
    }

    public XMPPConnectionListener getXmppConnectionListener(int type) {
        return mXmppConListeners[type];
    }

    public void start(NetworkStatusReport reportees[]) {
        for (NetworkStatusReport reportee : reportees) {
            mReportees.add(reportee);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(this, filter);
    }

    public void stop() {
        mContext.unregisterReceiver(this);
        mReportees.clear();
        mbNetworkConnected = false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            boolean networkConnected = false;
            if (intent.getBooleanExtra(
                    ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
                networkConnected = false;
            } else {
                networkConnected = true;
            }
            // currently, we don't care if the network type if wifi or mobile
            if (networkConnected != mbNetworkConnected) {
                mbNetworkConnected = networkConnected;
                for (NetworkStatusReport reportee : mReportees) {
                    reportee.reportNetworkStatus(mbNetworkConnected);
                }
            }

            /*
             * NetworkInfo info =
             * intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO
             * ); ConnectivityManager connManager = (ConnectivityManager)
             * mContext.getSystemService(Context.CONNECTIVITY_SERVICE); info =
             * connManager.getNetworkInfo(info.getType());
             * 
             * if(info.getType() == ConnectivityManager.TYPE_WIFI){
             * 
             * }
             */

        }
    }
}
