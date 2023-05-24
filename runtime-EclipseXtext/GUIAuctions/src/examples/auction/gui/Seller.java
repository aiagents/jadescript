package examples.auction.gui;

import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jadescript.lang.Duration;

import javax.swing.*;
import java.awt.*;

public class Seller extends JFrame {

    private final ContainerController container;

    public static void createSeller(ContainerController container) {
        //TODO
//        new Seller(container);
        //TODO close form after creation

        //TODO
        try {
            spawnAuctioneer(container,
                    "Hello",
                    "MonaLisa",
                    30,
                    1,
                    120,
                    50);
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
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(startingBidLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        mainPanel.add(startingBidField, gbc);

        // Minimum increment label and field
        JLabel incrementLabel = new JLabel("Minimum Increment:");
        incrementField = new JTextField(10);
        gbc.gridx = 0;
        gbc.gridy = 3;
        mainPanel.add(incrementLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        mainPanel.add(incrementField, gbc);

        // Timeout label and field
        JLabel timeoutLabel = new JLabel("Timeout (seconds):");
        timeoutField = new JTextField(10);
        gbc.gridx = 0;
        gbc.gridy = 4;
        mainPanel.add(timeoutLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 4;
        mainPanel.add(timeoutField, gbc);

        // Reserve price label and field
        JLabel reservePriceLabel = new JLabel("Reserve Price:");
        reservePriceField = new JTextField(10);
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

        try {
            spawnAuctioneer(container, sellerName, item, startingBid, increment, timeout, reservePrice);
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

        Auctioneer.create(
                container,
                name,
                EnglishAuction.Item(item),
                startingBid,
                reservePrice,
                increment,
                Duration.of(timeout, 0)
        );

    }
}
