package examples.auction.gui;

import jadescript.java.Jadescript;

public class NativesBinder {
    private NativesBinder() {
    }

    public static void bindAllNativeTypes() {
        Jadescript.bindNative(BuyerView.class, BuyerViewJava.class);
        Jadescript.bindNative(updateBuyerView.class, BuyerViewJava.UpdateViewJava.class);
        Jadescript.bindNative(BidderView.class, BidderViewJava.class);
        Jadescript.bindNative(randomInteger.class, RandomIntegerJava.class);

    }
}
