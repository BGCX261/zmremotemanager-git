package com.zm.xmpp.whackcomponent;

import java.util.ArrayList;
import java.util.Collection;

import org.jivesoftware.whack.ExternalComponentManager;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import com.zm.epad.structure.Application;
import com.zm.xmpp.communication.client.ClientCommandUtil;
import com.zm.xmpp.communication.command.Command4App;

public class WhackComponent  implements org.xmpp.component.Component {
	    private ExternalComponentManager mgr = null;

	    public String getName() {
	        return ("Upper case");
	    }

	    public String getDescription() {
	        return ("Echos your message back to you in upper case");
	    }

    
	    public void processPacket(Packet packet) {
	        if (packet instanceof Message) {
	        	if(((Message)packet).getType().equals("normal"))return; //广播类消息,会有客户端在键盘输入的广播消息
	        	
	            org.xmpp.packet.Message original = (Message) packet;
	            org.xmpp.packet.Message response = original.createCopy();
	            //Swap the sender/recipient fields
	            response.setTo(original.getFrom());
	            response.setFrom(original.getTo());
	            //Convert the text to upper case 
	            
	            response.setBody("Hello from WhackService");

	            Command4App c=new Command4App("up","id","install","20101112T13:10:11","app1","1.0","http://xx.xx");
        
//	            CommandUtil.packCommand(response, c);
	            
	            System.out.println(response.toXML());
	            mgr.sendPacket(this, response);
	        }
	    }

	    public void initialize(JID jid, ComponentManager componentManager) throws ComponentException {
	        mgr = (ExternalComponentManager) componentManager;     	
	    }

	    public void start() { }

	    public void shutdown() { }
	}


