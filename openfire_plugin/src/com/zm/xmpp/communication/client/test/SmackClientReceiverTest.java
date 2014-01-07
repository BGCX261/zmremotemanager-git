package com.zm.xmpp.communication.client.test;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.client.ClientCommandUtil;
import com.zm.xmpp.communication.client.SmackClient;
import com.zm.xmpp.communication.command.Command4App;

public class SmackClientReceiverTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ConnectionConfiguration config = new ConnectionConfiguration(
				"127.0.0.1", 5222);
		XMPPConnection.DEBUG_ENABLED = true;
		
		SmackClient.debug="reciver";
		SmackClient.listen=Constants.XMPP_NAMESPACE_CENTER;
		SmackClient.send=Constants.XMPP_NAMESPACE_PAD;
				SmackClient.clientId="hao@hafang-cn/Smack";
		
		System.out.println("Smack client receiver " + SmackClient.clientId);
		
		try {
			XMPPConnection conn = SmackClient.getConnection(config, "hao", "314159",new ReceiverCommandProcessor());
		
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

}
