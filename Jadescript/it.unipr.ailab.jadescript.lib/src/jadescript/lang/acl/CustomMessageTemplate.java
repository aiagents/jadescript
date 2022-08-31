package jadescript.lang.acl;

import jade.content.ContentElement;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jadescript.core.message.Message;

import java.util.function.Predicate;


@SuppressWarnings("rawtypes")
public class CustomMessageTemplate<T extends ContentElement> implements MessageTemplate.MatchExpression {

    private final Predicate<Message> __testPredicate;


    public CustomMessageTemplate(Predicate<Message> __testPredicate) {
        this.__testPredicate = __testPredicate;
    }



    @Override
    public boolean match(ACLMessage __receivedMessage){
        try{
            return __testPredicate.test(Message.wrap(__receivedMessage));
        }catch(Throwable e){
            e.printStackTrace();
            return false;
        }
    }
}
