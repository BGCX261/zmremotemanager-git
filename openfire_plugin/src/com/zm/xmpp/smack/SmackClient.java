package com.zm.xmpp.smack;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;

public class SmackClient {

	/**
	 * @param args
	 */
		public static void main(String args[]) throws XMPPException{
			ConnectionConfiguration config = new ConnectionConfiguration("127.0.0.1",5222);
			XMPPConnection.DEBUG_ENABLED = true;

			XMPPConnection conn= getConnection(config,"yan", "314159");
			buildMsg(conn);
			
			try {
				Thread.sleep(100000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		static void buildMsg(XMPPConnection conn2){
			Message req = new Message();
			req.setType(Message.Type.chat);
			req.setFrom("yan@hafang-cn/Smack");
			req.setTo("no@tinderservice.hafang-cn");
			req.setBody("This is client message");
			System.out.println("message sent: "+req.toXML());
			conn2.sendPacket(req);
			System.out.println("finished");
		}
		
		static XMPPConnection getConnection(ConnectionConfiguration config,String username,String password) throws XMPPException{
			XMPPConnection conn2 = new XMPPConnection(config);
			boolean auth = conn2.getSASLAuthentication().isAuthenticated();
			
			conn2.connect();
			conn2.login(username,password);

			String service = conn2.getServiceName();
			System.out.println("Connected,Domain name:"+service);
			PacketListener listener = new SimplePacketListener();
			PacketFilter filter = new SimplePacketFilter();
			conn2.addPacketListener(listener, filter);
			return conn2;
		}

	}

