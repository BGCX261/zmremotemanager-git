package com.zm.xmpp.communication.result;

public abstract class AbstractResult {
	protected String id;
	protected String status;
	protected String errorCode;
	protected String direction;
	protected String issueTime;
	protected String deviceId;
	
	
	public String getIssueTime() {
		return issueTime;
	}
	public void setIssueTime(String issuetime) {
		this.issueTime = issuetime;
	}
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorcode) {
		this.errorCode = errorcode;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}


}
