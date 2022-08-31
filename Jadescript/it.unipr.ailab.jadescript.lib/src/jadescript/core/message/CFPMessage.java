package jadescript.core.message;

import jadescript.content.JadescriptAction;


public class CFPMessage<C extends JadescriptAction> extends Message<C> {
    public CFPMessage() {
        super(CFP);
    }
}
