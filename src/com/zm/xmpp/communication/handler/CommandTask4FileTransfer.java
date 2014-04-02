package com.zm.xmpp.communication.handler;

import java.io.File;

import android.os.Handler;

import com.zm.epad.core.CoreConstants;
import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;
import com.zm.epad.plugins.RemoteFileManager;
import com.zm.epad.plugins.RemoteFileManager.FileDownloadTask;
import com.zm.epad.plugins.RemoteFileManager.FileTransferTask;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.result.IResult;
import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.command.Command4FileTransfer;
import com.zm.xmpp.communication.Constants;

public class CommandTask4FileTransfer extends CommandTask {
    private static final String TAG = "CommandTask4FileTransfer";

    public CommandTask4FileTransfer(SubSystemFacade subSystemFacade,
            Handler handler, ResultFactory factory, ZMIQCommand command) {
        super(subSystemFacade, handler, factory, command);
    }

    @Override
    protected int handleCommand(ZMIQCommand command) {
        int ret = FAILED;
        Command4FileTransfer cmd = (Command4FileTransfer) command.getCommand();
        LogManager.local(TAG, "handleCommand4FileTransfer:" + cmd.getAction());

        if (cmd.getAction().equals(Constants.XMPP_PUSH_WALLPAPER)) {
            ret = handleSetWallpaper(cmd);
        } else {
            LogManager.local(TAG, "handleCommand4FileTransfer bad action");
        }
        return ret;
    }

    private int handleSetWallpaper(Command4FileTransfer cmd) {

        mSubSystemFacade.downloadFile(cmd.getUrl(),
                new RemoteFileManager.FileTransferCallback() {

                    @Override
                    public void onDone(boolean success, FileTransferTask task) {

                        boolean ret = false;

                        File file = ((FileDownloadTask) task).getResult();

                        if (success == true && file != null) {
                            ret = mSubSystemFacade.changeWallpaper(file
                                    .toString());
                            // delete temp file after change wallpaper
                            file.delete();
                        }

                        if (success == false) {
                            postResult(false, "download failed");
                        } else {
                            postResult(ret, "set wallpaper failed");
                        }

                        endTask();
                    }

                    @Override
                    public void onCancel(FileTransferTask task) {
                        // when cancel, send err
                        postResult(false, "download canceled");
                        endTask();
                    }
                });

        return RUNNING;
    }

    private void postResult(boolean bOK, String failError) {
        IResult r = mResultFactory.getNormalResult(mIQCommand.getCommand(),
                bOK, bOK == true ? null : failError);
        postResult(r);
    }

}
