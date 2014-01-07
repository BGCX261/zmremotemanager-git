package com.zm.xmpp.communication.server;

import org.dom4j.Element;
import org.xmpp.packet.IQ;

import com.zm.epad.structure.Application;
import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.command.Command4App;
import com.zm.xmpp.communication.command.ICommand;

public class CommandPackager4App implements ICommandPackager{
	public final static String name="app";
	
	public void parseCommand(IQ iq,ICommand c){
		Command4App ac=(Command4App)c;
		
		ac.setId(iq.getChildElement().element("id").getText());
		ac.setAction(iq.getChildElement().element("action").getText());
		ac.setDirection(iq.getChildElement().getNamespaceURI());
		ac.setIssuetime(iq.getChildElement().element("issuetime").getText());
		Application app=new Application();
		ac.setApp(app);
		app.setAppName(iq.getChildElement().element("appname").getText());
		app.setVersion(iq.getChildElement().element("version").getText());
		app.setUrl(iq.getChildElement().element("url").getText());
		
	}

	public void packCommand(IQ p,ICommand c){
		
		Element e=p.setChildElement(Constants.XMPP_COMMAND,c.getDirection());
		e.addAttribute("type", c.getType());
		e.addElement("action").addText(c.getAction());
		e.addElement("id").addText(c.getId());
		e.addElement("issuetime").addText(c.getIssuetime());
		
		Application app=((Command4App)c).getApp();
		e.addElement("appname").addText(app.getAppName());
		e.addElement("version").addText(app.getVersion());
		e.addElement("url").addText(app.getUrl());
	}
		
	public ICommand createCommand(){
		return new Command4App();
	}

}
