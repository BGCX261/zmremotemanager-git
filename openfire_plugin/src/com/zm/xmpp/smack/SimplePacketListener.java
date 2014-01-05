package com.zm.xmpp.smack;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;

import com.zm.xmpp.command.Command;
import com.zm.xmpp.command.CommandUtil;

public class SimplePacketListener implements PacketListener {

	@Override
	public void processPacket(Packet packet) {
		// TODO Auto-generated method stub
		Command command=CommandUtil.parseCommand(packet);
		System.out.println("received: "+packet.toXML());
		System.out.println(command.toString());
	}

}