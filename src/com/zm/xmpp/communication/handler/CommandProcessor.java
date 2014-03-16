package com.zm.xmpp.communication.handler;

import com.zm.epad.core.CoreConstants;
import com.zm.epad.core.LogManager;
import com.zm.epad.core.NetCmdDispatcher.CmdDispatchInfo;
import com.zm.epad.core.SubSystemFacade;
import com.zm.epad.core.XmppClient;
import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.client.ZMIQCommandProvider;
import com.zm.xmpp.communication.client.ZMIQResult;
import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.result.IResult;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CommandProcessor extends CmdDispatchInfo {
    private static final String TAG = "CommandProcessor";

    private Context mContext;
    private XmppClient mXmppClient;
    private ZMIQCommandProvider mZMIQProvider;
    private SubSystemFacade mSubSystemFacade;

    private ResultFactory mResultFactory;
    private HandlerThread mThread;
    private Handler mHandler;
    private SystemNotifyTask mSystemTask = null;
    private List<CommandTask> mRunningTaskList = new ArrayList<CommandTask>() {
    };
    private List<PairCommandTask> mToPairTaskList = new ArrayList<PairCommandTask>() {
    };
    private final HashMap<String, Class<?>> mTaskMap = new HashMap<String, Class<?>>() {
        {
            put(Constants.XMPP_COMMAND_APP, CommandTask4App.class);
            put(Constants.XMPP_COMMAND_QUERY, CommandTask4Query.class);
            put(Constants.XMPP_COMMAND_REPORT, CommandTask4Report.class);
            put(Constants.XMPP_COMMAND_FILE_TRANSFER,
                    CommandTask4FileTransfer.class);
            put(Constants.XMPP_COMMAND_POLICY, CommandTask4Policy.class);
        }
    };

    @Override
    public void destroy() {
        if (mSystemTask != null) {
            mSystemTask.forceClose();
            mSystemTask = null;
        }
        for (CommandTask t : mRunningTaskList) {
            t.forceClose();
        }
        for (PairCommandTask t : mToPairTaskList) {
            t.forceClose();
        }
        super.destroy();
    }

    // don't show namespace out side of this file.
    public CommandProcessor(Context context, XmppClient xmppClient) {
        this(context, Constants.XMPP_NAMESPACE_CENTER, xmppClient);
    }

    public void setSubSystem(SubSystemFacade subSystemFacade) {
        mSubSystemFacade = subSystemFacade;
        mResultFactory.setSubSystem(subSystemFacade);
        mSystemTask = new SystemNotifyTask(mSubSystemFacade, mHandler,
                mResultFactory);
    }

    private CommandProcessor(Context context, String namespace,
            XmppClient XmppCliet) {
        LogManager.local(TAG, "create: " + namespace);
        mContext = context;

        mStrElementName = "command";
        mStrNameSpace = namespace;
        mXmppClient = XmppCliet;

        mZMIQProvider = new ZMIQCommandProvider();

        mResultFactory = new ResultFactory();

        mThread = new HandlerThread(TAG);
        mThread.start();
        mHandler = new CommandProcessorHandler(mThread.getLooper());

    }

    private class CommandProcessorHandler extends Handler {
        public CommandProcessorHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            boolean ret = false;
            switch (msg.what) {
            case CommandTask.EVT_COMMAND:
                ret = handleCommand((CommandTask) msg.obj);
                break;
            case CommandTask.EVT_RESULT:
                ret = handleResult(msg.obj);
                break;
            case CommandTask.EVT_TASK_END:
                ret = handleTaskEnd((CommandTask) msg.obj);
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
            ret = mZMIQProvider.parseIQ(parser);
        } catch (Exception e) {
            LogManager.local(TAG, "parseXMLStream:");
            e.printStackTrace();
        }

        return ret;
    }

    @Override
    public boolean handlePacket(Packet packet) {
        if (packet instanceof ZMIQCommand) {
            return postIQCommand((ZMIQCommand) packet);
        }
        return false;
    }

    private String getCommandType(ZMIQCommand cmd) {
        return cmd.getCommand().getType();
    }

    private boolean postIQCommand(ZMIQCommand iq) {
        LogManager.local(TAG, "postIQCommand:" + getCommandType(iq));
        boolean ret = true;
        try {
            Class<?> taskClass = mTaskMap.get(getCommandType(iq));
            Constructor<?> constructor = taskClass.getConstructor(
                    SubSystemFacade.class, Handler.class, ResultFactory.class,
                    ZMIQCommand.class);
            CommandTask task = (CommandTask) constructor.newInstance(
                    mSubSystemFacade, mHandler, mResultFactory, iq);
            task.postCommand();
        } catch (Exception e) {
            e.printStackTrace();
            ret = false;
        }

        return ret;
    }

    private boolean handleCommand(CommandTask task) {
        int ret = CommandTask.NOT_IMPLEMENTED;

        if (task instanceof PairCommandTask) {
            ret = handlePairCommand((PairCommandTask) task);
        } else {
            ret = handleCommandDefault(task);
        }

        if (ret == CommandTask.RUNNING) {
            synchronized (mRunningTaskList) {
                mRunningTaskList.add(task);
            }
        }
        LogManager.local(TAG, "handleCommandTask(" + task.getCommandType()
                + "):" + ret);
        return ret == CommandTask.NOT_IMPLEMENTED ? false : true;
    }

    private int handleCommandDefault(CommandTask task) {
        return task.handleCommand();
    }

    private int handlePairCommand(PairCommandTask task) {
        int ret = CommandTask.NOT_IMPLEMENTED;
        if (task.isStartCommand()) {
            synchronized (mToPairTaskList) {
                for (PairCommandTask pc : mToPairTaskList) {
                    if (pc.isDuplicated(task)) {
                        LogManager.local(TAG,
                                "Dulplicated task:" + task.getCommandType());
                        return CommandTask.NOT_IMPLEMENTED;
                    }
                }
                ret = task.handleCommand();
                if (ret == CommandTask.SUCCESS) {
                    mToPairTaskList.add(task);
                }
            }
        } else {
            synchronized (mToPairTaskList) {
                for (PairCommandTask pc : mToPairTaskList) {
                    if (pc.isPaired(task)) {
                        ret = task.handleCommand();
                        if (ret == CommandTask.SUCCESS) {
                            mToPairTaskList.remove(pc);
                        }
                        break;
                    }
                    LogManager.local(TAG,
                            "Task not paired:" + task.getCommandType());
                }
            }
        }
        return ret;
    }

    private boolean handleResult(Object result) {
        boolean ret = false;
        if (result instanceof ZMIQResult) {
            IResult zmresult = ((ZMIQResult) result).getResult();
            String type = null;
            if(zmresult == null){
                type = "no-type";
            }else{
                type = zmresult.getType();
            }
            
            LogManager.local(TAG, "send result:" + type);
            ret = mXmppClient.sendPacketAsync((Packet) result, 0);
            LogManager.getInstance().addLog(
                    CoreConstants.CONSTANT_INT_LOGTYPE_COMMON,
                    zmresult!=null?zmresult.toXML():"send result with no content");
        }
        return ret;
    }

    private boolean handleTaskEnd(CommandTask task) {
        LogManager.local(TAG, "Task End:" + task.getCommandType());
        synchronized (mRunningTaskList) {
            mRunningTaskList.remove(task);
        }
        return true;
    }

}
