package com.zm.xmpp.communication.handler;

import android.os.Handler;

import com.zm.epad.core.CoreConstants;
import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.result.IResult;
import com.zm.xmpp.communication.Constants;

public class CommandTask4Policy extends CommandTask {
    private static final String TAG = "CommandTask4Policy";

    public CommandTask4Policy(SubSystemFacade subSystemFacade, Handler handler,
            ResultFactory factory, ZMIQCommand command) {
        super(subSystemFacade, handler, factory, command);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected int handleCommand(ZMIQCommand command) {
        ICommand cmd = command.getCommand();
        IResult result = null;
        try {
            mSubSystemFacade.updatePolicy(cmd.toXML());
            result = mResultFactory.getNormalResult(mIQCommand.getCommand(),
                    true, null);
        } catch (Exception e) {
            LogManager.local(TAG, "Fail to update policy");
            e.printStackTrace();
            result = mResultFactory.getNormalResult(mIQCommand.getCommand(),
                    false, e.toString());
        }

        postResult(result);

        return SUCCESS;
    }
}
