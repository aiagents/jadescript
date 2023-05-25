package examples.auction.gui;

import jade.core.AID;
import jade.util.Logger;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jadescript.java.InvokerAgent;
import jadescript.java.JadescriptAgentController;
import jadescript.lang.Duration;
import jadescript.util.JadescriptSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class AuctioneerViewJava extends AuctioneerView {
    private static final Logger logger = Logger.getMyLogger(AuctioneerViewJava.class.getName());
    private JadescriptAgentController auctioneerAgent = null;
    private final Map<AID, ParticipantPanel> participants = new HashMap<>();


    private final JFrame auctioneerFrame;
    private final JPanel mainPanel;

    private final JLabel stateLabel;
    private final JLabel countdownLabel;
    private final Timer countdownTimer;

    private final JLabel messageLabel;
    private final JButton stopButton;

    private static final Color awaiting = Color.orange;
    private static final Color running = new Color(0, 0, 0, 0);
    private static final Color open = Color.pink;
    private static final Color done = Color.gray;
    private static final Color removed = Color.darkGray;


    private static final Color idle = Color.pink;
    private static final Color leading = Color.BLUE;
    private static final Color outbid = Color.RED;
    private static final Color winner = Color.GREEN;
    private static final Color left = Color.gray;
    private AuctionState state = null;

    private ContainerController container = null;
    private final Runnable closeAuctioneer = () -> {
        try {
            if (auctioneerAgent != null) {
                try {
                    auctioneerAgent.emit(AuctioneerGUI.CloseAuctioneer());
                } catch (Throwable ignored) {

                }
                if (container != null) {
                    container.kill();
                }
            }
        } catch (StaleProxyException e) {
            throw new RuntimeException(e);
        }
    };

    public AuctioneerViewJava() {
        auctioneerFrame = new JFrame();
        mainPanel = new JPanel();
        stateLabel = new JLabel("---");
        messageLabel = new JLabel("---");
        countdownLabel = new JLabel("...");
        stopButton = new JButton("Stop");
        countdownTimer = new Timer(500, (__) -> {
            final AuctionState s = getState();
            if (s instanceof RunningAuction) {
                final long seconds = ((RunningAuction) state).getDeadline().toZonedDateTime().toEpochSecond()
                        - ZonedDateTime.now().toEpochSecond();
                countdownLabel.setText("T: " + Math.max(0L, seconds));
                countdownLabel.repaint();
            }
        });
    }

    public void start(
            ContainerController container,
            String name,
            Item item,
            int startingBid,
            int reservePrice,
            int increment,
            Duration timeout
    ) {
        this.container = container;
        this.auctioneerFrame.setTitle(name + " (Auctioneer)");
        this.auctioneerFrame.setMinimumSize(new Dimension(500, 400));
        this.auctioneerFrame.setLayout(new BorderLayout());

        JPanel northPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0.8;
        northPanel.add(stateLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0.2;
        northPanel.add(countdownLabel, gbc);

        JPanel southPanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0.8;
        southPanel.add(messageLabel, gbc);


        stopButton.addActionListener((__) -> closeAuctioneer.run());
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0.2;
        southPanel.add(stopButton, gbc);

        this.auctioneerFrame.getContentPane().add(northPanel, BorderLayout.NORTH);
        this.auctioneerFrame.getContentPane().add(southPanel, BorderLayout.SOUTH);

        this.auctioneerFrame.getContentPane().add(mainPanel, BorderLayout.CENTER);
        this.mainPanel.setLayout(new BoxLayout(this.mainPanel, BoxLayout.X_AXIS));

        try {
            this.auctioneerAgent = Auctioneer.create(
                    container,
                    name,
                    this,
                    item,
                    startingBid,
                    reservePrice,
                    increment,
                    timeout
            );
        } catch (StaleProxyException e) {
            throw new RuntimeException(e);
        }

        this.auctioneerFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeAuctioneer.run();
            }
        });

        this.auctioneerFrame.pack();
        this.auctioneerFrame.setVisible(true);

    }

    private synchronized void setState(AuctionState state) {
        this.state = state;
    }

    private synchronized AuctionState getState() {
        return this.state;
    }

    public void updateParticipantMap(AuctionState state, JadescriptSet<AID> participants) {

        this.setState(state);

        final Set<AID> removed = getRemoved(this.participants, participants);
        final Set<AID> added = getAdded(this.participants, participants);
        logger.log(Logger.INFO, "removed: " + removed);
        logger.log(Logger.INFO, "added: " + added);

        // "Removing"
        for (AID r : removed) {
            final ParticipantPanel panel = this.participants.get(r);
            if (panel != null) {
                panel.setLeft();
            }
        }

        // Adding
        for (AID a : added) {
            this.participants.put(a, new ParticipantPanel(a));
        }

        // Updating
        for (Map.Entry<AID, ParticipantPanel> entry : this.participants.entrySet()) {
            if (this.participants.containsKey(entry.getKey())) {
                final ParticipantPanel panel = this.participants.get(entry.getKey());
                if (!added.contains(entry.getKey())
                        && !removed.contains(entry.getKey())) {
                    panel.setState(state);
                }
            }
        }

        updateMainPanel();
    }

    private void updateMainPanel() {
        SwingUtilities.invokeLater(() -> {
            this.mainPanel.removeAll();

            if (participants.isEmpty()) {
                final JLabel label = new JLabel("(No participants)");
                label.setAlignmentX(Component.CENTER_ALIGNMENT);
                mainPanel.add(label);
                logger.log(Level.INFO, "No auctions.");
            } else {
                //noinspection ComparatorCombinators
                participants.entrySet().stream()
                        .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                        .forEach(e -> {
                            mainPanel.add(e.getValue());
                        });

            }
            auctioneerFrame.revalidate();
            SwingUtilities.invokeLater(auctioneerFrame::repaint);
        });
    }

    private Set<AID> getRemoved(
            Map<AID, ParticipantPanel> before,
            JadescriptSet<AID> after
    ) {
        Set<AID> result = new HashSet<>();
        for (var entry : before.entrySet()) {
            if (!after.contains(entry.getKey())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private Set<AID> getAdded(
            Map<AID, ParticipantPanel> before,
            JadescriptSet<AID> after
    ) {
        Set<AID> result = new HashSet<>();
        for (var aid : after) {
            if (!before.containsKey(aid)) {
                result.add(aid);
            }
        }
        return result;
    }


    private void update(AuctionState state, JadescriptSet<AID> participants) {
        this.state = state;
        SwingUtilities.invokeLater(() -> {
            if (state instanceof AwaitingBidders) {
                updateAwaiting(((AwaitingBidders) state), participants);
            } else if (state instanceof RunningAuction) {
                updateRunning(((RunningAuction) state), participants);
            } else if (state instanceof AuctionRemoved) {
                updateRemoved(((AuctionRemoved) state), participants);
            } else if (state instanceof AuctionEnded) {
                updateEnded((AuctionEnded) state, participants);
            }
        });
    }

    private void setTitleAndColor(String title, Color color) {
        stateLabel.setText(title);
        stateLabel.setOpaque(true);
        stateLabel.setBackground(color);
        mainPanel.setBorder(BorderFactory.createLineBorder(color, 5));
    }

    private void updateAwaiting(AwaitingBidders state, JadescriptSet<AID> participants) {
        setTitleAndColor("(Awaiting for participants)", awaiting);
        updateParticipantMap(state, participants);
        countdownTimer.stop();
        countdownLabel.setText("");
        messageLabel.setText("---");
    }

    private void updateRunning(RunningAuction state, JadescriptSet<AID> participants) {
        if (state.getCurrentlyWinning().isBlank()) {
            setTitleAndColor("(Awaiting bids)", open);
        } else {
            setTitleAndColor("(Running Auction)", running);
        }
        updateParticipantMap(state, participants);
        countdownTimer.start();
        messageLabel.setText("Price: " + state.getCurrentBid());
    }

    private void updateEnded(AuctionEnded state, JadescriptSet<AID> participants) {
        setTitleAndColor("(Auction Ended)", done);
        updateParticipantMap(state, participants);
        countdownTimer.stop();
        countdownLabel.setText("");
        messageLabel.setText("Price: " + state.getCurrentBid());
    }

    private void updateRemoved(AuctionRemoved state, JadescriptSet<AID> participants) {
        setTitleAndColor("(Auctioneer Offline)", removed);
        updateParticipantMap(state, participants);
        countdownTimer.stop();
        countdownLabel.setText("");
        messageLabel.setText("---");
    }


    private static class ParticipantPanel extends JPanel {
        private final AID participant;
        private final JLabel statusLabel;
        private final JLabel lastBidLabel;

        public ParticipantPanel(AID participant) {
            this.participant = participant;
            this.setLayout(new GridBagLayout());
            this.setMinimumSize(new Dimension(240, 120));
//            this.setMaximumSize(new Dimension(240, 120));

            this.setBorder(BorderFactory.createLineBorder(idle, 3));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(3, 3, 3, 3);

            gbc.gridx = 0;
            gbc.gridy = 0;
            JLabel nameLabel = new JLabel(participant.getLocalName());
            this.add(nameLabel, gbc);
            gbc.gridy = 1;

            this.statusLabel = new JLabel("(Idle)");
            this.statusLabel.setOpaque(true);
            this.statusLabel.setBackground(idle);
            this.add(statusLabel, gbc);

            gbc.gridy = 2;
            this.lastBidLabel = new JLabel("");
            this.add(lastBidLabel, gbc);
        }


        public void setLeft() {
            setStatus("(Left Auction)", left);
        }

        private void setStatus(String s, Color left) {
            this.setBorder(BorderFactory.createLineBorder(left, 3));
            this.statusLabel.setOpaque(true);
            this.statusLabel.setBackground(left);
            this.statusLabel.setText(s);
        }

        public void setState(AuctionState state) {
            if (state instanceof AwaitingBidders) {
                setStatus("(Idle)", idle);
                this.lastBidLabel.setText("");
            } else if (state instanceof RunningAuction) {
                if ("".equals(((RunningAuction) state).getCurrentlyWinning())) {
                    setStatus("(Idle)", idle);
                    this.lastBidLabel.setText("");

                } else if (participant.getName().equals(((RunningAuction) state).getCurrentlyWinning())) {
                    setStatus("(Leading)", leading);
                    this.lastBidLabel.setText("Last bid: " + state.getCurrentBid());
                } else {
                    setStatus("(Outbid)", outbid);
                }
            } else if (state instanceof AuctionEnded) {
                if (((AuctionEnded) state).getSold()) {
                    if (participant.equals(((AuctionEnded) state).getWinner())) {
                        setStatus("(Won)", winner);
                        this.lastBidLabel.setText("Winning bid:" + state.getCurrentBid());
                    } else {
                        setStatus("(Lost)", left);
                    }
                } else {
                    setStatus("(Lost)", left);
                }
            }
        }
    }

    public static class UpdateAuctioneerGUIJava extends updateAuctioneerGUI {

        @Override
        public void updateAuctioneerGUI(
                InvokerAgent invokerAgent,
                AuctioneerView view,
                AuctionState state,
                JadescriptSet<AID> participants
        ) {
            if (view instanceof AuctioneerViewJava) {
                ((AuctioneerViewJava) view).update(state, participants);
            }
        }

    }
}
