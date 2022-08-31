package jadescript.core.message;

import jadescript.content.JadescriptProposition;

public class DisconfirmMessage<C extends JadescriptProposition> extends Message<C> {
    public DisconfirmMessage() {
        super(DISCONFIRM);
    }
}
