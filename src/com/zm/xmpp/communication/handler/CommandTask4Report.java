package com.zm.xmpp.communication.handler;

import java.util.List;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;

import com.zm.epad.core.CoreConstants;
import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;
import com.zm.epad.plugins.RemoteDeviceManager;
import com.zm.epad.plugins.RemoteDeviceManager.RemoteLocation;
import com.zm.epad.plugins.RemotePackageManager;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.client.ZMIQResult;
import com.zm.xmpp.communication.command.Command4Report;
import com.zm.xmpp.communication.result.IResult;
import com.zm.xmpp.communication.Constants;

public class CommandTask4Report extends PairCommandTask {
    private static final String TAG = "CommandTask4Report";

    private static final PairAction[] pairActions = { new PairAction(
            Constants.XMPP_REPORT_LOCATE, Constants.XMPP_REPORT_UNLOCATE) };

    private static class PairAction {
        public String start;
        public String end;

        public PairAction(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }

    private final String mAction;
    private final String mPair;
    private final boolean mIsStart;
    private final int NORMAL_RESULT_COUNT = 1;
    private int mResultCount;

    private long APP_DEFAULT_INTERVAL = 5 * 1000;
    private long POSITION_DEFAULT_INTERVAL = 5 * 60 * 1000;  //milliseconds
    private int POSITION_DEFAULT_DISTANCE = 50; //meters

    public CommandTask4Report(SubSystemFacade subSystemFacade, Handler handler,
            ResultFactory factory, ZMIQCommand command) {
        super(subSystemFacade, handler, factory, command);
        mAction = command.getCommand().getAction();
        for (PairAction pa : pairActions) {
            if (mAction.equals(pa.start)) {
                mPair = pa.end;
                mIsStart = true;
                return;
            } else if (mAction.equals(pa.end)) {
                mPair = pa.start;
                mIsStart = false;
                return;
            }
        }
        LogManager.local(TAG, "bad action:" + mAction);
        mPair = null;
        mIsStart = false;
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
                POSITION_DEFAULT_INTERVAL, POSITION_DEFAULT_DISTANCE,
                new LocationCallback());
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

    public String getCommandAction() {
        return mAction;
    }

    private String getNameWithoutResource(String wholeName) {
        int slash = wholeName.indexOf("/");
        return wholeName.substring(0, slash);
    }

    @Override
    protected void postResult(IResult result) {
        ZMIQResult IQResult = new ZMIQResult(mIQCommand);
        if (++mResultCount > NORMAL_RESULT_COUNT + 1) {
            // As the request of server team, send result without
            // resource if it's not the 1st feature result
            String to = getNameWithoutResource(IQResult.getTo());
            IQResult.setTo(to);
        }

        LogManager.local(TAG, "result count:" + mResultCount + "; to:"
                + IQResult.getTo());

        IQResult.setResult(result);
        Message msg = mHandler.obtainMessage(EVT_RESULT, IQResult);
        mHandler.sendMessage(msg);
    }

    @Override
    public boolean isDuplicated(PairCommandTask task) {
        if (task instanceof CommandTask4Report) {
            return ((CommandTask4Report) task).getCommandAction().equals(
                    mAction);
        }
        return false;
    }

    @Override
    public boolean isPaired(PairCommandTask task) {
        if (task instanceof CommandTask4Report) {
            return ((CommandTask4Report) task).getCommandAction().equals(mPair);
        }
        return false;
    }

    @Override
    public boolean isStartCommand() {
        return mIsStart;
    }

    @Override
    protected void closeWithoutPair() {
        if (mAction.equals(Constants.XMPP_REPORT_LOCATE)) {
            mResultCount = 0;
            stopTrackLocation();
        }
    }

    @Override
    protected int handleCommand(ZMIQCommand command) {
        int ret = FAILED;
        if (mAction.equals(Constants.XMPP_REPORT_LOCATE)) {
            mResultCount = 0;
            ret = startTrackLocation();
        } else if (mAction.equals(Constants.XMPP_REPORT_UNLOCATE)) {
            ret = stopTrackLocation();
        }
        postResult(ret);
        return ret;
    }

    private void postResult(int status) {
        IResult r = null;
        switch (status) {
        case SUCCESS:
            r = mResultFactory.getNormalResult(mIQCommand.getCommand(), true,
                    null);
            break;
        case FAILED:
            r = mResultFactory.getNormalResult(mIQCommand.getCommand(), false,
                    "API failed");
            break;
        default:
            break;
        }

        if (r != null) {
            postResult(r);
        }
    }

}
