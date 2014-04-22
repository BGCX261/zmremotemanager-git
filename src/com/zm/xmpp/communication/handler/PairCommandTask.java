package com.zm.xmpp.communication.handler;

import android.os.Handler;
import android.os.Message;

import com.zm.epad.core.CoreConstants;
import com.zm.epad.core.SubSystemFacade;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.client.ZMIQResult;
import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.result.IResult;
import com.zm.xmpp.communication.Constants;

public abstract class PairCommandTask extends CommandTask {
    protected boolean mStarted = false;

    public PairCommandTask(SubSystemFacade subSystemFacade, Handler handler,
            ResultFactory factory, ZMIQCommand command) {
        super(subSystemFacade, handler, factory, command);
    }

    @Override
    public void forceClose() {
        closeWithoutPair();
    }

    abstract public boolean isDuplicated(PairCommandTask task);

    abstract public boolean isPaired(PairCommandTask task);

    abstract public boolean isStartCommand();

    abstract protected void closeWithoutPair();

    public void invalidate() {
        // in normal case, only send done result
        IResult result = mResultFactory.getResult(ResultFactory.RESULT_NORMAL,
                mIQCommand.getCommand().getId(), Constants.RESULT_DONE,
                mIQCommand.getCommand().getAction(), null, null);
        postResult(result);
    }
}
