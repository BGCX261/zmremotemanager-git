package com.zm.xmpp.communication.client;

import java.util.HashMap;

import org.jivesoftware.smack.packet.IQ;

import com.zm.xmpp.communication.command.ICommand;

public class ClientCommandUtil {
	private static HashMap packagers=new HashMap();
	private static boolean initialed=false;
	
	public static void init(){
		packagers.put(ClientCommandPackager4App.name, new ClientCommandPackager4App());
		packagers.put(ClientCommandPackager4Query.name, new ClientCommandPackager4Query());
		initialed=true;
	}
	
	public static void addPackager(String name,IClientCommandPackager p){
		packagers.put(name, p);
	}
	
	public static void parseCommand(String paraName,String value,ICommand c){
		if(!initialed)init();
		((IClientCommandPackager)packagers.get(c.getType())).parseCommand(paraName,value, c);
	}

//	public static void packCommand(Packet p,Command c){
//		
//		Element e=p.getElement().addElement(Constants.XMPP_COMMAND, Constants.XMPP_NAMESPACE);
//		e.addElement("action").addText(c.getAction());
//		e.addElement("issuetime").addText(c.getIssueTime());
//		
//		Application app=c.getApp();
//		e.addElement("appname").addText(app.getAppName());
//		e.addElement("version").addText(app.getVersion());
//		e.addElement("url").addText(app.getUrl());
//		
//	}
	
	
	public static void packCommand(String type,org.jivesoftware.smack.packet.IQ p,ICommand c){
		if(!initialed)init();
		((IClientCommandPackager)packagers.get(type)).packCommand(p,c);
	}
	
	public static ICommand createCommand(String type){
		if (!initialed)
			init();
		return ((IClientCommandPackager)packagers.get(type)).createCommand();
	}

	
//	public static Command parseCommand(org.jivesoftware.smack.packet.Packet p){
//		System.out.println(p.getExtension(Constants.XMPP_COMMAND, Constants.XMPP_NAMESPACE).getClass().getName());
//		DefaultPacketExtension ex=(DefaultPacketExtension)p.getExtension(Constants.XMPP_COMMAND, Constants.XMPP_NAMESPACE);
//		Collection col=ex.getNames();
//		Iterator it=col.iterator();
//		Command command=new Command();
//		while(it.hasNext()){
//			String name=(String)it.next();
//			if(name.equals("action")){
//				command.setId(ex.getValue("id"));
//				command.setAction(ex.getValue("action"));
//				command.setIssueTime(ex.getValue("issuetime"));
//				
//				Application app=new Application(ex.getValue("appname"),ex.getValue("version"),ex.getValue("url"));
//				command.setApp(app);
//				
//				break;
//			}
//		}
//		
//		
//		return command;
//	}
}
