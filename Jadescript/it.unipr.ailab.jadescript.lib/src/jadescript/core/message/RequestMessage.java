package jadescript.core.message;

import jadescript.content.JadescriptAction;

public class RequestMessage<C extends JadescriptAction> extends Message<C> {

    public RequestMessage() {
        super(REQUEST);
    }
}
