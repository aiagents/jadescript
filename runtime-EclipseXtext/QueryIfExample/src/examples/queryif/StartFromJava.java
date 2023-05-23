package examples.queryif;

import jade.wrapper.StaleProxyException;
import jadescript.java.Jadescript;

public class StartFromJava {

	public static void main(String[] args) throws StaleProxyException {
		Jadescript.bindNative(randomInteger.class, RandomIntegerImpl.class);
		
		var container = Jadescript.newMainContainer();
		
		Test.create(container, "Test");

	}

}
