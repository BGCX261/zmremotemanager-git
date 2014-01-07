package com.zm.xmpp.tindercompent;


import org.xmpp.component.AbstractComponent;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import com.zm.xmpp.communication.command.CommandSimulator;
import com.zm.xmpp.communication.server.ResultUtil;
 
public class TinderComponent extends AbstractComponent {
 
    private JID myAddress = null;//new JID("bot@TinderService.hafang-cn");
 
    public TinderComponent(){
//        server.getIQRouter().addHandler(new GroupTreeIQHander()); //1

    }
    @Override
    public String getDescription() {
        return "A component that will respond with a friendly "
                + "'hello' to every message it receives.";
    }
    
    @Override
    protected IQ handleIQGet(IQ iq)
            throws java.lang.Exception{
    	
    	System.out.println("Enter IQ Get");
    	IQ reply=CommandSimulator.simulateInstall();
    	this.send(reply);
    	return reply;
    	
    }
    
    @Override
    protected IQ handleIQSet(IQ iq){
    	System.out.println("Enter IQ Set");
    	System.out.println(iq.toXML());
    	
    	IQ rq=ResultUtil.resultOK(iq);
    	
    	System.out.println("reply:"+rq.toXML());
    	return rq;
    }
 
    
    @Override
    protected void handleMessage(Message received) {
        if(received.getType().equals("normal"))return; //广播类消息,会有客户端在键盘输入的广播消息
        System.out.println("xxxxxxxxxxxxxxx");
        IQ response=CommandSimulator.simulateInstall();
        // send the response using AbstractComponent#send(Packet)
        this.send(response);

    }


	@Override
	public String getName() {
		
		return "TinderService";
	}
}
