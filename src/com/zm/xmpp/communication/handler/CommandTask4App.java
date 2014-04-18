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

        postResult(ret, "API error");

        LogManager.local(TAG, "handleCommand4App return:" + ret);
        return result;

    }

    private int handleCommand4AppInstall(ICommand4App cmd) {

        mSubSystemFacade.acquireWakeLock(TAG);
        if (mSubSystemFacade
                .isNewPackage(cmd.getAppName(), cmd.getAppVersion())) {
            LogManager.local(TAG, "download new APK");
            int install = mSubSystemFacade.installPkgForUser(cmd.getAppUrl(),
                    cmd.getUserId(),
                    new RemotePackageManager.installCallback() {

                        @Override
                        public void callback(int result) {
                            postResult(result, getInstallErrorCode(result));
                            endTask();
                            mSubSystemFacade.releaseWakeLock(TAG);
                        }
                    });
            if (install == RemotePackageManager.INSTALL_DOWNLOADING) {
                return RUNNING;
            } else {
                postResult(install, getInstallErrorCode(install));
            }
            mSubSystemFacade.releaseWakeLock(TAG);
            return install == RemotePackageManager.INSTALL_SUCCESS ? SUCCESS
                    : FAILED;
        } else {
            LogManager.local(TAG, "Install Exsited Package");
            boolean ret = mSubSystemFacade.InstallExsitedPackage(
                    cmd.getAppName(), cmd.getUserId());
            postResult(
                    ret,
                    getInstallErrorCode(RemotePackageManager.INSTALL_ALREADY_EXISTED));
            return ret == true ? SUCCESS : FAILED;
        }

    }

    private void postResult(int install, String failError) {
        IResult r = mResultFactory.getAppResult(
                (ICommand4App) mIQCommand.getCommand(),
                install == RemotePackageManager.INSTALL_SUCCESS ? true : false,
                failError);
        postResult(r);
    }

    private void postResult(boolean bOK, String failError) {
        IResult r = mResultFactory.getAppResult(
                (ICommand4App) mIQCommand.getCommand(), bOK, failError);
        postResult(r);
    }

    private String getInstallErrorCode(int status) {
        String ret = null;
        switch (status) {
        case RemotePackageManager.INSTALL_DOWNLOAD_FAIL:
            ret = "download failed";
            break;
        case RemotePackageManager.INSTALL_ALREADY_EXISTED:
            ret = "already existed";
            break;
        default:
            ret = "API error:" + status;
            break;
        }
        return ret;
    }
}
