package com.zm.xmpp.command;

import com.zm.epad.structure.Application;

public class Command {

	private String id;
	private String command;
	private String issueTime;
	private Application app;
	
	public Command(){
		
	}
	
	public Command(String id,String command,String time,String appName,String version,String url){
		this.id=id;
		this.command=command;
		this.issueTime=time;
		Application app=new Application(appName,version,url);
		this.app=app;
		
		
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}

	public Application getApp() {
		return app;
	}

	public void setApp(Application app) {
		this.app = app;
	}

	public String getIssueTime() {
		return issueTime;
	}
	public void setIssueTime(String issueTime) {
		this.issueTime = issueTime;
	}
	
	public String toString(){
		
		StringBuffer buf=new StringBuffer();
		buf.append("Command:[");
		buf.append("id=");
		buf.append(this.id);
		buf.append("/action=");
		buf.append(this.command);
		buf.append("/time=");
		buf.append(this.issueTime);		
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

}
