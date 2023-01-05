package it.unipr.ailab.jadescript.semantics.helpers;


import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.c0outer.RawTypeReferenceSolverContext;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.FlowTypeInferringTerm;
import it.unipr.ailab.jadescript.semantics.context.search.JadescriptTypeLocation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.*;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.JvmTypeQualifiedNameParser;
import it.unipr.ailab.jadescript.semantics.utils.JvmTypeReferenceSet;
import it.unipr.ailab.maybe.Maybe;
import jade.content.onto.Ontology;
import jadescript.content.*;
import jadescript.core.Agent;
import jadescript.core.behaviours.*;
import jadescript.core.message.*;
import jadescript.lang.Performative;
import jadescript.lang.Tuple;
import jadescript.util.JadescriptMap;
import jadescript.util.JadescriptSet;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.*;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypeReferenceBuilder;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;
import static jadescript.lang.Performative.*;


public class TypeHelper implements SemanticsConsts {



    //Associates JVM fully-qualified-name strings to Jadescript types.
    private final Map<String, IJadescriptType> defaultJVMToDescriptorTable = new HashMap<>();
    private final Map<String, Function<List<TypeArgument>, ? extends IJadescriptType>>
            defaultJVMToGenericDescriptorTable = new HashMap<>();

    private final Map<String, Integer> expectedGenericDescriptorArguments = new HashMap<>();
    private final List<ImplicitConversionDefinition> implicitConversions = new ArrayList<>();


    private final Map<Performative, Function<List<TypeArgument>, MessageSubType>> messageSubTypeMap = new HashMap<>();
    private final Map<String, Performative> nameToPerformativeMap = new HashMap<>();
    private final Map<Performative, Supplier<IJadescriptType>> messageContentTypeRequirements = new HashMap<>();

    //Associates to some performatives (ACCEPT_PROPOSAL, CFP, etc...) with a set of default elements (mainly TRUE propositions)
    private final Map<Performative, MessageContentTupleDefaultElements> defaultContentElementsMap = new HashMap<>();


    // Top and bottom
    //TODO use new TOP/BOTTOM system when possible
    public final Function<String, UtilityType> TOP;
    public final Function<String, UtilityType> BOTTOM;
    public final UtilityType ANY;
    public final UtilityType NOTHING;

    // Common useful Jvm types
    public final UtilityType VOID;
    public final UtilityType NUMBER;
    public final UtilityType SERIALIZABLE;
    public final UtilityType ANYMESSAGE;

    // Jadescript basic types
    public final BasicType INTEGER;
    public final BasicType BOOLEAN;
    public final BasicType TEXT;
    public final BasicType REAL;
    public final BasicType PERFORMATIVE;
    public final BasicType AID;
    public final BasicType DURATION;
    public final BasicType TIMESTAMP;

    // Jadescript collection types
    public final Function<List<TypeArgument>, ListType> LIST;
    public final Function<List<TypeArgument>, MapType> MAP;
    public final Function<List<TypeArgument>, SetType> SET;

    public final Function<List<TypeArgument>, TupleType> TUPLE;

    // Jadescript Basic Agent type
    public final BaseAgentType AGENT;

    // Jadescript Message types
    public final Function<List<TypeArgument>, ? extends BaseMessageType> MESSAGE;


    // Jadescript Basic Ontology type
    public final BaseOntologyType ONTOLOGY;

    // Jadescript Basic Behaviour type
    public final Function<List<TypeArgument>, BaseBehaviourType> BEHAVIOUR;
    public final Function<List<TypeArgument>, BaseBehaviourType> CYCLIC_BEHAVIOUR;
    public final Function<List<TypeArgument>, BaseBehaviourType> ONESHOT_BEHAVIOUR;

    public final BaseBehaviourType ANYBEHAVIOUR;

    public final BaseOntoContentType CONCEPT;
    public final BaseOntoContentType PROPOSITION;
    public final BaseOntoContentType PREDICATE;
    public final BaseOntoContentType ATOMIC_PROPOSITION;
    public final BaseOntoContentType ACTION;

    public final UserDefinedOntoContentType INTERNAL_EXCEPTION;
    public final Function<List<TypeArgument>, MessageSubType> ACCEPTPROPOSAL_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> AGREE_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> CANCEL_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> CFP_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> CONFIRM_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> DISCONFIRM_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> FAILURE_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> INFORM_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> INFORMIF_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> INFORMREF_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> NOTUNDERSTOOD_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> PROPOSE_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> QUERYIF_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> QUERYREF_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> REFUSE_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> REJECTPROPOSAL_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> REQUEST_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> REQUESTWHEN_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> REQUESTWHENEVER_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> SUBSCRIBE_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> PROXY_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> PROPAGATE_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> UNKNOWN_MESSAGE;

    public static final String builtinPrefix = "BUILTIN#";
    public static final String VOID_TYPEID = builtinPrefix + "JAVAVOID";

    private final SemanticsModule module;


