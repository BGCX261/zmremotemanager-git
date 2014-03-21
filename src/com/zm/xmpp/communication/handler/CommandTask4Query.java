package com.zm.xmpp.communication.handler;

import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.zm.epad.core.CoreConstants;
import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;
import com.zm.epad.plugins.RemoteFileManager;
import com.zm.epad.plugins.RemoteFileManager.FileTransferTask;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.client.ZMIQResult;
import com.zm.xmpp.communication.result.IResult;
import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.command.ICommand4Query;
import com.zm.xmpp.communication.Constants;

public class CommandTask4Query extends CommandTask {
    public CommandTask4Query(SubSystemFacade subSystemFacade, Handler handler,
            ResultFactory factory, ZMIQCommand command) {
        super(subSystemFacade, handler, factory, command);
    }

    private static final String TAG = "CommandTask4Query";

    @Override
    protected int handleCommand(ZMIQCommand command) {
        int ret = FAILED;
        ICommand4Query cmd = (ICommand4Query) command.getCommand();
        String action = cmd.getAction();
        LogManager.local(TAG, "handleCommand4Query:" + action);

        if (action.equals(Constants.XMPP_QUERY_APP)) {
            ret = handleQueryApp(cmd);
        } else if (action.equals(Constants.XMPP_QUERY_DEVICE)) {
            ret = handleQueryDevice(cmd);
        } else if (action.equals(Constants.XMPP_QUERY_ENV)) {
            ret = handleQueryEnv(cmd);
        } else if (action.equals(Constants.XMPP_QUERY_CAPTURE)) {
            ret = handleQueryCapture(cmd);
        } else if (action.equals(Constants.XMPP_QUERY_LOG)) {
            ret = handleQueryLogUpload(cmd);
        } else {
            LogManager.local(TAG, "handleCommand4Query bad action");
        }

        return ret;
    }

    private String getResultStr(boolean bOK) {
        return bOK == true ? CoreConstants.CONSTANT_RESULT_OK
                : CoreConstants.CONSTANT_RESULT_NG;
    }

    private String getResultStr(Object bOK) {
        return bOK != null ? CoreConstants.CONSTANT_RESULT_OK
                : CoreConstants.CONSTANT_RESULT_NG;
    }

    private int handleQueryApp(ICommand4Query cmd) {
        List<IResult> results = mResultFactory.getResults(
                ResultFactory.RESULT_APP, cmd.getId());
        if (results == null) {
            return FAILED;
        }

        for (IResult r : results) {
            postResult(r);
        }

        return SUCCESS;
    }

    private int handleQueryDevice(ICommand4Query cmd) {
        IResult result = mResultFactory.getResult(ResultFactory.RESULT_DEVICE,
                cmd.getId());
        if (result == null) {
            return FAILED;
        }
        postResult(result);

        return SUCCESS;
    }

    private int handleQueryEnv(ICommand4Query cmd) {
        List<IResult> results = mResultFactory.getResults(
                ResultFactory.RESULT_ENV, cmd.getId());
        if (results == null) {
            return FAILED;
        }

        for (IResult r : results) {
            postResult(r);
        }

        return SUCCESS;
    }

    private void setCaptureBundleInfo(Bundle info, ICommand4Query cmd) {
        info.putString(CoreConstants.CONSTANT_COMMANDID, cmd.getId());
        info.putString(CoreConstants.CONSTANT_TYPE, cmd.getType());
        info.putString(CoreConstants.CONSTANT_ACTION, cmd.getAction());
        info.putString(CoreConstants.CONSTANT_MIME,
                CoreConstants.CONSTANT_IMG_PNG);
    }

    private int handleQueryCapture(ICommand4Query cmd) {
        Bundle info = new Bundle();
        setCaptureBundleInfo(info, cmd);

        mSubSystemFacade.uploadScreenshot(cmd.getUrl(), info,
                new RemoteFileManager.FileTransferCallback() {

                    @Override
                    public void onDone(boolean success, FileTransferTask task) {
                        String fileName = (String) task.getResult();
                        IResult result = mResultFactory.getResult(
                                ResultFactory.RESULT_NORMAL, getCommandId(),
                                getResultStr(fileName));

                        postResult(result);
                        endTask();
                    }

                    @Override
                    public void onCancel(FileTransferTask task) {
                        // when cancel, send NG
                        IResult result = mResultFactory.getResult(
                                ResultFactory.RESULT_NORMAL, getCommandId(),
                                CoreConstants.CONSTANT_RESULT_NG);

                        postResult(result);
                        endTask();
                    }
                });
        return RUNNING;
    }

    private int handleQueryLogUpload(ICommand4Query cmd) {
        LogManager logMgr = LogManager.getInstance();
        int type = CoreConstants.CONSTANT_INT_LOGTYPE_COMMON;
        String[] logs = logMgr.listLogFiles(type);
        for (String filename : logs) {
            logMgr.uploadLog(cmd.getUrl(), type, filename);
        }
        return SUCCESS;
    }
}
