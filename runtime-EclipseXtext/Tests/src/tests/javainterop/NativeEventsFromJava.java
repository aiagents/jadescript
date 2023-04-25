package tests.javainterop;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JFrame;

import jadescript.java.Jadescript;
import jadescript.java.JadescriptAgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class NativeEventsFromJava {
	
	public static String nativeInvocationTest(Integer count) {
		System.out.println("Hello from Java!");
		return "The counter is "+count;
	}
	
	public static void doExit() {
		System.exit(0);
	}
	
	public static void main(String[] args) throws StaleProxyException {
		ContainerController container = Jadescript.newMainContainer();
		JadescriptAgentController agent = NativeEventsAgent.create(container, "PerceptionTestAgent");
		
		JFrame simpleFrame = new JFrame("PerceptionTestAgent");
		simpleFrame.setLayout(new BorderLayout());
		
		simpleFrame.setMaximumSize(new Dimension(100, 100));
		simpleFrame.setMinimumSize(new Dimension(100, 100));
		
		JButton button = new JButton("Notify agent!");
		simpleFrame.add(button, BorderLayout.CENTER);

		var counter = new AtomicInteger();
		
		button.addActionListener((e)->{
			int i = counter.getAndIncrement();
			agent.emit(NativeEventTest.ButtonClicked(i));
		});
		
		simpleFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		simpleFrame.setVisible(true);
	
	}
}
