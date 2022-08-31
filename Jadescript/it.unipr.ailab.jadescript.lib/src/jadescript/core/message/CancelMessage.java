package jadescript.core.message;

import jadescript.content.JadescriptAction;


public class CancelMessage<C extends JadescriptAction> extends Message<C> {
    public CancelMessage() {
        super(CANCEL);
    }
}
