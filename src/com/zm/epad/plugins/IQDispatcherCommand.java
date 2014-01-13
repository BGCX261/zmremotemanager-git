package com.zm.epad.plugins;

import org.xmlpull.v1.XmlPullParser;

import android.app.Application;
import android.content.Context;
import android.os.HandlerThread;

import com.zm.epad.core.LogManager;
import com.zm.epad.core.XmppClient;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.packet.Packet;

import com.zm.epad.core.NetCmdDispatcher.CmdDispatchInfo;
import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.client.ResultFactory;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.client.ZMIQCommandProvider;
import com.zm.xmpp.communication.client.ZMIQResult;
import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.command.ICommand4App;
import com.zm.xmpp.communication.command.ICommand4Query;
import com.zm.xmpp.communication.result.IResult;

public class IQDispatcherCommand extends CmdDispatchInfo {
	private static final String  TAG="IQDispatcherCommand";
	
	private Context mContext;
	private XmppClient mXmppClient;
	private ZMIQCommandProvider mProvider;
	private RemotePkgsManager mPkgManager;
	private ResultFactory mResultFactory;

	
	public IQDispatcherCommand(Context context, String namespace, XmppClient XmppCliet)
	{
        LogManager.local(TAG, "create: " + namespace);
		mContext = context;
		
		mStrElementName = "command";
		mStrNameSpace = namespace;
		mPkgManager = new RemotePkgsManager(mContext);		
		mProvider = new ZMIQCommandProvider();
		mResultFactory = new ResultFactory(mContext);
	}
	
	@Override
    public IQ parseXMLStream(XmlPullParser parser){
		IQ ret = null;
		
        LogManager.local(TAG, "parseXMLStream");
		try{
			ret = mProvider.parseIQ(parser);
		}catch(Exception e)
		{
            LogManager.local(TAG, "parseXMLStream:" + e.toString());
		}
		
        return ret;
    }
    
    @Override
    public boolean handlePacket(Packet packet){
    	   	
    	if(!(packet instanceof ZMIQCommand))
    	{
            LogManager.local(TAG, "not ZMIQCommand");
    		return false;
    	}
    	boolean ret = true;

    	ICommand cmd = ((ZMIQCommand)packet).getCommand();
        LogManager.local(TAG, "handlePacket:" + cmd.getType());
    	
    	if(cmd.getType().equals("app"))
    	{
    		ret = handleCommand4App((ICommand4App)cmd);
    	}else if(cmd.getType().equals("query"))
    	{
    		ret = handleCommand4Query((ICommand4Query)cmd);
    	}else{
            LogManager.local(TAG, "bad command: " + cmd.getType());
    		ret = false;
    	}
    	return ret;
    }

    private boolean handleCommand4App(ICommand4App cmd)
    {
    	boolean ret = false;    	
        LogManager.local(TAG, "handleCommand4App:" + cmd.getAction());
    	
    	if(cmd.getAction().equals(Constants.XMPP_APP_ENABLE))
    	{
    		String name = cmd.getAppName();
    		int userId = cmd.getUserId();
    		ret = mPkgManager.enablePkgForUser(name, userId);
    	}
    	else if(cmd.getAction().equals(Constants.XMPP_APP_DISABLE))
    	{
    		String name = cmd.getAppName();
    		int userId = cmd.getUserId();
    		ret = mPkgManager.disablePkgForUser(name, userId);
    	}
    	else if(cmd.getAction().equals(Constants.XMPP_APP_INSTALL))
    	{
    		String url = cmd.getAppUrl();
    		int userId = cmd.getUserId();
    		ret = mPkgManager.installPkgForUser(url, userId);
    	}
    	else if(cmd.getAction().equals(Constants.XMPP_APP_REMOVE))
    	{
    		String name = cmd.getAppName();
    		int userId = cmd.getUserId();
    		ret = mPkgManager.uninstallPkgForUser(name, userId);
    	}
    	else
    	{
            LogManager.local(TAG, "bad action");
    	}
    	   	
    	IResult result = mResultFactory.getResult(ResultFactory.RESULT_NORMAL, 
    			cmd.getId(), ret==true?"OK":"NG");
    	if(result != null)
    	{
    		ZMIQResult resultIQ = new ZMIQResult();
        	resultIQ.setResult(result);
        	mXmppClient.sendPacketAsync((Packet)resultIQ);
    	}
    	
        LogManager.local(TAG, "handleCommand4App return:" + ret);
    	return ret;
    	
    }
    
    private boolean handleCommand4Query(ICommand4Query cmd)
    {
    	boolean ret = false;
    	IResult result = null;
    	String action = cmd.getAction();
    	LogManager.local(TAG, "handleCommand4Query:" + action);
    	
    	if(action.equals(Constants.XMPP_QUERY_APP))
    	{
    		result = mResultFactory.getResult(ResultFactory.RESULT_APP,cmd.getId(),null);
    	}
    	else if(action.equals(Constants.XMPP_QUERY_DEVICE))
    	{
    		result = mResultFactory.getResult(ResultFactory.RESULT_DEVICE,cmd.getId(),null);
    	}
    	else if(action.equals(Constants.XMPP_QUERY_ENV))
    	{
    		result = mResultFactory.getResult(ResultFactory.RESULT_ENV,cmd.getId(),null);
    	}
    	else
    	{
    		LogManager.local(TAG, "bad action");
    	}

    	ZMIQResult resultIQ = new ZMIQResult();
    	if(result != null)
    	{
    		ret = true;
        	resultIQ.setResult(result);
        	mXmppClient.sendPacketAsync((Packet)resultIQ);
    	}else
    	{
        	result = mResultFactory.getResult(ResultFactory.RESULT_NORMAL, 
        			cmd.getId(), "NG");
        	if(result != null)
        	{
            	resultIQ.setResult(result);
            	mXmppClient.sendPacketAsync((Packet)resultIQ);
        	}
    	}

    	LogManager.local(TAG, "handleCommand4Query return: "+ret);
    	return ret;
    }
		
}
