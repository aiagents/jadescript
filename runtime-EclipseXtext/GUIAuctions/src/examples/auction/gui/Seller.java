package examples.auction.gui;

import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jadescript.lang.Duration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class Seller extends JFrame {

    private final ContainerController container;

    public static void createSellerWithForm(ContainerController container) {
        new Seller(container);
    }

    public static void createSeller(
            ContainerController container,
            String name,
            String item,
            String startBid,
            String timeout,
            String increment,
            String reserve
    ) {
        try {
            spawnAuctioneer(
                    container,
                    name,
                    item,
                    Integer.parseInt(startBid),
                    Integer.parseInt(increment),
                    Integer.parseInt(timeout),
                    Integer.parseInt(reserve)
            );
        } catch (StaleProxyException e) {
            throw new RuntimeException(e);
        }
    }


    private final JTextField sellerField;
    private final JTextField itemField;
    private final JTextField startingBidField;
    private final JTextField incrementField;
    private final JTextField timeoutField;
    private final JTextField reservePriceField;
    private final WindowListener killOnClose = new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
            try {
                container.kill();
            } catch (StaleProxyException ex) {
                throw new RuntimeException(ex);
            }
        }
    };

    public Seller(ContainerController container) {
        this.container = container;
        setTitle("Create Auction");
        setLayout(new BorderLayout());
        setSize(500, 400);


        // Create the main panel with GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Add spacing between components

        // Seller name label and field
        JLabel sellerNameLabel = new JLabel("Seller Name:");
        sellerField = new JTextField(20);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(sellerNameLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(sellerField, gbc);

        // Item label and field
        JLabel itemLabel = new JLabel("Item:");
        itemField = new JTextField(20);
        gbc.gridx = 0;
        gbc.gridy = 1;
        mainPanel.add(itemLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        mainPanel.add(itemField, gbc);

        // Starting bid label and field
        JLabel startingBidLabel = new JLabel("Starting Bid:");
        startingBidField = new JTextField(10);
        startingBidField.setText("30");
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(startingBidLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        mainPanel.add(startingBidField, gbc);

        // Minimum increment label and field
        JLabel incrementLabel = new JLabel("Minimum Increment:");
        incrementField = new JTextField(10);
        incrementField.setText("1");
        gbc.gridx = 0;
        gbc.gridy = 3;
        mainPanel.add(incrementLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        mainPanel.add(incrementField, gbc);

        // Timeout label and field
        JLabel timeoutLabel = new JLabel("Timeout (seconds):");
        timeoutField = new JTextField(10);
        timeoutField.setText("120");
        gbc.gridx = 0;
        gbc.gridy = 4;
        mainPanel.add(timeoutLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 4;
        mainPanel.add(timeoutField, gbc);

        // Reserve price label and field
        JLabel reservePriceLabel = new JLabel("Reserve Price:");
        reservePriceField = new JTextField(10);
        reservePriceField.setText("50");
        gbc.gridx = 0;
        gbc.gridy = 5;
        mainPanel.add(reservePriceLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 5;
        mainPanel.add(reservePriceField, gbc);

        // Spawn button
        JButton spawnButton = new JButton("Create auction");
        spawnButton.addActionListener(__ -> spawnAuctioneer());
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.CENTER;
        mainPanel.add(spawnButton, gbc);

        addWindowListener(killOnClose);

        add(mainPanel, BorderLayout.CENTER);
        setVisible(true);
        pack();
    }

    private void spawnAuctioneer() {
        String sellerName = sellerField.getText();
        String item = itemField.getText();
        int startingBid = Integer.parseInt(startingBidField.getText());
        int increment = Integer.parseInt(incrementField.getText());
        int timeout = Integer.parseInt(timeoutField.getText());
        int reservePrice = Integer.parseInt(reservePriceField.getText());

        removeWindowListener(killOnClose);
        try {
            spawnAuctioneer(container, sellerName, item, startingBid, increment, timeout, reservePrice);
            this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } catch (StaleProxyException e) {
            throw new RuntimeException(e);
        }
    }

    public static void spawnAuctioneer(
            ContainerController container,
            String name,
            String item,
            int startingBid,
            int increment,
            int timeout,
            int reservePrice
    ) throws StaleProxyException {
        System.out.println("Spawning Auctioneer '" + name + "'...");
        System.out.println("Item: " + item);
        System.out.println("Starting Bid: " + startingBid);
        System.out.println("Increment: " + increment);
        System.out.println("Timeout: " + timeout);
        System.out.println("Reserve Price: " + reservePrice);

        new AuctioneerViewJava().start(
                container,
                name,
                EnglishAuction.Item(item),
                startingBid,
                reservePrice,
                increment,
                Duration.of(timeout, 0)
        );


//        Auctioneer.create(
//                container,
//                name,
//                EnglishAuction.Item(item),
//                startingBid,
//                reservePrice,
//                increment,
//                Duration.of(timeout, 0)
//        );


    }
}
