package com.zm.xmpp.communication.result;

public interface IResult {
	
	public String getType();
	public String getId();
	public void setId(String id);
	public String getStatus();
	public void setStatus(String status);
	public String getErrorCode();
	public void setErrorCode(String errorcode);
	public String getDirection();
	public void setDirection(String direction);
	public String getIssueTime() ;
	public void setIssueTime(String issuetime) ;
	public void setDeviceId(String deviceId);
	public String getDeviceId();
	public String toString();
	public String toXML();
}
