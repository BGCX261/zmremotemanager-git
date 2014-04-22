package com.zm.xmpp.communication.result;

import java.util.List;
import java.util.ArrayList;

public class ResultWebVisit extends AbstractResult implements IResult {
    private final static String type = "webvisit";

    private List<VisitInfo> mVisitInfoList = new ArrayList<VisitInfo>();

    public class VisitInfo {
        String url;
        String title;
        long lastDate;
        int visits;
    }

    public ResultWebVisit() {
    }

    public String getType() {
        return type;
    }

    public void setVisitInfo(String url, String title, long lastDate, int visits) {
        VisitInfo vi = new VisitInfo();
        vi.url = url;
        vi.title = title;
        vi.lastDate = lastDate;
        vi.visits = visits;
        mVisitInfoList.add(vi);
    }

    public List<VisitInfo> getVisitInfo() {
        return mVisitInfoList;
    }

    @Override
    public String toXML() {
        StringBuffer buf = new StringBuffer();
        toXMLBase(buf, type);
        for (VisitInfo vi : mVisitInfoList) {
            buf.append("<bookmark>");
            buf.append("<url>");
            buf.append(vi.url);
            buf.append("</url>");
            buf.append("<title>");
            buf.append(vi.title);
            buf.append("</title>");
            buf.append("<lastdate>");
            buf.append(vi.lastDate);
            buf.append("</lastdate>");
            buf.append("<visits>");
            buf.append(vi.visits);
            buf.append("</visits>");
            buf.append("</bookmark>");
        }
        return buf.toString();
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(" Result:[");
        buf.append("\r\n");
        buf.append("id=");
        buf.append(this.id);
        buf.append("/deviceid=");
        buf.append(this.deviceId);
        buf.append("/action=");
        buf.append(this.action);
        buf.append("/issuetime=");
        buf.append(this.issueTime);
        for (VisitInfo vi : mVisitInfoList) {
            buf.append("\r\n");
            buf.append("/bookmark");
            buf.append(" url=");
            buf.append(vi.url);
            buf.append(" title=");
            buf.append(vi.title);
            buf.append(" lastdate=");
            buf.append(vi.lastDate);
            buf.append(" visits=");
            buf.append(vi.visits);
        }
        return buf.toString();
    }
}
