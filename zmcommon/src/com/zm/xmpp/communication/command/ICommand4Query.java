package com.zm.xmpp.communication.command;

public interface ICommand4Query extends ICommand {
    public String getUrl();
    public void setUrl(String url);
    public void toCommand(String paraName,String value);
}
