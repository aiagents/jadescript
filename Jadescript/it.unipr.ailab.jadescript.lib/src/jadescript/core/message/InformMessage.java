package jadescript.core.message;

import jadescript.content.JadescriptProposition;

public class InformMessage<C extends JadescriptProposition> extends Message<C> {
    public InformMessage() {
        super(INFORM);
    }
}
