package com.zm.xmpp.whackcomponent;



import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.whack.ExternalComponentManager;
import org.xmpp.component.ComponentException;

public class WhackComponentTest{
	   public static void main(String[] args) {
	      ExternalComponentManager mgr = new ExternalComponentManager("127.0.0.1", 5275);
	      mgr.setSecretKey("WhackService", "314159");
	      try{
	      try {
	         mgr.addComponent("WhackService", new WhackComponent());
	      } catch (ComponentException e) {
	    	  e.printStackTrace();
//	         Logger.getLogger(WhackComponentTest.class.getName()).log(Level.SEVERE, "main", e);
	         System.exit(-1);
	      }
	      //Keep it alive
	      while (true)
	         try {
	            Thread.sleep(10000);
	         } catch (Exception e) {
	        	 e.printStackTrace();
//	            Logger.getLogger(WhackComponentTest.class.getName()).log(Level.SEVERE, "main", e);
	         }
	      }finally{
	    	  try {
				mgr.removeComponent("WhackComponent");
			} catch (ComponentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	      }
	   }
	}

