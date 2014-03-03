package com.zm.xmpp.communication.result;

public class ResultDeviceReport extends AbstractResult implements IResult {

    private final static String type = "devicereport";
    private String action = "";
    private String longitude = "";
    private String latitude = "";
    private long loctime = 0;

    public ResultDeviceReport() {

    }

    public ResultDeviceReport(String id, String status) {
        this.id = id;
        this.status = status;
        this.errorCode = "0";
    }

    public ResultDeviceReport(String id, String status, String errorcode) {
        this.id = id;
        this.status = status;
        this.errorCode = errorcode;
    }

    public String getType() {
        return type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public long getLoctime() {
        return loctime;
    }

    public void setLoctime(long loctime) {
        this.loctime = loctime;
    }

    @Override
    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<result xmlns=\"");
        buf.append(this.direction);
        buf.append("\" type=\"");
        buf.append(type);
        buf.append("\">");
        buf.append("<id>");
        buf.append(this.id);
        buf.append("</id>");
        buf.append("<deviceid>");
        buf.append(this.deviceId);
        buf.append("</deviceid>");
        buf.append("<status>");
        buf.append(this.status);
        buf.append("</status>");
        buf.append("<errorcode>");
        buf.append(this.errorCode);
        buf.append("</errorcode>");
        buf.append("<action>");
        buf.append(this.action);
        buf.append("</action>");
        buf.append("<longitude>");
        buf.append(this.longitude);
        buf.append("</longitude>");
        buf.append("<latitude>");
        buf.append(this.latitude);
        buf.append("</latitude>");
        buf.append("<loctime>");
        buf.append(this.loctime);
        buf.append("</loctime>");

        buf.append("</result>");
        return buf.toString();
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(type);
        buf.append(" Result:[");
        buf.append("id=");
        buf.append(this.id);
        buf.append("/deviceid=");
        buf.append(this.deviceId);
        buf.append("/status=");
        buf.append(this.status);
        buf.append("/errorcode=");
        buf.append(this.errorCode);
        buf.append("/action=");
        buf.append(this.action);
        buf.append("/longitude=");
        buf.append(this.longitude);
        buf.append("/latitude=");
        buf.append(this.latitude);
        buf.append("/loctime=");
        buf.append(this.loctime);

        buf.append("]");
        return buf.toString();

    }

}
