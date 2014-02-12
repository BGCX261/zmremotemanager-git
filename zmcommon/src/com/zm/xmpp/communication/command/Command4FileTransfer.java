package com.zm.xmpp.communication.command;

public class Command4FileTransfer extends AbstractCommand implements ICommand{
    private final static String type = "push";
    private String mUrl = "";
    private String mExpire = null;
    private String mParameter = null;

    public Command4FileTransfer() {

    }

    public Command4FileTransfer(String id, String action, String issueTime,
            String url, String expire, String parameter) {
        this.id = id;
        this.action = action;
        this.issueTime = issueTime;
        this.mUrl = url;
        this.mExpire = expire;
        this.mParameter = parameter;
    }
    
    public String getType(){
        return type;
    }
    
    public String getUrl(){
        return mUrl;
    }
    
    public String getExpire(){
        return mExpire;
    }
    
    public String getParameter(){
        return mParameter;
    }
    
    public String toString(){
        StringBuffer buf=new StringBuffer();
        buf.append("FileTransfer Command:[");
        buf.append("id=");
        buf.append(this.id);
        buf.append("/action=");
        buf.append(this.action);
        buf.append("/issuetime=");
        buf.append(this.issueTime); 
        buf.append("/url=");
        buf.append(this.mUrl);
        buf.append("/expire=");
        buf.append(this.mExpire);
        buf.append("/parameter=");
        buf.append(this.mParameter);
        return buf.toString();
    }

    public String toXML(){
        StringBuffer buf=new StringBuffer();
        buf.append("<command xmlns=\"");
        buf.append(this.direction);
        buf.append("\" type=\"");
        buf.append(type);       
        buf.append("\">");
        buf.append("<id>");
        buf.append(this.id);
        buf.append("</id>");
        buf.append("<action>");
        buf.append(this.action);
        buf.append("</action>");
        buf.append("<issuetime>");
        buf.append(this.issueTime);
        buf.append("</issuetime>");
        buf.append("<url>");
        buf.append(this.mUrl);
        buf.append("</url>");
        buf.append("<expire>");
        buf.append(this.mExpire);
        buf.append("</expire>");
        buf.append("<parameter>");
        buf.append(this.mParameter);
        buf.append("</parameter>");
        
        buf.append("</command>");
        return buf.toString();
    }
    
    public void toCommand(String paraName, String value){
        
        if(paraName.equals("action")){
            this.setAction(value);
        }else if(paraName.equals("id")){
            this.setId(value);
        }else if(paraName.equals("issuetime")){
            this.setIssueTime(value);
        }else if(paraName.equals("url")){
            this.mUrl = value;
        }else if(paraName.equals("expire")){
            this.mExpire = value;
        }else if(paraName.equals("parameter")){
            this.mParameter = value;
        }
        return;        
    }
}
