package jadescript.content.onto;

import jade.content.ContentManager;
import jade.content.onto.BasicOntology;
import jade.content.onto.Introspector;
import jade.content.onto.OntologyException;
import jade.content.onto.ReflectiveIntrospector;
import jade.content.schema.*;
import jade.core.AID;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAAgentManagement.InternalError;
import jadescript.content.*;
import jadescript.content.onto.basic.*;
import jadescript.core.nativeevent.NativeEvent;
import jadescript.lang.Duration;
import jadescript.lang.Performative;
import jadescript.lang.Timestamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static jade.content.lang.sl.SL0Vocabulary.ACLMSG;

/**
 * @author Giuseppe Petrosino - giuseppe.petrosino@studenti.unipr.it
 */
public class Ontology extends jade.content.onto.Ontology implements Ontology_Vocabulary {
    protected static final Ontology _instance = new Ontology();

    private final jade.content.onto.Ontology[] superOntologies;

    public static void registerOntology(
        jade.content.onto.Ontology o,
        ContentManager cm
    ) {
        if(o instanceof Ontology){
            registerJadescriptOntology(((Ontology) o), cm);
        }else{
            cm.registerOntology(o);
        }
    }

    public static void registerJadescriptOntology(Ontology o, ContentManager cm) {
        cm.registerOntology(o);
        registerSuperOntologies(o, cm);
    }

    private static void registerSuperOntologies(Ontology o, ContentManager cm) {
        for (jade.content.onto.Ontology ontology : o.superOntologies) {
            registerOntology(ontology, cm);
        }
    }

    public static void __populateMapSchema(
        TermSchema keySchema,
        TermSchema valueSchema,
        ConceptSchema mapSchema
    ) {
        mapSchema.add(KEYS, keySchema, 0, ObjectSchema.UNLIMITED);
        mapSchema.add(VALUES, valueSchema, 0, ObjectSchema.UNLIMITED);
    }

    public static void __populateSetSchema(
        TermSchema elementSchema,
        ConceptSchema setSchema
    ) {
        setSchema.add(ELEMENTS, elementSchema, 0, ObjectSchema.UNLIMITED);
    }

    public static void __populateListSchema(
        TermSchema elementSchema,
        ConceptSchema listSchema
    ) {
        listSchema.add(ELEMENTS, elementSchema, 0, ObjectSchema.UNLIMITED);
    }


    public static JadescriptConcept emptyConcept() {
        return new Nothing();
    }

    public static JadescriptAction emptyAction() {
        return new DoNothing();
    }

    public static JadescriptPredicate emptyPredicate() {
        return new FalsePredicate();
    }

    public static JadescriptProposition emptyProposition() {
        return new False();
    }

    public static JadescriptAtomicProposition emptyAtomicProposition() {
        return new False();
    }


    private static jade.content.onto.Ontology[] superOntologies(
            jade.content.onto.Ontology... userDefinedSuperOntologies
    ) {
        List<jade.content.onto.Ontology> ontos = new ArrayList<>();
        ontos.add(BasicOntology.getInstance());
        ontos.add(ExceptionOntology.getInstance());
        ontos.addAll(Arrays.asList(userDefinedSuperOntologies));
        return ontos.toArray(new jade.content.onto.Ontology[0]);
    }

    public Ontology(String name, jade.content.onto.Ontology[] base, Introspector introspector) {
        super(name, base, introspector);
        this.superOntologies = base;
    }

    public Ontology(String name, jade.content.onto.Ontology base, Introspector introspector) {
        super(name, base, introspector);
        this.superOntologies = new jade.content.onto.Ontology[]{base};
    }

    public Ontology(String name, Introspector introspector) {
        super(name, introspector);
        this.superOntologies = superOntologies();
    }

    public Ontology(String name, jade.content.onto.Ontology base) {
        super(name, base);
        this.superOntologies = new jade.content.onto.Ontology[]{base};
    }

