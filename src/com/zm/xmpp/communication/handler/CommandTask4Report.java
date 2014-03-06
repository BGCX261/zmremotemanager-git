package com.zm.xmpp.communication.handler;

import java.util.List;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.os.Handler;

import com.zm.epad.core.SubSystemFacade;
import com.zm.epad.plugins.RemotePackageManager;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.command.Command4Report;
import com.zm.xmpp.communication.result.IResult;
import com.zm.xmpp.communication.Constants;

public class CommandTask4Report extends PairCommandTask {
    private static final String TAG = "CommandTask4Report";
    private String mReport;

    private long APP_DEFAULT_INTERVAL = 5 * 1000;

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

    private int start(ZMIQCommand start) {
        int ret = FAILED;
        if (mReport.equals(Constants.XMPP_REPORT_APP)) {
            ret = startMonitorAppRunningInfo(APP_DEFAULT_INTERVAL);
        } else if (mReport.equals(Constants.XMPP_REPORT_POS)) {

        }
        return ret;
    }

    private int end(ZMIQCommand end) {
        int ret = FAILED;
        if (mReport.equals(Constants.XMPP_REPORT_APP)) {
            ret = stopMonitorAppRunningInfo();
        } else if (mReport.equals(Constants.XMPP_REPORT_POS)) {

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