    public TypeHelper(SemanticsModule module) {
        this.module = module;
        ANY = new UtilityType(
                module,
                builtinPrefix + "ANY",
                "(error)any",
                this.objectTypeRef()
        ) {
            @Override
            public boolean isAssignableFrom(IJadescriptType other) {
                return true;
            }

            @Override
            public boolean isSendable() {
                return false;
            }

            @Override
            public boolean isErroneous() {
                return true;
            }

            @Override
            public Maybe<OntologyType> getDeclaringOntology() {
                return nothing();
            }

            @Override
            public TypeNamespace namespace() {
                return new BuiltinOpsNamespace(
                        module,
                        Maybe.nothing(),
                        List.of(),
                        List.of(),
                        getLocation()
                );
            }

        };
        defineJVMToDescriptor(Object.class, ANY);

        TOP = (String s) -> ErroneousType.top(module, s, ANY);


        NOTHING = new UtilityType(
                module,
                builtinPrefix + "NOTHING",
                "(error)nothing",
                module.get(JvmTypeReferenceBuilder.class).typeRef("/*NOTHING*/java.lang.Object")
        ) {
            @Override
            public boolean isAssignableFrom(IJadescriptType other) {
                return false;
            }

            @Override
            public boolean isSendable() {
                return false;
            }

            @Override
            public boolean isErroneous() {
                return true;
            }

            @Override
            public Maybe<OntologyType> getDeclaringOntology() {
                return nothing();
            }

            @Override
            public TypeNamespace namespace() {
                return new BuiltinOpsNamespace(
                        module,
                        Maybe.nothing(),
                        List.of(),
                        List.of(),
                        getLocation()
                );
            }
        };

        BOTTOM = (String s) -> ErroneousType.bottom(module, s, NOTHING);

        VOID = new UtilityType(
                module,
                VOID_TYPEID,
                "(error)javaVoid",
                this.typeRef(void.class)
        ) {
            @Override
            public boolean isSendable() {
                return false;
            }

            @Override
            public boolean isErroneous() {
                return true;
            }

            @Override
            public Maybe<OntologyType> getDeclaringOntology() {
                return nothing();
            }

            @Override
            public TypeNamespace namespace() {
                return new BuiltinOpsNamespace(
                        module,
                        Maybe.nothing(),
                        List.of(),
                        List.of(),
                        getLocation()
                );
            }

            @Override
            public boolean isJavaVoid() {
                return true;
            }
        };
        defineJVMToDescriptor(Void.class, VOID);
        defineJVMToDescriptor(Void.TYPE, VOID);


        INTEGER = new BasicType(
                this.module,
                builtinPrefix + "integer",
                "integer",
                "jade.content.onto.BasicOntology.INTEGER",
                this.typeRef(Integer.class),
                "0"
        );
        defineJVMToDescriptor(Integer.class, INTEGER);
        defineJVMToDescriptor(Integer.TYPE, INTEGER);
        BOOLEAN = new BasicType(
                this.module,
                builtinPrefix + "boolean",
                "boolean",
                "jade.content.onto.BasicOntology.BOOLEAN",
                this.typeRef(Boolean.class),
                "false"
        );
        defineJVMToDescriptor(Boolean.class, BOOLEAN);
        defineJVMToDescriptor(Boolean.TYPE, BOOLEAN);
        TEXT = new BasicType(
                this.module,
                builtinPrefix + "text",
                "text",
                "jade.content.onto.BasicOntology.STRING",
                this.typeRef(String.class),
                "\"\""
        );
        defineJVMToDescriptor(String.class, TEXT);
        TEXT.addProperty(new Property("length", INTEGER, true, new JadescriptTypeLocation(TEXT))
                .setCompileByCustomJVMMethod("length", "length"));
        REAL = new BasicType(
                this.module,
                builtinPrefix + "real",
                "real",
                "jade.content.onto.BasicOntology.FLOAT",
                this.typeRef(Float.class),
                "0.0f"
        );
        defineJVMToDescriptor(Float.class, REAL);
        defineJVMToDescriptor(Float.TYPE, REAL);
        defineJVMToDescriptor(Double.class, REAL);
        defineJVMToDescriptor(Double.TYPE, REAL);
        PERFORMATIVE = new BasicType(
                this.module,
                builtinPrefix + "performative",
                "performative",
                "jadescript.content.onto.Ontology.PERFORMATIVE",
                this.typeRef(jadescript.lang.Performative.class),
                "jadescript.lang.Performative.UNKNOWN"
        );
        defineJVMToDescriptor(jadescript.lang.Performative.class, PERFORMATIVE);
        AID = new BasicType(
                this.module,
                builtinPrefix + "aid",
                "aid",
                "jade.content.lang.sl.SL0Vocabulary.AID",
                this.typeRef(jade.core.AID.class),
                "new jade.core.AID()"
        );
        final SearchLocation aidLocation = new JadescriptTypeLocation(AID);
        AID.addProperty(new Property("name", TEXT, true, aidLocation)
                .setCompileByJVMAccessors());
        AID.addProperty(new Property("platform", TEXT, true, aidLocation)
                .setCompileByCustomJVMMethod("getPlatformID", "setPlatformID"));
        defineJVMToDescriptor(jade.core.AID.class, AID);
        DURATION = new BasicType(
                this.module,
                builtinPrefix + "duration",
                "duration",
                "jadescript.content.onto.Ontology.DURATION",
                this.typeRef(jadescript.lang.Duration.class),
                "new jadescript.lang.Duration()"
        );
        defineJVMToDescriptor(jadescript.lang.Duration.class, DURATION);
        TIMESTAMP = new BasicType(
                this.module,
                builtinPrefix + "timestamp",
                "timestamp",
                "jadescript.content.onto.Ontology.TIMESTAMP",
                this.typeRef(jadescript.lang.Timestamp.class),
                "jadescript.lang.Timestamp.now()"
        );
        defineJVMToDescriptor(jadescript.lang.Timestamp.class, TIMESTAMP);

        defineImplicitConversionPath(INTEGER, REAL, (compiledRExpr) ->
                "((" + REAL.compileAsJavaCast() + "((float) " + compiledRExpr + ")))"
        );


        // for java.io.Serializable, not usable in the language, used by
        //      type engine as a supertype for ontology content types
        SERIALIZABLE = new UtilityType(
                module,
                builtinPrefix + "serializable",
                this.typeRef(Serializable.class).getQualifiedName('.'),
                this.typeRef(Serializable.class)
        ) {
            @Override
            public boolean isSendable() {
                return false;
            }

            @Override
            public boolean isErroneous() {
                return false;
            }

            @Override
            public Maybe<OntologyType> getDeclaringOntology() {
                return nothing();
            }

            @Override
            public TypeNamespace namespace() {
                return new BuiltinOpsNamespace(
                        module,
                        Maybe.nothing(),
                        List.of(),
                        List.of(),
                        getLocation()
                );
            }
        };
        defineJVMToDescriptor(Serializable.class, SERIALIZABLE);

        ANYMESSAGE = new UtilityType(
                module,
                builtinPrefix + "ANYMESSAGE",
                "Message",
                this.typeRef(Message.class)
        ) {
            @Override
            public boolean isErroneous() {
                return false;
            }

            @Override
            public Maybe<OntologyType> getDeclaringOntology() {
                return some(ONTOLOGY);
            }

            @Override
            public TypeNamespace namespace() {
                return new BuiltinOpsNamespace(
                        module,
                        Maybe.nothing(),
                        List.of(),
                        List.of(),
                        getLocation()
                );
            }

            @Override
            public boolean isAssignableFrom(IJadescriptType other) {
                other = other.postResolve();
                if (other instanceof BaseMessageType) {
                    return true;
                } else {
                    return super.isAssignableFrom(other);
                }
            }

            @Override
            public boolean isSendable() {
                return true;
            }
        };


        LIST = (arguments) -> new ListType(module, arguments.get(0));
        defineJVMToGenericDescriptor(List.class, LIST, 1);

        MAP = (arguments) -> new MapType(module, arguments.get(0), arguments.get(1));
        defineJVMToGenericDescriptor(JadescriptMap.class, MAP, 2);

        SET = (arguments) -> new SetType(module, arguments.get(0));
        defineJVMToGenericDescriptor(JadescriptSet.class, SET, 1);

        TUPLE = (arguments) -> new TupleType(module, arguments);

        AGENT = new BaseAgentType(module);
        defineJVMToDescriptor(jadescript.core.Agent.class, AGENT);

        MESSAGE = (arguments) -> new BaseMessageType(module, arguments.get(0));
        defineJVMToGenericDescriptor(jadescript.core.message.Message.class, MESSAGE, 1);


        ONTOLOGY = new BaseOntologyType(module);
        defineJVMToDescriptor(jadescript.content.onto.Ontology.class, ONTOLOGY);
        defineJVMToDescriptor(jade.content.onto.Ontology.class, ONTOLOGY);

        // for java.lang.Number, not usable in the language, used by
        //      type engine as a non-direct supertype for number basic types
        //  (non-direct supertype = you can ask NUMBER if INTEGER is its subtype,
        //    but you will not find NUMBER in the list of INTEGER's direct sypertypes)
        NUMBER = new UtilityType(
                module,
                builtinPrefix + "number",
                this.typeRef(Number.class).getQualifiedName('.'),
                this.typeRef(Number.class)
        ) {
            @Override
            public boolean isAssignableFrom(IJadescriptType other) {
                other = other.postResolve();
                if (other.typeEquals(INTEGER) || other.typeEquals(REAL)) {
                    return true;
                } else {
                    return super.isAssignableFrom(other);
                }
            }

            @Override
            public boolean isSendable() {
                return false;
            }

            @Override
            public boolean isErroneous() {
                return false;
            }

            @Override
            public Maybe<OntologyType> getDeclaringOntology() {
                return some(ONTOLOGY);
            }

            @Override
            public TypeNamespace namespace() {
                return new BuiltinOpsNamespace(
                        module,
                        Maybe.nothing(),
                        List.of(),
                        List.of(),
                        getLocation()
                );
            }
        };
        defineJVMToDescriptor(Number.class, NUMBER);

        BEHAVIOUR = (arguments) ->
                new BaseBehaviourType(module, BaseBehaviourType.Kind.Base, arguments.get(0), AGENT);
        defineJVMToGenericDescriptor(jadescript.core.behaviours.Behaviour.class, BEHAVIOUR, 1);

        CYCLIC_BEHAVIOUR = (arguments) ->
                new BaseBehaviourType(module, BaseBehaviourType.Kind.Cyclic, arguments.get(0), AGENT);
        defineJVMToGenericDescriptor(jadescript.core.behaviours.CyclicBehaviour.class, CYCLIC_BEHAVIOUR, 1);

        ONESHOT_BEHAVIOUR = (arguments) ->
                new BaseBehaviourType(module, BaseBehaviourType.Kind.OneShot, arguments.get(0), AGENT);
        defineJVMToGenericDescriptor(jadescript.core.behaviours.OneShotBehaviour.class, ONESHOT_BEHAVIOUR, 1);

        ANYBEHAVIOUR = BEHAVIOUR.apply(List.of(covariant(AGENT)));

        CONCEPT = new BaseOntoContentType(
                module,
                BaseOntoContentType.Kind.Concept
        );
        defineJVMToDescriptor(jadescript.content.JadescriptConcept.class, CONCEPT);


        PROPOSITION = new BaseOntoContentType(
                module,
                BaseOntoContentType.Kind.Proposition
        );
        defineJVMToDescriptor(jadescript.content.JadescriptProposition.class, PROPOSITION);

        PREDICATE = new BaseOntoContentType(
                module,
                BaseOntoContentType.Kind.Predicate
        );
        defineJVMToDescriptor(jadescript.content.JadescriptPredicate.class, PREDICATE);

        ATOMIC_PROPOSITION = new BaseOntoContentType(
                module,
                BaseOntoContentType.Kind.AtomicProposition
        );
        defineJVMToDescriptor(jadescript.content.JadescriptAtomicProposition.class, ATOMIC_PROPOSITION);

        ACTION = new BaseOntoContentType(
                module,
                BaseOntoContentType.Kind.Action
        );
        defineJVMToDescriptor(jadescript.content.JadescriptAction.class, ACTION);

        INTERNAL_EXCEPTION = new UserDefinedOntoContentType(
                module,
                typeRef(jadescript.content.onto.basic.InternalException.class),
                PREDICATE
        );
        defineJVMToDescriptor(jadescript.content.onto.basic.InternalException.class, INTERNAL_EXCEPTION);

        ACCEPTPROPOSAL_MESSAGE = (args) -> new MessageSubType(
                module,
                AcceptProposalMessage.class,
                args,
                Arrays.asList(ACTION, PROPOSITION)
        );
        nameToPerformativeMap.put("AcceptProposalMessage", ACCEPT_PROPOSAL);
        messageSubTypeMap.put(ACCEPT_PROPOSAL, ACCEPTPROPOSAL_MESSAGE);
        messageContentTypeRequirements.put(ACCEPT_PROPOSAL, () -> TUPLE.apply(Arrays.asList(
                covariant(ACTION), covariant(PROPOSITION)
        )));
        defaultContentElementsMap.put(ACCEPT_PROPOSAL, new MessageContentTupleDefaultElements(2)
                .addEntry(1, PROPOSITION, MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                )));
        defineJVMToGenericDescriptor(AcceptProposalMessage.class, ACCEPTPROPOSAL_MESSAGE, 2);

