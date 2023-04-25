package tests.nativeops;

import jadescript.java.InvokerAgent;

public class MyReverseImplementation extends reverse {

	@Override
	public String reverse(InvokerAgent invokerAgent, String t) {
		StringBuilder sb = new StringBuilder();
		for (int i = t.length()-1; i >= 0; i--) {
			sb.append(t.charAt(i));
		}
		return sb.toString();
	}
	
}