package com.zm.epad.plugins;

import com.zm.epad.core.CoreConstants;
import com.zm.epad.core.LogManager;
import com.zm.epad.core.NetCmdDispatcher.CmdDispatchInfo;
import com.zm.epad.core.SubSystemFacade;
import com.zm.epad.core.XmppClient;
import com.zm.epad.plugins.RemoteFileManager.FileDownloadTask;
import com.zm.epad.plugins.RemoteFileManager.FileTransferTask;
import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.client.OutputIQCommand;
import com.zm.xmpp.communication.client.OutputIQCommandProvider;
import com.zm.xmpp.communication.client.ResultFactory;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.client.ZMIQCommandProvider;
import com.zm.xmpp.communication.client.ZMIQResult;
import com.zm.xmpp.communication.command.Command4FileTransfer;
import com.zm.xmpp.communication.command.Command4Report;
import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.command.ICommand4App;
import com.zm.xmpp.communication.command.ICommand4Query;
import com.zm.xmpp.communication.result.IResult;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.xmlpull.v1.XmlPullParser;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.util.List;

public class RemoteCmdProcessor extends CmdDispatchInfo {
    private static final String TAG = "IQDispatcherCommand";

    private static final int EVT_COMMAND = 101;
    private static final int EVT_CALLBACK = 102;
    private static final int EVT_OUTPUT = 103;

    private Context mContext;
    private XmppClient mXmppClient;
    private ZMIQCommandProvider mZMIQProvider;
    private OutputIQCommandProvider mOutputProvider;
    private SubSystemFacade mSubSystemFacade;

    private ResultFactory mResultFactory;
    private HandlerThread mThread;
    private Handler mHandler;

    // private final long DEFAULT_INTERVAL = 15*60*1000; /* 15 minutes*/
    private final long DEFAULT_INTERVAL = 5 * 1000; /*
                                                     * change interval to 5s to
                                                     * test
                                                     */
   

    @Override
    public void destroy() {
       stopMonitorAppRunningInfo();
       super.destroy();
    }

    // don't show namespace out side of this file.
    public RemoteCmdProcessor(Context context, XmppClient xmppClient) {
        this(context, Constants.XMPP_NAMESPACE_CENTER,
                xmppClient);
    }

    public void setSubSystem(SubSystemFacade subSystemFacade) {
        mSubSystemFacade = subSystemFacade;
        mResultFactory.setSubSystem(subSystemFacade);
    }

    private RemoteCmdProcessor(Context context, String namespace,
            XmppClient XmppCliet) {
        LogManager.local(TAG, "create: " + namespace);
        mContext = context;

        mStrElementName = "command";
        mStrNameSpace = namespace;
        mXmppClient = XmppCliet;

        mZMIQProvider = new ZMIQCommandProvider();
        mOutputProvider = new OutputIQCommandProvider();

        mResultFactory = new ResultFactory();

        mThread = new HandlerThread(TAG);
        mThread.start();
        mHandler = new RemoteCmdHandler(mThread.getLooper());

    }

