package com.zm.xmpp.communication.result;

public interface IResult {
	
	public String getType();
	public String getId();
	public void setId(String id);
	public String getStatus();
	public void setStatus(String status);
	public String getErrorcode();
	public void setErrorcode(String errorcode);
	public String getDirection();
	public void setDirection(String direction);
	public String getIssuetime() ;
	public void setIssuetime(String issuetime) ;
	public String toString();
	public String toXML();
}