    public Ontology() {
        super("JADESCRIPT_ONTOLOGY", superOntologies(), new ReflectiveIntrospector());

        this.superOntologies = superOntologies();

        try {
            //Support types for Jadescript collections
            add(new ConceptSchema(MAP_ENTRY), JadescriptMapEntry.class);
            add(new ConceptSchema(SET_ENTRY), JadescriptSetEntry.class);
            add(new ConceptSchema(LIST_ENTRY), JadescriptListEntry.class);

            //Jadescript basic types
            add(new AgentActionSchema(PERFORMATIVE), Performative.class);
            add(new ConceptSchema(DURATION), Duration.class);
            add(new ConceptSchema(TIMESTAMP), Timestamp.class);
            add(new PredicateSchema(NATIVE_EVENT), NativeEvent.class);

            //Built-in onto elements
            add(new ConceptSchema(NOTHING), Nothing.class);
            add(new AgentActionSchema(DO_NOTHING), DoNothing.class);
            add(new PredicateSchema(FALSE_PREDICATE), FalsePredicate.class);
            add(new PredicateSchema(TRUE_PREDICATE), TruePredicate.class);
            add(new PredicateSchema(FALSE), False.class);
            add(new PredicateSchema(TRUE), True.class);


            add(new PredicateSchema(INTERNAL_EXCEPTION), InternalException.class);
            add(new PredicateSchema(COULD_NOT_CONVERT), CouldNotConvert.class);

            // Generated by jadescript.content.onto.MessageContent#main(String[]), for Ontology.java, inside its zero-parameter ctor (part 1):
            add(new AgentActionSchema(ACCEPTPROPOSALMESSAGE_CONTENT), jadescript.content.onto.MessageContent.AcceptProposalMessageContent.class);
            add(new AgentActionSchema(AGREEMESSAGE_CONTENT), jadescript.content.onto.MessageContent.AgreeMessageContent.class);
            add(new AgentActionSchema(CFPMESSAGE_CONTENT), jadescript.content.onto.MessageContent.CFPMessageContent.class);
            add(new AgentActionSchema(FAILUREMESSAGE_CONTENT), jadescript.content.onto.MessageContent.FailureMessageContent.class);
            add(new AgentActionSchema(NOTUNDERSTOODMESSAGE_CONTENT), jadescript.content.onto.MessageContent.NotUnderstoodMessageContent.class);
            add(new AgentActionSchema(PROPAGATEMESSAGE_CONTENT), jadescript.content.onto.MessageContent.PropagateMessageContent.class);
            add(new AgentActionSchema(PROPOSEMESSAGE_CONTENT), jadescript.content.onto.MessageContent.ProposeMessageContent.class);
            add(new AgentActionSchema(PROXYMESSAGE_CONTENT), jadescript.content.onto.MessageContent.ProxyMessageContent.class);
            add(new AgentActionSchema(REFUSEMESSAGE_CONTENT), jadescript.content.onto.MessageContent.RefuseMessageContent.class);
            add(new AgentActionSchema(REJECTPROPOSALMESSAGE_CONTENT), jadescript.content.onto.MessageContent.RejectProposalMessageContent.class);
            add(new AgentActionSchema(REQUESTWHENEVERMESSAGE_CONTENT), jadescript.content.onto.MessageContent.RequestWheneverMessageContent.class);
            add(new AgentActionSchema(REQUESTWHENMESSAGE_CONTENT), jadescript.content.onto.MessageContent.RequestWhenMessageContent.class);
            // End generated part.

            AgentActionSchema aas = (AgentActionSchema) getSchema(PERFORMATIVE);
            aas.add(PERFORMATIVE_CODE, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));

            ConceptSchema cs = (ConceptSchema) getSchema(DURATION);
            cs.add(DURATION_SECONDSA, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            cs.add(DURATION_SECONDSB, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            cs.add(DURATION_MILLIS, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));

            ConceptSchema cs2 = (ConceptSchema) getSchema(TIMESTAMP);
            cs2.add(TIMESTAMP_DATE, (PrimitiveSchema) getSchema(BasicOntology.DATE));
            cs2.add(TIMESTAMP_ZONE_OFFSET, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));

            PredicateSchema cs3 = (PredicateSchema) getSchema(NATIVE_EVENT);
            cs3.add(NATIVE_EVENT_CONTENT, PredicateSchema.getBaseSchema());

            PredicateSchema ps = (PredicateSchema) getSchema(INTERNAL_EXCEPTION);
            ps.add(INTERNAL_EXCEPTION_DESCRIPTION, getSchema(BasicOntology.STRING));

            PredicateSchema ps2 = (PredicateSchema) getSchema(COULD_NOT_CONVERT);
            ps2.add(COULD_NOT_CONVERT_VALUE, getSchema(BasicOntology.STRING));
            ps2.add(COULD_NOT_CONVERT_FROM_TYPE_NAME, getSchema(BasicOntology.STRING));
            ps2.add(COULD_NOT_CONVERT_TO_TYPE_NAME, getSchema(BasicOntology.STRING));


            // Generated by jadescript.content.onto.MessageContent#main(String[]), for Ontology.java, inside its zero-parameter ctor (part 2):
            AgentActionSchema AcceptProposalMessage_schema = (AgentActionSchema) getSchema(ACCEPTPROPOSALMESSAGE_CONTENT);
            AcceptProposalMessage_schema.add(ACCEPTPROPOSALMESSAGE_ELEMENT0, (AgentActionSchema) AgentActionSchema.getBaseSchema());
            AcceptProposalMessage_schema.add(ACCEPTPROPOSALMESSAGE_ELEMENT1, (PredicateSchema) PredicateSchema.getBaseSchema());

            AgentActionSchema AgreeMessage_schema = (AgentActionSchema) getSchema(AGREEMESSAGE_CONTENT);
            AgreeMessage_schema.add(AGREEMESSAGE_ELEMENT0, (AgentActionSchema) AgentActionSchema.getBaseSchema());
            AgreeMessage_schema.add(AGREEMESSAGE_ELEMENT1, (PredicateSchema) PredicateSchema.getBaseSchema());

            AgentActionSchema CFPMessage_schema = (AgentActionSchema) getSchema(CFPMESSAGE_CONTENT);
            CFPMessage_schema.add(CFPMESSAGE_ELEMENT0, (AgentActionSchema) AgentActionSchema.getBaseSchema());
            CFPMessage_schema.add(CFPMESSAGE_ELEMENT1, (PredicateSchema) PredicateSchema.getBaseSchema());

            AgentActionSchema FailureMessage_schema = (AgentActionSchema) getSchema(FAILUREMESSAGE_CONTENT);
            FailureMessage_schema.add(FAILUREMESSAGE_ELEMENT0, (AgentActionSchema) AgentActionSchema.getBaseSchema());
            FailureMessage_schema.add(FAILUREMESSAGE_ELEMENT1, (PredicateSchema) PredicateSchema.getBaseSchema());

            AgentActionSchema NotUnderstoodMessage_schema = (AgentActionSchema) getSchema(NOTUNDERSTOODMESSAGE_CONTENT);
            NotUnderstoodMessage_schema.add(NOTUNDERSTOODMESSAGE_ELEMENT0, (AgentActionSchema) getSchema(ACLMSG));
            NotUnderstoodMessage_schema.add(NOTUNDERSTOODMESSAGE_ELEMENT1, (PredicateSchema) PredicateSchema.getBaseSchema());

            AgentActionSchema PropagateMessage_schema = (AgentActionSchema) getSchema(PROPAGATEMESSAGE_CONTENT);
            PropagateMessage_schema.add(PROPAGATEMESSAGE_ELEMENT0, (ConceptSchema) getSchema(BasicOntology.AID), 0, ObjectSchema.UNLIMITED);
            PropagateMessage_schema.add(PROPAGATEMESSAGE_ELEMENT1, (AgentActionSchema) getSchema(ACLMSG));
            PropagateMessage_schema.add(PROPAGATEMESSAGE_ELEMENT2, (PredicateSchema) PredicateSchema.getBaseSchema());

            AgentActionSchema ProposeMessage_schema = (AgentActionSchema) getSchema(PROPOSEMESSAGE_CONTENT);
            ProposeMessage_schema.add(PROPOSEMESSAGE_ELEMENT0, (AgentActionSchema) AgentActionSchema.getBaseSchema());
            ProposeMessage_schema.add(PROPOSEMESSAGE_ELEMENT1, (PredicateSchema) PredicateSchema.getBaseSchema());

            AgentActionSchema ProxyMessage_schema = (AgentActionSchema) getSchema(PROXYMESSAGE_CONTENT);
            ProxyMessage_schema.add(PROXYMESSAGE_ELEMENT0, (ConceptSchema) getSchema(BasicOntology.AID), 0, ObjectSchema.UNLIMITED);
            ProxyMessage_schema.add(PROXYMESSAGE_ELEMENT1, (AgentActionSchema) getSchema(ACLMSG));
            ProxyMessage_schema.add(PROXYMESSAGE_ELEMENT2, (PredicateSchema) PredicateSchema.getBaseSchema());

            AgentActionSchema RefuseMessage_schema = (AgentActionSchema) getSchema(REFUSEMESSAGE_CONTENT);
            RefuseMessage_schema.add(REFUSEMESSAGE_ELEMENT0, (AgentActionSchema) AgentActionSchema.getBaseSchema());
            RefuseMessage_schema.add(REFUSEMESSAGE_ELEMENT1, (PredicateSchema) PredicateSchema.getBaseSchema());

            AgentActionSchema RejectProposalMessage_schema = (AgentActionSchema) getSchema(REJECTPROPOSALMESSAGE_CONTENT);
            RejectProposalMessage_schema.add(REJECTPROPOSALMESSAGE_ELEMENT0, (AgentActionSchema) AgentActionSchema.getBaseSchema());
            RejectProposalMessage_schema.add(REJECTPROPOSALMESSAGE_ELEMENT1, (PredicateSchema) PredicateSchema.getBaseSchema());
            RejectProposalMessage_schema.add(REJECTPROPOSALMESSAGE_ELEMENT2, (PredicateSchema) PredicateSchema.getBaseSchema());

            AgentActionSchema RequestWheneverMessage_schema = (AgentActionSchema) getSchema(REQUESTWHENEVERMESSAGE_CONTENT);
            RequestWheneverMessage_schema.add(REQUESTWHENEVERMESSAGE_ELEMENT0, (AgentActionSchema) AgentActionSchema.getBaseSchema());
            RequestWheneverMessage_schema.add(REQUESTWHENEVERMESSAGE_ELEMENT1, (PredicateSchema) PredicateSchema.getBaseSchema());

            AgentActionSchema RequestWhenMessage_schema = (AgentActionSchema) getSchema(REQUESTWHENMESSAGE_CONTENT);
            RequestWhenMessage_schema.add(REQUESTWHENMESSAGE_ELEMENT0, (AgentActionSchema) AgentActionSchema.getBaseSchema());
            RequestWhenMessage_schema.add(REQUESTWHENMESSAGE_ELEMENT1, (PredicateSchema) PredicateSchema.getBaseSchema());

            // End generated part.
        } catch (OntologyException e) {
            e.printStackTrace();
        }
    }

    public static Ontology getInstance() {
        return _instance;
    }

    public static AID aid(String localName) {
        return new AID(localName, AID.ISLOCALNAME);
    }

    public static Nothing Nothing() {
        return new Nothing();
    }

    public static DoNothing DoNothing() {
        return new DoNothing();
    }

    public static FalsePredicate FalsePredicate() {
        return new FalsePredicate();
    }

    public static TruePredicate TruePredicate() {
        return new TruePredicate();
    }

    public static False False() {
        return new False();
    }

    public static True True() {
        return new True();
    }

    public static InternalException InternalException(String description) {
        return new InternalException(description);
    }

    public static CouldNotConvert CouldNotConvert(String value, String fromTypeName, String toTypeName) {
        return new CouldNotConvert(value, fromTypeName, toTypeName);
    }

    public static Unauthorised Unauthorised() {
        return new Unauthorised();
    }


    public static UnsupportedAct UnsupportedAct(String act) {
        return new UnsupportedAct(act);
    }

    public static UnexpectedAct UnexpectedAct(String act) {
        return new UnexpectedAct(act);
    }

    public static UnsupportedValue UnsupportedValue(String value) {
        return new UnsupportedValue(value);
    }

    public static UnrecognisedValue UnrecognisedValue(String value) {
        return new UnrecognisedValue(value);
    }

    public static MissingArgument MissingArgument(String argumentName) {
        return new MissingArgument(argumentName);
    }

    public static UnexpectedArgument UnexpectedArgument(String argumentName) {
        return new UnexpectedArgument(argumentName);
    }

    public static UnexpectedArgumentCount UnexpectedArgumentCount() {
        return new UnexpectedArgumentCount();
    }

    public static UnsupportedFunction UnsupportedFunction(String function) {
        return new UnsupportedFunction(function);
    }

    public static MissingParameter MissingParameter(String objectName, String parameterName) {
        return new MissingParameter(objectName, parameterName);
    }

    public static UnexpectedParameter UnexpectedParameter(String objectName, String parameterName) {
        return new UnexpectedParameter(objectName, parameterName);
    }

    public static UnrecognisedParameterValue UnrecognisedParameterValue(String parameterName, String parameterValue) {
        return new UnrecognisedParameterValue(parameterName, parameterValue);
    }

    public static InternalError InternalError(String msg) {
        return new InternalError(msg);
    }


}