    private class RemoteCmdHandler extends Handler {
        public RemoteCmdHandler(Looper looper){
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
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
            case EVT_OUTPUT:
                ret = handleOutputIQCommand((OutputIQCommand) msg.obj);
                break;
            default:
                break;
            }
        }

    }

    @Override
    public IQ parseXMLStream(XmlPullParser parser) {
        IQ ret = null;

        LogManager.local(TAG, "parseXMLStream");
        try {
            if (isOutputType(parser.getAttributeValue(null, CoreConstants.CONSTANT_TYPE))) {
                ret = mOutputProvider.parseIQ(parser);
            } else {
                ret = mZMIQProvider.parseIQ(parser);
            }
        } catch (Exception e) {
            LogManager.local(TAG, "parseXMLStream:" + e.toString());
        }

        return ret;
    }

    private boolean isOutputType(String type) {
        return type.equals(CoreConstants.CONSTANT_POLICY);
    }

    @Override
    public boolean handlePacket(Packet packet) {
        if (packet instanceof ZMIQCommand) {
            return postIQCommand((ZMIQCommand) packet);
        } else if (packet instanceof OutputIQCommand) {
            return postOutputIQCommand((OutputIQCommand) packet);
        } else {
            return false;
        }
    }

    private boolean postIQCommand(ZMIQCommand iq) {
        LogManager.local(TAG, "postIQCommand:" + iq.getCommand().getType());
        Message msg = mHandler.obtainMessage(EVT_COMMAND, iq);

        return mHandler.sendMessage(msg);
    }

    private boolean postOutputIQCommand(OutputIQCommand iq) {
        LogManager.local(TAG, "postOutputIQCommand:" + iq.getCommandType());
        Message msg = mHandler.obtainMessage(EVT_OUTPUT, iq);

        return mHandler.sendMessage(msg);
    }

    private void sendResultToServer(IQ iq, IResult result) {
        ZMIQResult resultIQ = new ZMIQResult(iq);
        if (iq != null) {
            resultIQ.setTo(iq.getFrom());
            resultIQ.setFrom(iq.getTo());
        }
        resultIQ.setResult(result);

        mXmppClient.sendPacketAsync((Packet) resultIQ, 0);
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

        if (cmdType.equals(Constants.XMPP_COMMAND_APP)) {
            IResult result = null;
            if (cmd.getAction().equals(Constants.XMPP_APP_INSTALL)) {
                ICommand4App cmdApp = (ICommand4App) cmd;
                final ZMIQResult resultIQ = new ZMIQResult(iq);
                final String cmdId = cmdApp.getId();

                int install = mSubSystemFacade.installPkgForUser(cmdApp.getAppUrl(),
                        cmdApp.getUserId(),
                        new RemotePackageManager.installCallback() {
                            ZMIQResult mResultIQ = resultIQ;
                            String mCmdId = cmdId;

                            @Override
                            public void callback(boolean result) {
                                IResult r = mResultFactory.getResult(
                                        ResultFactory.RESULT_NORMAL, mCmdId,
                                        result == true ? "OK" : "NG");
                                mResultIQ.setResult(r);
                                Message msg = mHandler.obtainMessage(
                                        EVT_CALLBACK, mResultIQ);
                                mHandler.sendMessage(msg);
                            }
                        });
                if (install < 0) {
                    return true;
                } else {
                    result = mResultFactory.getResult(
                            ResultFactory.RESULT_NORMAL, cmdApp.getId(),
                            install == 0 ? "OK" : "NG");
                }
            } else {
                result = handleCommand4App((ICommand4App) cmd);
            }
            sendResultToServer(iq, result);
        } else if (cmdType.equals(Constants.XMPP_COMMAND_QUERY)) {
            List<IResult> resultList = null;
            try {
                resultList = handleCommand4Query((ICommand4Query) cmd,
                        new CommandResultCallback(iq));

                if (resultList != null) {
                    // when resultList is not null, send the result immediately
                    for (IResult r : resultList) {
                        LogManager.local(TAG, "send packet start ");
                        sendResultToServer(null, r);
                        LogManager.local(TAG, "send packet end ");
                    }
                } else {
                    // if resultList is null, the result will be sent from
                    // callback
                }

            } catch (Exception e) {
                // when exception, it means failed to get info, send NG

                IResult r = mResultFactory.getResult(
                        ResultFactory.RESULT_NORMAL, cmd.getId(),
                        CoreConstants.CONSTANT_RESULT_NG);

                sendResultToServer(iq, r);

            }
        } else if (cmdType.equals(Constants.XMPP_COMMAND_REPORT)) {
            ret = handleCommand4Report(iq);

            if (ret == false) {
                // set NG result

                IResult r = mResultFactory.getResult(
                        ResultFactory.RESULT_NORMAL, cmd.getId(),
                        CoreConstants.CONSTANT_RESULT_NG);
                sendResultToServer(iq, r);
            }

        } else if (cmdType.equals(Constants.XMPP_COMMAND_FILE_TRANSFER)) {
            ret = handleCommand4FileTransfer((Command4FileTransfer) cmd,
                    new CommandResultCallback(iq));
        } else {
            LogManager.local(TAG, "bad command: " + cmdType);
            ret = false;
        }

        return ret;
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

    // handleCommand4App is ok. All core feature are actually implemented by
    // RemotePackageManager
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

    private String getResultStr(Object bOK) {
        return bOK != null ? CoreConstants.CONSTANT_RESULT_OK
                : CoreConstants.CONSTANT_RESULT_NG;
    }

    private String getResultStr(boolean bOK) {
        return bOK == true ? CoreConstants.CONSTANT_RESULT_OK
                : CoreConstants.CONSTANT_RESULT_NG;
    }

    private void setCaptureBundleInfo(Bundle info, ICommand4Query cmd) {
        info.putString(CoreConstants.CONSTANT_COMMANDID, cmd.getId());
        info.putString(CoreConstants.CONSTANT_TYPE, cmd.getType());
        info.putString(CoreConstants.CONSTANT_ACTION, cmd.getAction());
        info.putString(CoreConstants.CONSTANT_MIME,
                CoreConstants.CONSTANT_IMG_PNG);
    }

    private List<IResult> handleCommand4Query(ICommand4Query cmd,
            final CommandResultCallback callback) throws Exception {
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
        } else if (action.equals(Constants.XMPP_QUERY_CAPTURE)) {
            Bundle info = new Bundle();
            setCaptureBundleInfo(info, cmd);

            final String cmdId = cmd.getId();
            mSubSystemFacade.uploadScreenshot(cmd.getUrl(), info,
                    new RemoteFileManager.FileTransferCallback() {
                        ResultFactory.ResultCallback cb = callback;
                        String id = cmdId;

                        @Override
                        public void onDone(FileTransferTask task) {
                            String fileName = (String) task.getResult();
                            IResult result = mResultFactory.getResult(
                                    ResultFactory.RESULT_NORMAL, id,
                                    getResultStr(fileName));

                            cb.handleResult(result);
                        }

                        @Override
                        public void onCancel(FileTransferTask task) {
                            // when cancel, send NG
                            IResult result = mResultFactory.getResult(
                                    ResultFactory.RESULT_NORMAL, id,
                                    CoreConstants.CONSTANT_RESULT_NG);

                            cb.handleResult(result);
                        }
                    });
            results = null;
        } else {
            LogManager.local(TAG, "handleCommand4Query bad action");
        }

        LogManager.local(TAG, "handleCommand4Query return: "
                + (results == null ? 0 : results.size()));
        return results;
    }

    private boolean handleCommand4Report(ZMIQCommand iq) {

        if (iq == null
                || !iq.getCommand().getType()
                        .equals(Constants.XMPP_COMMAND_REPORT)) {
            return false;
        }

        boolean ret = false;

        Command4Report cmd = (Command4Report) iq.getCommand();

        if (cmd.getReport().equals(Constants.XMPP_REPORT_APP)) {
            if (cmd.getAction().equals(Constants.XMPP_REPORT_ACT_TRACE)) {
                ret = startMonitorAppRunningInfo(iq, DEFAULT_INTERVAL);
            } else if (cmd.getAction()
                    .equals(Constants.XMPP_REPORT_ACT_UNTRACE)) {
                ret = stopMonitorAppRunningInfo();
            }
        } else if (cmd.getReport().equals(Constants.XMPP_REPORT_POS)) {
            // to be added
        }

        return ret;
    }

    private boolean startMonitorAppRunningInfo(final ZMIQCommand iq,long interval) {
        
        mSubSystemFacade.startMonitorRunningAppInfo(interval,new RemotePackageManager.ReportRunningAppInfo(){

            @Override
            public void reportRunningAppProcessInfos(
                    List<RunningAppProcessInfo> infos) {
                IResult iResult = mResultFactory.getRunningAppResult(infos);
                sendResultToServer(iq, iResult);
            }
            
        });
        return true;
    }

    private boolean stopMonitorAppRunningInfo() {
        mSubSystemFacade.stopMonitorRunningAppInfo();
        return true;
    }

    private boolean handleCommand4FileTransfer(Command4FileTransfer cmd,
            final ResultFactory.ResultCallback callback) {

        boolean ret = false;

        if (cmd.getAction().equals(Constants.XMPP_FILE_TRANSFER_WALLPAPER)) {
            final String cmdid = cmd.getId();
            mSubSystemFacade.downloadFile(cmd.getUrl(),
                    new RemoteFileManager.FileTransferCallback() {
                        String id = cmdid;
                        ResultFactory.ResultCallback cb = callback;

                        @Override
                        public void onDone(FileTransferTask task) {

                            boolean ret = false;

                            File file = ((FileDownloadTask) task).getResult();

                            if (file != null) {
                                ret = mSubSystemFacade.changeWallpaper(file
                                        .toString());
                            }
                            // delete temp file after change wallpaper
                            file.delete();

                            IResult result = mResultFactory.getResult(
                                    ResultFactory.RESULT_NORMAL, id,
                                    getResultStr(ret));
                            cb.handleResult(result);
                        }

                        @Override
                        public void onCancel(FileTransferTask task) {
                            // when cancel, send NG
                            IResult result = mResultFactory.getResult(
                                    ResultFactory.RESULT_NORMAL, id,
                                    CoreConstants.CONSTANT_RESULT_NG);

                            cb.handleResult(result);
                        }
                    });
            ret = true;
        }
        return ret;
    }

    private boolean handleOutputIQCommand(OutputIQCommand iq) {
        boolean ret = false;
        if (iq.getCommandType().equals("policy")) {
            mSubSystemFacade.updatePolicy(iq.getOutput());
            ret = true;
        }
        IResult result = mResultFactory.getResult(ResultFactory.RESULT_NORMAL,
                iq.getId(), getResultStr(ret));
        // send result
        sendResultToServer(iq, result);

        return ret;
    }
}
