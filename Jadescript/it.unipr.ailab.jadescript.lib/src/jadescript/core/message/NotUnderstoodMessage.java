package jadescript.core.message;

import jadescript.content.JadescriptProposition;
import jadescript.lang.Tuple;

import java.io.Serializable;

public class NotUnderstoodMessage<C1 extends Message<Serializable>, C2 extends JadescriptProposition>
        extends Message<Tuple.Tuple2<C1, C2>> {
    public NotUnderstoodMessage() {
        super(NOT_UNDERSTOOD);
    }
}
