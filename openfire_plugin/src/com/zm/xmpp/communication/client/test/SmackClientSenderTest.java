package com.zm.xmpp.communication.client.test;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.ProviderManager;

import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.client.ClientCommandUtil;
import com.zm.xmpp.communication.client.SmackClient;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.command.Command4App;
import com.zm.xmpp.communication.command.ICommand;

public class SmackClientSenderTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ConnectionConfiguration config = new ConnectionConfiguration(
				"127.0.0.1", 5222);
		XMPPConnection.DEBUG_ENABLED = true;

		try {
			SmackClient.debug="sender";
			SmackClient.clientId="yan@hafang-cn/Smack";
			SmackClient.destId="hao@hafang-cn";
			SmackClient.send=Constants.XMPP_NAMESPACE_MANAGE;
			SmackClient.listen=Constants.XMPP_NAMESPACE_CENTER;
			
			
			System.out.println("Smack client sender " + SmackClient.clientId);
			
			XMPPConnection conn = SmackClient.getConnection(config, "yan", "314159",null);
			Command4App command = new Command4App(SmackClient.send,"id888", "install", "20101112T13:10:11", "app1",
					"1.0", "http://xx.xx");
			sendCommand(conn, "app", SmackClient.clientId, SmackClient.destId, command);
			
			
		} catch (XMPPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			Thread.sleep(100000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * @param args
	 */
	
	public static void sendCommand(XMPPConnection conn,String commandType,String from,String to,ICommand command){
		IQ request = new ZMIQCommand();

		request.setTo(to);
		request.setFrom(from);
		request.setType(IQ.Type.SET);

		ClientCommandUtil.packCommand(commandType,request, command);
		conn.sendPacket(request);
	
		System.out.println("send "+ request.toXML());
	}
}
