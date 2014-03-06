package com.zm.xmpp.communication.handler;

import android.os.Handler;
import android.os.Message;

import com.zm.epad.core.SubSystemFacade;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.client.ZMIQResult;
import com.zm.xmpp.communication.result.IResult;

public abstract class CommandTask {
    public static final int SUCCESS = 0;
    public static final int FAILED = 1;
    public static final int RUNNING = 2;
    public static final int NOT_SUPPORTED = 3;
    public static final int NOT_IMPLEMENTED = 4;

    public static final int EVT_COMMAND = 100;
    public static final int EVT_RESULT = 101;
    public static final int EVT_TASK_END = 102;

    protected SubSystemFacade mSubSystemFacade;
    protected Handler mHandler;
    protected ResultFactory mResultFactory;
    protected ZMIQCommand mIQCommand;

    public CommandTask(SubSystemFacade subSystemFacade, Handler handler,
            ResultFactory factory, ZMIQCommand command) {
        mSubSystemFacade = subSystemFacade;
        mHandler = handler;
        mResultFactory = factory;
        mIQCommand = command;
    }

    public ZMIQCommand getIQCommand() {
        return mIQCommand;
    }

    public String getCommandId() {
        return mIQCommand.getCommand().getId();
    }

    public String getCommandType() {
        return mIQCommand.getCommand().getType();
    }

    public void postCommand() {
        Message msg = mHandler.obtainMessage(EVT_COMMAND, this);
        mHandler.sendMessage(msg);
    }

    public int handleCommand() {
        return handleCommand(mIQCommand);
    }

    public void forceClose() {
        // do nothing in parent class
        return;
    }

    abstract protected int handleCommand(ZMIQCommand command);

    protected void postResult(IResult result) {
        ZMIQResult IQResult = new ZMIQResult(mIQCommand);
        IQResult.setResult(result);
        Message msg = mHandler.obtainMessage(EVT_RESULT, IQResult);
        mHandler.sendMessage(msg);
    }

    protected void endTask() {
        Message msg = mHandler.obtainMessage(EVT_TASK_END, this);
        mHandler.sendMessage(msg);
    }
}
