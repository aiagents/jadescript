package jadescript.core.message;

import java.util.Iterator;

import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jadescript.core.exception.JadescriptException;
import jadescript.lang.Performative;

public class Message<C> extends ACLMessage {
    public static final int ACCEPT_PROPOSAL = 0;
    public static final int AGREE = 1;
    public static final int CANCEL = 2;
    public static final int CFP = 3;
    public static final int CONFIRM = 4;
    public static final int DISCONFIRM = 5;
    public static final int FAILURE = 6;
    public static final int INFORM = 7;
    public static final int INFORM_IF = 8;
    public static final int INFORM_REF = 9;
    public static final int NOT_UNDERSTOOD = 10;
    public static final int PROPOSE = 11;
    public static final int QUERY_IF = 12;
    public static final int QUERY_REF = 13;
    public static final int REFUSE = 14;
    public static final int REJECT_PROPOSAL = 15;
    public static final int REQUEST = 16;
    public static final int REQUEST_WHEN = 17;
    public static final int REQUEST_WHENEVER = 18;
    public static final int SUBSCRIBE = 19;
    public static final int PROXY = 20;
    public static final int PROPAGATE = 21;
    public static final int UNKNOWN = -1;

    public C content = null;

    public Message(int perf) {
        super(perf);
    }

    public Performative getJadescriptPerformative() {
        return Performative.fromCode(getPerformative());
    }

    @SuppressWarnings("unchecked")
    public C getContent(ContentManager contentManager) {
        if (content == null) {
            try {
                content = (C) contentManager.extractContent(this);
            } catch (OntologyException | Codec.CodecException e) {
                throw JadescriptException.wrap(e);
            }
        }
        if(content == null){
            // If content is still null, then something's wrong with the decoding phase
            throw JadescriptException.wrap(new Codec.CodecException("Decoding of content returned a null object."));
        }
        return content;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Message) {
            return this.toString().equals(other.toString());
        }
        return super.equals(other);
    }

    @SuppressWarnings("unchecked")
    public static <T> Message<T> wrap(ACLMessage msg) {
        Message<T> wrapper = null;
        if (msg != null) {
            if (msg instanceof Message) {
                wrapper = (Message<T>) msg;
            } else {
                final int performative = msg.getPerformative();
                wrapper = (Message<T>) getEmptyWrapper(performative);
                // This automatically performs the wrapping
                wrapper.setSender(msg.getSender());
                Iterator<?> it = msg.getAllReceiver();
                while (it.hasNext()) {
                    // This automatically performs the wrapping
                    wrapper.addReceiver((AID) it.next());
                }

                it = msg.getAllReplyTo();
                while (it.hasNext()) {
                    // This automatically performs the wrapping
                    wrapper.addReplyTo((AID) it.next());
                }

                wrapper.setLanguage(msg.getLanguage());
                wrapper.setOntology(msg.getOntology());
                wrapper.setProtocol(msg.getProtocol());
                wrapper.setInReplyTo(msg.getInReplyTo());
                wrapper.setReplyWith(msg.getReplyWith());
                wrapper.setConversationId(msg.getConversationId());
                wrapper.setReplyByDate(msg.getReplyByDate());
                if (msg.hasByteSequenceContent()) {
                    wrapper.setByteSequenceContent(msg.getByteSequenceContent());
                } else {
                    wrapper.setContent(msg.getContent());
                }
                wrapper.setEncoding(msg.getEncoding());

                wrapper.setEnvelope(msg.getEnvelope());
            }
        }
        return wrapper;
    }

    private static Message<?> getEmptyWrapper(int performative) {
        switch (performative) {
            case ACCEPT_PROPOSAL:
                return new AcceptProposalMessage<>();
            case AGREE:
                return new AgreeMessage<>();
            case CANCEL:
                return new CancelMessage<>();
            case CFP:
                return new CFPMessage<>();
            case CONFIRM:
                return new ConfirmMessage<>();
            case DISCONFIRM:
                return new DisconfirmMessage<>();
            case FAILURE:
                return new FailureMessage<>();
            case INFORM:
                return new InformMessage<>();
            case INFORM_IF:
                return new InformIfMessage<>();
            case INFORM_REF:
                return new InformRefMessage<>();
            case NOT_UNDERSTOOD:
                return new NotUnderstoodMessage<>();
            case PROPOSE:
                return new ProposeMessage<>();
            case QUERY_IF:
                return new QueryIfMessage<>();
            case QUERY_REF:
                return new QueryRefMessage<>();
            case REFUSE:
                return new RefuseMessage<>();
            case REJECT_PROPOSAL:
                return new RejectProposalMessage<>();
            case REQUEST:
                return new RequestMessage<>();
            case REQUEST_WHEN:
                return new RequestWhenMessage<>();
            case REQUEST_WHENEVER:
                return new RequestWheneverMessage<>();
            case SUBSCRIBE:
                return new SubscribeMessage<>();
            case PROXY:
                return new ProxyMessage<>();
            case PROPAGATE:
                return new PropagateMessage<>();
            case UNKNOWN:
                return new UnknownMessage<>();
            default:
                return new Message<>(performative);
        }
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
