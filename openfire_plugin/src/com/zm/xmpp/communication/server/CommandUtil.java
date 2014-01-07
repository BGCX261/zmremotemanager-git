package com.zm.xmpp.communication.server;

import java.util.HashMap;

import org.xmpp.packet.IQ;

import com.zm.xmpp.communication.command.ICommand;

public class CommandUtil {
	private static HashMap packagers=new HashMap();
	private static boolean initialed=false;
	
	public static void init(){
		packagers.put(CommandPackager4App.name, new CommandPackager4App());
		initialed=true;
	}
	
	public static void addPackager(String name,ICommandPackager p){
		packagers.put(name, p);
	}
	
	public static void parseCommand(IQ iq,ICommand c){
		if(!initialed)init();
		((ICommandPackager)packagers.get(c.getType())).parseCommand(iq,c);
	}
	
	public static ICommand parseCommand(IQ iq){
		if(!initialed)init();
		String type=iq.getChildElement().attributeValue("type");
		ICommand c=((ICommandPackager)packagers.get(type)).createCommand();
		
		((ICommandPackager)packagers.get(type)).parseCommand(iq,c);
		
		return c;
	}
	
	public static void packCommand(IQ p,ICommand c){
		if(!initialed)init();
		((ICommandPackager)packagers.get(c.getType())).packCommand(p, c);
	}
	
	public static ICommand createCommand(String type){
		return ((ICommandPackager)packagers.get(type)).createCommand();
	}

}
