package com.zm.xmpp.communication.handler;

import java.util.List;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.os.Handler;
import android.provider.Settings;

import com.zm.epad.core.SubSystemFacade;
import com.zm.epad.plugins.RemoteDeviceManager;
import com.zm.epad.plugins.RemoteDeviceManager.RemoteLocation;
import com.zm.epad.plugins.RemotePackageManager;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.command.Command4Report;
import com.zm.xmpp.communication.result.IResult;
import com.zm.xmpp.communication.Constants;

public class CommandTask4Report extends PairCommandTask {
    private static final String TAG = "CommandTask4Report";
    private String mReport;

    private long APP_DEFAULT_INTERVAL = 5 * 1000;
    private long POSITION_DEFAULT_INTERVAL = 5 * 1000;

    public CommandTask4Report(SubSystemFacade subSystemFacade, Handler handler,
            ResultFactory factory, ZMIQCommand command) {
        super(subSystemFacade, handler, factory, command);
        mReport = getCommandReport();
    }

    private int startMonitorAppRunningInfo(long interval) {

        mSubSystemFacade.startMonitorRunningAppInfo(interval,
                new RemotePackageManager.ReportRunningAppInfo() {

                    @Override
                    public void reportRunningAppProcessInfos(
                            List<RunningAppProcessInfo> infos) {
                        IResult iResult = mResultFactory
                                .getRunningAppResult(infos);
                        postResult(iResult);
                    }

                });
        return SUCCESS;
    }

    private int stopMonitorAppRunningInfo() {
        mSubSystemFacade.stopMonitorRunningAppInfo();
        return SUCCESS;
    }

    private int startTrackLocation() {
        boolean ret = false;
        ret = mSubSystemFacade.startTrackLocation(
                Settings.Secure.LOCATION_MODE_HIGH_ACCURACY,
                POSITION_DEFAULT_INTERVAL, 100, new LocationCallback());
        return ret == true ? SUCCESS : FAILED;
    }

    private int stopTrackLocation() {
        mSubSystemFacade.stopTrackLocation();
        return SUCCESS;
    }

    private class LocationCallback implements
            RemoteDeviceManager.LocationReportCallback {

        @Override
        public void reportLocation(RemoteLocation loc) {
            IResult result = mResultFactory
                    .getResult(ResultFactory.RESULT_POSITION, getCommandId(),
                            (Object) loc);
            postResult(result);
        }

        @Override
        public void reportLocationTrackStatus(boolean bRunning) {
            // do nothing

        }

    }

    private int start(ZMIQCommand start) {
        int ret = FAILED;
        if (mReport.equals(Constants.XMPP_REPORT_APP)) {
            ret = startMonitorAppRunningInfo(APP_DEFAULT_INTERVAL);
        } else if (mReport.equals(Constants.XMPP_REPORT_POS)) {
            ret = startTrackLocation();
        }
        return ret;
    }

    private int end(ZMIQCommand end) {
        int ret = FAILED;
        if (mReport.equals(Constants.XMPP_REPORT_APP)) {
            ret = stopMonitorAppRunningInfo();
        } else if (mReport.equals(Constants.XMPP_REPORT_POS)) {
            ret = stopTrackLocation();
        }
        return ret;
    }

    public String getCommandReport() {
        return ((Command4Report) mIQCommand.getCommand()).getReport();
    }

    @Override
    public boolean isDuplicated(PairCommandTask task) {
        if (task instanceof CommandTask4Report) {
            return mReport.equals(((CommandTask4Report) task)
                    .getCommandReport())
                    && task.isStartCommand() == isStartCommand();
        }
        return false;
    }

    @Override
    public boolean isPaired(PairCommandTask task) {
        if (task instanceof CommandTask4Report) {
            return mReport.equals(((CommandTask4Report) task)
                    .getCommandReport())
                    && task.isStartCommand() != isStartCommand();
        }
        return false;
    }

    @Override
    public boolean isStartCommand() {
        Command4Report cmd = (Command4Report) mIQCommand.getCommand();

        return cmd.getAction().equals(Constants.XMPP_REPORT_ACT_TRACE) ? true
                : false;
    }

    @Override
    protected void closeWithoutPair() {
        if (mReport.equals(Constants.XMPP_REPORT_APP)) {
            stopMonitorAppRunningInfo();
        } else if (mReport.equals(Constants.XMPP_REPORT_POS)) {
            stopTrackLocation();
        }
    }

    @Override
    protected int handleCommand(ZMIQCommand command) {
        int ret = FAILED;
        if (isStartCommand()) {
            ret = start(command);
        } else {
            ret = end(command);
        }
        return ret;
    }

}
