package com.zm.xmpp.communication.client;

import java.util.Collection;
import java.util.Iterator;

import org.dom4j.Element;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.xmpp.packet.IQ;

import com.zm.epad.structure.Application;
import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.command.Command4App;
import com.zm.xmpp.communication.command.ICommand;

public class ClientCommandPackager4App implements IClientCommandPackager{
	public final static String name="app";
	
	public void parseCommand(String paraName,String value,ICommand c){
				
		if(paraName.equals("action")){
			c.setAction(value);
		}else if(paraName.equals("id")){
			c.setId(value);
		}else if(paraName.equals("issuetime")){
			c.setIssuetime(value);
		}else if(paraName.equals("appname")){
			if(((Command4App)c).getApp()==null){
				((Command4App)c).setApp(new Application());
			}
			((Command4App)c).getApp().setAppName(value);
		}else if(paraName.equals("version")){
			if(((Command4App)c).getApp()==null){
				((Command4App)c).setApp(new Application());
			}
			((Command4App)c).getApp().setVersion(value);
		}else if(paraName.equals("url")){
			if(((Command4App)c).getApp()==null){
				((Command4App)c).setApp(new Application());
			}
			((Command4App)c).getApp().setUrl(value);
		}
	}


	public void packCommand(org.jivesoftware.smack.packet.IQ p,ICommand c){
		((ZMIQCommand)p).setCommand(c);
	}
	
	public ICommand createCommand(){
		return new Command4App();
	}

}
