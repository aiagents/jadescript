package jadescript.core.message;


import jadescript.content.JadescriptConcept;

import java.util.List;

public class InformRefMessage<C extends List<JadescriptConcept>> extends Message<C>
        implements NotJadescriptSupportedMessage{
    public InformRefMessage() {
        super(INFORM_REF);
    }
}
