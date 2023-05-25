package examples.auction.gui;

import jade.core.AID;
import jade.util.Logger;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jadescript.java.InvokerAgent;
import jadescript.java.JadescriptAgentController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class BuyerViewJava extends BuyerView {

    private static final Logger logger = Logger.getMyLogger(BidderViewJava.class.getName());

    private static final SimpleDateFormat formatter = new SimpleDateFormat("HHmmssSSS");
    private String inputName = "";
    private ContainerController container = null;
    private int delayMin = 0;
    private int delayMax = 0;

    public static final int MAX_WIDTH = 720;
    public static final int MAX_INNER_WIDTH = MAX_WIDTH - 30;

    private JadescriptAgentController agent;
    private JFrame jframe;
    private JPanel auctionsView;

    private String agentName;

    private final Map<AID, BidderViewJava> auctions = new ConcurrentHashMap<>();

    public static void createBuyer(
            String name,
            ContainerController container,
            int delayMin,
            int delayMax
    ) throws StaleProxyException {
        new BuyerViewJava(name, container, delayMin, delayMax).start();
    }


    public BuyerViewJava() {
        //Do nothing
    }

    private BuyerViewJava(String inputName, ContainerController container, int delayMin, int delayMax) {
        this.inputName = inputName;
        this.container = container;
        this.delayMin = delayMin;
        this.delayMax = delayMax;
    }

    public void start() throws StaleProxyException {
        if (this.inputName.isBlank()) {
            this.agentName = "buyer" + formatter.format(new Date());
        } else {
            this.agentName = inputName;
        }

        agent = BuyerAgent.create(
                container,
                agentName,
                this
        );

        this.jframe = new JFrame(agentName);

        this.jframe.setMinimumSize(new Dimension(MAX_WIDTH, 300));
        this.jframe.setMaximumSize(new Dimension(MAX_WIDTH, Integer.MAX_VALUE));
        this.jframe.setLayout(new BorderLayout());
        this.jframe.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (agent != null) {
                        agent.emit(BuyerGUI.CloseCommand());
                    }
                } catch (Throwable ignored) {

                }
                if (container != null) {
                    try {
                        container.kill();
                    } catch (StaleProxyException ex) {
                        ex.printStackTrace();
                    }
                }

            }
        });

        this.auctionsView = new JPanel();
        auctionsView.setLayout(new BoxLayout(auctionsView, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(auctionsView);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);


        jframe.add(scrollPane, BorderLayout.CENTER);


        jframe.setVisible(true);
        updateAuctionsGUI();
    }


    public String getAgentName() {
        return agentName;
    }

    public void addAuction(AID auctioneer, AuctionState state) {
        logger.log(Logger.INFO, "Added auction of " + auctioneer.getLocalName() + ": " + state);


        if (!this.auctions.containsKey(auctioneer)) {

            var view = new BidderViewJava(
                    container,
                    this,
                    auctioneer,
                    state,
                    delayMin,
                    delayMax
            );
            view.start();
            this.auctions.put(auctioneer, view);
        } else {
            final BidderViewJava bidderViewJava = this.auctions.get(auctioneer);
            if (!bidderViewJava.getState().equals(state)) {
                bidderViewJava.setState(state);
            }
        }

        updateAuctionsGUI();
    }

    public void updateAuctionsGUI() {
        SwingUtilities.invokeLater(() -> {
            auctionsView.removeAll();

            if (auctions.isEmpty()) {
                final JLabel label = new JLabel("(No auctions found)");
                label.setAlignmentX(Component.CENTER_ALIGNMENT);
                auctionsView.add(label);
                logger.log(Level.INFO, "No auctions.");
            } else {
                //noinspection ComparatorCombinators
                this.auctions.entrySet().stream()
                        .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                        .forEach(entry -> {
                            final JPanel panel = entry.getValue().getPanel();
                            panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                            auctionsView.add(panel);
                        });
            }

            jframe.revalidate();
            SwingUtilities.invokeLater(jframe::repaint);
        });
    }

    public void removeAuctionView(AID auctioneer) {
        this.auctions.remove(auctioneer);
        updateAuctionsGUI();
    }


    public void revalidateAll() {
        SwingUtilities.invokeLater(jframe::revalidate);
    }


    public static class addAuctionViewJava extends addAuctionView {

        @Override
        public void addAuctionView(InvokerAgent invokerAgent,
                                   BuyerView view,
                                   AID auctioneer,
                                   AuctionState auctionState) {
            ((BuyerViewJava) view).addAuction(auctioneer, auctionState);
        }
    }


}
