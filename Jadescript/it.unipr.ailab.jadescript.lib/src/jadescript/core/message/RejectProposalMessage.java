package jadescript.core.message;

import jadescript.content.JadescriptAction;
import jadescript.content.JadescriptProposition;
import jadescript.lang.Tuple;

public class RejectProposalMessage<C1 extends JadescriptAction, C2 extends JadescriptProposition, C3 extends JadescriptProposition>
        extends Message<Tuple.Tuple3<C1, C2, C3>> {
    public RejectProposalMessage() {
        super(REJECT_PROPOSAL);
    }
}
