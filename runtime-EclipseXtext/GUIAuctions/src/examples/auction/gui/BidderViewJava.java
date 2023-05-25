package examples.auction.gui;

import jade.core.AID;
import jade.util.Logger;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jadescript.java.InvokerAgent;
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
import java.util.logging.Level;

public class BidderViewJava extends BidderView {
    private static final Logger logger = Logger.getMyLogger(BidderViewJava.class.getName());

    private final JPanel singleAuctionViewPanel;
    private final JPanel biddingPanel;
    private ContainerController container = null;
    private BuyerViewJava buyerJava = null;
    private AID auctioneer = null;
    private AuctionState state = new AuctionState();
    private int delayMin = 0;
    private int delayMax = 0;
    private final List<Runnable> every500 = new ArrayList<>();
    private final Timer timer500 = new Timer(500, (__) -> every500.forEach(Runnable::run));
    private JadescriptAgentController bidderAgent = null;

    private final JLabel labelTop;

    private final JLabel labelStatus;
    private final JLabel labelCurrent;
    private final JLabel labelCountdown;

    private final JComboBox<String> comboStategy;
    private final JTextField fieldMoney;
    private final JButton stratButton;

    private final JLabel labelMessage;
    private final JButton leaveButton;


    public BidderViewJava() {
        this.biddingPanel = new JPanel();
        this.singleAuctionViewPanel = new JPanel();
        this.labelTop = new JLabel();
        this.labelStatus = new JLabel("(...)");
        this.labelCurrent = new JLabel(" --- ");
        this.labelCountdown = new JLabel();
        String[] options = {"Auto Bidding", "Manual Bid"};
        this.comboStategy = new JComboBox<>(options);
        this.fieldMoney = new JTextField(state.getCurrentBid() + state.getBidMinimumIncrement());
        this.stratButton = new JButton("Submit");
        this.labelMessage = new JLabel(" ");
        this.leaveButton = new JButton("Leave");
    }


