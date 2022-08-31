package jadescript.core.message;

import jadescript.content.JadescriptProposition;

public class ConfirmMessage<C extends JadescriptProposition> extends Message<C> {
    public ConfirmMessage() {
        super(CONFIRM);
    }
}
