package jadescript.core.nativeevent;

import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class NotNativeEventTemplate implements MessageTemplate.MatchExpression {
    private final ContentManager manager;

    private NotNativeEventTemplate(ContentManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean match(ACLMessage msg) {
        try {
            ContentElement t = this.manager.extractContent(msg);
            return !(t instanceof NativeEvent);
        } catch (Throwable var3) {
            var3.printStackTrace();
            return false;
        }
    }

    public static MessageTemplate MatchNotNative(ContentManager manager) {
        return new MessageTemplate(new NotNativeEventTemplate(manager));
    }
}
