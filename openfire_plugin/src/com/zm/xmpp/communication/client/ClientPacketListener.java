package com.zm.xmpp.communication.client;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;

import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.result.IResult;

public class ClientPacketListener implements PacketListener {

	private IProcessor processor;
	
	public ClientPacketListener(IProcessor processor){
		this.processor=processor;
	}
	
	@Override
	public void processPacket(Packet packet) {
		System.out.println("process packet in listener");
		System.out.println(packet.toXML());
		
		if(packet instanceof ZMIQCommand){
			if(processor!=null)processor.process((ZMIQCommand)packet);
		}else if(packet instanceof ZMIQResult){
			if(processor!=null)processor.process((ZMIQResult)packet);
		}

		
	}

}