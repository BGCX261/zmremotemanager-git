package com.zm.xmpp.communication.handler;

import android.os.Handler;
import android.os.Message;

import com.zm.epad.core.SubSystemFacade;
import com.zm.epad.core.SubSystemFacade.NotifyListener;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.client.ZMIQResult;
import com.zm.xmpp.communication.result.IResult;

public class SystemNotifyTask extends CommandTask implements NotifyListener {

    private static final String TAG = "SystemNotifyTask";

    private final String DEFAULT_SERVER = "dummy";

    public SystemNotifyTask(SubSystemFacade subSystemFacade, Handler handler,
            ResultFactory factory) {
        super(subSystemFacade, handler, factory, null);
        mSubSystemFacade.setListener(this);
    }

    @Override
    public String getCommandId() {
        // dummy
        return TAG;
    }

    @Override
    public void forceClose() {
        mSubSystemFacade.cancelListener(this);
    }

    @Override
    public String getCommandType() {
        // dummy
        return TAG;
    }

    @Override
    public void notify(int type, Object obj) {
        switch (type) {
        case SubSystemFacade.NOTIFY_APP_USAGE:
            handleAppUsage(obj);
            break;
        default:
            break;
        }

    }

    @Override
    protected int handleCommand(ZMIQCommand command) {
        // do nothing
        return NOT_IMPLEMENTED;
    }

    @Override
    protected void postResult(IResult result) {
        ZMIQResult IQResult = new ZMIQResult();
        IQResult.setTo(DEFAULT_SERVER);
        IQResult.setResult(result);
        Message msg = mHandler.obtainMessage(EVT_RESULT, IQResult);
        mHandler.sendMessage(msg);
    }

    private void handleAppUsage(Object obj) {
        IResult result = mResultFactory.getResult(
                ResultFactory.RESULT_APPUSAGE, null, obj);
        postResult(result);
    }
}