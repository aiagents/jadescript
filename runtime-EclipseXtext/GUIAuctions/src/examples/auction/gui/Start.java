package examples.auction.gui;


import jade.core.Runtime;
import jade.wrapper.StaleProxyException;
import jadescript.java.InvokerAgent;
import jadescript.java.Jadescript;

public class Start {

    public static void main(String[] args) throws StaleProxyException {
        if (args.length != 1
                || (!args[0].equals("-facilitator") && !args[0].equals("-seller") && !args[0].equals("-buyer"))
        ) {
            System.err.println("Usage: java -jar <jarfile> [-facilitator | -seller | -buyer]");
            System.exit(1);
            ;
        }

        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);
        Jadescript.bindNative(randomInteger.class, (new randomInteger() {

            @Override
            public Integer randomInteger(InvokerAgent invokerAgent, Integer max) {
                return null;
            }
        }).getClass());

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
            Seller.seller(container);
            return;
        }

        if (args[0].equals("-buyer")) {
            var container = Jadescript.newContainer();
            Buyer.buyer(container);
        }

    }

}
