package com.zm.epad.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import java.util.ArrayList;

public class NetworkStatusMonitor extends BroadcastReceiver {

    public interface NetworkStatusReport {
        public void reportNetworkStatus(boolean bConnected);
    }

    public static int XMPPCONLISTENER_TYPE_WORK = 0;
    public static int XMPPCONLISTENER_TYPE_LOG = 1;
    private static String TAG = "NetworkStatusMonitor";

    public NetworkStatusMonitor(Context context) {
        mContext = context;
    }

    private boolean mbNetworkConnected = false;
    private Context mContext = null;
    private ArrayList<NetworkStatusReport> mReportees = new ArrayList<NetworkStatusReport>();

    public boolean isNetworkConnected() {
        return mbNetworkConnected;
    }

    public void start() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(this, filter);
    }

    public void addReportee(NetworkStatusReport reportee) {
        synchronized (mReportees) {
            mReportees.add(reportee);
        }
    }

    public void stop() {
        mContext.unregisterReceiver(this);
        synchronized (mReportees) {
            mReportees.clear();
            mbNetworkConnected = false;
        }

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
            Object[] reports = null;
            boolean shouldReport = false;
            synchronized (this) {
                reports = mReportees.toArray(); // multithread handling. we just
                                                // get a snapshot of current
                                                // mReportees.
                if (networkConnected != mbNetworkConnected) {
                    mbNetworkConnected = networkConnected;
                    if (reports != null)
                        shouldReport = true;
                }
            }
            if (shouldReport) {
                for (Object reportee : reports) {
                    ((NetworkStatusReport) reportee)
                            .reportNetworkStatus(networkConnected);
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
