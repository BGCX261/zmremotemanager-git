package com.zm.epad.plugins;

import org.xmlpull.v1.XmlPullParser;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.packet.Packet;

import com.zm.epad.core.NetCmdDispatcher.CmdDispatchInfo;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.client.ZMIQCommandProvider;
import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.command.ICommand4App;

public class IQDispatcherCommand extends CmdDispatchInfo {
	private static final String  TAG="IQDispatcherCommand";
	
	private static final String APP_INSTALL = "install";
	private static final String APP_REMOVE = "remove";
	private static final String APP_ENABLE = "enable";
	private static final String APP_DISABLE = "disable";
	
	private Context mContext;
	private ZMIQCommandProvider mProvider;
	private RemotePkgsManager mPkgManager;

	
	public IQDispatcherCommand(Context context, String namespace)
	{
		Log.v(TAG, "create: "+namespace);
		mContext = context;
		mStrElementName = "command";
		mStrNameSpace = namespace;
		mPkgManager = new RemotePkgsManager(mContext);		
		mProvider = new ZMIQCommandProvider();
	}
	
	@Override
    public IQ parseXMLStream(XmlPullParser parser){
		IQ ret = null;
		
		Log.v(TAG, "parseXMLStream");
		try{
			ret = mProvider.parseIQ(parser);
		}catch(Exception e)
		{
			Log.e(TAG, "parseXMLStream:"+e.toString());
		}
		
        return ret;
    }
    
    @Override
    public boolean handlePacket(Packet packet){
    	   	
    	if(!(packet instanceof ZMIQCommand))
    	{
    		Log.w(TAG,"not ZMIQCommand");
    		return false;
    	}
    	boolean ret = true;

    	ICommand cmd = ((ZMIQCommand)packet).getCommand();
    	Log.v(TAG, "handlePacket:"+cmd.getType());
    	
    	if(cmd.getType().equals("app"))
    	{
    		ret = handleCommand4App((ICommand4App)cmd);
    	}else
    	{
    		Log.w(TAG, "bad command: "+cmd.getType());
    		ret = false;
    	}
    	return ret;
    }

    private boolean handleCommand4App(ICommand4App cmd)
    {
    	boolean ret = false;    	
    	Log.v(TAG, "handleCommand4App:"+cmd.getAction());
    	
    	if(cmd.getAction().equals(APP_ENABLE))
    	{
    		String name = cmd.getAppName();
    		int userId = cmd.getUserId();
    		ret = mPkgManager.enablePkgForUser(name, userId);
    	}
    	else if(cmd.getAction().equals(APP_DISABLE))
    	{
    		String name = cmd.getAppName();
    		int userId = cmd.getUserId();
    		ret = mPkgManager.disablePkgForUser(name, userId);
    	}
    	else if(cmd.getAction().equals(APP_INSTALL))
    	{
    		String url = cmd.getAppUrl();
    		int userId = cmd.getUserId();
    		ret = mPkgManager.installPkgForUser(url, userId);
    	}
    	else if(cmd.getAction().equals(APP_REMOVE))
    	{
    		String name = cmd.getAppName();
    		int userId = cmd.getUserId();
    		ret = mPkgManager.uninstallPkgForUser(name, userId);
    	}
    	else
    	{
    		Log.d(TAG,"bad action");
    		return false;
    	}
    	
    	Log.v(TAG, "return:"+ret);
    	return ret;
    	
    }
		
}
