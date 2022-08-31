package jadescript.core.message;

import jadescript.content.JadescriptConcept;

import java.util.List;


public class SubscribeMessage<C extends List<JadescriptConcept>>
        extends Message<C>
        implements NotJadescriptSupportedMessage {
    public SubscribeMessage() {
        super(SUBSCRIBE);
    }
}
