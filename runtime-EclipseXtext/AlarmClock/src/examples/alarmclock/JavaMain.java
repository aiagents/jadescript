package examples.alarmclock;

import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class JavaMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Runtime rt = Runtime.instance();
		rt.setCloseVM(true);

		Profile pMain = new ProfileImpl(null, 1099, null);

		AgentContainer mc = rt.createMainContainer(pMain);
 
		try {

			AgentController alarmClock = mc.createNewAgent("AlarmClock", "examples.alarmclock.AlarmClock",
					new Object[] {});
			alarmClock.start();
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
		
		
		
		SwingUtilities.invokeLater(()->{
			JPanel gui = new JPanel(new java.awt.GridLayout());
            JTextField textField = new JTextField();
            
            gui.add(textField);
            
            
            JButton button = new JButton("Start AlarmClockUser agent");
            AtomicInteger counter = new AtomicInteger(0);
            JFrame f = new JFrame("Insert date");
            button.addActionListener((event) -> {
            	
            	System.out.println("Starting agent...");
            	AgentController alarmClockUser;
				try {
					alarmClockUser = mc.createNewAgent("AlarmClockUser"+counter.incrementAndGet(), "examples.alarmclock.AlarmClockUser",
							new Object[] {textField.getText()});
					alarmClockUser.start();
					f.dispatchEvent(new java.awt.event.WindowEvent(f, java.awt.event.WindowEvent.WINDOW_CLOSING));
				} catch (StaleProxyException e) {
					e.printStackTrace();
				}
            });

            gui.add(button);
            
            f.add(gui);
           
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
           

            f.pack();

            f.setVisible(true);
		});

		
	}

}
