package examples.auction.gui;

import jade.core.AID;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jadescript.java.JadescriptAgentController;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidderViewJava extends BidderView {
    private boolean started = false;
    private final JPanel singleAuctionViewPanel;
    private JPanel biddingPanel;
    private ContainerController container = null;
    private BuyerViewJava buyerJava = null;
    private AID auctioneer = null;
    private AuctionState state = new AuctionState();
    private int delayMin = 0;
    private int delayMax = 0;
    private final List<Runnable> every500 = new ArrayList<>();
    private final Timer timer500 = new Timer(500, (__) -> {
        every500.forEach(Runnable::run);
    });
    private JadescriptAgentController agent = null;


    public BidderViewJava() {
        this.biddingPanel = new JPanel();
        this.singleAuctionViewPanel = new JPanel();
    }


    public BidderViewJava(
            ContainerController container,
            BuyerViewJava buyerJava,
            AID auctioneer,
            AuctionState auction,
            int delayMin,
            int delayMax
    ) {
        this.singleAuctionViewPanel = new JPanel();
        this.biddingPanel = new JPanel();
        this.container = container;
        this.buyerJava = buyerJava;
        this.auctioneer = auctioneer;
        this.state = auction;
        this.delayMin = delayMin;
        this.delayMax = delayMax;
    }

    private static int bidderCounter = 0;

    private synchronized static int nextID() {
        return bidderCounter++;
    }

    public void start() {
        String myAgentName = buyerJava.getAgentName() + "_bidder_" + nextID();

        this.singleAuctionViewPanel.setLayout(new BorderLayout());
        this.singleAuctionViewPanel.setMaximumSize(new Dimension(
                BuyerViewJava.MAX_INNER_WIDTH,
                140
        ));


        this.biddingPanel.setLayout(new GridBagLayout());
        this.biddingPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 5));
        this.singleAuctionViewPanel.add(biddingPanel, BorderLayout.CENTER);


        try {
            agent = Bidder.create(
                    container,
                    myAgentName,
                    auctioneer,
                    this,
                    delayMin,
                    delayMax
            );
        } catch (StaleProxyException e) {
            e.printStackTrace();
            errorCreatingAgent(e);
        }

        //TODO if closed, exit from auction

        SwingUtilities.invokeLater(this::updateBidderGUI);
        started = true;
    }

    public JPanel getPanel() {
        return singleAuctionViewPanel;
    }

    private void updateBidderGUI() {
        if (!started) {
            return;
        }
        if (state instanceof AwaitingBidders) {
            updateAwaiting(((AwaitingBidders) state));
        } else if (state instanceof RunningAuction) {
            updateRunning(((RunningAuction) state));
        } else if (state instanceof AuctionRemoved) {
            updateRemoved(((AuctionRemoved) state));
        } else if (state instanceof AuctionEnded) {
            updateEnded((AuctionEnded) state);
        }
        buyerJava.revalidateAll();
    }

    private Color lighter(Color c) {
        final double augm = 50;
        return new Color(
                Math.min(255, (int) ((double) c.getRed() + augm)),
                Math.min(255, (int) ((double) c.getGreen() + augm)),
                Math.min(255, (int) ((double) c.getBlue() + augm))
        );
    }

    private GridBagConstraints addTitleAndStatusLabel(AuctionState state, String status, Color color) {
        String rowText = "Auction \"" + auctioneer.getLocalName() + " selling " + state.getItem().getName() + "\"";
        JLabel label = new JLabel();
        label.setText(rowText);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        biddingPanel.add(label, gbc);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        final JLabel statusLabel = new JLabel("(" + status + ")");
        statusLabel.setOpaque(true);
        statusLabel.setBackground(color);
        this.biddingPanel.setBorder(BorderFactory.createLineBorder(color, 5));
        biddingPanel.add(statusLabel, gbc);

        return gbc;
    }

    private void updateAwaiting(AwaitingBidders state) {
        biddingPanel.removeAll();
        timer500.stop();

        GridBagConstraints gbc = addTitleAndStatusLabel(state, "Awaiting", lighter(Color.orange));

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        biddingPanel.add(new JLabel("Starting Bid: " + state.getStartingBid()), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        biddingPanel.add(new JLabel(" ... awaiting bidders (" + state.getCurrentlyRegistered() + "/2) ..."), gbc);

        addStatusLabel(gbc, " ");
    }

    private void updateEnded(AuctionEnded state) {
        biddingPanel.removeAll();
        timer500.stop();


        GridBagConstraints gbc;
        if (state.getSold()) {
            if (state.getWinner().getName().equals(agent.getName())) {
                gbc = addTitleAndStatusLabel(state, "Won!", lighter(Color.GREEN));

            } else {
                gbc = addTitleAndStatusLabel(state, "Lost", lighter(Color.gray));

            }
            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = 1;
            gbc.weightx = 1.0;
            biddingPanel.add(new JLabel("Winning Bid: " + state.getCurrentBid()), gbc);
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            biddingPanel.add(new JLabel("Item sold to '" + state.getWinner().getLocalName() + "'"), gbc);
        } else {
            gbc = addTitleAndStatusLabel(state, "Terminated", lighter(Color.gray));
            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = 1;
            gbc.weightx = 1.0;
            biddingPanel.add(new JLabel("Last Bid: " + state.getCurrentBid()), gbc);
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            biddingPanel.add(new JLabel("Item not sold."), gbc);
        }

        addStatusLabel(gbc, " ");
    }

    private void updateRemoved(AuctionRemoved state) {
        biddingPanel.removeAll();
        timer500.stop();

        GridBagConstraints gbc = addTitleAndStatusLabel(state, "Removed", lighter(Color.darkGray));

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.weightx = 0.7;
        biddingPanel.add(new JLabel("Last Bid: " + state.getCurrentBid()), gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.weightx = 0.3;
        JButton remove = new JButton("Remove");
        remove.addActionListener((__) -> {
            if (agent != null && !(state instanceof AuctionRemoved)) {
                agent.emit(BuyerGUI.ClosingWindow());
            }
            buyerJava.removeAuctionView(this.auctioneer);
        });
        this.biddingPanel.add(remove, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        biddingPanel.add(new JLabel("Last Bid: " + state.getCurrentBid()), gbc);

        addStatusLabel(gbc, " ");

    }



    private void updateRunning(RunningAuction state) {
        biddingPanel.removeAll();


        final String currentlyWinning = state.getCurrentlyWinning();
        final String me = agent.getName();
        final String status;
        final Color color;
        if (currentlyWinning.isBlank()) {
            status = "Open";
            color = Color.pink;
        } else if (currentlyWinning.equals(me)) {
            status = "Leading";
            color = Color.BLUE;
        } else {
            status = "Outbid";
            color = Color.RED;
        }

        GridBagConstraints gbc = addTitleAndStatusLabel(state, status, lighter(color));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.weightx = 0.8;
        biddingPanel.add(new JLabel("Current Bid: " + state.getCurrentBid()), gbc);
        //TODO successful bid
        //TODO proposal rejected

        final JLabel timeoutLabel = new JLabel();


        every500.add(() -> {
            final long seconds = state.getDeadline().toZonedDateTime().toEpochSecond()
                    - ZonedDateTime.now().toEpochSecond();
            timeoutLabel.setText("T: " + Math.max(0L, seconds));
            timeoutLabel.repaint();
        });
        timer500.start();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0.2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        biddingPanel.add(timeoutLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.weightx = 0.2;


        String[] options = {"Auto Bidding", "Manual Bid"};

        JComboBox<String> combo = new JComboBox<>(options);
        combo.setMaximumSize(combo.getPreferredSize());

        biddingPanel.add(combo, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0.8;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;
        final JTextField bidField = new JTextField(state.getCurrentBid() + state.getBidMinimumIncrement());
        biddingPanel.add(bidField, gbc);


        ((AbstractDocument) bidField.getDocument()).setDocumentFilter(new IntegerDocumentFilter());
        bidField.setInputVerifier(new IntegerInputVerifier());
        bidField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // Do nothing
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (!bidField.getInputVerifier().verify(bidField)) {
                    bidField.setText("");
                }
            }
        });


        final JButton submit = new JButton("Submit");
        submit.addActionListener((__) -> {
            if (!bidField.getInputVerifier().verify(bidField)) {
                bidField.setText("");
            } else {
                final String selectedItem = (String) combo.getSelectedItem();
                if("Auto Bidding".equals(selectedItem)){
                    agent.emit(BuyerGUI.SetStrategy(BuyerGUI.AutoBudget(Integer.parseInt(bidField.getText()))));
                }else {
                    agent.emit(BuyerGUI.SetStrategy(BuyerGUI.ManualBid(Integer.parseInt(bidField.getText()))));
                }
            }

        });
        gbc.gridx = 3;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        biddingPanel.add(submit, gbc);


        if (!currentlyWinning.isBlank()) {
            addStatusLabel(gbc, "Leading: " + extractLocalName(currentlyWinning));
        } else {
            addStatusLabel(gbc, " ");
        }
    }

    public void addStatusLabel(GridBagConstraints gbc, String string) {
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        biddingPanel.add(new JLabel(string), gbc);
    }

    public static String extractLocalName(String s) {
        int atIndex = s.indexOf("@");
        if (atIndex != -1) {
            return s.substring(0, atIndex);
        } else {
            return s;
        }
    }

    public void errorCreatingAgent(Throwable throwable) {
        showError(throwable.getMessage());
    }

    public void showError(String str) {
        //TODO
//        if (errorLabel == null) {
//            errorLabel = new JLabel();
//        }
//
//        errorLabel.setText(str);
//        errorLabel.setVisible(true);
        biddingPanel.setVisible(false);
        timer500.stop();
    }


    @Override
    public AuctionState getState() {
        return state;
    }

    @Override
    public void setState(AuctionState _value) {
        this.state = _value;
        SwingUtilities.invokeLater(this::updateBidderGUI);
    }


    private static class IntegerDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (isInteger(string)) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (isInteger(text)) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        private boolean isInteger(String text) {
            return text.matches("\\d*");
        }
    }

    private static class IntegerInputVerifier extends InputVerifier {
        @Override
        public boolean verify(JComponent input) {
            JTextField textField = (JTextField) input;
            String text = textField.getText();
            return text.matches("\\d*");
        }
    }
}
