package jadescript.core.message;

import jade.core.AID;
import jadescript.content.JadescriptProposition;
import jadescript.lang.Tuple;

import java.io.Serializable;
import java.util.List;

public class PropagateMessage<C1 extends List<AID>, C2 extends Message<Serializable>, C3 extends JadescriptProposition>
        extends Message<Tuple.Tuple3<C1, C2, C3>> implements NotJadescriptSupportedMessage{
    public PropagateMessage() {
        super(PROPAGATE);
    }
}
