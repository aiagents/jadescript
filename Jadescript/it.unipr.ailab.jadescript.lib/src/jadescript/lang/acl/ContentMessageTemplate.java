package jadescript.lang.acl;

import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


public class ContentMessageTemplate implements MessageTemplate.MatchExpression {
    private final Class<?> clazz;
    private final ContentManager manager;

    private ContentMessageTemplate(ContentManager manager, Class<?> clazz) {
        this.clazz = clazz;
        this.manager = manager;
    }

    public boolean match(ACLMessage msg) {
        try {
            ContentElement t = this.manager.extractContent(msg);
            return t != null && this.clazz.isAssignableFrom(t.getClass());
        } catch (Throwable var3) {
            var3.printStackTrace();
            return false;
        }
    }

    public static MessageTemplate MatchClass(ContentManager manager, Class<?> clazz) {
        return new MessageTemplate(new ContentMessageTemplate(manager, clazz));
    }
}
