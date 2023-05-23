package examples.auction.gui;

import jade.core.AID;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jadescript.java.InvokerAgent;
import jadescript.java.Jadescript;
import jadescript.java.JadescriptAgentController;
import jadescript.util.JadescriptMap;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Buyer {


    private static final SimpleDateFormat formatter = new SimpleDateFormat("HHmmssSSS");
    private final ContainerController container;

    private JadescriptAgentController jadescriptAgentController;
    private BuyerViewJava view;
    private JFrame jframe;

    public static void buyer(ContainerController container) throws StaleProxyException {
        Jadescript.bindNative(BuyerView.class, BuyerViewJava.class);
        new Buyer(container).start();
    }



    private Buyer(ContainerController container) {
        this.container = container;
    }

    public void start() throws StaleProxyException {
        this.view = new BuyerViewJava(this);

        final String agentName = "buyer" + formatter.format(new Date());
        jadescriptAgentController = BuyerAgent.create(
                container,
                agentName,
                view
        );

        this.jframe = new JFrame(agentName);
        this.jframe.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                jadescriptAgentController.emit(BuyerGUI.ClosingWindow());
            }
        });
    }

    public void updateAuctions(JadescriptMap<AID, AuctionState> auctions){
        SwingUtilities.invokeLater(() -> {

        });
    }

    public static class UpdateViewJava extends updateView {

        @Override
        public void updateView(InvokerAgent invokerAgent,
                               BuyerView view,
                               JadescriptMap<AID, AuctionState> auctions) {
            if(view instanceof BuyerViewJava){
                ((BuyerViewJava) view).getBuyerJava().updateAuctions(auctions);
            }
        }
    }


    public static class BuyerViewJava extends BuyerView {
        @SuppressWarnings("UnusedAssignment")
        private Buyer buyerJava = null;

        public BuyerViewJava(Buyer buyerJava) {
            this.buyerJava = buyerJava;
        }

        public Buyer getBuyerJava() {
            return buyerJava;
        }
    }

}
