package com.zm.xmpp.tindercompent;

import org.jivesoftware.whack.ExternalComponentManager;
import org.xmpp.component.ComponentException;

public class TinderComponentTest {

	/**
	 * @param args
	 */
	public static void main(String args[]) throws ComponentException{
		TinderComponent comp = new TinderComponent();
		final ExternalComponentManager mgr = new ExternalComponentManager("127.0.0.1", 5275);
		mgr.setSecretKey("TinderService", "314159");
		mgr.addComponent("TinderService", comp);
		
		while(true){
			try {
				Thread.sleep(100000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
	}


}
