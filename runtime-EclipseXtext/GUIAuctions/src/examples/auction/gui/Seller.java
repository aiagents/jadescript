package examples.auction.gui;

import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jadescript.java.Jadescript;
import jadescript.lang.Duration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;

import static examples.auction.gui.AuctioneerViewJava.WIN_SIZE;

public class Seller extends JFrame {

    private static final int COLUMNS = 8;
    private final int port;
    private final ContainerController sellerContainer;
    private final HashMap<String, Integer> names = new HashMap<>();
    private int x_counter = 0;

    public static void createSellerWithForm(int port, int x, int y) {
        new Seller(port, x, y);
    }

    public static void createSeller(
            int port,
            int x,
            int y,
            String name,
            String item,
            String startBid,
            String timeout,
            String increment,
            String reserve
    ) {
        try {
            spawnAuctioneer(
                    Jadescript.newContainer(port),
                    x,
                    y,
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


    public Seller(int port, int x, int y) {
        this.port = port;
        this.sellerContainer = Jadescript.newContainer(port);
        setTitle("Create Auction");
        setLayout(new BorderLayout());
        setSize(WIN_SIZE, WIN_SIZE);
        setMaximumSize(new Dimension(WIN_SIZE, WIN_SIZE));
        setMinimumSize(new Dimension(WIN_SIZE, WIN_SIZE));
        setLocation(x*WIN_SIZE, y*WIN_SIZE);


        // Create the main panel with GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Add spacing between components

        // Seller name label and field
        JLabel sellerNameLabel = new JLabel("Seller Name:");
        sellerField = new JTextField(COLUMNS);
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
        itemField = new JTextField(COLUMNS);
        gbc.gridx = 0;
        gbc.gridy = 1;
        mainPanel.add(itemLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        mainPanel.add(itemField, gbc);

        // Starting bid label and field
        JLabel startingBidLabel = new JLabel("Starting Bid:");
        startingBidField = new JTextField(COLUMNS);
        startingBidField.setText("30");
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(startingBidLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        mainPanel.add(startingBidField, gbc);

        // Minimum increment label and field
        JLabel incrementLabel = new JLabel("Increment:");
        incrementField = new JTextField(COLUMNS);
        incrementField.setText("1");
        gbc.gridx = 0;
        gbc.gridy = 3;
        mainPanel.add(incrementLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        mainPanel.add(incrementField, gbc);

        // Timeout label and field
        JLabel timeoutLabel = new JLabel("Timeout (seconds):");
        timeoutField = new JTextField(COLUMNS);
        timeoutField.setText("120");
        gbc.gridx = 0;
        gbc.gridy = 4;
        mainPanel.add(timeoutLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 4;
        mainPanel.add(timeoutField, gbc);

        // Reserve price label and field
        JLabel reservePriceLabel = new JLabel("Reserve Price:");
        reservePriceField = new JTextField(COLUMNS);
        reservePriceField.setText("50");
        gbc.gridx = 0;
        gbc.gridy = 5;
        mainPanel.add(reservePriceLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 5;
        mainPanel.add(reservePriceField, gbc);

        // Spawn button
        JButton spawnButton = new JButton("Create auction");
        spawnButton.addActionListener(__ -> {
            x_counter++;
            spawnAuctioneer(x+x_counter, y);
        });
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.CENTER;
        mainPanel.add(spawnButton, gbc);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(sellerContainer!=null && sellerContainer.isJoined()){
                    try {
                        sellerContainer.kill();
                    } catch (StaleProxyException ex) {
                        ex.printStackTrace();
                    }
                }
                Seller.this.dispose();
            }
        });

        add(mainPanel, BorderLayout.CENTER);
        setVisible(true);
        pack();
    }

    private void spawnAuctioneer(int x, int y) {
        String sellerName = sellerField.getText();
        String item = itemField.getText();
        int startingBid = Integer.parseInt(startingBidField.getText());
        int increment = Integer.parseInt(incrementField.getText());
        int timeout = Integer.parseInt(timeoutField.getText());
        int reservePrice = Integer.parseInt(reservePriceField.getText());

        int nameUsedCount = names.getOrDefault(sellerName, 0);

        if (nameUsedCount > 0) {
            nameUsedCount++;
            names.put(sellerName, nameUsedCount);
            sellerName = sellerName +"_"+ nameUsedCount;
        }

        names.put(sellerName, 1);

        try {
            spawnAuctioneer(
                    Jadescript.newContainer(port),
                    x,
                    y,
                    sellerName,
                    item,
                    startingBid,
                    increment,
                    timeout,
                    reservePrice
            );
        } catch (StaleProxyException e) {
            throw new RuntimeException(e);
        }
    }

    public static void spawnAuctioneer(
            ContainerController container,
            int x,
            int y,
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
                x,
                y,
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
