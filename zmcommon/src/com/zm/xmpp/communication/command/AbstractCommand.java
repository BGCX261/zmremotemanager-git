package com.zm.xmpp.communication.command;

public abstract class AbstractCommand {
	protected String id;
	protected String action;
	protected String issueTime;
	protected String direction;
	protected String to;
	
	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getIssueTime() {
		return issueTime;
	}
	public void setIssueTime(String issuetime) {
		this.issueTime = issuetime;
	}
	
	

}
