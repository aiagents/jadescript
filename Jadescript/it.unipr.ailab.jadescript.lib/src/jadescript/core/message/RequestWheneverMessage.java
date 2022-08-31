package jadescript.core.message;

import jadescript.content.JadescriptAction;
import jadescript.content.JadescriptProposition;
import jadescript.lang.Tuple;

public class RequestWheneverMessage<C1 extends JadescriptAction, C2 extends JadescriptProposition>
        extends Message<Tuple.Tuple2<C1, C2>> {
    public RequestWheneverMessage() {
        super(REQUEST_WHENEVER);
    }
}
