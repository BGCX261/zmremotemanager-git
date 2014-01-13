package com.zm.xmpp.communication;

public class CommunicationException extends Exception{

	private String errorCode;
	private String message;
	
	public CommunicationException(String errorCode,String message){
		this.errorCode=errorCode;
		this.message=message;
	}
	
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String toString(){
		StringBuffer buf=new StringBuffer();
		buf.append("Error:");
		buf.append(this.message);
		
		return buf.toString();
	}

}