    public BidderViewJava(
            ContainerController container,
            BuyerViewJava buyerJava,
            AID auctioneer,
            AuctionState auction,
            int delayMin,
            int delayMax
    ) {
        this();
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
            bidderAgent = Bidder.create(
                    container,
                    myAgentName,
                    auctioneer,
                    this,
                    delayMin,
                    delayMax
            );

            String rowText = "Auction \"" + auctioneer.getLocalName() + " selling " + state.getItem().getName() + "\"";
            labelTop.setText(rowText);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(3, 3, 3, 3);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            biddingPanel.add(labelTop, gbc);

            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = 1;
            gbc.gridy = 1;
            gbc.gridx = 0;
            Color color = Color.gray;
            labelStatus.setOpaque(true);
            labelStatus.setBackground(color);
            this.biddingPanel.setBorder(BorderFactory.createLineBorder(color, 5));
            biddingPanel.add(labelStatus, gbc);

            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.weightx = 1.0;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = 1;
            biddingPanel.add(labelCurrent, gbc);

            every500.add(() -> {
                if (state instanceof RunningAuction) {
                    final long seconds = ((RunningAuction) state).getDeadline().toZonedDateTime().toEpochSecond()
                            - ZonedDateTime.now().toEpochSecond();
                    labelCountdown.setText("T: " + Math.max(0L, seconds));
                    labelCountdown.repaint();
                }
            });
            timer500.stop();
            gbc.gridx = 2;
            gbc.gridy = 1;
            gbc.weightx = 0.2;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = 1;
            biddingPanel.add(labelCountdown, gbc);


            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = 1;
            gbc.weightx = 0.2;

            comboStategy.setMaximumSize(comboStategy.getPreferredSize());
            biddingPanel.add(comboStategy, gbc);

            gbc.gridx = 1;
            gbc.gridy = 2;
            gbc.weightx = 0.8;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = 1;
            biddingPanel.add(fieldMoney, gbc);


            ((AbstractDocument) fieldMoney.getDocument()).setDocumentFilter(new IntegerDocumentFilter());
            fieldMoney.setInputVerifier(new IntegerInputVerifier());
            fieldMoney.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    // Do nothing
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (!fieldMoney.getInputVerifier().verify(fieldMoney)) {
                        fieldMoney.setText("");
                    }
                }
            });


            stratButton.addActionListener((__) -> {
                if (!fieldMoney.getInputVerifier().verify(fieldMoney)) {
                    fieldMoney.setText("");
                } else {
                    final String selectedItem = (String) comboStategy.getSelectedItem();
                    if ("Auto Bidding".equals(selectedItem)) {
                        bidderAgent.emit(BuyerGUI.SetStrategy(
                                BuyerGUI.AutoBudget(Integer.parseInt(fieldMoney.getText()))
                        ));
                    } else {
                        bidderAgent.emit(BuyerGUI.SetStrategy(
                                BuyerGUI.ManualBid(Integer.parseInt(fieldMoney.getText()))
                        ));
                    }
                }

            });
            gbc.gridx = 3;
            gbc.gridy = 2;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = 1;
            biddingPanel.add(stratButton, gbc);


            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = 2;
            biddingPanel.add(labelMessage, gbc);


            leaveButton.addActionListener((__) -> {
                if (leaveButton.getText().equals("Remove")) {
                    buyerJava.removeAuctionView(this.auctioneer);
                } else {
                    bidderAgent.emit(BuyerGUI.CloseCommand());
                }
            });
            gbc.gridx = 2;
            gbc.gridy = 3;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            biddingPanel.add(leaveButton, gbc);


        } catch (StaleProxyException e) {
            e.printStackTrace();
            errorCreatingAgent(e);
        }


        SwingUtilities.invokeLater(this::updateBidderGUI);
    }

    public JPanel getPanel() {
        return singleAuctionViewPanel;
    }

    private void updateBidderGUI() {
        if (state instanceof AwaitingBidders) {
            updateAwaiting(((AwaitingBidders) state));
        } else if (state instanceof RunningAuction) {
            updateRunning(((RunningAuction) state));
        } else if (state instanceof AuctionRemoved) {
            updateRemoved(((AuctionRemoved) state));
        } else if (state instanceof AuctionEnded) {
            updateEnded((AuctionEnded) state);
        }
        if (buyerJava != null) {
            buyerJava.revalidateAll();
        }
    }

    private Color lighter(Color c) {
        final double augm = 10;
        return new Color(
                Math.min(255, (int) ((double) c.getRed() + augm)),
                Math.min(255, (int) ((double) c.getGreen() + augm)),
                Math.min(255, (int) ((double) c.getBlue() + augm))
        );
    }

    private void setTitleAndStatus(AuctionState state, String status, Color color) {
        labelTop.setText("Auction \"" + auctioneer.getLocalName() + " selling " + state.getItem().getName() + "\"");
        labelStatus.setText("(" + status + ")");
        labelStatus.setOpaque(true);
        Color lighter = lighter(color);
        labelStatus.setBackground(lighter);
        this.biddingPanel.setBorder(BorderFactory.createLineBorder(lighter, 5));
    }

    private void setActionRowEnabled(boolean enabled) {
        stratButton.setEnabled(enabled);
    }

    private void updateAwaiting(AwaitingBidders state) {
        timer500.stop();

        setTitleAndStatus(state, "Awaiting", Color.orange);
        labelCurrent.setText("Starting Bid: " + state.getStartingBid());

        labelCurrent.setText(" --- ");
        labelCountdown.setText("");

        setActionRowEnabled(false);

        labelMessage.setText(" ... awaiting bidders (" + state.getCurrentlyRegistered() + "/2) ...");

        leaveButton.setText("Leave");
    }

    private void updateEnded(AuctionEnded state) {
        timer500.stop();

        if (state.getSold()) {
            if (state.getWinner().getName().equals(bidderAgent.getName())) {
                setTitleAndStatus(state, "Won!", Color.GREEN);
                labelMessage.setText("Item bought.");
            } else {
                setTitleAndStatus(state, "Lost", Color.gray);
                labelMessage.setText("Item sold to '" + state.getWinner().getLocalName() + "'");
            }

            labelCurrent.setText("Winning Bid: " + state.getCurrentBid());
            labelCountdown.setText("");

            setActionRowEnabled(false);
            leaveButton.setText("Remove");
        } else {
            setTitleAndStatus(state, "Terminated", Color.gray);
            labelCurrent.setText("Last Bid: " + state.getCurrentBid());
            labelCountdown.setText("");
            setActionRowEnabled(false);
            labelMessage.setText("Item not sold.");
            leaveButton.setText("Remove");
        }
    }

    private void updateRemoved(AuctionRemoved state) {
        timer500.stop();

        setTitleAndStatus(state, "Removed", Color.darkGray);

        labelCurrent.setText("Last Bid: " + state.getCurrentBid());


        labelCountdown.setText("");
        setActionRowEnabled(false);
        labelMessage.setText("");
        leaveButton.setText("Remove");
    }


    private void updateRunning(RunningAuction state) {
        final String currentlyWinning = state.getCurrentlyWinning();
        final String me = bidderAgent.getName();
        final String status;
        final Color color;
        if (currentlyWinning.isBlank()) {
            status = "Open";
            color = Color.pink;
            labelMessage.setText("Auctioneer awaiting first bid.");
        } else if (currentlyWinning.equals(me)) {
            status = "Leading";
            color = Color.BLUE;
            labelMessage.setText("Leading: " + extractLocalName(currentlyWinning));
        } else {
            status = "Outbid";
            color = Color.RED;
            labelMessage.setText("Leading: " + extractLocalName(currentlyWinning));
        }

        setTitleAndStatus(state, status, color);

        labelCurrent.setText("Current Bid: " + state.getCurrentBid());

        timer500.start();

        setActionRowEnabled(true);


        leaveButton.setText("Leave");
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

    private void updateResponse(String msg) {
        SwingUtilities.invokeLater(() -> labelMessage.setText(msg));
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
        leaveButton.setText("Leave");
    }


    @Override
    public AuctionState getState() {
        return state;
    }

    private int getStateLevel(AuctionState s) {
        if (s instanceof AwaitingBidders) {
            return 1;
        } else if (s instanceof RunningAuction) {
            return 2;
        } else if (s instanceof AuctionRemoved) {
            return 4;//Not a bug
        } else if (s instanceof AuctionEnded) {
            return 3;//Not a bug
        } else {
            return 0;
        }
    }

    private boolean validateTransition(AuctionState before, AuctionState after) {
        return getStateLevel(before) <= getStateLevel(after);
    }

    @Override
    public void setState(AuctionState _value) {
        logger.log(Level.INFO, "Old state: " + state);
        logger.log(Level.INFO, "New state: " + _value);
        if (validateTransition(state, _value)) {
            this.state = _value;
            SwingUtilities.invokeLater(this::updateBidderGUI);
        }else{
            logger.log(Level.WARNING, "New state rejected.");
        }
    }




    private static class IntegerDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (isInteger(string)) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
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

    public static class NotifyResponseJava extends notifyResponse {
        @Override
        public void notifyResponse(InvokerAgent invokerAgent, BidderView view, String msg) {
            if (view instanceof BidderViewJava) {
                ((BidderViewJava) view).updateResponse(msg);
            }
        }

    }
}
