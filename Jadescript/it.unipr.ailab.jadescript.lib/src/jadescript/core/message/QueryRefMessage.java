package jadescript.core.message;

import jadescript.content.JadescriptConcept;

import java.util.List;


public class QueryRefMessage<C extends List<JadescriptConcept>> extends Message<C>
        implements NotJadescriptSupportedMessage {
    public QueryRefMessage() {
        super(QUERY_REF);
    }
}
