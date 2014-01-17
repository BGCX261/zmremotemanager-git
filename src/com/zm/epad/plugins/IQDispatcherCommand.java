package com.zm.epad.plugins;

import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

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
	
	private static final int EVT_COMMAND = 1;
	private static final int EVT_CALLBACK = 2;
	
	private Context mContext;
	private XmppClient mXmppClient;
	private ZMIQCommandProvider mProvider;
	private RemotePkgsManager mPkgManager;
	private ResultFactory mResultFactory;
	private HandlerThread mThread;
	private Handler mHandler;

	
	@Override
	public void destroy() {
		try{
			mThread.quit();
			mThread.join();			
		}catch(Exception e){
			LogManager.local(TAG, "destroy:"+e.toString());
		}
		
		super.destroy();
	}

	public IQDispatcherCommand(Context context, String namespace, XmppClient XmppCliet)
	{
        LogManager.local(TAG, "create: " + namespace);
		mContext = context;
		
		mStrElementName = "command";
		mStrNameSpace = namespace;
		mXmppClient = XmppCliet;
		
		mPkgManager = new RemotePkgsManager(mContext);		
		mProvider = new ZMIQCommandProvider();
		mResultFactory = new ResultFactory(mContext);
		
		mThread = new HandlerThread(TAG);
		mThread.start();
		mHandler = new Handler(mThread.getLooper(), new IQCommandCallback());
		
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
        
        return postIQCommand((ZMIQCommand)packet);
    }
    
    private boolean postIQCommand(ZMIQCommand iq)
    {
    	LogManager.local(TAG, "handlePacket:" + iq.getCommand().getType());
    	Message msg = mHandler.obtainMessage(EVT_COMMAND,iq);

    	return mHandler.sendMessage(msg);
    }
    
    private class IQCommandCallback implements Handler.Callback{

		@Override
		public boolean handleMessage(Message msg) {
			
			boolean ret = false;
			
			switch(msg.what){
			case EVT_COMMAND:
				ret = handleIQCommand((ZMIQCommand)msg.obj);
		    	break;
			case EVT_CALLBACK:
				if(msg.obj instanceof Packet){
					ret = mXmppClient.sendPacketAsync((Packet)msg.obj, 0);
				}
				break;
			default:
				break;
			}

	    	return ret;
		}
   	
    }

	private boolean handleIQCommand(ZMIQCommand iq)
	{
		boolean ret = true;
		
		
		ICommand cmd = iq.getCommand();
		LogManager.local(TAG, "handleIQCommand:"+cmd.getType());
		
    	if(cmd.getType().equals("app"))
    	{
    		IResult result = null;
    		result = handleCommand4App((ICommand4App)cmd);
    		
        	ZMIQResult resultIQ = new ZMIQResult();
        	resultIQ.setTo(iq.getFrom());
        	resultIQ.setFrom(iq.getTo());
        	
        	if(result != null)
        	{
            	resultIQ.setResult(result);
            	mXmppClient.sendPacketAsync((Packet)resultIQ, 0);
        	}
        	
    	}else if(cmd.getType().equals("query"))
    	{
    		List<IResult> resultList = null;
    		try{
	    		resultList = handleCommand4Query((ICommand4Query)cmd, 
	    				new CommandResultCallback(iq.getTo(), iq.getFrom()));
	    		
	        	if(resultList != null)
	        	{
	        		int i = 0;
	        		for(IResult r : resultList)
	        		{
	        			LogManager.local(TAG,"send packet start "+ (i+1));
	                	ZMIQResult resultIQ = new ZMIQResult();
	                	resultIQ.setTo(iq.getFrom());
	                	resultIQ.setFrom(iq.getTo());
	                	
	                	resultIQ.setResult(r);
	                	mXmppClient.sendPacketAsync((Packet)resultIQ, (1000)*i++);
	                	LogManager.local(TAG,"send packet end "+i);
	        		}
	        	}
    		}catch(Exception e){
            	ZMIQResult resultIQ = new ZMIQResult();
            	resultIQ.setTo(iq.getFrom());
            	resultIQ.setFrom(iq.getTo());
            	
            	IResult r = mResultFactory.getResult(ResultFactory.RESULT_NORMAL, cmd.getId(), "NG");
            	resultIQ.setResult(r);
            	mXmppClient.sendPacketAsync((Packet)resultIQ, 0);
    		}
    	}else{
            LogManager.local(TAG, "bad command: " + cmd.getType());
            ret = false;
    	}
    	
    	return ret;
	}
	
    private IResult handleCommand4App(ICommand4App cmd)
    {
    	boolean ret = false; 
    	IResult result = null;
    	
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
    	   	
    	result = mResultFactory.getResult(ResultFactory.RESULT_NORMAL, 
    			cmd.getId(), ret==true?"OK":"NG");
    	
        LogManager.local(TAG, "handleCommand4App return:"+ret);
    	return result;
    	
    }
    
    private List<IResult> handleCommand4Query(ICommand4Query cmd, 
    		CommandResultCallback callback) throws Exception
    {
    	List<IResult> results = null;
    	String action = cmd.getAction();
    	LogManager.local(TAG, "handleCommand4Query:" + action);
    	
    	if(action.equals(Constants.XMPP_QUERY_APP))
    	{
    		results = mResultFactory.getResults(ResultFactory.RESULT_APP, cmd.getId());
    		if(results == null)
    		{
    			throw new Exception("failed to get app info");
    		}
    	}
    	else if(action.equals(Constants.XMPP_QUERY_DEVICE))
    	{
    		results = mResultFactory.getResults(ResultFactory.RESULT_DEVICE, cmd.getId(), callback);
    	}
    	else if(action.equals(Constants.XMPP_QUERY_ENV))
    	{
    		results = mResultFactory.getResults(ResultFactory.RESULT_ENV, cmd.getId());
    		if(results == null)
    		{
    			throw new Exception("failed to get env info");
    		}
    	}
    	else
    	{
    		LogManager.local(TAG, "handleCommand4Query bad action");
    	}
    	

    	LogManager.local(TAG, "handleCommand4Query return: "+(results==null?0:results.size()));
    	return results;
    }
    
    private class CommandResultCallback implements ResultFactory.ResultCallback{
    	String mFrom = null;
    	String mTo = null;
    	
    	public CommandResultCallback(String from, String to)
    	{
    		mFrom = from;
    		mTo = to;
    	}
    	
		@Override
		public void handleResult(IResult result) {
			// TODO Auto-generated method stub
	    	LogManager.local(TAG, "handleResult:" + result.getType());
	    	
	    	ZMIQResult resultIQ = new ZMIQResult();
        	resultIQ.setTo(mTo);
        	resultIQ.setFrom(mFrom);
	    	resultIQ.setResult(result);
	    	
	    	Message msg = mHandler.obtainMessage(EVT_CALLBACK, resultIQ);

	    	mHandler.sendMessage(msg);			
		}
    	
    }
	
}
