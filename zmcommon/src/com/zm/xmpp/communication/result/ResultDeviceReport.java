package com.zm.xmpp.communication.result;

public class ResultDeviceReport extends AbstractResult implements IResult {

    private final static String type = "devicereport";
    private String action = "";
    private String longitude = "";
    private String latitude = "";
    private long loctime = 0;
    private String locmode = "";
    private String bstype = "";
    private String bsinfo = "";

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

    public void setMode(String locmode) {
        this.locmode = locmode;
    }

    public void setBaseStationType(String type) {
        this.bstype = type;
    }

    public void setBaseStationInfo(String info) {
        this.bsinfo = info;
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

    public String getMode(String locmode) {
        return this.locmode;
    }

    public String getBaseStationType() {
        return this.bstype;
    }

    public String getBaseStationInfo() {
        return this.bsinfo;
    }

    @Override
    public String toXML() {
        StringBuffer buf = new StringBuffer();
        toXMLBase(buf, type);
        buf.append("<locmode>");
        buf.append(this.locmode);
        buf.append("</locmode>");
        buf.append("<longitude>");
        buf.append(this.longitude);
        buf.append("</longitude>");
        buf.append("<latitude>");
        buf.append(this.latitude);
        buf.append("</latitude>");
        buf.append("<loctime>");
        buf.append(this.loctime);
        buf.append("</loctime>");
        buf.append("<bstype>");
        buf.append(this.bstype);
        buf.append("</bstype>");
        buf.append("<bsinfo>");
        buf.append(this.bsinfo);
        buf.append("</bsinfo>");
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
        buf.append("/locmode=");
        buf.append(this.locmode);
        buf.append("/longitude=");
        buf.append(this.longitude);
        buf.append("/latitude=");
        buf.append(this.latitude);
        buf.append("/loctime=");
        buf.append(this.loctime);
        buf.append("/bstype=");
        buf.append(this.bstype);
        buf.append("/bsinfo=");
        buf.append(this.bsinfo);

        buf.append("]");
        return buf.toString();

    }

}
