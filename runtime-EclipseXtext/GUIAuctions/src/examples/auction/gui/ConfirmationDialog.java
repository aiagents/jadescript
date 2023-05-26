package examples.auction.gui;

import javax.swing.*;
import java.awt.*;

public class ConfirmationDialog extends JFrame {

    public static void show(String title, String message, Runnable runnable) {
        SwingUtilities.invokeLater(() -> new ConfirmationDialog(title, message, runnable));
    }

    public ConfirmationDialog(String title, String message, Runnable runnable) {
        super(title);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 150);
        setResizable(false);
        setLocationRelativeTo(null);

        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(280, 50));
        add(label, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton yesButton = new JButton("Yes");
        yesButton.addActionListener(__ -> {
            if (runnable != null) {
                runnable.run();
            }
            dispose();
        });
        buttonPanel.add(yesButton);

        JButton noButton = new JButton("No");
        noButton.addActionListener(__ -> dispose());
        buttonPanel.add(noButton);

        add(buttonPanel, BorderLayout.CENTER);

        setVisible(true);
    }

}
