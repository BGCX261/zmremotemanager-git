package com.zm.xmpp.communication.command;

import com.zm.xmpp.communication.Constants;

public class Command4Report extends AbstractCommand implements ICommand {

    private final static String type = "report";

    private String report;

    public Command4Report() {

    }

    public Command4Report(String direction, String id, String report,
            String action, String time) {
        this.direction = direction;
        this.id = id;
        this.report = report;
        this.action = action;
        this.issueTime = time;
    }

    public String getType() {
        return type;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public String toString() {

        StringBuffer buf = new StringBuffer();
        buf.append("Report Command:[");
        buf.append("id=");
        buf.append(this.id);
        buf.append("/report=");
        buf.append(this.report);
        buf.append("/action=");
        buf.append(this.action);
        buf.append("/issuetime=");
        buf.append(this.issueTime);
        buf.append("]");
        return buf.toString();

    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<command xmlns=\"");
        buf.append(this.direction);
        buf.append("\" type=\"");
        buf.append(type);
        buf.append("\">");
        buf.append("<id>");
        buf.append(this.id);
        buf.append("</id>");
        buf.append("<report>");
        buf.append(this.report);
        buf.append("</report>");
        buf.append("<action>");
        buf.append(this.action);
        buf.append("</action>");
        buf.append("<issuetime>");
        buf.append(this.issueTime);
        buf.append("</issuetime>");
        buf.append("</command>");
        return buf.toString();
    }

    public void toCommand(String paraName, String value) {
        // TODO Auto-generated method stub
        if (paraName.equals("id")) {
            this.setId(value);
        } else if (paraName.equals("report")) {
            this.setReport(value);
        } else if (paraName.equals("action")) {
            this.setAction(value);
        } else if (paraName.equals("issuetime")) {
            this.setIssueTime(value);
        }
    }

}
