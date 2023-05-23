package examples.auction.gui;

import java.util.Random;
import jadescript.java.InvokerAgent;

public class RandomIntegerJava extends randomInteger {

	private Random random = new Random();
	
	@Override
	public Integer randomInteger(InvokerAgent invokerAgent, Integer max) {
		return random.nextInt(max);
	}

}
