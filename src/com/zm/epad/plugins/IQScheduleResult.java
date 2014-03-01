package com.zm.epad.plugins;

import com.zm.epad.core.LogManager;
import com.zm.epad.core.XmppClient;
import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.client.ResultFactory;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.client.ZMIQResult;
import com.zm.xmpp.communication.result.IResult;

import org.jivesoftware.smack.packet.Packet;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

public class IQScheduleResult {
    protected static final String TAG = "IQScheduleResult";

    protected static final String DEFAULT_SERVER = Constants.XMPP_NAMESPACE_CENTER;
    protected static final int EVT_SCHEDULE_START = 100;
    protected static final int EVT_SCHEDULE = 101;
    protected static final int EVT_SCHEDULE_STOP = 102;

    protected HandlerThread mThread;
    protected Handler mHandler;
    protected XmppClient mXmppClient;

    protected boolean mRunning;
    protected ZMIQCommand mIQCommand;
    protected String mResultId = "0";

    protected boolean mImmediateResult = true;
    protected long mInterval = 60 * 60 * 1000; /* default is 1 hour */

    protected int mResultType = 0;
    protected ResultFactory mFactory;

    protected ResultMakerCallback mCallback;

    public IQScheduleResult(String name) {
        this(name, null);
    }

    public IQScheduleResult(String name, ZMIQCommand commandIQ) {
        mIQCommand = commandIQ;
        mResultId = mIQCommand.getCommand().getId();

        mThread = new HandlerThread(name);
        mThread.start();
        mHandler = new ScheduleHandler(mThread.getLooper());

    }

    public boolean start(long interval, XmppClient client) {
        boolean bRet = false;
        LogManager.local(TAG, "start interval:" + mInterval + " running:"
                + mRunning);
        if (mRunning == false) {
            mInterval = interval;
            mXmppClient = client;

            Message msg = mHandler.obtainMessage(EVT_SCHEDULE_START);
            mHandler.sendMessage(msg);

            bRet = true;
        }
        return bRet;
    }

    public boolean restart() {
        if (mXmppClient == null) {
            return false;
        }
        return start(mInterval, mXmppClient);
    }

    public void stop() {
        mHandler.removeMessages(EVT_SCHEDULE_START);
        mHandler.removeMessages(EVT_SCHEDULE);
        Message msg = mHandler.obtainMessage(EVT_SCHEDULE_STOP);
        mHandler.sendMessage(msg);
        mRunning = false;
    }

    public void destroy() {
        try {
            mThread.quit();
            mThread.join();
        } catch (Exception e) {
            LogManager.local(TAG, "destroy:" + e.toString());
        }
    }

    public void setInterval(long interval) {
        mInterval = interval;
    }

    interface ResultMakerCallback {
        IResult makeResult();
    }

    public void setResultMaker(ResultMakerCallback callback) {
        mCallback = callback;
    }

    public void setResultMaker(ResultFactory factory, int resultType) {
        mFactory = factory;
        mResultType = resultType;
    }

    public boolean isRunning() {
        return mRunning;
    }

    public void sendFirstResultImmediately(boolean immediate) {
        mImmediateResult = immediate;
    }

    protected IResult getResult() {
        if (mFactory != null) {
            return mFactory.getResult(mResultType, mResultId);
        } else if (mCallback != null) {
            return mCallback.makeResult();
        } else {
            return null;
        }
    }

    protected void scheduleMessage() {
        Message schedule = mHandler.obtainMessage(EVT_SCHEDULE);
        mHandler.sendMessageDelayed(schedule, mInterval);
    }

    protected void sendResult(IResult result) {
        ZMIQResult resultIQ = null;

        if (mIQCommand != null) {
            resultIQ = new ZMIQResult(mIQCommand);
        } else {
            resultIQ = new ZMIQResult();
            resultIQ.setTo(DEFAULT_SERVER);
        }

        resultIQ.setResult(result);
        if (mXmppClient != null) {
            mXmppClient.sendPacketAsync((Packet) resultIQ, 0);
        }
    }

    protected class ScheduleHandler extends Handler {

        public ScheduleHandler() {
        }

        public ScheduleHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            LogManager.local(TAG, "handleMessage:" + msg.what);
            switch (msg.what) {
            case EVT_SCHEDULE_START:
                mRunning = true;
                if (mImmediateResult) {
                    IResult result = getResult();
                    sendResult(result);
                }
                scheduleMessage();
                break;
            case EVT_SCHEDULE:
                if (mRunning == true) {
                    IResult result = getResult();
                    sendResult(result);
                    scheduleMessage();
                }
                break;
            case EVT_SCHEDULE_STOP:
                break;
            default:
                break;
            }

        }
    }
}
