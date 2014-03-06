package com.zm.xmpp.communication.handler;

import android.os.Handler;
import android.os.Message;

import com.zm.epad.core.CoreConstants;
import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;
import com.zm.epad.plugins.RemotePackageManager;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.client.ZMIQResult;
import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.command.ICommand4App;
import com.zm.xmpp.communication.result.IResult;
import com.zm.xmpp.communication.Constants;

public class CommandTask4App extends CommandTask {
    private static final String TAG = "CommandTask4App";

    public CommandTask4App(SubSystemFacade subSystemFacade, Handler handler,
            ResultFactory factory, ZMIQCommand command) {
        super(subSystemFacade, handler, factory, command);
    }

    @Override
    protected int handleCommand(ZMIQCommand command) {
        ICommand4App cmd = (ICommand4App) command.getCommand();
        int ret = SUCCESS;
        IResult result = null;

        if (cmd.getAction().equals(Constants.XMPP_APP_INSTALL)) {
            return handleCommand4AppInstall(cmd);
        }

        result = handleCommand4App(cmd);
        postResult(result);

        return ret;
    }

    private IResult handleCommand4App(ICommand4App cmd) {
        boolean ret = false;
        IResult result = null;

        LogManager.local(TAG, "handleCommand4App:" + cmd.getAction());

        if (cmd.getAction().equals(Constants.XMPP_APP_ENABLE)) {
            String name = cmd.getAppName();
            int userId = cmd.getUserId();
            ret = mSubSystemFacade.enablePkgForUser(name, userId);
        } else if (cmd.getAction().equals(Constants.XMPP_APP_DISABLE)) {
            String name = cmd.getAppName();
            int userId = cmd.getUserId();
            ret = mSubSystemFacade.disablePkgForUser(name, userId);
        } else if (cmd.getAction().equals(Constants.XMPP_APP_REMOVE)) {
            String name = cmd.getAppName();
            int userId = cmd.getUserId();
            ret = mSubSystemFacade.uninstallPkgForUser(name, userId);
        } else {
            LogManager.local(TAG, "bad action");
        }

        result = mResultFactory.getResult(ResultFactory.RESULT_NORMAL,
                cmd.getId(), getResultStr(ret));

        LogManager.local(TAG, "handleCommand4App return:" + ret);
        return result;

    }

    private int handleCommand4AppInstall(ICommand4App cmd) {

        int install = mSubSystemFacade.installPkgForUser(cmd.getAppUrl(),
                cmd.getUserId(), new RemotePackageManager.installCallback() {

                    @Override
                    public void callback(boolean result) {
                        postResult(result);
                        endTask();
                    }
                });
        if (install < 0) {
            return RUNNING;
        } else {
            postResult(install == 0 ? true : false);
        }
        return install == 0 ? SUCCESS : FAILED;
    }

    private void postResult(boolean bOK) {
        IResult r = mResultFactory.getResult(ResultFactory.RESULT_NORMAL,
                mIQCommand.getCommand().getId(), getResultStr(bOK));
        postResult(r);
    }

    private String getResultStr(Object bOK) {
        return bOK != null ? CoreConstants.CONSTANT_RESULT_OK
                : CoreConstants.CONSTANT_RESULT_NG;
    }
}
