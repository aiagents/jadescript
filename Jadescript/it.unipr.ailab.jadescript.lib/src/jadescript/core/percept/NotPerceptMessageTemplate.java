package jadescript.core.percept;

import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class NotPerceptMessageTemplate implements MessageTemplate.MatchExpression {
    private final ContentManager manager;

    private NotPerceptMessageTemplate(ContentManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean match(ACLMessage msg) {
        try {
            ContentElement t = this.manager.extractContent(msg);
            return !(t instanceof Percept);
        } catch (Throwable var3) {
            var3.printStackTrace();
            return false;
        }
    }

    public static MessageTemplate MatchNotPercept(ContentManager manager) {
        return new MessageTemplate(new NotPerceptMessageTemplate(manager));
    }
}
