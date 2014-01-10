package com.zm.xmpp.communication.command;

public interface ICommand4App extends ICommand {
	public String getAppName();
	public String getAppVersion();
	public String getAppUrl();
	public int getUserId();
	public String getValidation();
	public void setAppName(String name);
	public void setAppVersion(String version);
	public void setAppUrl(String url);
	public void setUserId(int userId);
	public void setValidation(String validation);
	public void configCommand(String paraName,String value);
}
