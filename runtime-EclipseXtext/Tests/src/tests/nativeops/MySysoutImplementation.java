package tests.nativeops;

import java.util.logging.Level;

import jadescript.java.InvokerAgent;

public class MySysoutImplementation extends sysout{

	@Override
	public void sysout(InvokerAgent invokerAgent, String t) {
		invokerAgent.log(Level.INFO, "From Java: "+t);
		
	}

}
