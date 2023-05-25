package examples.auction.gui;


import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import jadescript.java.Jadescript;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Start {


    public static void main(String[] args) throws StaleProxyException {

        NativesBinder.bindAllNativeTypes();

        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);

        final Font defaultFont = UIManager.getFont("Label.font");
        final Font newFont = defaultFont.deriveFont((float) defaultFont.getSize() + (float) 4.0);
        UIManager.put("Label.font", newFont);
        UIManager.put("Button.font", newFont);
        UIManager.put("TextField.font", newFont);

        List<String> l = new ArrayList<>(Arrays.asList(args));

        final String first = safeGet(l, 0, null);

        int port = 1099;
        if (first.startsWith("-p")) {
            port = Integer.parseInt(first.substring(2));
            l.remove(0);
        }

        if (safeGet(l, 0, null).equals("-main")) {
            var container = Jadescript.newMainContainer(port);

            try {
                if(safeGet(l, 1, "-norma").equals("-rma")) {
                    AgentController rma = container.createNewAgent("rma", "jade.tools.rma.rma", null);
                    rma.start();
                }
                Facilitator.create(container, "Facilitator");
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
            return;
        }

        if (safeGet(l, 0, null).equals("-seller")) {
            final String name = safeGet(l, 1, null);
            if(name.equals("-form")) {
                var container = Jadescript.newContainer(port);
                Seller.createSellerWithForm(container);
            }else{
                final String item = safeGet(l, 2, null);
                final String startBid = safeGet(l, 3, "30");
                final String timeout = safeGet(l, 4, "120");
                final String increment = safeGet(l, 5, "1");
                final String reserve = safeGet(l, 6, "40");

                var container = Jadescript.newContainer(port);
                Seller.createSeller(container, name, item, startBid, timeout, increment, reserve);
            }
            return;
        }

        if (safeGet(l, 0, null).equals("-buyer")) {
            var container = Jadescript.newContainer(port);

            var name = safeGet(l, 1, null);
            var delayMin = Integer.parseInt(safeGet(l, 2, "1"));
            var delayMax = Integer.parseInt(safeGet(l, 3, "3"));
            BuyerViewJava.createBuyer(name, container, delayMin, delayMax);
        }

    }

    private static String safeGet(List<String> l, int i, String defaultValue) {
        if (i < 0 || i >= l.size()) {
            if(defaultValue == null) {
                usage();
                return "";
            }else{
                return defaultValue;
            }
        }
        return l.get(i);
    }

    private static void usage() {
        System.err.println("Usage: ([-pNNNN] is the optional port, defaults to 1099) \n" +
                "java -jar <jarfile> [-pNNNN] -main [-rma] \n" +
                "   => Starts the facilitator and the main container. (-rma) starts also the JADE GUI.\n" +
                "java -jar <jarfile> [-pNNNN] -seller -form \n" +
                "   => Shows a form to create a new auction.\n" +
                "java -jar <jarfile> [-pNNNN] -seller <name> <item> [<startBid>] [<timeout>] [<increment>] [<reserve>] \n" +
                "   => Starts the auctioneer with specified name, item, starting bid, timeout, bid minimum " +
                "increment, reserve price. All prices numbers are integers, the timeout is in seconds.\n" +
                "      Defaults: \n" +
                "           startBid = 30\n" +
                "           timeout = 120\n" +
                "           increment = 1\n" +
                "           reserve = 40\n" +
                "java -jar <jarfile> [-pNNNN] -buyer <name> [<delayMin> <delayMax>]\n" +
                "   => Starts a buyer. With specified name and range of artificial random delays when submitting bids " +
                "(defaults to min=1 max=3).\n"
        );
        System.exit(1);
    }

}
