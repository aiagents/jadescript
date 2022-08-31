package jadescript.core.message;

import jadescript.content.JadescriptProposition;

public class InformIfMessage<C extends JadescriptProposition> extends Message<C> {
    public InformIfMessage() {
        super(INFORM_IF);
    }
}
