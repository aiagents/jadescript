package jadescript.core.message;


import jadescript.content.JadescriptProposition;

public class QueryIfMessage<C extends JadescriptProposition> extends Message<C> {
    public QueryIfMessage() {
        super(QUERY_IF);
    }
}
