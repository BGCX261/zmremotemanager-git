package com.zm.epad.plugins;

import com.zm.epad.core.LogManager;
import com.zm.epad.core.NetCmdDispatcher.CmdDispatchInfo;
import com.zm.epad.core.XmppClient;
import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.client.ResultFactory;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.client.ZMIQCommandProvider;
import com.zm.xmpp.communication.client.ZMIQResult;
import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.command.ICommand4App;
import com.zm.xmpp.communication.command.ICommand4Query;
import com.zm.xmpp.communication.result.IResult;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.util.List;

public class IQDispatcherCommand extends CmdDispatchInfo{
    private static final String TAG = "IQDispatcherCommand";

    private static final int EVT_COMMAND = 101;
    private static final int EVT_CALLBACK = 102;

    private Context mContext;
    private XmppClient mXmppClient;
    private ZMIQCommandProvider mProvider;
    private RemotePkgsManager mPkgManager;
    private RemoteDeviceManager mDeviceManager;
    private ResultFactory mResultFactory;
    private HandlerThread mThread;
    private Handler mHandler;

    @Override
    public void destroy() {
        try {
            mThread.quit();
            mThread.join();
        } catch (Exception e) {
            LogManager.local(TAG, "destroy:" + e.toString());
        }

        super.destroy();
    }
    
 
    public IQDispatcherCommand(Context context, String namespace,
            XmppClient XmppCliet) {
        LogManager.local(TAG, "create: " + namespace);
        mContext = context;

        mStrElementName = "command";
        mStrNameSpace = namespace;
        mXmppClient = XmppCliet;

        mPkgManager = new RemotePkgsManager(mContext);
        mDeviceManager = new RemoteDeviceManager(mContext);
        mProvider = new ZMIQCommandProvider();
        mResultFactory = new ResultFactory(mPkgManager,mDeviceManager);

        mThread = new HandlerThread(TAG);
        mThread.start();
        mHandler = new Handler(mThread.getLooper(), new IQCommandCallback());

    }

    @Override
    public IQ parseXMLStream(XmlPullParser parser) {
        IQ ret = null;

        LogManager.local(TAG, "parseXMLStream");
        try {
            ret = mProvider.parseIQ(parser);
        } catch (Exception e) {
            LogManager.local(TAG, "parseXMLStream:" + e.toString());
        }

        return ret;
    }

    @Override
    public boolean handlePacket(Packet packet) {

        if (!(packet instanceof ZMIQCommand)) {
            LogManager.local(TAG, "not ZMIQCommand");
            return false;
        }

        return postIQCommand((ZMIQCommand) packet);
    }

    private boolean postIQCommand(ZMIQCommand iq) {
        LogManager.local(TAG, "handlePacket:" + iq.getCommand().getType());
        Message msg = mHandler.obtainMessage(EVT_COMMAND, iq);

        return mHandler.sendMessage(msg);
    }

    private class IQCommandCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {

            boolean ret = false;

            switch (msg.what) {
            case EVT_COMMAND:
                ret = handleIQCommand((ZMIQCommand) msg.obj);
                break;
            case EVT_CALLBACK:
                if (msg.obj instanceof Packet) {
                    ret = mXmppClient.sendPacketAsync((Packet) msg.obj, 0);
                }
                break;
            case 1:
                ProminentFeature f = new ProminentFeature(mContext);
                mXmppClient.sendFile(f.getLatestScreenshot(), "Screen Shot");
                break;
            default:
                break;
            }

