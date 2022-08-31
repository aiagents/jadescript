package jadescript.core.message;

import java.io.Serializable;

public class UnknownMessage<C extends Serializable> extends Message<C> {
    public UnknownMessage() {
        super(UNKNOWN);
    }
}
