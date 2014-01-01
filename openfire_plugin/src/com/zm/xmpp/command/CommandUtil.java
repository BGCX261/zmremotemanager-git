package com.zm.xmpp.command;

import java.util.Collection;
import java.util.Iterator;

import org.dom4j.Element;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.xmpp.packet.Packet;

import com.zm.epad.structure.Application;

public class CommandUtil {

	public static void packCommand(Packet p,Command c){
		Element e=p.getElement().addElement("command", "com.zm.epad");
		e.addElement("action").addText(c.getCommand());
		e.addElement("id").addText(c.getId());
		e.addElement("issuetime").addText(c.getIssueTime());
		
		Application app=c.getApp();
		e.addElement("appname").addText(app.getAppName());
		e.addElement("version").addText(app.getVersion());
		e.addElement("url").addText(app.getUrl());
		
	}

	public static void packReplay(Packet p,Command c){
		
	}
	
	public static Command parseCommand(org.jivesoftware.smack.packet.Packet p){
		System.out.println(p.getExtension("command", "com.zm.epad").getClass().getName());
		DefaultPacketExtension ex=(DefaultPacketExtension)p.getExtension("command", "com.zm.epad");
		Collection col=ex.getNames();
		Iterator it=col.iterator();
		Command command=new Command();
		while(it.hasNext()){
			String name=(String)it.next();
			if(name.equals("action")){
				command.setId(ex.getValue("id"));
				command.setCommand(ex.getValue("action"));
				command.setIssueTime(ex.getValue("issuetime"));
				
				Application app=new Application(ex.getValue("appname"),ex.getValue("version"),ex.getValue("url"));
				command.setApp(app);
				
				break;
			}
		}
		
		
		return command;
	}
}
