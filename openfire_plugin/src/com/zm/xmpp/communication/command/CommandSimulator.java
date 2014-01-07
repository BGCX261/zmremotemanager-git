package com.zm.xmpp.communication.command;

import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import com.zm.epad.structure.Application;
import com.zm.xmpp.communication.Constants;

public class CommandSimulator {

	/*
	 * 
	 */
	public static IQ simulateInstall(){
        IQ request = new IQ();
                
        request.setFrom("bot@tinderservice.hafang-cn");
        request.setTo("yan@hafang-cn/Smack");
        request.setType(IQ.Type.set);
                
        Command4App c=new Command4App("down","idxxxxxxxx","install","20101112T13:10:11","app1","1.0","http://xx.xx");
        
        packCommand(request, c);
        System.out.println(request.toXML());
        return request;
		
	}
	
	public static void packCommand(Packet p,Command4App c){
		
		Element e=p.getElement().addElement(Constants.XMPP_COMMAND, Constants.XMPP_NAMESPACE_CENTER);
		e.addAttribute("type", "app");
		e.addElement("action").addText(c.getAction());
		e.addElement("id").addText(c.getId());
		e.addElement("issuetime").addText(c.getIssuetime());
		
		Application app=c.getApp();
		e.addElement("appname").addText(app.getAppName());
		e.addElement("version").addText(app.getVersion());
		e.addElement("url").addText(app.getUrl());
		
	}

}
