package jadescript.content.onto;

import jade.content.ContentElement;
import jade.core.AID;
import jadescript.content.JadescriptAction;
import jadescript.content.JadescriptProposition;
import jadescript.core.message.*;
import jadescript.lang.Performative;
import jadescript.lang.Tuple;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MessageContent {
    public static void main(String[] argv) {
        List<Class<?>> messageClasses = new ArrayList<>();
        messageClasses.add(AcceptProposalMessage.class);
        messageClasses.add(AgreeMessage.class);
        messageClasses.add(CancelMessage.class);
        messageClasses.add(CFPMessage.class);
        messageClasses.add(ConfirmMessage.class);
        messageClasses.add(DisconfirmMessage.class);
        messageClasses.add(FailureMessage.class);
        messageClasses.add(InformIfMessage.class);
        messageClasses.add(InformMessage.class);
        messageClasses.add(InformRefMessage.class);
        messageClasses.add(NotUnderstoodMessage.class);
        messageClasses.add(PropagateMessage.class);
        messageClasses.add(ProposeMessage.class);
        messageClasses.add(ProxyMessage.class);
        messageClasses.add(QueryIfMessage.class);
        messageClasses.add(QueryRefMessage.class);
        messageClasses.add(RefuseMessage.class);
        messageClasses.add(RejectProposalMessage.class);
        messageClasses.add(RequestMessage.class);
        messageClasses.add(RequestWheneverMessage.class);
        messageClasses.add(RequestWhenMessage.class);
        messageClasses.add(SubscribeMessage.class);
        messageClasses.add(UnknownMessage.class);


        System.out.println("// Generated, for MessageContent.java:");

        for (Class<?> messageClass : messageClasses) {
            final TypeVariable<? extends Class<?>>[] typeParameters = messageClass.getTypeParameters();
            if (typeParameters.length > 1) {
                String superClass = "Tuple.Tuple" + typeParameters.length + "<"
                        + Arrays.stream(typeParameters)
                        .map(TypeVariable::getBounds)
                        .filter(b -> b.length == 1)
                        .map(b -> b[0])
                        .map(Type::getTypeName)
                        .collect(Collectors.joining(", "))
                        + ">";
                String className = messageClass.getSimpleName() + "Content";
                System.out.println("public static class " + className + " extends " + superClass + "{");

                System.out.println("public static " + className + " __fromTuple(" + superClass + " tuple) {");
                System.out.println(className + " result = new " + className + "();");
                for (int i = 0; i < typeParameters.length; i++) {
                    System.out.println("result.setElement" + i + "(tuple.getElement" + i + "());");
                }
                System.out.println("return result;");
                System.out.println("}");
                System.out.println("}");

                System.out.println("public static ContentElement prepare" + messageClass.getSimpleName() + "(" + superClass + " content){");
                System.out.println("return " + className + ".__fromTuple(content);");
                System.out.println("}");
            } else if (typeParameters.length == 1) {
                final TypeVariable<? extends Class<?>> typeParameter = typeParameters[0];
                if (typeParameter.getBounds().length == 1) {
                    final String superClass = typeParameter.getBounds()[0].getTypeName();
                    System.out.println("public static ContentElement prepare" + messageClass.getSimpleName() + "(" + superClass + " content){");
                    System.out.println("return content;");
                    System.out.println("}");
                }
            }
        }


        System.out.println("// Generated, for Ontology_Vocabulary.java:");
        for (Class<?> messageClass : messageClasses) {
            final TypeVariable<? extends Class<?>>[] typeParameters = messageClass.getTypeParameters();
            if (typeParameters.length > 1) {
                System.out.println("String " + messageClass.getSimpleName().toUpperCase() + "_CONTENT" +
                        " = \"" + messageClass.getSimpleName() + "Content\";");
                for (int i = 0; i < typeParameters.length; i++) {
                    TypeVariable<? extends Class<?>> typeParameter = typeParameters[i];
                    if (typeParameter.getBounds().length == 1) {
                        System.out.println("String " + messageClass.getSimpleName().toUpperCase() + "_ELEMENT" + i +
                                "= \"element" + i + "\";");
                    }
                }
                System.out.println();
            }
        }

        System.out.println("// Generated, for Ontology.java, inside its zero-parameter ctor (part 1):");

        for (Class<?> messageClass : messageClasses) {
            final TypeVariable<? extends Class<?>>[] typeParameters = messageClass.getTypeParameters();
            if (typeParameters.length > 1) {
                System.out.println("add(new AgentActionSchema(" + messageClass.getSimpleName().toUpperCase() + "_CONTENT), " +
                        "jadescript.content.onto.MessageContent." + messageClass.getSimpleName() + "Content.class);");
            }
        }


        System.out.println("// Generated, for Ontology.java, inside its zero-parameter ctor (part 1):");

        for (Class<?> messageClass : messageClasses) {
            final TypeVariable<? extends Class<?>>[] typeParameters = messageClass.getTypeParameters();
            if (typeParameters.length > 1) {
                final String schemaName = messageClass.getSimpleName() + "_schema";
                System.out.println("AgentActionSchema " + schemaName + " = " +
                        "(AgentActionSchema) getSchema(" +
                        messageClass.getSimpleName().toUpperCase() +
                        "_CONTENT);"
                );
                for (int i = 0; i < typeParameters.length; i++) {
                    TypeVariable<? extends Class<?>> typeParameter = typeParameters[i];
                    if (typeParameter.getBounds().length == 1) {
                        final String typeName = typeParameter.getBounds()[0].getTypeName();
                        if (typeName.contains("java.util.List")) {
                            //list of aid
                            System.out.println(schemaName + ".add(" + messageClass.getSimpleName().toUpperCase() +
                                    "_ELEMENT" + i + ", (ConceptSchema) getSchema(BasicOntology.AID), 0, ObjectSchema.UNLIMITED);");
                        } else if (typeName.contains("JadescriptAction")) {
                            System.out.println(schemaName + ".add(" + messageClass.getSimpleName().toUpperCase() +
                                    "_ELEMENT" + i + ", (AgentActionSchema) getSchema(ACTION));");
                        } else if (typeName.contains("JadescriptProposition")) {
                            System.out.println(schemaName + ".add(" + messageClass.getSimpleName().toUpperCase() +
                                    "_ELEMENT" + i + ", (PredicateSchema) getSchema(PROPOSITION));");
                        } else if (typeName.contains("Message")) {
                            System.out.println(schemaName + ".add(" + messageClass.getSimpleName().toUpperCase() +
                                    "_ELEMENT" + i + ", (AgentActionSchema) getSchema(ACLMSG));");
                        }
                    }
                }

                System.out.println();
            }
        }

    }

    public static ContentElement prepareContent(ContentElement contentElement, String performative) {
        return prepareContent(contentElement, Performative.performativeByName.get(performative));
    }

    @SuppressWarnings("unchecked")
    public static ContentElement prepareContent(ContentElement content, Performative performative) {
        if (performative.equals(Performative.ACCEPT_PROPOSAL)) {
            return prepareAcceptProposalMessage((Tuple.Tuple2<JadescriptAction, JadescriptProposition>) content);

        }
        if (performative.equals(Performative.AGREE)) {
            return prepareAgreeMessage((Tuple.Tuple2<JadescriptAction, JadescriptProposition>) content);

        }
        if (performative.equals(Performative.CANCEL)) {
            return prepareCancelMessage((JadescriptAction) content);

        }
        if (performative.equals(Performative.CFP)) {
            return prepareCFPMessage((JadescriptAction) content);

        }
        if (performative.equals(Performative.CONFIRM)) {
            return prepareConfirmMessage((JadescriptProposition) content);

        }
        if (performative.equals(Performative.DISCONFIRM)) {
            return prepareDisconfirmMessage((JadescriptProposition) content);

        }
        if (performative.equals(Performative.FAILURE)) {
            return prepareFailureMessage((Tuple.Tuple2<JadescriptAction, JadescriptProposition>) content);

        }
        if (performative.equals(Performative.INFORM_IF)) {
            return prepareInformIfMessage((JadescriptProposition) content);

        }
        if (performative.equals(Performative.INFORM)) {
            return prepareInformMessage((JadescriptProposition) content);

        }

        if (performative.equals(Performative.NOT_UNDERSTOOD)) {
            return prepareNotUnderstoodMessage((Tuple.Tuple2<Message<Serializable>, JadescriptProposition>) content);

        }
        if (performative.equals(Performative.PROPAGATE)) {
            return preparePropagateMessage((Tuple.Tuple3<List<AID>, Message<Serializable>, JadescriptProposition>) content);

        }
        if (performative.equals(Performative.PROPOSE)) {
            return prepareProposeMessage((Tuple.Tuple2<JadescriptAction, JadescriptProposition>) content);

        }
        if (performative.equals(Performative.PROXY)) {
            return prepareProxyMessage((Tuple.Tuple3<List<AID>, Message<Serializable>, JadescriptProposition>) content);

        }
        if (performative.equals(Performative.QUERY_IF)) {
            return prepareQueryIfMessage((JadescriptProposition) content);

        }
        if (performative.equals(Performative.REFUSE)) {
            return prepareRefuseMessage((Tuple.Tuple2<JadescriptAction, JadescriptProposition>) content);

        }
        if (performative.equals(Performative.REJECT_PROPOSAL)) {
            return prepareRejectProposalMessage((Tuple.Tuple3<JadescriptAction, JadescriptProposition, JadescriptProposition>) content);

        }
        if (performative.equals(Performative.REQUEST)) {
            return prepareRequestMessage((JadescriptAction) content);

        }
        if (performative.equals(Performative.REQUEST_WHENEVER)) {
            return prepareRequestWheneverMessage((Tuple.Tuple2<JadescriptAction, JadescriptProposition>) content);

        }
        if (performative.equals(Performative.REQUEST_WHEN)) {
            return prepareRequestWhenMessage((Tuple.Tuple2<JadescriptAction, JadescriptProposition>) content);

        }
        if (performative.equals(Performative.UNKNOWN)) {
            return prepareUnknownMessage(content);
        }
        return null;
    }

    // Generated, for MessageContent.java:
    public static class AcceptProposalMessageContent extends Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> {
        public static AcceptProposalMessageContent __fromTuple(Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> tuple) {
            AcceptProposalMessageContent result = new AcceptProposalMessageContent();
            result.setElement0(tuple.getElement0());
            result.setElement1(tuple.getElement1());
            return result;
        }
    }

    public static ContentElement prepareAcceptProposalMessage(Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> content) {
        return AcceptProposalMessageContent.__fromTuple(content);
    }

    public static class AgreeMessageContent extends Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> {
        public static AgreeMessageContent __fromTuple(Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> tuple) {
            AgreeMessageContent result = new AgreeMessageContent();
            result.setElement0(tuple.getElement0());
            result.setElement1(tuple.getElement1());
            return result;
        }
    }

    public static ContentElement prepareAgreeMessage(Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> content) {
        return AgreeMessageContent.__fromTuple(content);
    }

    public static ContentElement prepareCancelMessage(jadescript.content.JadescriptAction content) {
        return content;
    }

    public static ContentElement prepareCFPMessage(jadescript.content.JadescriptAction content) {
        return content;
    }

    public static ContentElement prepareConfirmMessage(jadescript.content.JadescriptProposition content) {
        return content;
    }

    public static ContentElement prepareDisconfirmMessage(jadescript.content.JadescriptProposition content) {
        return content;
    }

    public static class FailureMessageContent extends Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> {
        public static FailureMessageContent __fromTuple(Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> tuple) {
            FailureMessageContent result = new FailureMessageContent();
            result.setElement0(tuple.getElement0());
            result.setElement1(tuple.getElement1());
            return result;
        }
    }

    public static ContentElement prepareFailureMessage(Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> content) {
        return FailureMessageContent.__fromTuple(content);
    }

    public static ContentElement prepareInformIfMessage(jadescript.content.JadescriptProposition content) {
        return content;
    }

    public static ContentElement prepareInformMessage(jadescript.content.JadescriptProposition content) {
        return content;
    }

    public static class NotUnderstoodMessageContent extends Tuple.Tuple2<jadescript.core.message.Message<java.io.Serializable>, jadescript.content.JadescriptProposition> {
        public static NotUnderstoodMessageContent __fromTuple(Tuple.Tuple2<jadescript.core.message.Message<java.io.Serializable>, jadescript.content.JadescriptProposition> tuple) {
            NotUnderstoodMessageContent result = new NotUnderstoodMessageContent();
            result.setElement0(tuple.getElement0());
            result.setElement1(tuple.getElement1());
            return result;
        }
    }

    public static ContentElement prepareNotUnderstoodMessage(Tuple.Tuple2<jadescript.core.message.Message<java.io.Serializable>, jadescript.content.JadescriptProposition> content) {
        return NotUnderstoodMessageContent.__fromTuple(content);
    }

    public static class PropagateMessageContent extends Tuple.Tuple3<java.util.List<jade.core.AID>, jadescript.core.message.Message<java.io.Serializable>, jadescript.content.JadescriptProposition> {
        public static PropagateMessageContent __fromTuple(Tuple.Tuple3<java.util.List<jade.core.AID>, jadescript.core.message.Message<java.io.Serializable>, jadescript.content.JadescriptProposition> tuple) {
            PropagateMessageContent result = new PropagateMessageContent();
            result.setElement0(tuple.getElement0());
            result.setElement1(tuple.getElement1());
            result.setElement2(tuple.getElement2());
            return result;
        }
    }

    public static ContentElement preparePropagateMessage(Tuple.Tuple3<java.util.List<jade.core.AID>, jadescript.core.message.Message<java.io.Serializable>, jadescript.content.JadescriptProposition> content) {
        return PropagateMessageContent.__fromTuple(content);
    }

    public static class ProposeMessageContent extends Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> {
        public static ProposeMessageContent __fromTuple(Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> tuple) {
            ProposeMessageContent result = new ProposeMessageContent();
            result.setElement0(tuple.getElement0());
            result.setElement1(tuple.getElement1());
            return result;
        }
    }

    public static ContentElement prepareProposeMessage(Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> content) {
        return ProposeMessageContent.__fromTuple(content);
    }

    public static class ProxyMessageContent extends Tuple.Tuple3<java.util.List<jade.core.AID>, jadescript.core.message.Message<java.io.Serializable>, jadescript.content.JadescriptProposition> {
        public static ProxyMessageContent __fromTuple(Tuple.Tuple3<java.util.List<jade.core.AID>, jadescript.core.message.Message<java.io.Serializable>, jadescript.content.JadescriptProposition> tuple) {
            ProxyMessageContent result = new ProxyMessageContent();
            result.setElement0(tuple.getElement0());
            result.setElement1(tuple.getElement1());
            result.setElement2(tuple.getElement2());
            return result;
        }
    }

    public static ContentElement prepareProxyMessage(Tuple.Tuple3<java.util.List<jade.core.AID>, jadescript.core.message.Message<java.io.Serializable>, jadescript.content.JadescriptProposition> content) {
        return ProxyMessageContent.__fromTuple(content);
    }

    public static ContentElement prepareQueryIfMessage(jadescript.content.JadescriptProposition content) {
        return content;
    }

    public static class RefuseMessageContent extends Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> {
        public static RefuseMessageContent __fromTuple(Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> tuple) {
            RefuseMessageContent result = new RefuseMessageContent();
            result.setElement0(tuple.getElement0());
            result.setElement1(tuple.getElement1());
            return result;
        }
    }

    public static ContentElement prepareRefuseMessage(Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> content) {
        return RefuseMessageContent.__fromTuple(content);
    }

    public static class RejectProposalMessageContent extends Tuple.Tuple3<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition, jadescript.content.JadescriptProposition> {
        public static RejectProposalMessageContent __fromTuple(Tuple.Tuple3<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition, jadescript.content.JadescriptProposition> tuple) {
            RejectProposalMessageContent result = new RejectProposalMessageContent();
            result.setElement0(tuple.getElement0());
            result.setElement1(tuple.getElement1());
            result.setElement2(tuple.getElement2());
            return result;
        }
    }

    public static ContentElement prepareRejectProposalMessage(Tuple.Tuple3<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition, jadescript.content.JadescriptProposition> content) {
        return RejectProposalMessageContent.__fromTuple(content);
    }

    public static ContentElement prepareRequestMessage(jadescript.content.JadescriptAction content) {
        return content;
    }

    public static class RequestWheneverMessageContent extends Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> {
        public static RequestWheneverMessageContent __fromTuple(Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> tuple) {
            RequestWheneverMessageContent result = new RequestWheneverMessageContent();
            result.setElement0(tuple.getElement0());
            result.setElement1(tuple.getElement1());
            return result;
        }
    }

    public static ContentElement prepareRequestWheneverMessage(Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> content) {
        return RequestWheneverMessageContent.__fromTuple(content);
    }

    public static class RequestWhenMessageContent extends Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> {
        public static RequestWhenMessageContent __fromTuple(Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> tuple) {
            RequestWhenMessageContent result = new RequestWhenMessageContent();
            result.setElement0(tuple.getElement0());
            result.setElement1(tuple.getElement1());
            return result;
        }
    }

    public static ContentElement prepareRequestWhenMessage(Tuple.Tuple2<jadescript.content.JadescriptAction, jadescript.content.JadescriptProposition> content) {
        return RequestWhenMessageContent.__fromTuple(content);
    }

    public static ContentElement prepareUnknownMessage(java.io.Serializable content) {
        return (ContentElement) content;
    }
}
