package com.zm.xmpp.communication.command;

import com.zm.epad.structure.Application;
import com.zm.xmpp.communication.Constants;

public class Command4App extends AbstractCommand implements ICommand{

	private final static String type="app";
	private Application app;
	
	
	public Command4App(){
		
	}
	
	public Command4App(String direction,String id,String action,String time,String appName,String version,String url){
		this.direction=direction;
		this.id=id;
		this.action=action;
		this.issuetime=time;
		Application app=new Application(appName,version,url);
		this.app=app;
		
		
	}

	public Application getApp() {
		return app;
	}

	public void setApp(Application app) {
		this.app = app;
	}

	public String getType(){
		return type;
	}
	
	public String toString(){
		
		StringBuffer buf=new StringBuffer();
		buf.append("App Command:[");
		buf.append("id=");
		buf.append(this.id);
		buf.append("/action=");
		buf.append(this.action);
		buf.append("/issuetime=");
		buf.append(this.issuetime);		
		if(this.app!=null){
			buf.append("/appname=");
			buf.append(this.app.getAppName());
			buf.append("/version=");
			buf.append(this.app.getVersion());
			buf.append("/url=");
			buf.append(this.app.getUrl());
			
		}
		buf.append("]");
		return buf.toString();
		
	}
	
	public String toXML(){
		StringBuffer buf=new StringBuffer();
		buf.append("<command xmlns=\"");
		buf.append(this.direction);
		buf.append("\" type=\"app\">");
		buf.append("<id>");
		buf.append(this.id);
		buf.append("</id>");
		buf.append("<action>");
		buf.append(this.action);
		buf.append("</action>");
		buf.append("<issuetime>");
		buf.append(this.issuetime);
		buf.append("</issuetime>");
		if(this.app!=null){
			buf.append("<appname>");
			buf.append(this.app.getAppName());
			buf.append("</appname>");
			buf.append("<version>");
			buf.append(this.app.getVersion());
			buf.append("</version>");
			buf.append("<url>");
			buf.append(this.app.getUrl());
			buf.append("</url>");			
		
		}
		buf.append("</command>");
		return buf.toString();
	}

}
