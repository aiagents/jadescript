package jadescript.lang;

import jade.lang.acl.ACLMessage;
import jadescript.content.JadescriptAction;

import java.util.HashMap;
import java.util.Map;

public class Performative implements JadescriptAction {
    public static final Performative ACCEPT_PROPOSAL = new Performative(ACLMessage.ACCEPT_PROPOSAL);
    public static final Performative AGREE = new Performative(ACLMessage.AGREE);
    public static final Performative CANCEL = new Performative(ACLMessage.CANCEL);
    public static final Performative CFP = new Performative(ACLMessage.CFP);
    public static final Performative CONFIRM = new Performative(ACLMessage.CONFIRM);
    public static final Performative DISCONFIRM = new Performative(ACLMessage.DISCONFIRM);
    public static final Performative FAILURE = new Performative(ACLMessage.FAILURE);
    public static final Performative INFORM = new Performative(ACLMessage.INFORM);
    public static final Performative INFORM_IF = new Performative(ACLMessage.INFORM_IF);
    public static final Performative INFORM_REF = new Performative(ACLMessage.INFORM_REF);
    public static final Performative NOT_UNDERSTOOD = new Performative(ACLMessage.NOT_UNDERSTOOD);
    public static final Performative PROPOSE = new Performative(ACLMessage.PROPOSE);
    public static final Performative QUERY_IF = new Performative(ACLMessage.QUERY_IF);
    public static final Performative QUERY_REF = new Performative(ACLMessage.QUERY_REF);
    public static final Performative REFUSE = new Performative(ACLMessage.REFUSE);
    public static final Performative REJECT_PROPOSAL = new Performative(ACLMessage.REJECT_PROPOSAL);
    public static final Performative REQUEST = new Performative(ACLMessage.REQUEST);
    public static final Performative REQUEST_WHEN = new Performative(ACLMessage.REQUEST_WHEN);
    public static final Performative REQUEST_WHENEVER = new Performative(ACLMessage.REQUEST_WHENEVER);
    public static final Performative SUBSCRIBE = new Performative(ACLMessage.SUBSCRIBE);
    public static final Performative PROXY = new Performative(ACLMessage.PROXY);
    public static final Performative PROPAGATE = new Performative(ACLMessage.PROPAGATE);
    public static final Performative UNKNOWN = new Performative(ACLMessage.UNKNOWN);
    public static final Map<String, Performative> performativeByName = new HashMap<>();
    public static final Map<Performative, String> nameByPerformative = new HashMap<>();

    static {
        performativeByName.put("accept_proposal", ACCEPT_PROPOSAL);
        nameByPerformative.put(ACCEPT_PROPOSAL, "accept_proposal");
        performativeByName.put("agree", AGREE);
        nameByPerformative.put(AGREE, "agree");
        performativeByName.put("cancel", CANCEL);
        nameByPerformative.put(CANCEL, "cancel");
        performativeByName.put("cfp", CFP);
        nameByPerformative.put(CFP, "cfp");
        performativeByName.put("confirm", CONFIRM);
        nameByPerformative.put(CONFIRM, "confirm");
        performativeByName.put("disconfirm", DISCONFIRM);
        nameByPerformative.put(DISCONFIRM, "disconfirm");
        performativeByName.put("failure", FAILURE);
        nameByPerformative.put(FAILURE, "failure");
        performativeByName.put("inform", INFORM);
        nameByPerformative.put(INFORM, "inform");
        performativeByName.put("inform_if", INFORM_IF);
        nameByPerformative.put(INFORM_IF, "inform_if");
        performativeByName.put("inform_ref", INFORM_REF);
        nameByPerformative.put(INFORM_REF, "inform_ref");
        performativeByName.put("not_understood", NOT_UNDERSTOOD);
        nameByPerformative.put(NOT_UNDERSTOOD, "not_understood");
        performativeByName.put("propose", PROPOSE);
        nameByPerformative.put(PROPOSE, "propose");
        performativeByName.put("query_if", QUERY_IF);
        nameByPerformative.put(QUERY_IF, "query_if");
        performativeByName.put("query_ref", QUERY_REF);
        nameByPerformative.put(QUERY_REF, "query_ref");
        performativeByName.put("refuse", REFUSE);
        nameByPerformative.put(REFUSE, "refuse");
        performativeByName.put("reject_proposal", REJECT_PROPOSAL);
        nameByPerformative.put(REJECT_PROPOSAL, "reject_proposal");
        performativeByName.put("request", REQUEST);
        nameByPerformative.put(REQUEST, "request");
        performativeByName.put("request_when", REQUEST_WHEN);
        nameByPerformative.put(REQUEST_WHEN, "request_when");
        performativeByName.put("request_whenever", REQUEST_WHENEVER);
        nameByPerformative.put(REQUEST_WHENEVER, "request_whenever");
        performativeByName.put("subscribe", SUBSCRIBE);
        nameByPerformative.put(SUBSCRIBE, "subscribe");
        performativeByName.put("proxy", PROXY);
        nameByPerformative.put(PROXY, "proxy");
        performativeByName.put("propagate", PROPAGATE);
        nameByPerformative.put(PROPAGATE, "propagate");
        performativeByName.put("unknown", UNKNOWN);
        nameByPerformative.put(UNKNOWN, "unknown");
    }

    private int performativeCode;


    public Performative(int performativeCode) {
        this.performativeCode = performativeCode;
    }

    public Performative() {
        this.performativeCode = ACLMessage.UNKNOWN;
    }

    public int getCode() {
        return this.performativeCode;
    }

    public void setCode(int code) {
        this.performativeCode = code;
    }

    public int toCode() {
        return performativeCode;
    }

    @Override
    public String toString() {
        return nameByPerformative.getOrDefault(this, "unknown");
    }

    public static Performative fromCode(int code) {
        switch (code) {
            case 0:
                return ACCEPT_PROPOSAL;
            case 1:
                return AGREE;
            case 2:
                return CANCEL;
            case 3:
                return CFP;
            case 4:
                return CONFIRM;
            case 5:
                return DISCONFIRM;
            case 6:
                return FAILURE;
            case 7:
                return INFORM;
            case 8:
                return INFORM_IF;
            case 9:
                return INFORM_REF;
            case 10:
                return NOT_UNDERSTOOD;
            case 11:
                return PROPOSE;
            case 12:
                return QUERY_IF;
            case 13:
                return QUERY_REF;
            case 14:
                return REFUSE;
            case 15:
                return REJECT_PROPOSAL;
            case 16:
                return REQUEST;
            case 17:
                return REQUEST_WHEN;
            case 18:
                return REQUEST_WHENEVER;
            case 19:
                return SUBSCRIBE;
            case 20:
                return PROXY;
            case 21:
                return PROPAGATE;
            case -1:
            default:
                return UNKNOWN;
        }
    }


    @Override
    public jadescript.content.onto.Ontology __getDeclaringOntology() {
        return jadescript.content.onto.Ontology.getInstance();
    }
}
