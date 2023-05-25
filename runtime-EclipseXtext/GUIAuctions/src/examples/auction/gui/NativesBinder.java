package examples.auction.gui;

import jadescript.java.Jadescript;

public class NativesBinder {
    private NativesBinder() {
    }

    public static void bindAllNativeTypes() {
        Jadescript.bindNative(BuyerView.class, BuyerViewJava.class);
        Jadescript.bindNative(addAuctionView.class, BuyerViewJava.addAuctionViewJava.class);
        Jadescript.bindNative(BidderView.class, BidderViewJava.class);
        Jadescript.bindNative(notifyResponse.class, BidderViewJava.NotifyResponseJava.class);
        Jadescript.bindNative(randomInteger.class, RandomIntegerJava.class);
        Jadescript.bindNative(AuctioneerView.class, AuctioneerViewJava.class);
        Jadescript.bindNative(updateAuctioneerGUI.class, AuctioneerViewJava.UpdateAuctioneerGUIJava.class);

    }
}
