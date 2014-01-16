package com.zm.xmpp.communication.command;

public interface ICommand {
	public String getType();
	public String getId() ;
	public void setId(String id) ;
	public String getAction();
	public void setAction(String action);
	public String getIssueTime() ;
	public void setIssueTime(String issuetime);
	public String getDirection() ;
	public void setDirection(String direction);
	public String toString();
	public String toXML();

}
