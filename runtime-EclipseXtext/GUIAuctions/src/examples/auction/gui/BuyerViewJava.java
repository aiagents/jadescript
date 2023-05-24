package examples.auction.gui;

import jade.core.AID;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jadescript.java.InvokerAgent;
import jadescript.java.Jadescript;
import jadescript.java.JadescriptAgentController;
import jadescript.util.JadescriptMap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BuyerViewJava extends BuyerView {

    private static final SimpleDateFormat formatter = new SimpleDateFormat("HHmmssSSS");
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
            ContainerController container,
            int delayMin,
            int delayMax
    ) throws StaleProxyException {
        new BuyerViewJava(container, delayMin, delayMax).start();
    }


    public BuyerViewJava() {
        //Do nothing
    }

    private BuyerViewJava(ContainerController container, int delayMin, int delayMax) {
        this.container = container;
        this.delayMin = delayMin;
        this.delayMax = delayMax;
    }

    public void start() throws StaleProxyException {
        this.agentName = "buyer" + formatter.format(new Date());
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
                agent.emit(BuyerGUI.ClosingWindow());
            }
        });

        this.auctionsView = new JPanel();
        auctionsView.setLayout(new BoxLayout(auctionsView, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(auctionsView);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);


        jframe.add(scrollPane, BorderLayout.CENTER);

//        final JButton refresh = new JButton("Refresh");
//        jframe.add(refresh, BorderLayout.NORTH);
//        refresh.addActionListener((__) -> agent.emit(BuyerGUI.Refresh()));

        jframe.setVisible(true);
    }


    public String getAgentName() {
        return agentName;
    }

    public void updateAuctions(JadescriptMap<AID, AuctionState> auctions) {
        final Map<AID, BidderViewJava> removed = getRemoved(this.auctions, auctions);
        final Map<AID, AuctionState> added = getAdded(this.auctions, auctions);
        System.out.println("removed: " + removed); //TODO
        System.out.println("added: " + added); //TODO

        for (Map.Entry<AID, BidderViewJava> r : removed.entrySet()) {
            final AuctionState prevState = r.getValue().getState();
            r.getValue().setState(
                    EnglishAuction.AuctionRemoved(
                            prevState.getItem(),
                            prevState.getCurrentBid(),
                            prevState.getBidMinimumIncrement(),
                            prevState instanceof AuctionEnded ? ((AuctionEnded) prevState).getSold() : false,
                            prevState instanceof AuctionEnded ? ((AuctionEnded) prevState).getWinner() :
                                    Jadescript.asAid("ams")
                    )
            );
        }

        for (Map.Entry<AID, AuctionState> a : added.entrySet()) {
            var view = new BidderViewJava(
                    container,
                    this,
                    a.getKey(),
                    a.getValue(),
                    delayMin,
                    delayMax
            );
            view.start();
            this.auctions.put(a.getKey(), view);
        }

        for (Map.Entry<AID, AuctionState> entry : auctions.entrySet()) {
            if (this.auctions.containsKey(entry.getKey())) {
                final BidderViewJava bidderViewJava = this.auctions.get(entry.getKey());
                if (!bidderViewJava.getState().equals(entry.getValue())) {
                    bidderViewJava.setState(entry.getValue());
                }
            }
        }

        updateAuctionsGUI();
    }

    public void updateAuctionsGUI() {
        SwingUtilities.invokeLater(() -> {
            auctionsView.removeAll(); //TODO

            if(auctions.isEmpty()){
                final JLabel label = new JLabel("(No sellers found)");
                label.setAlignmentX(Component.CENTER_ALIGNMENT);
                auctionsView.add(label);
            }else {
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
        });
    }

    public void removeAuctionView(AID auctioneer) {
        this.auctions.remove(auctioneer);
        updateAuctionsGUI();
    }

    private Map<AID, BidderViewJava> getRemoved(
            Map<AID, BidderViewJava> before,
            Map<AID, AuctionState> after
    ) {
        Map<AID, BidderViewJava> result = new HashMap<>();
        for (var entry : before.entrySet()) {
            if (!after.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private Map<AID, AuctionState> getAdded(
            Map<AID, BidderViewJava> before,
            Map<AID, AuctionState> after
    ) {
        Map<AID, AuctionState> result = new HashMap<>();
        for (var entry : after.entrySet()) {
            if (!before.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public void revalidateAll() {
        SwingUtilities.invokeLater(jframe::revalidate);
    }


    public static class UpdateViewJava extends updateBuyerView {

        @Override
        public void updateBuyerView(InvokerAgent invokerAgent,
                                    BuyerView view,
                                    JadescriptMap<AID, AuctionState> auctions) {
            if (view instanceof BuyerViewJava) {
                ((BuyerViewJava) view).updateAuctions(auctions);
            }
        }
    }


}
