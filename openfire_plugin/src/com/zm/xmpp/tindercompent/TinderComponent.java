package com.zm.xmpp.tindercompent;


import org.xmpp.component.AbstractComponent;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import com.zm.xmpp.command.Command;
import com.zm.xmpp.command.CommandUtil;
 
public class TinderComponent extends AbstractComponent {
 
    private JID myAddress = null;//new JID("bot@TinderService.hafang-cn");
 
    @Override
    public String getDescription() {
        return "A component that will respond with a friendly "
                + "'hello' to every message it receives.";
    }
 
    @Override
    public String getDomain() {
        return "hafang-cn";
    }
 
    @Override
    public String getName() {
        return "hello";
    }
 
    @Override
    protected void handleMessage(Message received) {
        // construct the response
        Message response = new Message();
        if(received.getType().equals("normal"))return; //广播类消息,会有客户端在键盘输入的广播消息
        
        response.setFrom(myAddress);
        response.setTo(received.getFrom());
        response.setBody("Hello from TinderService");
        
        Command c=new Command("id","install","20101112T13:10:11","app1","1.0","http://xx.xx");
        
        CommandUtil.packCommand(response, c);
 
        // send the response using AbstractComponent#send(Packet)
        send(response);
    }
}
