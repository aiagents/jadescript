package examples.auction.gui;


import jade.core.Runtime;
import jade.wrapper.StaleProxyException;
import jadescript.java.Jadescript;

import javax.swing.*;
import java.awt.*;

public class Start {


    public static void main(String[] args) throws StaleProxyException {
        if (args.length < 1
                || (!args[0].equals("-facilitator") && !args[0].equals("-seller") && !args[0].equals("-buyer"))
        ) {
            System.err.println("Usage: java -jar <jarfile> [-facilitator | -seller | -buyer <delayMin> <delayMax>]");
            System.exit(1);
        }

        NativesBinder.bindAllNativeTypes();

        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);

        final Font defaultFont = UIManager.getFont("Label.font");
        final Font newFont = defaultFont.deriveFont((float) defaultFont.getSize() + (float) 4.0);
        UIManager.put("Label.font", newFont);
        UIManager.put("Button.font", newFont);
        UIManager.put("TextField.font", newFont);

        if (args[0].equals("-facilitator")) {
            var container = Jadescript.newMainContainer();

            try {
                Facilitator.create(container, "Facilitator");
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
            return;
        }

        if (args[0].equals("-seller")) {
            var container = Jadescript.newContainer();
            Seller.createSeller(container);
            return;
        }

        if (args[0].equals("-buyer")) {
            var container = Jadescript.newContainer();
            BuyerViewJava.createBuyer(container, Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        }

    }

}
