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
        final Font newFont = defaultFont.deriveFont((float) defaultFont.getSize() + (float) 2.0);
        UIManager.put("Label.font", newFont);
        UIManager.put("Button.font", newFont);
        UIManager.put("TextField.font", newFont);

        List<String> l = new ArrayList<>(Arrays.asList(args));

        final Additionals additionals = parseAdditionalOptions(l);

        int port = additionals.p;
        int x = additionals.x;
        int y = additionals.y;


        if (safeGet(l, 0, null).equals("-main")) {
            var container = Jadescript.newMainContainer(port);

            try {
                if (safeGet(l, 1, "-norma").equals("-rma")) {
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
            if (name.equals("-form")) {

                Seller.createSellerWithForm(port, x, y);
            } else {
                final String item = safeGet(l, 2, null);
                final String startBid = safeGet(l, 3, "30");
                final String timeout = safeGet(l, 4, "120");
                final String increment = safeGet(l, 5, "1");
                final String reserve = safeGet(l, 6, "40");

                Seller.createSeller(port, x, y, name, item, startBid, timeout, increment, reserve);
            }
            return;
        }

        if (safeGet(l, 0, null).equals("-buyer")) {
            var container = Jadescript.newContainer(port);

            var name = safeGet(l, 1, null);
            var delayMin = Integer.parseInt(safeGet(l, 2, "1"));
            var delayMax = Integer.parseInt(safeGet(l, 3, "3"));
            BuyerViewJava.createBuyer(name, x, y, container, delayMin, delayMax);
        }

    }

    private static String safeGet(List<String> l, int i, String defaultValue) {
        if (i < 0 || i >= l.size()) {
            if (defaultValue == null) {
                usage();
                return "";
            } else {
                return defaultValue;
            }
        }
        return l.get(i);
    }

    private static class Additionals {
        int x = 0;
        int y = 0;
        int p = 1099;
    }

    public static Additionals parseAdditionalOptions(List<String> options) {
        Additionals result = new Additionals();
        String first = safeGet(options, 0, null);
        while (true) {
            if (first.startsWith("-p")) {
                result.p = Integer.parseInt(first.substring(2));
                options.remove(0);
                first = safeGet(options, 0, null);
            } else if (first.startsWith("-x")) {
                result.x = Integer.parseInt(first.substring(2));
                options.remove(0);
                first = safeGet(options, 0, null);
            } else if (first.startsWith("-y")) {
                result.y = Integer.parseInt(first.substring(2));
                options.remove(0);
                first = safeGet(options, 0, null);
            } else {
                break;
            }
        }
        return result;
    }

    private static void usage() {
        System.err.println("Usage: ([-pNNNN] is the optional port, defaults to 1099) \n" +
                "java -jar <jarfile> [-pNNNN] [-xX] [-yY] -main [-rma] \n" +
                "   => Starts the facilitator and the main container. (-rma) starts also the JADE GUI.\n" +
                "java -jar <jarfile> [-pNNNN] [-xX] [-yY] -seller -form \n" +
                "   => Shows a form to create a new auction.\n" +
                "java -jar <jarfile> [-pNNNN] [-xX] [-yY] -seller <name> <item> [<startBid>] [<timeout>] " +
                "[<increment>] [<reserve>] \n" +
                "   => Starts the auctioneer with specified name, item, starting bid, timeout, bid minimum " +
                "increment, reserve price. All prices numbers are integers, the timeout is in seconds.\n" +
                "      Defaults: \n" +
                "           startBid = 30\n" +
                "           timeout = 120\n" +
                "           increment = 1\n" +
                "           reserve = 40\n" +
                "java -jar <jarfile> [-pNNNN] [-xX] [-yY] -buyer <name> [<delayMin> <delayMax>]\n" +
                "   => Starts a buyer. With specified name and range of artificial random delays when submitting bids "
                + "(defaults to min=1 max=3).\n" +
                "\n\n" +
                "   => Additional feature: place the window with -xN and -yM placed as options as first options: " +
                "the position will be at (windowsize*x, windowsize*y) with (0,0) considered the top-left corner.");

        System.exit(1);
    }

}
