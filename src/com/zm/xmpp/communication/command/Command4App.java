package com.zm.xmpp.communication.command;

import com.zm.epad.structure.Application;
import com.zm.xmpp.communication.Constants;

public class Command4App extends AbstractCommand implements ICommand4App{
	
	private final static String type="app";
	private Application app;
	private int userId = 0;
	private String validation = null;
	
	
	public Command4App(){
		
	}
	
	public Command4App(String direction,String id,String action,String time,String appName,String version,String url){
		this.direction=direction;
		this.id=id;
		this.action=action;
		this.issueTime=time;
		Application app=new Application(appName,version,url);
		this.app=app;
		
		
	}

	public Command4App(String direction,String id,String action,String time,
			String appName,String version,String url, int userId, String validation){
		
		this.direction=direction;
		this.id=id;
		this.action=action;
		this.issueTime=time;
		this.app=new Application(appName,version,url);

		this.userId = userId;
		this.validation = validation;
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
		buf.append(this.issueTime);		
		if(this.app!=null){
			buf.append("/appname=");
			buf.append(this.app.getAppName());
			buf.append("/version=");
			buf.append(this.app.getVersion());
			buf.append("/url=");
			buf.append(this.app.getUrl());
			
		}
		buf.append("/userId=");
		buf.append(this.userId);
		buf.append("/validation=");
		buf.append(this.validation);

		buf.append("]");
		return buf.toString();
		
	}
	
	public String toXML(){
		StringBuffer buf=new StringBuffer();
		buf.append("<command xmlns=\"");
		buf.append(this.direction);
		buf.append("\" type=\"");
		buf.append(type);		
		buf.append("\">");
		buf.append("<id>");
		buf.append(this.id);
		buf.append("</id>");
		buf.append("<action>");
		buf.append(this.action);
		buf.append("</action>");
		buf.append("<issuetime>");
		buf.append(this.issueTime);
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
		buf.append("<userid>");
		buf.append(this.userId);
		buf.append("</userid>");
		buf.append("<validation>");
		buf.append(this.validation);
		buf.append("</validation>");
		
		buf.append("</command>");
		return buf.toString();
	}

	@Override
	public String getAppName() {
		// TODO Auto-generated method stub
		if(this.app == null)
		{
			return null;
		}
		return this.app.getAppName();
	}

	@Override
	public String getAppVersion() {
		// TODO Auto-generated method stub
		if(this.app == null)
		{
			return null;
		}
		return this.app.getVersion();
	}

	@Override
	public String getAppUrl() {
		// TODO Auto-generated method stub
		if(this.app == null)
		{
			return null;
		}
		return this.app.getUrl();
	}

	@Override
	public int getUserId() {
		// TODO Auto-generated method stub
		return userId;
	}

	@Override
	public String getValidation() {
		// TODO Auto-generated method stub
		return validation;
	}

	@Override
	public void setAppName(String name) {
		// TODO Auto-generated method stub
		if(this.app == null)
		{
			this.app = new Application();
		}
		this.app.setAppName(name);
	}

	@Override
	public void setAppVersion(String version) {
		// TODO Auto-generated method stub
		if(this.app == null)
		{
			this.app = new Application();
		}
		this.app.setVersion(version);
	}

	@Override
	public void setAppUrl(String url) {
		// TODO Auto-generated method stub
		if(this.app == null)
		{
			this.app = new Application();
		}
		this.app.setUrl(url);
	}

	@Override
	public void setUserId(int userId) {
		// TODO Auto-generated method stub
		this.userId = userId;
	}

	@Override
	public void setValidation(String validation) {
		// TODO Auto-generated method stub
		this.validation = validation;
	}

	@Override
	public void configCommand(String paraName, String value) {
		// TODO Auto-generated method stub

		if(paraName.equals("action")){
			this.setAction(value);
		}else if(paraName.equals("id")){
			this.setId(value);
		}else if(paraName.equals("issuetime")){
			this.setIssueTime(value);
		}else if(paraName.equals("appname")){
			this.setAppName(value);
		}else if(paraName.equals("version")){
			this.setAppVersion(value);
		}else if(paraName.equals("url")){
			this.setAppUrl(value);
		}else if(paraName.equals("userid")){
			this.setUserId(Integer.valueOf(value));
		}else if(paraName.equals("validation")){
			this.setValidation(value);
		}
		return;
	}


}
