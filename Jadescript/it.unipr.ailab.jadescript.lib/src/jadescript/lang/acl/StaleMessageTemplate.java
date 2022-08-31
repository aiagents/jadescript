package jadescript.lang.acl;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jadescript.core.Agent;

import java.util.function.Supplier;

public class StaleMessageTemplate implements MessageTemplate.MatchExpression {
    private final Supplier<Agent> theAgent;

    public StaleMessageTemplate(Supplier<Agent> theAgent) {
        this.theAgent = theAgent;
    }

    @Override
    public boolean match(ACLMessage msg) {
        Agent a = null;
        if (theAgent != null) {
            a = theAgent.get();
        }
        return a != null && a.__isMessageStale(msg);
    }

    public static MessageTemplate matchStale(Supplier<Agent> theAgent) {
        return new MessageTemplate(new StaleMessageTemplate(theAgent));
    }
}