        AGREE_MESSAGE = (args) -> new MessageSubType(
                module,
                AgreeMessage.class,
                args,
                Arrays.asList(ACTION, PROPOSITION)
        );
        nameToPerformativeMap.put("AgreeMessage", AGREE);
        messageSubTypeMap.put(AGREE, AGREE_MESSAGE);
        messageContentTypeRequirements.put(AGREE, () -> TUPLE.apply(Arrays.asList(
                covariant(ACTION), covariant(PROPOSITION)
        )));
        defaultContentElementsMap.put(AGREE, new MessageContentTupleDefaultElements(2)
                .addEntry(1, PROPOSITION, MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                )));
        defineJVMToGenericDescriptor(AgreeMessage.class, AGREE_MESSAGE, 2);

        CANCEL_MESSAGE = (args) -> new MessageSubType(
                module,
                CancelMessage.class,
                args,
                Arrays.asList(ACTION)
        );
        nameToPerformativeMap.put("CancelMessage", CANCEL);
        messageSubTypeMap.put(CANCEL, CANCEL_MESSAGE);
        messageContentTypeRequirements.put(CANCEL, () -> ACTION);

        defineJVMToGenericDescriptor(CancelMessage.class, CANCEL_MESSAGE, 1);

        CFP_MESSAGE = (args) -> new MessageSubType(
                module,
                CFPMessage.class,
                args,
                Arrays.asList(ACTION, PROPOSITION)
        );
        nameToPerformativeMap.put("CFPMessage", CFP);
        messageSubTypeMap.put(CFP, CFP_MESSAGE);
        messageContentTypeRequirements.put(CFP, () -> TUPLE.apply(Arrays.asList(
                covariant(ACTION), covariant(PROPOSITION)
        )));
        defaultContentElementsMap.put(CFP, new MessageContentTupleDefaultElements(2)
                .addEntry(1, PROPOSITION, MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                )));
        defineJVMToGenericDescriptor(CFPMessage.class, CFP_MESSAGE, 2);

        CONFIRM_MESSAGE = (args) -> new MessageSubType(
                module,
                ConfirmMessage.class,
                args,
                Arrays.asList(PROPOSITION)
        );
        nameToPerformativeMap.put("ConfirmMessage", CONFIRM);
        messageSubTypeMap.put(CONFIRM, CONFIRM_MESSAGE);
        messageContentTypeRequirements.put(CONFIRM, () -> PROPOSITION);
        defineJVMToGenericDescriptor(ConfirmMessage.class, CONFIRM_MESSAGE, 1);

        DISCONFIRM_MESSAGE = (args) -> new MessageSubType(
                module,
                DisconfirmMessage.class,
                args,
                Arrays.asList(PROPOSITION)
        );
        nameToPerformativeMap.put("DisconfirmMessage", DISCONFIRM);
        messageSubTypeMap.put(DISCONFIRM, DISCONFIRM_MESSAGE);
        messageContentTypeRequirements.put(DISCONFIRM, () -> PROPOSITION);
        defineJVMToGenericDescriptor(DisconfirmMessage.class, DISCONFIRM_MESSAGE, 1);

        FAILURE_MESSAGE = (args) -> new MessageSubType(
                module,
                FailureMessage.class,
                args,
                Arrays.asList(ACTION, PROPOSITION)
        );
        nameToPerformativeMap.put("FailureMessage", FAILURE);
        messageSubTypeMap.put(FAILURE, FAILURE_MESSAGE);
        messageContentTypeRequirements.put(FAILURE, () -> TUPLE.apply(Arrays.asList(
                covariant(ACTION), covariant(PROPOSITION)
        )));
        defaultContentElementsMap.put(FAILURE, new MessageContentTupleDefaultElements(2)
                .addEntry(1, PROPOSITION, MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                )));
        defineJVMToGenericDescriptor(FailureMessage.class, FAILURE_MESSAGE, 2);

        INFORM_MESSAGE = (args) -> new MessageSubType(
                module,
                InformMessage.class,
                args,
                Arrays.asList(PROPOSITION)
        );
        nameToPerformativeMap.put("InformMessage", INFORM);
        messageSubTypeMap.put(INFORM, INFORM_MESSAGE);
        messageContentTypeRequirements.put(INFORM, () -> PROPOSITION);
        defineJVMToGenericDescriptor(InformMessage.class, INFORM_MESSAGE, 1);

        INFORMIF_MESSAGE = (args) -> new MessageSubType(
                module,
                InformIfMessage.class,
                args,
                Arrays.asList(PROPOSITION)
        );
        nameToPerformativeMap.put("InformIfMessage", INFORM_IF);
        messageSubTypeMap.put(INFORM_IF, INFORMIF_MESSAGE);
        messageContentTypeRequirements.put(INFORM_IF, () -> PROPOSITION);
        defineJVMToGenericDescriptor(InformIfMessage.class, INFORMIF_MESSAGE, 1);

        INFORMREF_MESSAGE = (args) -> new MessageSubType(
                module,
                InformRefMessage.class,
                args,
                Arrays.asList(LIST.apply(Arrays.asList(covariant(CONCEPT))))
        );
        nameToPerformativeMap.put("InformRefMessage", INFORM_REF);
        messageSubTypeMap.put(INFORM_REF, INFORMREF_MESSAGE);
        messageContentTypeRequirements.put(INFORM_REF, () -> LIST.apply(Arrays.asList(covariant(CONCEPT))));
        defineJVMToGenericDescriptor(InformRefMessage.class, INFORMREF_MESSAGE, 1);

        NOTUNDERSTOOD_MESSAGE = (args) -> new MessageSubType(
                module,
                NotUnderstoodMessage.class,
                args,
                Arrays.asList(ANYMESSAGE, PROPOSITION)
        );
        nameToPerformativeMap.put("NotUnderstoodMessage", NOT_UNDERSTOOD);
        messageSubTypeMap.put(NOT_UNDERSTOOD, NOTUNDERSTOOD_MESSAGE);
        messageContentTypeRequirements.put(NOT_UNDERSTOOD, () -> TUPLE.apply(Arrays.asList(
                covariant(ANYMESSAGE), covariant(PROPOSITION)
        )));
        defaultContentElementsMap.put(NOT_UNDERSTOOD, new MessageContentTupleDefaultElements(2)
                .addEntry(1, PROPOSITION, MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                )));
        defineJVMToGenericDescriptor(NotUnderstoodMessage.class, NOTUNDERSTOOD_MESSAGE, 2);

        PROPOSE_MESSAGE = (args) -> new MessageSubType(
                module,
                ProposeMessage.class,
                args,
                Arrays.asList(ACTION, PROPOSITION)
        );
        nameToPerformativeMap.put("ProposeMessage", PROPOSE);
        messageSubTypeMap.put(PROPOSE, PROPOSE_MESSAGE);
        messageContentTypeRequirements.put(PROPOSE, () -> TUPLE.apply(Arrays.asList(
                covariant(ACTION), covariant(PROPOSITION)
        )));
        defaultContentElementsMap.put(PROPOSE, new MessageContentTupleDefaultElements(2)
                .addEntry(1, PROPOSITION, MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                )));
        defineJVMToGenericDescriptor(ProposeMessage.class, PROPOSE_MESSAGE, 2);

        QUERYIF_MESSAGE = (args) -> new MessageSubType(
                module,
                QueryIfMessage.class,
                args,
                Arrays.asList(PROPOSITION)
        );
        nameToPerformativeMap.put("QueryIfMessage", QUERY_IF);
        messageSubTypeMap.put(QUERY_IF, QUERYIF_MESSAGE);
        messageContentTypeRequirements.put(QUERY_IF, () -> PROPOSITION);
        defineJVMToGenericDescriptor(QueryIfMessage.class, QUERYIF_MESSAGE, 1);

        QUERYREF_MESSAGE = (args) -> new MessageSubType(
                module,
                QueryRefMessage.class,
                args,
                Arrays.asList(LIST.apply(Arrays.asList(covariant(CONCEPT))))
        );
        nameToPerformativeMap.put("QueryRefMessage", QUERY_REF);
        messageSubTypeMap.put(QUERY_REF, QUERYREF_MESSAGE);
        messageContentTypeRequirements.put(QUERY_REF, () -> LIST.apply(Arrays.asList(covariant(CONCEPT))));
        defineJVMToGenericDescriptor(QueryRefMessage.class, QUERYREF_MESSAGE, 1);

        REFUSE_MESSAGE = (args) -> new MessageSubType(
                module,
                RefuseMessage.class,
                args,
                Arrays.asList(ACTION, PROPOSITION)
        );
        nameToPerformativeMap.put("RefuseMessage", REFUSE);
        messageSubTypeMap.put(REFUSE, REFUSE_MESSAGE);
        messageContentTypeRequirements.put(REFUSE, () -> TUPLE.apply(Arrays.asList(
                covariant(ACTION), covariant(PROPOSITION)
        )));
        defaultContentElementsMap.put(REFUSE, new MessageContentTupleDefaultElements(2)
                .addEntry(1, PROPOSITION, MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                )));
        defineJVMToGenericDescriptor(RefuseMessage.class, REFUSE_MESSAGE, 2);

        REJECTPROPOSAL_MESSAGE = (args) -> new MessageSubType(
                module,
                RejectProposalMessage.class,
                args,
                Arrays.asList(ACTION, PROPOSITION, PROPOSITION)
        );
        nameToPerformativeMap.put("RejectProposalMessage", REJECT_PROPOSAL);
        messageSubTypeMap.put(REJECT_PROPOSAL, REJECTPROPOSAL_MESSAGE);
        messageContentTypeRequirements.put(REJECT_PROPOSAL, () -> TUPLE.apply(Arrays.asList(
                covariant(ACTION), covariant(PROPOSITION), covariant(PROPOSITION)
        )));
        defaultContentElementsMap.put(REJECT_PROPOSAL, new MessageContentTupleDefaultElements(3)
                .addEntry(1, PROPOSITION, MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                ))
                .addEntry(2, PROPOSITION, MessageContentTupleDefaultElements.addToTuple(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                )));
        defineJVMToGenericDescriptor(RejectProposalMessage.class, REJECTPROPOSAL_MESSAGE, 3);

        REQUEST_MESSAGE = (args) -> new MessageSubType(
                module,
                RequestMessage.class,
                args,
                Arrays.asList(ACTION)
        );
        nameToPerformativeMap.put("RequestMessage", REQUEST);
        messageSubTypeMap.put(REQUEST, REQUEST_MESSAGE);
        messageContentTypeRequirements.put(REQUEST, () -> ACTION);
        defineJVMToGenericDescriptor(RequestMessage.class, REQUEST_MESSAGE, 2);

        REQUESTWHEN_MESSAGE = (args) -> new MessageSubType(
                module,
                RequestWhenMessage.class,
                args,
                Arrays.asList(ACTION, PROPOSITION)
        );
        nameToPerformativeMap.put("RequestWhenMessage", REQUEST_WHEN);
        messageSubTypeMap.put(REQUEST_WHEN, REQUESTWHEN_MESSAGE);
        messageContentTypeRequirements.put(REQUEST_WHEN, () -> TUPLE.apply(Arrays.asList(
                covariant(ACTION), covariant(PROPOSITION)
        )));
        defineJVMToGenericDescriptor(RequestWhenMessage.class, REQUESTWHEN_MESSAGE, 2);

        REQUESTWHENEVER_MESSAGE = (args) -> new MessageSubType(
                module,
                RequestWheneverMessage.class,
                args,
                Arrays.asList(ACTION, PROPOSITION)
        );
        nameToPerformativeMap.put("RequestWheneverMessage", REQUEST_WHENEVER);
        messageSubTypeMap.put(REQUEST_WHENEVER, REQUESTWHENEVER_MESSAGE);
        messageContentTypeRequirements.put(REQUEST_WHENEVER, () -> TUPLE.apply(Arrays.asList(
                covariant(ACTION), covariant(PROPOSITION)
        )));
        defineJVMToGenericDescriptor(RequestWheneverMessage.class, REQUESTWHENEVER_MESSAGE, 2);

        SUBSCRIBE_MESSAGE = (args) -> new MessageSubType(
                module,
                SubscribeMessage.class,
                args,
                Arrays.asList(LIST.apply(Arrays.asList(covariant(CONCEPT))))
        );
        nameToPerformativeMap.put("SubscribeMessage", SUBSCRIBE);
        messageSubTypeMap.put(SUBSCRIBE, SUBSCRIBE_MESSAGE);
        messageContentTypeRequirements.put(SUBSCRIBE, () -> LIST.apply(Arrays.asList(covariant(CONCEPT))));
        defineJVMToGenericDescriptor(SubscribeMessage.class, SUBSCRIBE_MESSAGE, 1);

        PROXY_MESSAGE = (args) -> new MessageSubType(
                module,
                ProxyMessage.class,
                args,
                Arrays.asList(LIST.apply(Arrays.asList(AID)), ANYMESSAGE, PROPOSITION)
        );
        nameToPerformativeMap.put("ProxyMessage", PROXY);
        messageSubTypeMap.put(PROXY, PROXY_MESSAGE);
        messageContentTypeRequirements.put(PROXY, () -> TUPLE.apply(Arrays.asList(
                LIST.apply(Arrays.asList(AID)), covariant(ANYMESSAGE), covariant(PROPOSITION))));
        defaultContentElementsMap.put(PROXY, new MessageContentTupleDefaultElements(3)
                .addEntry(2, PROPOSITION, MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                )));
        defineJVMToGenericDescriptor(ProxyMessage.class, PROXY_MESSAGE, 3);

        PROPAGATE_MESSAGE = (args) -> new MessageSubType(
                module,
                PropagateMessage.class,
                args,
                Arrays.asList(LIST.apply(Arrays.asList(AID)), ANYMESSAGE, PROPOSITION)
        );
        nameToPerformativeMap.put("PropagateMessage", PROPAGATE);
        messageSubTypeMap.put(PROPAGATE, PROPAGATE_MESSAGE);
        messageContentTypeRequirements.put(PROPAGATE, () -> TUPLE.apply(Arrays.asList(
                LIST.apply(Arrays.asList(AID)), covariant(ANYMESSAGE), covariant(PROPOSITION))));
        defaultContentElementsMap.put(PROPAGATE, new MessageContentTupleDefaultElements(3)
                .addEntry(2, PROPOSITION, MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                )));
        defineJVMToGenericDescriptor(PropagateMessage.class, PROPAGATE_MESSAGE, 3);


        UNKNOWN_MESSAGE = (args) -> new MessageSubType(
                module,
                UnknownMessage.class,
                args,
                Arrays.asList(SERIALIZABLE)
        ) {
            @Override
            public boolean isSendable() {
                return false;
            }
        };
        nameToPerformativeMap.put("UnknownMessage", UNKNOWN);
        messageSubTypeMap.put(UNKNOWN, UNKNOWN_MESSAGE);
        messageContentTypeRequirements.put(UNKNOWN, () -> SERIALIZABLE);
        defineJVMToGenericDescriptor(UnknownMessage.class, UNKNOWN_MESSAGE, 1);


    }

    public BaseMessageType instantiateMessageType(
            Maybe<String> performative,
            IJadescriptType computedContentType,
            boolean normalizeToUpperBounds
    ) {
        final Maybe<? extends Function<List<TypeArgument>, ? extends BaseMessageType>> functionMaybe
                = performative.__(performativeByName::get).__(this::getMessageType);
        List<TypeArgument> resultTypeArgs;
        if (computedContentType instanceof TupleType) {
            resultTypeArgs = new ArrayList<>(((TupleType) computedContentType).getTypeArguments());
        } else {
            resultTypeArgs = List.of(computedContentType);
        }

        if (normalizeToUpperBounds) {
            resultTypeArgs = limitMsgContentTypesToUpperBounds(
                    performative.__(performativeByName::get), resultTypeArgs);
        }

        if (functionMaybe.isPresent()) {
            return functionMaybe.toNullable().apply(resultTypeArgs);
        } else {
            return this.MESSAGE.apply(resultTypeArgs);
        }
    }

    private List<TypeArgument> limitMsgContentTypesToUpperBounds(
            Maybe<Performative> performative,
            List<TypeArgument> arguments
    ) {
        if (performative == null || performative.isNothing() || performative.wrappedEquals(UNKNOWN)) {
            return arguments;
        }

        List<TypeArgument> result = new ArrayList<>(arguments.size());
        final IJadescriptType requiredContentType = messageContentTypeRequirements.get(performative.toNullable()).get();
        final List<IJadescriptType> requiredTypes;
        if (requiredContentType instanceof TupleType) {
            requiredTypes = ((TupleType) requiredContentType).getElementTypes();
        } else {
            requiredTypes = List.of(requiredContentType);
        }
        int i;
        for (i = 0; i < Math.min(requiredTypes.size(), arguments.size()); i++) {
            final IJadescriptType glb = getGLB(requiredTypes.get(i), arguments.get(i).ignoreBound());
            if (glb.typeEquals(requiredTypes.get(i))) {
                result.add(covariant(glb));
            } else {
                result.add(glb);
            }
        }
        if (i < requiredTypes.size()) {
            for (; i < requiredTypes.size(); i++) {
                result.add(covariant(requiredTypes.get(i)));
            }
        }

        return result;

    }

    public Function<List<TypeArgument>, ? extends BaseMessageType> getMessageType(
            /* Nullable */ Performative performative
    ) {
        if (performative == null) {
            return MESSAGE;
        }
        return messageSubTypeMap.get(performative);
    }

    public Function<List<TypeArgument>, ? extends BaseMessageType> getMessageType(
            String performative
    ) {
        return getMessageType(nameToPerformativeMap.get(performative));
    }

    public List<TypeArgument> getDefaultTypeArguments(
            String name
    ) {
        if (name.equals("Message")) {
            return Collections.singletonList(ANY);
        }
        final Performative performative = nameToPerformativeMap.get(name);
        final Supplier<IJadescriptType> supplyDefaultArg = messageContentTypeRequirements.get(performative);
        if (supplyDefaultArg == null) {
            return Collections.emptyList();
        }
        final IJadescriptType jadescriptType = supplyDefaultArg.get();
        if (jadescriptType instanceof TupleType) {
            return ((TupleType) jadescriptType).getTypeArguments();
        }
        return Collections.singletonList(jadescriptType);
    }

    public IJadescriptType getContentBound(Performative performative) {
        return messageContentTypeRequirements.get(performative).get();
    }



    private IJadescriptType jtFromJvmTypeRefWithoutReattempts(
            JvmTypeReference reference
    ) {
        final Maybe<IJadescriptType> fromJVMTypeReference = this.getFromJVMTypeReference(reference);

        IJadescriptType result;
        if (fromJVMTypeReference.isPresent()) {
            result = fromJVMTypeReference.toNullable();
        } else if (isAssignable(Tuple.class, reference) && reference instanceof JvmParameterizedTypeReference) {
            result = resolveTupleType(((JvmParameterizedTypeReference) reference));
        } else {
            result = resolveNonBuiltinType(reference);
        }
        return result;
    }

    public IJadescriptType jtFromJvmTypeRef(
            JvmTypeReference reference
    ) {
        IJadescriptType result = jtFromJvmTypeRefWithoutReattempts(reference);

        if (result.isJavaVoid()) {
            ICompositeNode node = NodeModelUtils.getNode(reference);
            if (node == null) {
                node = NodeModelUtils.findActualNodeFor(reference);
            }

            if (node != null) {
                final INode finalNode = node;
                final JvmTypeReference reattempt = module.get(ContextManager.class).currentContext()
                        .searchAs(
                                RawTypeReferenceSolverContext.class,
                                solver -> solver.rawResolveTypeReference(finalNode.getText().trim())
                        )
                        .findAny()
                        .orElse(typeRef(finalNode.getText()));

                return jtFromJvmTypeRefWithoutReattempts(reattempt);
            }
        }
        return result;
    }

    private IJadescriptType resolveTupleType(JvmParameterizedTypeReference reference) {
        List<TypeArgument> args = new ArrayList<>();
        for (JvmTypeReference arg : reference.getArguments()) {
            IJadescriptType typeDescriptor = jtFromJvmTypeRef(arg);
            args.add(typeDescriptor);
        }
        return TUPLE.apply(args);
    }

    public BoundedTypeArgument covariant(IJadescriptType jadescriptType) {
        return new BoundedTypeArgument(module, jadescriptType, BoundedTypeArgument.Variance.EXTENDS);
    }

    public BoundedTypeArgument contravariant(IJadescriptType jadescriptType) {
        return new BoundedTypeArgument(module, jadescriptType, BoundedTypeArgument.Variance.SUPER);
    }


    private IJadescriptType resolveNonBuiltinType(JvmTypeReference reference) {
        if (isAssignable(JadescriptConcept.class, reference)) {
            return new UserDefinedOntoContentType(
                    module,
                    reference,
                    CONCEPT
            );
        } else if (isAssignable(JadescriptAction.class, reference)) {
            return new UserDefinedOntoContentType(
                    module,
                    reference,
                    ACTION
            );
        } else if (isAssignable(JadescriptPredicate.class, reference)) {
            return new UserDefinedOntoContentType(
                    module,
                    reference,
                    PREDICATE
            );
        } else if (isAssignable(JadescriptAtomicProposition.class, reference)) {
            return new UserDefinedOntoContentType(
                    module,
                    reference,
                    ATOMIC_PROPOSITION
            );
        } else if (isAssignable(JadescriptProposition.class, reference)) {
            return new UserDefinedOntoContentType(
                    module,
                    reference,
                    PROPOSITION
            );
        } else if (isAssignable(Agent.class, reference)) {
            return new UserDefinedAgentType(
                    module,
                    reference,
                    AGENT
            );
        } else if (isAssignable(Cyclic.class, reference)) {
            final List<JvmTypeReference> args = getTypeArgumentsOfParent(reference, typeRef(CyclicBehaviour.class));
            if (args.isEmpty()) {
                args.add(AGENT.asJvmTypeReference());
            }
            if (args.size() == 1) {
                return new UserDefinedBehaviourType(
                        module,
                        reference,
                        CYCLIC_BEHAVIOUR.apply(Arrays.asList(
                                jtFromJvmTypeRef(args.get(0)))
                        )
                );
            }
        } else if (isAssignable(OneShot.class, reference)) {
            final List<JvmTypeReference> args = getTypeArgumentsOfParent(reference, typeRef(OneShotBehaviour.class));
            if (args.isEmpty()) {
                args.add(AGENT.asJvmTypeReference());
            }
            if (args.size() == 1) {
                return new UserDefinedBehaviourType(
                        module,
                        reference,
                        ONESHOT_BEHAVIOUR.apply(Arrays.asList(
                                jtFromJvmTypeRef(args.get(0)))
                        )
                );
            }
        } else if (isAssignable(Base.class, reference)) {
            final List<JvmTypeReference> args = getTypeArgumentsOfParent(reference, typeRef(Behaviour.class));
            if (args.isEmpty()) {
                args.add(AGENT.asJvmTypeReference());
            }
            if (args.size() == 1) {
                return new UserDefinedBehaviourType(
                        module,
                        reference,
                        BEHAVIOUR.apply(Arrays.asList(
                                jtFromJvmTypeRef(args.get(0)))
                        )
                );
            }
        } else if (isAssignable(Ontology.class, reference)) {
            return new UserDefinedOntologyType(
                    module,
                    reference,
                    ONTOLOGY
            );
        }
        return new UnknownJVMType(module, reference);

    }

    public IJadescriptType jtFromJvmType(JvmDeclaredType itClass, JvmTypeReference... typeArguments) {
        return jtFromJvmTypeRef(this.typeRef(itClass, typeArguments));
    }

    public IJadescriptType jtFromClass(
            Class<?> class_,
            List<IJadescriptType> arguments
    ) {
        return jtFromJvmTypeRef(this.typeRef(class_, arguments.stream()
                .map(IJadescriptType::asJvmTypeReference)
                .collect(Collectors.toList())));
    }

    public IJadescriptType jtFromClass(
            Class<?> class_,
            IJadescriptType... arguments
    ) {
        return jtFromClass(class_, Arrays.asList(arguments));
    }


    private void defineJVMToDescriptor(Class<?> type, IJadescriptType descriptor) {
        defaultJVMToDescriptorTable.put(this.typeRef(type).getQualifiedName('.'), descriptor);
    }


    private void defineJVMToGenericDescriptor(
            JvmTypeReference typeReference,
            Function<List<TypeArgument>, ? extends IJadescriptType> descriptor,
            int expectedArguments
    ) {

        defaultJVMToGenericDescriptorTable.put(
                noGenericsTypeName(typeReference.getQualifiedName('.')),
                descriptor
        );
        expectedGenericDescriptorArguments.put(
                noGenericsTypeName(typeReference.getQualifiedName('.')),
                expectedArguments
        );
    }

    private void defineJVMToGenericDescriptor(
            Class<?> type,
            Function<List<TypeArgument>, ? extends IJadescriptType> descriptor,
            int expectedArguments
    ) {
        defineJVMToGenericDescriptor(this.typeRef(type), descriptor, expectedArguments);
    }


    private void defineImplicitConversionPath(
            IJadescriptType from,
            IJadescriptType to,
            Function<String, String> conversionCompilationMethod
    ) {
        implicitConversions.add(new ImplicitConversionDefinition(from, to, conversionCompilationMethod));
    }


    public IJadescriptType jtFromFullyQualifiedName(String fullyQualifiedName) {
        return jtFromJvmTypeRef(typeRef(fullyQualifiedName));
    }

    private Maybe<IJadescriptType> getFromJVMTypeReference(JvmTypeReference typeReference) {

        final String qualifiedName = typeReference.getQualifiedName('.');
        if (defaultJVMToDescriptorTable.containsKey(qualifiedName)) {
            return some(defaultJVMToDescriptorTable.get(qualifiedName));
        } else {
            final String noGenericsTypeName = noGenericsTypeName(qualifiedName);
            if (defaultJVMToGenericDescriptorTable.containsKey(noGenericsTypeName)
                    && typeReference instanceof JvmParameterizedTypeReference) {
                List<TypeArgument> args = new ArrayList<>();
                for (JvmTypeReference arg : ((JvmParameterizedTypeReference) typeReference).getArguments()) {
                    IJadescriptType typeDescriptor = jtFromJvmTypeRef(arg);
                    args.add(typeDescriptor);
                }
                final Integer expectedArguments = expectedGenericDescriptorArguments.get(noGenericsTypeName);
                if (expectedArguments != null && expectedArguments == args.size()) {
                    return some(defaultJVMToGenericDescriptorTable.get(noGenericsTypeName).apply(args));
                } else {
                    return nothing();
                }
            } else {
                return nothing();
            }
        }
    }


    public boolean implicitConversionCanOccur(IJadescriptType from, IJadescriptType to) {
        if (from.typeEquals(to)) {
            return true;
        }
        Set<IJadescriptType> visited = new HashSet<>();

        //BFS
        LinkedList<IJadescriptType> queue = new LinkedList<>();
        visited.add(from);
        queue.add(from);
        while (queue.size() > 0) {
            IJadescriptType td = queue.poll();
            for (ImplicitConversionDefinition def : implicitConversions) {
                if (def.getFrom().typeEquals(td)) {
                    IJadescriptType t = def.getTo();
                    if (t.typeEquals(to)) {
                        return true;
                    }
                    if (!visited.contains(t)) {
                        queue.addLast(t);
                    }
                }
            }
        }
        return false;
    }

    public String compileImplicitConversion(
            String compileExpression,
            IJadescriptType argType,
            IJadescriptType destType
    ) {
        List<ImplicitConversionDefinition> list = implicitConversionPath(argType, destType);
        String result = compileExpression;
        for (ImplicitConversionDefinition conv : list) {
            result = conv.compileConversion(result);
        }
        return result;
    }

    public String compileWithEventualImplicitConversions(
            String compiledExpression,
            IJadescriptType argType,
            IJadescriptType destType
    ) {
        if (implicitConversionCanOccur(argType, destType)) {
            return compileImplicitConversion(compiledExpression, argType, destType);
        } else {
            return compiledExpression;
        }
    }

    public Maybe<OntologyType> getOntologyGLB(Maybe<OntologyType> mt1, Maybe<OntologyType> mt2, List<Maybe<OntologyType>> mts) {//FUTURETODO multiple ontologies
        Maybe<OntologyType> result = getOntologyGLB(mt1, mt2);
        for (Maybe<OntologyType> mt : mts) {
            if (result.isNothing()) {
                return nothing();
            }
            result = getOntologyGLB(result, mt);
        }
        return result;
    }

    public Maybe<OntologyType> getOntologyGLB(Maybe<OntologyType> mt1, Maybe<OntologyType> mt2) {//FUTURETODO multiple ontologies
        if (mt1.isNothing()) {
            return nothing();
        }
        if (mt2.isNothing()) {
            return nothing();
        }
        final OntologyType t1 = mt1.toNullable();
        final OntologyType t2 = mt2.toNullable();
        if (t1.isSuperOrEqualOntology(t2)) {
            return some(t2);
        } else if (t2.isSuperOrEqualOntology(t1)) {
            return some(t1);
        } else {
            return nothing();
        }
    }

    public IJadescriptType adaptMessageContentDefaultTypes(Maybe<String> performative, IJadescriptType inputContentType) {
        if (performative.isNothing() || performative.toNullable().isBlank()) {
            return inputContentType;
        }
        final Performative perf = performativeByName.getOrDefault(performative.toNullable(), UNKNOWN);
        final MessageContentTupleDefaultElements messageContentTupleDefaultElements = defaultContentElementsMap.get(perf);
        if (messageContentTupleDefaultElements == null) {
            return inputContentType;
        } else if (inputContentType instanceof TupleType) {
            final List<IJadescriptType> inputElementTypes = ((TupleType) inputContentType).getElementTypes();
            final int inputArgsCount = inputElementTypes.size();
            final int requiredArgCount = messageContentTupleDefaultElements.getTargetCount()
                    - messageContentTupleDefaultElements.getDefaultCount();
            if (inputArgsCount >= requiredArgCount
                    && inputArgsCount < messageContentTupleDefaultElements.getTargetCount()) {
                List<TypeArgument> elements = new ArrayList<>(messageContentTupleDefaultElements.getTargetCount());
                for (int i = 0; i < messageContentTupleDefaultElements.getTargetCount(); i++) {
                    if (i < inputArgsCount) {
                        elements.add(inputElementTypes.get(i));
                    } else {
                        elements.add(covariant(messageContentTupleDefaultElements.getDefaultType(i).orElse(ANY)));
                    }
                }
                return TUPLE.apply(elements);
            } else {
                return inputContentType;
            }
        } else {
            final int requiredArgCount = messageContentTupleDefaultElements.getTargetCount()
                    - messageContentTupleDefaultElements.getDefaultCount();
            if (requiredArgCount <= 1) {
                List<TypeArgument> elements = new ArrayList<>(messageContentTupleDefaultElements.getTargetCount());
                for (int i = 0; i < messageContentTupleDefaultElements.getTargetCount(); i++) {
                    if (i == 0) {
                        elements.add(inputContentType);
                    } else {
                        elements.add(covariant(messageContentTupleDefaultElements.getDefaultType(i).orElse(ANY)));
                    }
                }
                return TUPLE.apply(elements);
            } else {
                return inputContentType;
            }
        }
    }

    public String adaptMessageContentDefaultCompile(
            Maybe<String> performative,
            IJadescriptType inputContentType,
            String inputExpression
    ) {
        if (performative.isNothing() || performative.toNullable().isBlank()) {
            return inputExpression;
        }
        final Performative perf = performativeByName.getOrDefault(performative.toNullable(), UNKNOWN);
        final MessageContentTupleDefaultElements messageContentTupleDefaultElements = defaultContentElementsMap.get(perf);
        if (messageContentTupleDefaultElements == null) {
            return inputExpression;
        } else {
            final int inputArgsCount = inputContentType instanceof TupleType
                    ? ((TupleType) inputContentType).getElementTypes().size()
                    : 1;
            String result = inputExpression;
            for (int i = inputArgsCount; i < messageContentTupleDefaultElements.getTargetCount(); i++) {
                result = messageContentTupleDefaultElements.compile(i, inputContentType, result);
            }
            return result;
        }
    }

    /**
     * If {@code t} is a Tuple type, it returns the types of its elements. Otherwise, it returns a singleton list
     * containing {@code t}.
     */
    public List<? extends TypeArgument> unpackTuple(TypeArgument t){
        if (t instanceof TupleType) {
            return ((TupleType) t).getTypeArguments();
        }else{
            return List.of(t);
        }
    }

    private static class Vertex implements Comparable<Vertex> {

        private final List<Edge> adjacents = new ArrayList<>();
        private boolean visited = false;
        private Edge linkToPredecessor = null;
        private int distance = Integer.MAX_VALUE;
        private final IJadescriptType type;

        Vertex(IJadescriptType type) {
            this.type = type;
        }


        public int getDistance() {
            return distance;
        }

        public void setDistance(int distance) {
            this.distance = distance;
        }

        public List<Edge> getAdjacents() {
            return adjacents;
        }

        public boolean isVisited() {
            return visited;
        }

        public void setVisited(boolean visited) {
            this.visited = visited;
        }

        public Edge getLinkToPredecessor() {
            return linkToPredecessor;
        }

        public void setLinkToPredecessor(Edge linkToPredecessor) {
            this.linkToPredecessor = linkToPredecessor;
        }

        @SuppressWarnings("unused")
        public IJadescriptType getType() {
            return type;
        }

        @Override
        public int compareTo(Vertex o) {
            return Integer.compare(this.distance, o.distance);
        }
    }

    private static class Edge {

        private final Vertex from;
        private final Vertex to;
        private final ImplicitConversionDefinition definition;

        public Edge(Vertex from, Vertex to, ImplicitConversionDefinition definition) {
            this.from = from;
            this.to = to;
            this.definition = definition;
        }

        public Vertex getFrom() {
            return from;
        }

        public Vertex getTo() {
            return to;
        }

        public ImplicitConversionDefinition getDefinition() {
            return definition;
        }
    }

    public List<ImplicitConversionDefinition> implicitConversionPath(IJadescriptType start, IJadescriptType end) {
        if (start.typeEquals(end)) {
            return Arrays.asList(new ImplicitConversionDefinition(start, end, (e) -> e));
        }
        final Map<String, Vertex> map = new HashMap<>();
        for (ImplicitConversionDefinition edge : implicitConversions) {
            IJadescriptType from = edge.getFrom();
            IJadescriptType to = edge.getTo();

            final Vertex fromV = map.computeIfAbsent(edge.getFrom().getID(), (__) -> new Vertex(from));
            final Vertex toV = map.computeIfAbsent(edge.getTo().getID(), (__) -> new Vertex(to));

            fromV.getAdjacents().add(new Edge(fromV, toV, edge));
        }

        Vertex startVertex = map.get(start.getID());
        Vertex endVertex = map.get(end.getID());

        //algo
        startVertex.setDistance(0);
        PriorityQueue<Vertex> priorityQueue = new PriorityQueue<>();
        priorityQueue.add(startVertex);
        startVertex.setVisited(true);

        while (!priorityQueue.isEmpty()) {
            Vertex currentVertex = priorityQueue.poll();

            for (Edge edge : currentVertex.getAdjacents()) {
                Vertex vertex = edge.getTo();
                if (!vertex.isVisited()) {
                    int newDistance = currentVertex.getDistance() + 1;
                    if (newDistance < vertex.getDistance()) {
                        priorityQueue.remove(vertex);
                        vertex.setDistance(newDistance);
                        vertex.setLinkToPredecessor(edge);
                        priorityQueue.add(vertex);
                    }
                }
            }
            currentVertex.setVisited(true);
        }

        List<Edge> path = new ArrayList<>();
        for (Edge edge = endVertex.getLinkToPredecessor(); edge != null; edge = edge.getFrom().getLinkToPredecessor()) {
            path.add(edge);
        }

        Collections.reverse(path);

        return path.stream().map(Edge::getDefinition).collect(Collectors.toList());
    }


    public String noGenericsTypeName(String type) {
        if (type == null) {
            return "";
        }
        int endIndex = type.indexOf('<');
        if (endIndex < 0) {
            return type;
        }
        return type.substring(0, endIndex);
    }

    private static String boxedName(String input) {
        switch (input) {
            case JAVA_PRIMITIVE_int:
                return JAVA_TYPE_Integer;
            case JAVA_PRIMITIVE_float:
                return JAVA_TYPE_Float;
            case JAVA_PRIMITIVE_long:
                return JAVA_TYPE_Long;
            case JAVA_PRIMITIVE_char:
                return JAVA_TYPE_Character;
            case JAVA_PRIMITIVE_short:
                return JAVA_TYPE_Short;
            case JAVA_PRIMITIVE_double:
                return JAVA_TYPE_Double;
            case JAVA_PRIMITIVE_boolean:
                return JAVA_TYPE_Boolean;
            case JAVA_PRIMITIVE_byte:
                return JAVA_TYPE_Byte;
            default:
                return input;
        }
    }

    public static String unBoxedName(String input) {
        switch (input) {
            case JAVA_TYPE_Integer:
                return JAVA_PRIMITIVE_int;
            case JAVA_TYPE_Float:
                return JAVA_PRIMITIVE_float;
            case JAVA_TYPE_Long:
                return JAVA_PRIMITIVE_long;
            case JAVA_TYPE_Character:
                return JAVA_PRIMITIVE_char;
            case JAVA_TYPE_Short:
                return JAVA_PRIMITIVE_short;
            case JAVA_TYPE_Double:
                return JAVA_PRIMITIVE_double;
            case JAVA_TYPE_Boolean:
                return JAVA_PRIMITIVE_boolean;
            case JAVA_TYPE_Byte:
                return JAVA_PRIMITIVE_byte;
            default:
                return input;
        }
    }

    public static boolean typeReferenceEquals(JvmTypeReference a, JvmTypeReference b) {
        return boxedName(a.getQualifiedName('.')).equals(boxedName(b.getQualifiedName('.')));
    }

    public static boolean typeReferenceEquals(Class<?> a, JvmTypeReference b) {
        return boxedName(a.getName()).equals(boxedName(b.getQualifiedName('.')));
    }

    public static String extractPackageName(JvmTypeReference jtr) {
        String[] split = jtr.getQualifiedName().split("\\.");
        StringBuilder packageName = new StringBuilder();
        for (int i = 0; i < split.length - 1; i++) {
            String s = split[i];
            packageName.append(s);
        }
        return packageName.toString();
    }

    public JvmTypeReference objectTypeRef() {
        return module.get(JvmTypeReferenceBuilder.class).typeRef(Object.class);
    }

    public JvmTypeReference typeRef(EObject eObject, JvmTypeReference... typeParameters) {
        return typeRef(
                module.get(IQualifiedNameProvider.class)
                        .getFullyQualifiedName(eObject)
                        .toString("."),
                typeParameters
        );
    }

    public JvmTypeReference typeRef(Class<?> objectClass, JvmTypeReference... typeParameters) {
        return module.get(JvmTypeReferenceBuilder.class).typeRef(objectClass, typeParameters);
    }

    public JvmTypeReference typeRef(Class<?> objectClass, List<JvmTypeReference> typeParameters) {
        return module.get(JvmTypeReferenceBuilder.class).typeRef(objectClass, typeParameters.toArray(new JvmTypeReference[0]));
    }

    public JvmTypeReference typeRef(JvmType componentType, JvmTypeReference... typeArgs) {
        return module.get(JvmTypeReferenceBuilder.class).typeRef(componentType, typeArgs);
    }

    public JvmTypeReference typeRef(JvmType componentType, List<JvmTypeReference> typeArgs) {
        return module.get(JvmTypeReferenceBuilder.class).typeRef(componentType, typeArgs.toArray(new JvmTypeReference[0]));
    }

    public JvmTypeReference typeRef(String ident, JvmTypeReference... typeArgs) {
        return module.get(JvmTypeReferenceBuilder.class).typeRef(ident, typeArgs);
    }

    public static JvmTypeReference attemptResolveTypeRef(SemanticsModule module, JvmTypeReference typeReference) {
        final JvmTypeQualifiedNameParser.GenericType type = JvmTypeQualifiedNameParser
                .parseJvmGenerics(typeReference.getIdentifier());
        if (type == null) {
            return typeReference;
        }
        return type.convertToTypeRef(
                module.get(JvmTypeReferenceBuilder.class)::typeRef,
                module.get(JvmTypeReferenceBuilder.class)::typeRef
        );
    }

    public boolean isPrimitiveWideningViable(JvmTypeReference from, JvmTypeReference to) {
        if (from == null || to == null) {
            return false;
        } else {
            if (typeReferenceEquals(from, to)) {
                return true;
            } else if (typeReferenceEquals(to, typeRef(Double.class))) {
                return isPrimitiveWideningViable(from, typeRef(Float.class));
            } else if (typeReferenceEquals(to, typeRef(Float.class))) {
                return isPrimitiveWideningViable(from, typeRef(Long.class));
            } else if (typeReferenceEquals(to, typeRef(Long.class))) {
                return isPrimitiveWideningViable(from, typeRef(Integer.class));
            } else if (typeReferenceEquals(to, typeRef(Integer.class))) {
                return isPrimitiveWideningViable(from, typeRef(Short.class));
            } else if (typeReferenceEquals(to, typeRef(Short.class))) {
                return isPrimitiveWideningViable(from, typeRef(Byte.class));
            }
        }

        return false;
    }

    public JvmTypeReference boxedReference(JvmPrimitiveType primitiveType) {
        switch (primitiveType.getSimpleName()) {
            case JAVA_PRIMITIVE_int:
                return typeRef(Integer.class);
            case JAVA_PRIMITIVE_float:
                return typeRef(Float.class);
            case JAVA_PRIMITIVE_long:
                return typeRef(Long.class);
            case JAVA_PRIMITIVE_char:
                return typeRef(Character.class);
            case JAVA_PRIMITIVE_short:
                return typeRef(Short.class);
            case JAVA_PRIMITIVE_double:
                return typeRef(Double.class);
            case JAVA_PRIMITIVE_boolean:
                return typeRef(Boolean.class);
            case JAVA_PRIMITIVE_byte:
                return typeRef(Byte.class);
            default:
                return typeRef(primitiveType);
        }
    }

    public boolean isAssignable(IJadescriptType toType, IJadescriptType fromType) {
        return toType.isAssignableFrom(fromType);
    }

    public boolean isAssignable(Class<?> toType, IJadescriptType fromType) {
        return jtFromClass(toType).isAssignableFrom(fromType);
    }

    public boolean isAssignable(Class<?> toType, JvmTypeReference fromType) {
        return isAssignable(typeRef(toType), fromType);
    }

    public boolean isAssignable(JvmTypeReference toType, Class<?> fromType) {
        return isAssignable(toType, typeRef(fromType));
    }

    /**
     * Determines if the class or interface represented by the <code>toType</code> type reference
     * is either the same as, or is a superclass or superinterface of, the class or interface
     * represented by <code>fromType</code> type reference. It tries to respond to the question,
     * in the context of the JVM type system, "can a Java instance of the type referenced by
     * <code>fromType</code> be assigned to a Java reference of the type referenced by
     * <code>toType</code>?".
     */
    public boolean isAssignable(JvmTypeReference toType, JvmTypeReference fromType) {
        JvmTypeReference left = toType;
        JvmTypeReference right = fromType;
        if (fromType == null || toType == null) {
            return false;
        }
        if (fromType.getIdentifier() == null
                || toType.getIdentifier() == null
                || fromType.getIdentifier().equals("void")
                || toType.getIdentifier().equals("void")) {
            return false;
        }
        if (isPrimitiveWideningViable(fromType, toType)) {
            return true;
        }
        if (toType.getType() instanceof JvmPrimitiveType) {
            left = boxedReference((JvmPrimitiveType) toType.getType());
        }
        if (fromType.getType() instanceof JvmPrimitiveType) {
            right = boxedReference((JvmPrimitiveType) fromType.getType());
        }
        if (typeReferenceEquals(left, right))
            return true;

        if (left.getQualifiedName().equals("jadescript.lang.Tuple")) {
            //ad hoc fix for tuple types
            return right.getQualifiedName('.').startsWith("jadescript.lang.Tuple") && right.getSimpleName().startsWith("Tuple");
        }

        if (left.getType() instanceof JvmDeclaredType && right.getType() instanceof JvmDeclaredType) {
            if (left instanceof JvmParameterizedTypeReference) {
                JvmParameterizedTypeReference leftJvmptr = (JvmParameterizedTypeReference) left;
                if (!(right instanceof JvmParameterizedTypeReference)) {
                    return false;
                } else {
                    JvmParameterizedTypeReference rightJvmptr = (JvmParameterizedTypeReference) right;
                    if (leftJvmptr.getArguments().size() != rightJvmptr.getArguments().size()) {
                        return false;
                    } else {
                        for (int i = 0; i < leftJvmptr.getArguments().size(); ++i) {
                            if (!typeReferenceEquals(leftJvmptr.getArguments().get(i), rightJvmptr.getArguments().get(i))) {
                                return false;
                            }
                        }
                        //all type parameters are the same
                    }
                }
            }
            return left.getType().equals(right.getType()) ||
                    JvmTypeReferenceSet.generateAllSupertypesSet((JvmDeclaredType) right.getType()).contains(left);
        } else if (left instanceof JvmGenericArrayTypeReference && right instanceof JvmGenericArrayTypeReference) {
            //if left and right are array types, just see if their component types matches isAssignable
            return isAssignable(getArrayListMapComponentType(left), getArrayListMapComponentType(right));
        } else if (left instanceof JvmGenericArrayTypeReference && right.getType() instanceof JvmDeclaredType ||
                left.getType() instanceof JvmDeclaredType && right instanceof JvmGenericArrayTypeReference) {
            //one is array, the other is not: not assignable
            return false;
        } else {
            return false;
        }
    }

    public JvmTypeReference getArrayListMapComponentType(
        JvmTypeReference arrayOrListType
    ) {
        if (arrayOrListType instanceof JvmGenericArrayTypeReference) {
            return ((JvmGenericArrayTypeReference) arrayOrListType).getComponentType();
        } else if (arrayOrListType instanceof JvmParameterizedTypeReference) {
            JvmParameterizedTypeReference genericReference = (JvmParameterizedTypeReference) arrayOrListType;
            if (genericReference.getArguments().size() == 1 && (isAssignable(typeRef(List.class, genericReference
                    .getArguments().toArray(new JvmTypeReference[0])), genericReference)
                    || isAssignable(typeRef(Set.class, genericReference
                    .getArguments().toArray(new JvmTypeReference[0])), genericReference))) {
                return genericReference.getArguments().get(0);
            } else if (genericReference.getArguments().size() == 2 && isAssignable(typeRef(Map.class, genericReference
                    .getArguments().toArray(new JvmTypeReference[0])), genericReference)) {
                return genericReference.getArguments().get(1);
            }
        }
        return this.typeRef(Object.class);
    }

    public boolean isSet(JvmTypeReference collectionType) {
        return collectionType instanceof JvmParameterizedTypeReference && (
                ((JvmParameterizedTypeReference) collectionType).getArguments().size() == 1 &&
                        isAssignable(typeRef(Set.class, ((JvmParameterizedTypeReference) collectionType)
                                .getArguments().toArray(new JvmTypeReference[0])), collectionType)
        );
    }

    public boolean isArrayOrList(JvmTypeReference collectionType) {
        return collectionType instanceof JvmGenericArrayTypeReference ||
                collectionType instanceof JvmParameterizedTypeReference && (
                        ((JvmParameterizedTypeReference) collectionType).getArguments().size() == 1 &&
                                isAssignable(typeRef(List.class, ((JvmParameterizedTypeReference) collectionType)
                                        .getArguments().toArray(new JvmTypeReference[0])), collectionType)
                );
    }


    public boolean isMap(JvmTypeReference collectionType) {
        return collectionType instanceof JvmParameterizedTypeReference &&
                ((JvmParameterizedTypeReference) collectionType).getArguments().size() == 2 &&
                isAssignable(typeRef(Map.class, ((JvmParameterizedTypeReference) collectionType)
                        .getArguments().toArray(new JvmTypeReference[0])), collectionType);
    }

    public boolean isMessage(JvmTypeReference messageType) {
        return messageType instanceof JvmParameterizedTypeReference &&
                ((JvmParameterizedTypeReference) messageType).getArguments().size() == 1 &&
                isAssignable(typeRef(jadescript.core.message.Message.class, ((JvmParameterizedTypeReference) messageType)
                        .getArguments().toArray(new JvmTypeReference[0])), messageType);
    }

    public boolean isArray(JvmTypeReference collectionType) {
        return collectionType instanceof JvmGenericArrayTypeReference;
    }

    public FlowTypeInferringTerm getGLB(FlowTypeInferringTerm t1, FlowTypeInferringTerm t2) {
        if (t1.getType().isAssignableFrom(t2.getType())) {
            return getGLBBetweenDerivedTypes(t1, t2);
        } else if (t2.getType().isAssignableFrom(t1.getType())) {
            return getGLBBetweenDerivedTypes(t2, t1);
        } else {
            if (!t1.isNegated()) {
                if (!t2.isNegated()) {
                    return FlowTypeInferringTerm.of(NOTHING);
                } else {
                    return t1;
                }
            } else {
                if (!t2.isNegated()) {
                    return t2;
                } else {
                    return FlowTypeInferringTerm.of(ANY);
                }
            }
        }
    }

    private FlowTypeInferringTerm getGLBBetweenDerivedTypes(FlowTypeInferringTerm parent, FlowTypeInferringTerm childOrEqual) {
        if (!parent.isNegated()) {
            if (!childOrEqual.isNegated()) {
                return childOrEqual;
            } else {
                return parent;
            }
        } else {
            if (!childOrEqual.isNegated()) {
                return FlowTypeInferringTerm.of(NOTHING);
            } else {
                return FlowTypeInferringTerm.of(ANY);
            }
        }
    }

    public FlowTypeInferringTerm getLUB(FlowTypeInferringTerm t1, FlowTypeInferringTerm t2) {
        if (t1.isNegated() || t2.isNegated()) {
            return FlowTypeInferringTerm.of(ANY);
        } else {
            return FlowTypeInferringTerm.of(getLUB(t1.getType(), t2.getType()));
        }
    }

    public IJadescriptType getLUB(IJadescriptType t0, IJadescriptType... ts){
        if(ts.length == 0){
            return t0;
        }else if(ts.length == 1){
            return getLUB(t0, ts[0]);
        }else{
            return Arrays.stream(ts).reduce(t0, this::getLUB);
        }
    }

    @SuppressWarnings("unused")
    public IJadescriptType getGLB(IJadescriptType t1, IJadescriptType t2) {
        if (t1.isAssignableFrom(t2)) {
            return t2;
        } else if (t2.isAssignableFrom(t1)) {
            return t1;
        } else {
            return NOTHING;
        }
    }

    public IJadescriptType getGLB(IJadescriptType t0, IJadescriptType... ts){
        if(ts.length == 0){
            return t0;
        }else if(ts.length == 1){
            return getGLB(t0, ts[0]);
        }else{
            return Arrays.stream(ts).reduce(t0, this::getGLB);
        }
    }

    @SuppressWarnings("unused")
    public Maybe<JvmTypeReference> getGLB(Maybe<JvmTypeReference> t1, Maybe<JvmTypeReference> t2) {
        Optional<JvmTypeReference> ot1 = t1.toOpt();
        Optional<JvmTypeReference> ot2 = t2.toOpt();
        if (ot1.isPresent()) {
            if (ot2.isPresent()) {
                if (isAssignable(
                        ot1.get(),
                        ot2.get()
                )) {
                    return Maybe.fromOpt(ot2);
                } else if (isAssignable(
                        ot2.get(),
                        ot1.get()
                )) {
                    return Maybe.fromOpt(ot1);
                } else {
                    return nothing();
                }
            } else {
                return Maybe.fromOpt(ot1);
            }
        } else {
            if (ot2.isPresent()) {
                return Maybe.fromOpt(ot2);
            } else {
                return nothing();
            }
        }
    }

    public IJadescriptType getLUB(IJadescriptType t1, IJadescriptType t2) {
        if (t1.isAssignableFrom(t2)) {
            return t1;
        } else if (t2.isAssignableFrom(t1)) {
            return t2;
        }
        if (t1.asJvmTypeReference().getType() instanceof JvmDeclaredType
                && t2.asJvmTypeReference().getType() instanceof JvmDeclaredType) {
            List<JvmTypeReference> parentChainOfA = getParentChain(t1.asJvmTypeReference());
            for (JvmTypeReference candidateCommonParent : parentChainOfA) {
                if (isAssignable(candidateCommonParent, t2.asJvmTypeReference())) {
                    return jtFromJvmTypeRef(candidateCommonParent);
                }
            }
        }
        return ANY;
    }

    public TypeRelationship getTypeRelationship(IJadescriptType t1, IJadescriptType t2) {
        if (t1.typeEquals(t2)) {
            return TypeRelationship.EQUAL;
        } else if (t1.isAssignableFrom(t2)) {
            return TypeRelationship.STRICT_SUBTYPE;
        } else if (t2.isAssignableFrom(t1)) {
            return TypeRelationship.STRICT_SUPERTYPE;
        } else {
            return TypeRelationship.NOT_RELATED;
        }
    }

    private List<JvmTypeReference> getParentChain(JvmTypeReference x) {
        List<JvmTypeReference> result = new ArrayList<>();
        result.add(x);
        if (x.getType() instanceof JvmDeclaredType) {
            if (((JvmDeclaredType) x.getType()).getExtendedClass() != null) {
                result.addAll(getParentChain(((JvmDeclaredType) x.getType()).getExtendedClass()));
            }
        } else {
            result.add(typeRef(Object.class));
        }
        return result;
    }

    private List<JvmTypeReference> getTypeArgumentsOfParent(
            JvmTypeReference type,
            JvmTypeReference targetParentNoParams
    ) {


        for (JvmTypeReference x : getParentChain(type)) {
            if (x instanceof JvmParameterizedTypeReference
                    && x.getType().getQualifiedName().equals(targetParentNoParams.getQualifiedName())) {
                return ((JvmParameterizedTypeReference) x).getArguments();
            }
        }
        return new ArrayList<>();
    }


    public boolean isTypeWithPrimitiveOntologySchema(IJadescriptType type) {
        TypeHelper typeHelper = this;
        return Stream.of(typeHelper.INTEGER, typeHelper.BOOLEAN, typeHelper.TEXT, typeHelper.REAL)
                .anyMatch(t1 -> isAssignable(t1, type));
    }

    private static class ImplicitConversionDefinition {
        private final IJadescriptType from;
        private final IJadescriptType to;
        private final Function<String, String> conversionCompilation;

        public ImplicitConversionDefinition(
                IJadescriptType from,
                IJadescriptType to,
                Function<String, String> conversionCompilation
        ) {
            this.from = from;
            this.to = to;
            this.conversionCompilation = conversionCompilation;
        }

        public IJadescriptType getFrom() {
            return from;
        }

        public IJadescriptType getTo() {
            return to;
        }

        public String compileConversion(String compiledRExpr) {
            return conversionCompilation.apply(compiledRExpr);
        }
    }

    private static final class MessageContentTupleDefaultElements {
        private final Map<Integer, IJadescriptType> argumentToDefault = new HashMap<>();
        private final Map<Integer, BiFunction<TypeArgument, String, String>> argumentToCompile = new HashMap<>();

        private final int targetCount;

        private MessageContentTupleDefaultElements(int targetCount) {
            this.targetCount = targetCount;
        }

        public MessageContentTupleDefaultElements addEntry(
                int argumentPosition,
                IJadescriptType defaultType,
                BiFunction<TypeArgument, String, String> compile
        ) {
            argumentToDefault.put(argumentPosition, defaultType);
            argumentToCompile.put(argumentPosition, compile);
            return this;
        }

        public Maybe<IJadescriptType> getDefaultType(int argumentPosition) {
            return Maybe.some(argumentToDefault.get(argumentPosition));
        }

        public String compile(int argumentPosition, TypeArgument inputType, String inputExpression) {
            return argumentToCompile.getOrDefault(argumentPosition, (t, s) -> s).apply(inputType, inputExpression);
        }

        public static BiFunction<TypeArgument, String, String> promoteToTuple2(
                String defaultValue,
                TypeArgument defaultType
        ) {
            return (inputType, inputExpression) -> TupleType.compileNewInstance(
                    List.of(inputExpression, defaultValue),
                    List.of(inputType, defaultType)
            );
        }

        public static BiFunction<TypeArgument, String, String> addToTuple(
                String defaultValue,
                TypeArgument defaultType
        ) {
            return (inputType, inputExpression) -> TupleType.compileAddToTuple(
                    inputExpression,
                    defaultValue,
                    defaultType
            );
        }


        public int getTargetCount() {
            return targetCount;
        }

        public int getDefaultCount() {
            return Math.min(argumentToDefault.size(), argumentToCompile.size());
        }
    }


    public static class ErroneousType extends UtilityType {

        private final String description;
        private final IJadescriptType relationshipDelegate;

        static ErroneousType top(SemanticsModule module, String description, IJadescriptType giveMeANY) {
            return new ErroneousType(
                    module,
                    builtinPrefix + "TOP_ERR",
                    "(error)any",
                    module.get(JvmTypeReferenceBuilder.class).typeRef("/*ANY*/java.lang.Object"),
                    description,
                    giveMeANY
            );
        }

        static ErroneousType bottom(SemanticsModule module, String description, IJadescriptType giveMeNOTHING) {
            return new ErroneousType(
                    module,
                    builtinPrefix + "BOTTOM_ERR",
                    "(error)nothing",
                    module.get(JvmTypeReferenceBuilder.class).typeRef("/*NOTHING*/java.lang.Object"),
                    description,
                    giveMeNOTHING
            );
        }

        private ErroneousType(
                SemanticsModule module,
                String typeID,
                String simpleName,
                JvmTypeReference jvmType,
                String description,
                IJadescriptType relationshipDelegate
        ) {
            super(module, typeID, simpleName, jvmType);
            this.description = description;
            this.relationshipDelegate = relationshipDelegate;
        }

        @Override
        public boolean typeEquals(IJadescriptType other) {
            return relationshipDelegate.typeEquals(other);
        }

        @Override
        public boolean isAssignableFrom(IJadescriptType other) {
            return relationshipDelegate.isAssignableFrom(other);
        }



        @Override
        public boolean validateType(Maybe<? extends EObject> input, ValidationMessageAcceptor acceptor) {
            return module.get(ValidationHelper.class).asserting(
                    false,
                    "InvalidType",
                    description,
                    input,
                    acceptor
            );
        }

        @Override
        public boolean isSendable() {
            return false;
        }

        @Override
        public boolean isErroneous() {
            return true;
        }

        @Override
        public Maybe<OntologyType> getDeclaringOntology() {
            return nothing();
        }

        @Override
        public TypeNamespace namespace() {
            return new BuiltinOpsNamespace(
                    module,
                    nothing(),
                    List.of(),
                    List.of(),
                    getLocation()
            );
        }
    }
}