            return ret;
        }

    }

    private boolean handleIQCommand(ZMIQCommand iq) {
        boolean ret = true;
        ICommand cmd = iq.getCommand();
        if (cmd == null) {
            LogManager.local(TAG, "handleIQCommand FAIL: no cmd exist");
        }
        
        String cmdType = cmd.getType();

        LogManager.local(TAG, "handleIQCommand:" + cmdType);
        if (cmdType == null)
            return false;

        if (cmdType.equals("app")) {
            IResult result = null;
            result = handleCommand4App((ICommand4App) cmd);

            // no need to call setTo/From explictly. in ZMIQResult constructor,
            // we
            // figure this out.
            ZMIQResult resultIQ = new ZMIQResult(iq);
            // resultIQ.setTo(iq.getFrom());
            // resultIQ.setFrom(iq.getTo());

            // we don't care if result is null or not. ZMIQResult will handle
            // null pointer issue
            // when its getChildElementXML is called. And we could add some
            // extra info
            // in getChildElementXML when result is null.
            resultIQ.setResult(result);
            mXmppClient.sendPacketAsync((Packet) resultIQ, 0);
        } else if (cmdType.equals("query")) {
            List<IResult> resultList = null;
            try {
                resultList = handleCommand4Query((ICommand4Query) cmd,
                        new CommandResultCallback(iq));               

                if (resultList != null) {
                    //when resultList is not null, send the result immediately
                    for (IResult r : resultList) {
                        LogManager.local(TAG, "send packet start ");
                        ZMIQResult resultIQ = new ZMIQResult();
                        resultIQ.setTo(iq.getFrom());
                        resultIQ.setFrom(iq.getTo());

                        resultIQ.setResult(r);
                        mXmppClient.sendPacketAsync((Packet) resultIQ,0);
                        LogManager.local(TAG, "send packet end ");
                    }
                }else{
                    //if resultList is null, the result will be sent from callback
                }

            } catch (Exception e) {
                //when exception, it means failed to get info, send NG
                ZMIQResult resultIQ = new ZMIQResult(iq);

                IResult r = mResultFactory.getResult(
                        ResultFactory.RESULT_NORMAL, cmd.getId(), "NG");                
                resultIQ.setResult(r);
                
                mXmppClient.sendPacketAsync((Packet) resultIQ, 0);
            }
        } else {
            LogManager.local(TAG, "bad command: " + cmdType);
            ret = false;
        }

        return ret;
    }

    // handleCommand4App is ok. All core feature are actually implemented by
    // RemotePackageManager
    private IResult handleCommand4App(ICommand4App cmd) {
        boolean ret = false;
        IResult result = null;

        LogManager.local(TAG, "handleCommand4App:" + cmd.getAction());

        if (cmd.getAction().equals(Constants.XMPP_APP_ENABLE)) {
            String name = cmd.getAppName();
            int userId = cmd.getUserId();
            ret = mPkgManager.enablePkgForUser(name, userId);
        } else if (cmd.getAction().equals(Constants.XMPP_APP_DISABLE)) {
            String name = cmd.getAppName();
            int userId = cmd.getUserId();
            ret = mPkgManager.disablePkgForUser(name, userId);
        } else if (cmd.getAction().equals(Constants.XMPP_APP_INSTALL)) {
            String url = cmd.getAppUrl();
            int userId = cmd.getUserId();
            ret = mPkgManager.installPkgForUser(url, userId);
        } else if (cmd.getAction().equals(Constants.XMPP_APP_REMOVE)) {
            String name = cmd.getAppName();
            int userId = cmd.getUserId();
            ret = mPkgManager.uninstallPkgForUser(name, userId);
        } else {
            LogManager.local(TAG, "bad action");
        }

        result = mResultFactory.getResult(ResultFactory.RESULT_NORMAL,
                cmd.getId(), ret == true ? "OK" : "NG");

        LogManager.local(TAG, "handleCommand4App return:" + ret);
        return result;

    }

    private List<IResult> handleCommand4Query(ICommand4Query cmd,
            CommandResultCallback callback) throws Exception {
        List<IResult> results = null;
        String action = cmd.getAction();
        LogManager.local(TAG, "handleCommand4Query:" + action);

        if (action.equals(Constants.XMPP_QUERY_APP)) {
            results = mResultFactory.getResults(ResultFactory.RESULT_APP,
                    cmd.getId());
            if (results == null) {
                throw new Exception("failed to get app info");
            }
        } else if (action.equals(Constants.XMPP_QUERY_DEVICE)) {
            results = mResultFactory.getResults(ResultFactory.RESULT_DEVICE,
                    cmd.getId(), callback);
        } else if (action.equals(Constants.XMPP_QUERY_ENV)) {
            results = mResultFactory.getResults(ResultFactory.RESULT_ENV,
                    cmd.getId());
            if (results == null) {
                throw new Exception("failed to get env info");
            }
        } else if (action.equals("capture")) {
            ProminentFeature f = new ProminentFeature(mContext);
            f.takeScreenshot(mHandler);
            results = null;
        } else {
            LogManager.local(TAG, "handleCommand4Query bad action");
        }

        LogManager.local(TAG, "handleCommand4Query return: "
                + (results == null ? 0 : results.size()));
        return results;
    }

    private class CommandResultCallback implements ResultFactory.ResultCallback {
        ZMIQResult resultIQ = null;

        public CommandResultCallback(IQ requestIq) {
            resultIQ = new ZMIQResult(requestIq);
        }

        @Override
        public void handleResult(IResult result) {
            LogManager.local(TAG, "handleResult:" + result.getType());

            resultIQ.setResult(result);

            Message msg = mHandler.obtainMessage(EVT_CALLBACK, resultIQ);

            mHandler.sendMessage(msg);
        }

    }

}
