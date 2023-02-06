package tests.perception;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JFrame;

import jadescript.java.Jadescript;
import jadescript.java.JadescriptAgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class PerceptsFromJava {
	public static void main(String[] args) throws StaleProxyException {
		ContainerController container = Jadescript.newMainContainer();
		JadescriptAgentController agent = PerceptionTestAgent.create(container, "PerceptionTestAgent");
		
		JFrame simpleFrame = new JFrame("PerceptionTestAgent");
		simpleFrame.setLayout(new BorderLayout());
		
		simpleFrame.setMaximumSize(new Dimension(100, 100));
		simpleFrame.setMinimumSize(new Dimension(100, 100));
		
		JButton button = new JButton("Ping agent!");
		simpleFrame.add(button, BorderLayout.CENTER);

		var counter = new AtomicInteger();
		
		button.addActionListener((e)->{
			int i = counter.getAndIncrement();
			agent.perceive(
					PerceptionTestOnto.ButtonClicked(i), 
					PerceptionTestOnto.getInstance()
			);
		});
		
		simpleFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		simpleFrame.setVisible(true);
	
	}
}
