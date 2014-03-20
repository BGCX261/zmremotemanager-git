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

    protected void toXMLBase(StringBuffer buf, String type) {
        buf.append("<result xmlns=\"");
        buf.append(this.direction);
        buf.append("\" type=\"");
        buf.append(type);
        buf.append("\">");
        if (this.id != null) {
            buf.append("<id>");
            buf.append(this.id);
            buf.append("</id>");
        }
        if (this.deviceId != null) {
            buf.append("<deviceid>");
            buf.append(this.deviceId);
            buf.append("</deviceid>");
        }
        if (this.issueTime != null) {
            buf.append("<issuetime>");
            buf.append(this.issueTime);
            buf.append("</issuetime>");
        }
        if (this.status != null) {
            buf.append("<status>");
            buf.append(this.status);
            buf.append("</status>");
        }
        if (this.errorCode != null) {
            buf.append("<errorcode>");
            buf.append(this.errorCode);
            buf.append("</errorcode>");
        }
    }

}
