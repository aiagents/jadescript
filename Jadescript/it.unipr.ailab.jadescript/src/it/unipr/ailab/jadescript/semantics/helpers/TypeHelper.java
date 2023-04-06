package it.unipr.ailab.jadescript.semantics.helpers;


import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.c0outer.RawTypeReferenceSolverContext;
import it.unipr.ailab.jadescript.semantics.context.search.JadescriptTypeLocation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.UnknownJVMType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agent.BaseAgentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agent.UserDefinedAgentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agentenv.AgentEnvType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.basic.BasicType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour.BaseBehaviourType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour.UserDefinedBehaviourType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.ListType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.MapType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.SetType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.TupleType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategory;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategoryAdapter;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.message.BaseMessageType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.message.MessageSubType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.message.MessageType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontocontent.BaseOntoContentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontocontent.OntoContentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontocontent.UserDefinedOntoContentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.BaseOntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.UserDefinedOntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.BoundedTypeArgument;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationship;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.util.*;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.JvmTypeReferenceSet;
import it.unipr.ailab.maybe.Maybe;
import jade.content.onto.Ontology;
import jadescript.content.*;
import jadescript.core.Agent;
import jadescript.core.behaviours.*;
import jadescript.core.message.*;
import jadescript.java.AgentEnv;
import jadescript.java.SideEffectsFlag;
import jadescript.lang.Performative;
import jadescript.lang.Tuple;
import jadescript.util.JadescriptList;
import jadescript.util.JadescriptMap;
import jadescript.util.JadescriptSet;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.*;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypeReferenceBuilder;
import org.jetbrains.annotations.Nullable;

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


    public static final String builtinPrefix = "BUILTIN#";
    public static final String VOID_TYPEID = builtinPrefix + "JAVAVOID";
    // Top and bottom: equal to ANY and NOTHING; but include an error text
    // message, to explain what caused the erroneous type to result from type
    // inferring:
    //TODO use new TOP & BOTTOM when possible
    public final Function<String, UtilityType> TOP;
    public final Function<String, UtilityType> BOTTOM;
    // ANY = Supertype of all types. NOTHING = Subtype of all types.
    public final AnyType ANY;
    public final NothingType NOTHING;
    // Common useful Jvm types:
    public final UtilityType VOID;
    public final UtilityType NUMBER;
    public final UtilityType ANY_ONTOLOGY_ELEMENT;
    public final UtilityType ANY_MESSAGE;
    public final AgentEnvType ANY_AGENTENV;
    public final Function<List<TypeArgument>, AgentEnvType> AGENTENV;
    // Jadescript basic types:
    public final BasicType INTEGER;
    public final BasicType BOOLEAN;
    public final BasicType TEXT;
    public final BasicType REAL;
    public final BasicType PERFORMATIVE;
    public final BasicType AID;
    public final BasicType DURATION;
    public final BasicType TIMESTAMP;
    // Jadescript collection types:
    public final Function<List<TypeArgument>, ListType> LIST;
    public final Function<List<TypeArgument>, MapType> MAP;
    public final Function<List<TypeArgument>, SetType> SET;
    public final Function<List<TypeArgument>, TupleType> TUPLE;
    // Jadescript Basic Agent type:
    public final BaseAgentType AGENT;
    // Jadescript Message types:
    public final Function<List<TypeArgument>, ? extends BaseMessageType>
        MESSAGE;
    // Jadescript Basic Ontology type:
    public final BaseOntologyType ONTOLOGY;
    // Jadescript Basic Behaviour type:
    public final Function<List<TypeArgument>, BaseBehaviourType> BEHAVIOUR;
    public final Function<List<TypeArgument>, BaseBehaviourType>
        CYCLIC_BEHAVIOUR;
    public final Function<List<TypeArgument>, BaseBehaviourType>
        ONESHOT_BEHAVIOUR;
    public final BaseBehaviourType ANYBEHAVIOUR;
    public final BaseOntoContentType CONCEPT;
    public final BaseOntoContentType PROPOSITION;
    public final BaseOntoContentType PREDICATE;
    public final BaseOntoContentType ATOMIC_PROPOSITION;
    public final BaseOntoContentType ACTION;
    public final UserDefinedOntoContentType INTERNAL_EXCEPTION;
    public final Function<List<TypeArgument>, MessageSubType>
        ACCEPTPROPOSAL_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> AGREE_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> CANCEL_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> CFP_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> CONFIRM_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType>
        DISCONFIRM_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> FAILURE_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> INFORM_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> INFORMIF_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> INFORMREF_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType>
        NOTUNDERSTOOD_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> PROPOSE_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> QUERYIF_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> QUERYREF_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> REFUSE_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType>
        REJECTPROPOSAL_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> REQUEST_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType>
        REQUESTWHEN_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType>
        REQUESTWHENEVER_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> SUBSCRIBE_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> PROXY_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> PROPAGATE_MESSAGE;
    public final Function<List<TypeArgument>, MessageSubType> UNKNOWN_MESSAGE;
    public final UnknownJVMType ANY_SE_MODE;
    //Associates JVM fully-qualified-name strings to Jadescript types.
    private final Map<String, IJadescriptType> defaultJVMToDescriptorTable =
        new HashMap<>();
    private final
    Map<String, Function<List<TypeArgument>, ? extends IJadescriptType>>
        defaultJVMToGenericDescriptorTable = new HashMap<>();
    private final Map<String, Integer> expectedGenericDescriptorArguments =
        new HashMap<>();
    private final List<ImplicitConversionDefinition> implicitConversions =
        new ArrayList<>();
    private final Map<
        Performative,
        Function<List<TypeArgument>, ? extends MessageType>
        > messageSubTypeMap = new HashMap<>();
    private final Map<String, Performative> nameToPerformativeMap =
        new HashMap<>();
    private final Map<Performative, Supplier<IJadescriptType>>
        messageContentTypeRequirements = new HashMap<>();
    //Associates to some performatives (ACCEPT_PROPOSAL, CFP, etc...) with a
    // set of default elements (mainly TRUE propositions)
    private final Map<Performative, MessageContentTupleDefaultElements>
        defaultContentElementsMap = new HashMap<>();
    private final SemanticsModule module;


    public TypeHelper(SemanticsModule module) {
        this.module = module;
        ANY = new AnyType(module, this);
        defineJVMToDescriptor(Object.class, ANY);
        TOP = (String s) -> ErroneousType.top(module, s, ANY);


        NOTHING = new NothingType(module);
        BOTTOM = (String s) -> ErroneousType.bottom(module, s, NOTHING);

        VOID = new JavaVoidType(this, module);
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
        TEXT.addBultinProperty(Property.readonlyProperty(
            "length",
            INTEGER,
            new JadescriptTypeLocation(TEXT),
            Property.compileGetWithCustomMethod("length")
        ));

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
        AID.addBultinProperty(Property.readonlyProperty(
            "name",
            TEXT,
            aidLocation,
            Property.compileWithJVMGetter("name")
        ));
        AID.addBultinProperty(Property.readonlyProperty(
            "platform",
            TEXT,
            aidLocation,
            Property.compileGetWithCustomMethod("getPlatformID")
        ));
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
            "((" + REAL.compileAsJavaCast() + "((float) " + compiledRExpr +
                ")))"
        );


        // not usable in the language, used by type engine as a supertype
        // for all ontology content types
        ANY_ONTOLOGY_ELEMENT = new AnyOntologyElementType(this, module);
        defineJVMToDescriptor(Serializable.class, ANY_ONTOLOGY_ELEMENT);

        ANY_MESSAGE = new AnyMessageType(this, module);


        LIST = (arguments) -> new ListType(module, arguments.get(0));
        defineJVMToGenericDescriptor(JadescriptList.class, LIST, 1);

        MAP = (arguments) -> new MapType(
            module,
            arguments.get(0),
            arguments.get(1)
        );
        defineJVMToGenericDescriptor(JadescriptMap.class, MAP, 2);

        SET = (arguments) -> new SetType(module, arguments.get(0));
        defineJVMToGenericDescriptor(JadescriptSet.class, SET, 1);

        TUPLE = (arguments) -> new TupleType(module, arguments);

        AGENT = new BaseAgentType(module);
        defineJVMToDescriptor(jadescript.core.Agent.class, AGENT);

        ANY_SE_MODE = new UnknownJVMType(
            module,
            typeRef(SideEffectsFlag.AnySideEffectFlag.class),
            /*permissive = */false
        );


        AGENTENV = (args) -> new AgentEnvType(
            module,
            args.get(0),
            args.get(1),
            AGENT,
            ANY_SE_MODE
        );
        defineJVMToGenericDescriptor(AgentEnv.class, AGENTENV, 2);


        ANY_AGENTENV = AGENTENV.apply(
            List.of(covariant(AGENT), covariant(ANY_SE_MODE))
        );

        MESSAGE = (arguments) -> new BaseMessageType(module, arguments.get(0));
        defineJVMToGenericDescriptor(
            jadescript.core.message.Message.class,
            MESSAGE,
            1
        );


        ONTOLOGY = new BaseOntologyType(module);
        defineJVMToDescriptor(jadescript.content.onto.Ontology.class, ONTOLOGY);
        defineJVMToDescriptor(jade.content.onto.Ontology.class, ONTOLOGY);

        // for java.lang.Number, not usable in the language, used by
        //  type engine as a non-direct supertype for number basic types
        //  (non-direct supertype = you can ask NUMBER if INTEGER is its
        //  subtype, but you will not find NUMBER in the list of INTEGER's
        //  direct sypertypes)
        NUMBER = new UtilityType(
            module,
            builtinPrefix + "number",
            this.typeRef(Number.class).getQualifiedName('.'),
            this.typeRef(Number.class)
        ) {
            @Override
            public TypeCategory category() {
                return new TypeCategoryAdapter();
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
            new BaseBehaviourType(
                module,
                BaseBehaviourType.Kind.Base,
                arguments.get(0),
                AGENT
            );
        defineJVMToGenericDescriptor(
            jadescript.core.behaviours.Behaviour.class,
            BEHAVIOUR,
            1
        );

        CYCLIC_BEHAVIOUR = (arguments) ->
            new BaseBehaviourType(
                module,
                BaseBehaviourType.Kind.Cyclic,
                arguments.get(0),
                AGENT
            );
        defineJVMToGenericDescriptor(
            jadescript.core.behaviours.CyclicBehaviour.class,
            CYCLIC_BEHAVIOUR,
            1
        );

        ONESHOT_BEHAVIOUR = (arguments) ->
            new BaseBehaviourType(
                module,
                BaseBehaviourType.Kind.OneShot,
                arguments.get(0),
                AGENT
            );
        defineJVMToGenericDescriptor(
            jadescript.core.behaviours.OneShotBehaviour.class,
            ONESHOT_BEHAVIOUR,
            1
        );

        ANYBEHAVIOUR = BEHAVIOUR.apply(List.of(covariant(AGENT)));

        CONCEPT = new BaseOntoContentType(
            module,
            OntoContentType.OntoContentKind.Concept
        );
        defineJVMToDescriptor(
            jadescript.content.JadescriptConcept.class,
            CONCEPT
        );


        PROPOSITION = new BaseOntoContentType(
            module,
            OntoContentType.OntoContentKind.Proposition
        );
        defineJVMToDescriptor(
            jadescript.content.JadescriptProposition.class,
            PROPOSITION
        );

        PREDICATE = new BaseOntoContentType(
            module,
            OntoContentType.OntoContentKind.Predicate
        );
        defineJVMToDescriptor(
            jadescript.content.JadescriptPredicate.class,
            PREDICATE
        );

        ATOMIC_PROPOSITION = new BaseOntoContentType(
            module,
            OntoContentType.OntoContentKind.AtomicProposition
        );
        defineJVMToDescriptor(
            jadescript.content.JadescriptAtomicProposition.class,
            ATOMIC_PROPOSITION
        );

        ACTION = new BaseOntoContentType(
            module,
            OntoContentType.OntoContentKind.Action
        );
        defineJVMToDescriptor(
            jadescript.content.JadescriptAction.class,
            ACTION
        );

        INTERNAL_EXCEPTION = new UserDefinedOntoContentType(
            module,
            typeRef(jadescript.content.onto.basic.InternalException.class),
            PREDICATE
        );
        defineJVMToDescriptor(
            jadescript.content.onto.basic.InternalException.class,
            INTERNAL_EXCEPTION
        );

        ACCEPTPROPOSAL_MESSAGE = (args) -> new MessageSubType(
            module,
            AcceptProposalMessage.class,
            args,
            Arrays.asList(ACTION, PROPOSITION)
        );
        nameToPerformativeMap.put("AcceptProposalMessage", ACCEPT_PROPOSAL);
        messageSubTypeMap.put(ACCEPT_PROPOSAL, ACCEPTPROPOSAL_MESSAGE);
        messageContentTypeRequirements.put(
            ACCEPT_PROPOSAL,
            () -> TUPLE.apply(Arrays.asList(
                covariant(ACTION), covariant(PROPOSITION)
            ))
        );
        defaultContentElementsMap.put(
            ACCEPT_PROPOSAL,
            new MessageContentTupleDefaultElements(2)
                .addEntry(
                    1,
                    PROPOSITION,
                    MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                    )
                )
        );
        defineJVMToGenericDescriptor(
            AcceptProposalMessage.class,
            ACCEPTPROPOSAL_MESSAGE,
            2
        );

        AGREE_MESSAGE = (args) -> new MessageSubType(
            module,
            AgreeMessage.class,
            args,
            Arrays.asList(ACTION, PROPOSITION)
        );
        nameToPerformativeMap.put("AgreeMessage", AGREE);
        messageSubTypeMap.put(AGREE, AGREE_MESSAGE);
        messageContentTypeRequirements.put(
            AGREE,
            () -> TUPLE.apply(Arrays.asList(
                covariant(ACTION), covariant(PROPOSITION)
            ))
        );
        defaultContentElementsMap.put(
            AGREE,
            new MessageContentTupleDefaultElements(2)
                .addEntry(
                    1,
                    PROPOSITION,
                    MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                    )
                )
        );
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
        defaultContentElementsMap.put(
            CFP,
            new MessageContentTupleDefaultElements(2)
                .addEntry(
                    1,
                    PROPOSITION,
                    MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                    )
                )
        );
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
        defineJVMToGenericDescriptor(
            DisconfirmMessage.class,
            DISCONFIRM_MESSAGE,
            1
        );

        FAILURE_MESSAGE = (args) -> new MessageSubType(
            module,
            FailureMessage.class,
            args,
            Arrays.asList(ACTION, PROPOSITION)
        );
        nameToPerformativeMap.put("FailureMessage", FAILURE);
        messageSubTypeMap.put(FAILURE, FAILURE_MESSAGE);
        messageContentTypeRequirements.put(
            FAILURE,
            () -> TUPLE.apply(Arrays.asList(
                covariant(ACTION), covariant(PROPOSITION)
            ))
        );
        defaultContentElementsMap.put(
            FAILURE,
            new MessageContentTupleDefaultElements(2)
                .addEntry(
                    1,
                    PROPOSITION,
                    MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                    )
                )
        );
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
        defineJVMToGenericDescriptor(
            InformIfMessage.class,
            INFORMIF_MESSAGE,
            1
        );

        INFORMREF_MESSAGE = (args) -> new MessageSubType(
            module,
            InformRefMessage.class,
            args,
            Arrays.asList(LIST.apply(Arrays.asList(covariant(CONCEPT))))
        );
        nameToPerformativeMap.put("InformRefMessage", INFORM_REF);
        messageSubTypeMap.put(INFORM_REF, INFORMREF_MESSAGE);
        messageContentTypeRequirements.put(
            INFORM_REF,
            () -> LIST.apply(Arrays.asList(covariant(CONCEPT)))
        );
        defineJVMToGenericDescriptor(
            InformRefMessage.class,
            INFORMREF_MESSAGE,
            1
        );

        NOTUNDERSTOOD_MESSAGE = (args) -> new MessageSubType(
            module,
            NotUnderstoodMessage.class,
            args,
            Arrays.asList(ANY_MESSAGE, PROPOSITION)
        );
        nameToPerformativeMap.put("NotUnderstoodMessage", NOT_UNDERSTOOD);
        messageSubTypeMap.put(NOT_UNDERSTOOD, NOTUNDERSTOOD_MESSAGE);
        messageContentTypeRequirements.put(
            NOT_UNDERSTOOD,
            () -> TUPLE.apply(Arrays.asList(
                covariant(ANY_MESSAGE), covariant(PROPOSITION)
            ))
        );
        defaultContentElementsMap.put(
            NOT_UNDERSTOOD,
            new MessageContentTupleDefaultElements(2)
                .addEntry(
                    1,
                    PROPOSITION,
                    MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                    )
                )
        );
        defineJVMToGenericDescriptor(
            NotUnderstoodMessage.class,
            NOTUNDERSTOOD_MESSAGE,
            2
        );

        PROPOSE_MESSAGE = (args) -> new MessageSubType(
            module,
            ProposeMessage.class,
            args,
            Arrays.asList(ACTION, PROPOSITION)
        );
        nameToPerformativeMap.put("ProposeMessage", PROPOSE);
        messageSubTypeMap.put(PROPOSE, PROPOSE_MESSAGE);
        messageContentTypeRequirements.put(
            PROPOSE,
            () -> TUPLE.apply(Arrays.asList(
                covariant(ACTION), covariant(PROPOSITION)
            ))
        );
        defaultContentElementsMap.put(
            PROPOSE,
            new MessageContentTupleDefaultElements(2)
                .addEntry(
                    1,
                    PROPOSITION,
                    MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                    )
                )
        );
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
        messageContentTypeRequirements.put(
            QUERY_REF,
            () -> LIST.apply(Arrays.asList(covariant(CONCEPT)))
        );
        defineJVMToGenericDescriptor(
            QueryRefMessage.class,
            QUERYREF_MESSAGE,
            1
        );

        REFUSE_MESSAGE = (args) -> new MessageSubType(
            module,
            RefuseMessage.class,
            args,
            Arrays.asList(ACTION, PROPOSITION)
        );
        nameToPerformativeMap.put("RefuseMessage", REFUSE);
        messageSubTypeMap.put(REFUSE, REFUSE_MESSAGE);
        messageContentTypeRequirements.put(
            REFUSE,
            () -> TUPLE.apply(Arrays.asList(
                covariant(ACTION), covariant(PROPOSITION)
            ))
        );
        defaultContentElementsMap.put(
            REFUSE,
            new MessageContentTupleDefaultElements(2)
                .addEntry(
                    1,
                    PROPOSITION,
                    MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                    )
                )
        );
        defineJVMToGenericDescriptor(RefuseMessage.class, REFUSE_MESSAGE, 2);

        REJECTPROPOSAL_MESSAGE = (args) -> new MessageSubType(
            module,
            RejectProposalMessage.class,
            args,
            Arrays.asList(ACTION, PROPOSITION, PROPOSITION)
        );
        nameToPerformativeMap.put("RejectProposalMessage", REJECT_PROPOSAL);
        messageSubTypeMap.put(REJECT_PROPOSAL, REJECTPROPOSAL_MESSAGE);
        messageContentTypeRequirements.put(
            REJECT_PROPOSAL,
            () -> TUPLE.apply(Arrays.asList(
                covariant(ACTION),
                covariant(PROPOSITION),
                covariant(PROPOSITION)
            ))
        );
        defaultContentElementsMap.put(
            REJECT_PROPOSAL,
            new MessageContentTupleDefaultElements(3)
                .addEntry(
                    1,
                    PROPOSITION,
                    MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                    )
                )
                .addEntry(
                    2,
                    PROPOSITION,
                    MessageContentTupleDefaultElements.addToTuple(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                    )
                )
        );
        defineJVMToGenericDescriptor(
            RejectProposalMessage.class,
            REJECTPROPOSAL_MESSAGE,
            3
        );

        REQUEST_MESSAGE = (args) -> new MessageSubType(
            module,
            RequestMessage.class,
            args,
            Arrays.asList(ACTION)
        );
        nameToPerformativeMap.put("RequestMessage", REQUEST);
        messageSubTypeMap.put(REQUEST, REQUEST_MESSAGE);
        messageContentTypeRequirements.put(REQUEST, () -> ACTION);
        defineJVMToGenericDescriptor(RequestMessage.class, REQUEST_MESSAGE, 1);

        REQUESTWHEN_MESSAGE = (args) -> new MessageSubType(
            module,
            RequestWhenMessage.class,
            args,
            Arrays.asList(ACTION, PROPOSITION)
        );
        nameToPerformativeMap.put("RequestWhenMessage", REQUEST_WHEN);
        messageSubTypeMap.put(REQUEST_WHEN, REQUESTWHEN_MESSAGE);
        messageContentTypeRequirements.put(
            REQUEST_WHEN,
            () -> TUPLE.apply(Arrays.asList(
                covariant(ACTION), covariant(PROPOSITION)
            ))
        );
        defineJVMToGenericDescriptor(
            RequestWhenMessage.class,
            REQUESTWHEN_MESSAGE,
            2
        );

        REQUESTWHENEVER_MESSAGE = (args) -> new MessageSubType(
            module,
            RequestWheneverMessage.class,
            args,
            Arrays.asList(ACTION, PROPOSITION)
        );
        nameToPerformativeMap.put("RequestWheneverMessage", REQUEST_WHENEVER);
        messageSubTypeMap.put(REQUEST_WHENEVER, REQUESTWHENEVER_MESSAGE);
        messageContentTypeRequirements.put(
            REQUEST_WHENEVER,
            () -> TUPLE.apply(Arrays.asList(
                covariant(ACTION), covariant(PROPOSITION)
            ))
        );
        defineJVMToGenericDescriptor(
            RequestWheneverMessage.class,
            REQUESTWHENEVER_MESSAGE,
            2
        );

        SUBSCRIBE_MESSAGE = (args) -> new MessageSubType(
            module,
            SubscribeMessage.class,
            args,
            Arrays.asList(LIST.apply(Arrays.asList(covariant(CONCEPT))))
        );
        nameToPerformativeMap.put("SubscribeMessage", SUBSCRIBE);
        messageSubTypeMap.put(SUBSCRIBE, SUBSCRIBE_MESSAGE);
        messageContentTypeRequirements.put(
            SUBSCRIBE,
            () -> LIST.apply(Arrays.asList(covariant(CONCEPT)))
        );
        defineJVMToGenericDescriptor(
            SubscribeMessage.class,
            SUBSCRIBE_MESSAGE,
            1
        );

        PROXY_MESSAGE = (args) -> new MessageSubType(
            module,
            ProxyMessage.class,
            args,
            Arrays.asList(
                LIST.apply(Arrays.asList(AID)),
                ANY_MESSAGE,
                PROPOSITION
            )
        );
        nameToPerformativeMap.put("ProxyMessage", PROXY);
        messageSubTypeMap.put(PROXY, PROXY_MESSAGE);
        messageContentTypeRequirements.put(
            PROXY,
            () -> TUPLE.apply(Arrays.asList(
                LIST.apply(Arrays.asList(AID)),
                covariant(ANY_MESSAGE),
                covariant(PROPOSITION)
            ))
        );
        defaultContentElementsMap.put(
            PROXY,
            new MessageContentTupleDefaultElements(3)
                .addEntry(
                    2,
                    PROPOSITION,
                    MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                    )
                )
        );
        defineJVMToGenericDescriptor(ProxyMessage.class, PROXY_MESSAGE, 3);

        PROPAGATE_MESSAGE = (args) -> new MessageSubType(
            module,
            PropagateMessage.class,
            args,
            Arrays.asList(
                LIST.apply(Arrays.asList(AID)),
                ANY_MESSAGE,
                PROPOSITION
            )
        );
        nameToPerformativeMap.put("PropagateMessage", PROPAGATE);
        messageSubTypeMap.put(PROPAGATE, PROPAGATE_MESSAGE);
        messageContentTypeRequirements.put(
            PROPAGATE,
            () -> TUPLE.apply(Arrays.asList(
                LIST.apply(Arrays.asList(AID)),
                covariant(ANY_MESSAGE),
                covariant(PROPOSITION)
            ))
        );
        defaultContentElementsMap.put(
            PROPAGATE,
            new MessageContentTupleDefaultElements(3)
                .addEntry(
                    2,
                    PROPOSITION,
                    MessageContentTupleDefaultElements.promoteToTuple2(
                        "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                        PROPOSITION
                    )
                )
        );
        defineJVMToGenericDescriptor(
            PropagateMessage.class,
            PROPAGATE_MESSAGE,
            3
        );


        UNKNOWN_MESSAGE = (args) -> new MessageSubType(
            module,
            UnknownMessage.class,
            args,
            Arrays.asList(ANY_ONTOLOGY_ELEMENT)
        ) {
            @Override
            public boolean isSendable() {
                return false;
            }
        };
        nameToPerformativeMap.put("UnknownMessage", UNKNOWN);
        messageSubTypeMap.put(UNKNOWN, UNKNOWN_MESSAGE);
        messageContentTypeRequirements.put(UNKNOWN, () -> ANY_ONTOLOGY_ELEMENT);
        defineJVMToGenericDescriptor(UnknownMessage.class, UNKNOWN_MESSAGE, 1);


    }



    //TODO ?
    public List<TypeArgument> getDefaultTypeArguments(
        String name
    ) {
        if (name.equals("Message")) {
            return Collections.singletonList(ANY);
        }
        final Performative performative = nameToPerformativeMap.get(name);
        final Supplier<IJadescriptType> supplyDefaultArg =
            messageContentTypeRequirements.get(
                performative);
        if (supplyDefaultArg == null) {
            return Collections.emptyList();
        }
        final IJadescriptType jadescriptType = supplyDefaultArg.get();
        if (jadescriptType.category().isTuple()) {
            return jadescriptType.typeArguments();
        }
        return Collections.singletonList(jadescriptType);
    }


    //TODO ?
    public IJadescriptType getContentBound(Performative performative) {
        return messageContentTypeRequirements.get(performative).get();
    }

    //TODO separation of concerns:
    // - TypeSolver
    // - TypeIndex
    // - BuiltinTypeProvider
    // - TypeComparator
    // - TypeLatticeComputer
    // - ImplicitConversionsHelper
    // - TypeHelper (unpackTuple, adaptMessageContentDefaultType etc...)
    // - JvmTypeHelper (isAssignable, typeReferenceEquals...)



    //TODO keep here
    public BoundedTypeArgument covariant(IJadescriptType jadescriptType) {
        return new BoundedTypeArgument(
            module,
            jadescriptType,
            BoundedTypeArgument.Variance.EXTENDS
        );
    }

    //TODO keep here
    public BoundedTypeArgument contravariant(IJadescriptType jadescriptType) {
        return new BoundedTypeArgument(
            module,
            jadescriptType,
            BoundedTypeArgument.Variance.SUPER
        );
    }




    //TODO keep here
    public IJadescriptType beingDeclaredAgentType(
        JvmDeclaredType itClass,
        Maybe<IJadescriptType> superType
    ) {
        return new UserDefinedAgentType(
            module,
            typeRef(itClass),
            superType,
            AGENT
        );
    }


    //TODO remove
    private void defineJVMToDescriptor(
        Class<?> type,
        IJadescriptType descriptor
    ) {
        defaultJVMToDescriptorTable.put(
            this.typeRef(type).getQualifiedName('.'),
            descriptor
        );
    }


    //TODO remove
    private void defineJVMToGenericDescriptor(
        JvmTypeReference typeReference,
        Function<List<TypeArgument>, ? extends IJadescriptType> descriptor,
        int expectedArguments
    ) {

        defaultJVMToGenericDescriptorTable.put(
            JvmTypeHelper.noGenericsTypeName(
                typeReference.getQualifiedName('.')
            ),
            descriptor
        );
        expectedGenericDescriptorArguments.put(
            JvmTypeHelper.noGenericsTypeName(
                typeReference.getQualifiedName('.')
            ),
            expectedArguments
        );
    }


    //TODO remove
    private void defineJVMToGenericDescriptor(
        Class<?> type,
        Function<List<TypeArgument>, ? extends IJadescriptType> descriptor,
        int expectedArguments
    ) {
        defineJVMToGenericDescriptor(
            this.typeRef(type),
            descriptor,
            expectedArguments
        );
    }


    //TODO -> ImplicitConversionHelper / TypeIndex
    private void defineImplicitConversionPath(
        IJadescriptType from,
        IJadescriptType to,
        Function<String, String> conversionCompilationMethod
    ) {
        implicitConversions.add(new ImplicitConversionDefinition(
            from,
            to,
            conversionCompilationMethod
        ));
    }









    //TODO -> ImplicitConversionHelper
    public boolean implicitConversionCanOccur(
        IJadescriptType from,
        IJadescriptType to
    ) {
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


    //TODO -> ImplicitConversionHelper
    public String compileImplicitConversion(
        String compileExpression,
        IJadescriptType argType,
        IJadescriptType destType
    ) {
        List<ImplicitConversionDefinition> list = implicitConversionPath(
            argType,
            destType
        );
        String result = compileExpression;
        for (ImplicitConversionDefinition conv : list) {
            result = conv.compileConversion(result);
        }
        return result;
    }


    //TODO -> ImplicitConversionHelper
    public String compileWithEventualImplicitConversions(
        String compiledExpression,
        IJadescriptType argType,
        IJadescriptType destType
    ) {
        if (implicitConversionCanOccur(argType, destType)) {
            return compileImplicitConversion(
                compiledExpression,
                argType,
                destType
            );
        } else {
            return compiledExpression;
        }
    }


    //TODO -> TypeLatticeComputer
    public Maybe<OntologyType> getOntologyGLB(
        Maybe<OntologyType> mt1,
        Maybe<OntologyType> mt2,
        List<Maybe<OntologyType>> mts
    ) {
        //TODO multiple ontologies
        Maybe<OntologyType> result = getOntologyGLB(mt1, mt2);
        for (Maybe<OntologyType> mt : mts) {
            if (result.isNothing()) {
                return nothing();
            }
            result = getOntologyGLB(result, mt);
        }
        return result;
    }


    //TODO -> ImplicitConversionHelper
    public Maybe<OntologyType> getOntologyGLB(
        Maybe<OntologyType> mt1,
        Maybe<OntologyType> mt2
    ) {
        //TODO multiple ontologies
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


    //TODO -> Keep
    public IJadescriptType adaptMessageContentDefaultTypes(
        Maybe<String> performative,
        IJadescriptType inputContentType
    ) {
        if (performative.isNothing() || performative.toNullable().isBlank()) {
            return inputContentType;
        }
        final Performative perf = performativeByName.getOrDefault(
            performative.toNullable(),
            UNKNOWN
        );
        final MessageContentTupleDefaultElements mctde =
            defaultContentElementsMap.get(perf);
        if (mctde == null) {
            return inputContentType;
        } else if (inputContentType instanceof TupleType) {
            final List<IJadescriptType> inputElementTypes =
                ((TupleType) inputContentType).getElementTypes();
            final int inputArgsCount = inputElementTypes.size();
            final int requiredArgCount =
                mctde.getTargetCount() - mctde.getDefaultCount();

            if (inputArgsCount >= requiredArgCount
                && inputArgsCount < mctde.getTargetCount()) {
                List<TypeArgument> elements = new ArrayList<>(
                    mctde.getTargetCount()
                );
                for (int i = 0; i < mctde.getTargetCount(); i++) {
                    if (i < inputArgsCount) {
                        elements.add(inputElementTypes.get(i));
                    } else {
                        elements.add(covariant(
                            mctde.getDefaultType(i).orElse(ANY)
                        ));
                    }
                }
                return TUPLE.apply(elements);
            } else {
                return inputContentType;
            }
        } else {
            final int requiredArgCount =
                mctde.getTargetCount() - mctde.getDefaultCount();
            if (requiredArgCount <= 1) {
                List<TypeArgument> elements =
                    new ArrayList<>(mctde.getTargetCount());
                for (int i = 0; i < mctde.getTargetCount(); i++) {
                    if (i == 0) {
                        elements.add(inputContentType);
                    } else {
                        elements.add(covariant(mctde.getDefaultType(i)
                            .orElse(ANY)));
                    }
                }
                return TUPLE.apply(elements);
            } else {
                return inputContentType;
            }
        }
    }


    //TODO -> Keep
    public String adaptMessageContentDefaultCompile(
        Maybe<String> performative,
        IJadescriptType inputContentType,
        String inputExpression
    ) {
        if (performative.isNothing() || performative.toNullable().isBlank()) {
            return inputExpression;
        }
        final Performative perf = performativeByName.getOrDefault(
            performative.toNullable(),
            UNKNOWN
        );
        final MessageContentTupleDefaultElements mctde =
            defaultContentElementsMap.get(perf);

        if (mctde == null) {
            return inputExpression;
        } else {
            final int inputArgsCount = inputContentType instanceof TupleType
                ? ((TupleType) inputContentType).getElementTypes().size()
                : 1;
            String result = inputExpression;
            for (int i = inputArgsCount; i < mctde.getTargetCount(); i++) {
                result = mctde.compile(
                    i,
                    inputContentType,
                    result
                );
            }
            return result;
        }
    }


    /**
     * If {@code t} is a Tuple type, it returns the types of its elements.
     * Otherwise, it returns a singleton list
     * containing {@code t}.
     */
    //TODO -> Keep
    public List<? extends TypeArgument> unpackTuple(TypeArgument t) {
        if (t instanceof TupleType) {
            return ((TupleType) t).getTypeArguments();
        } else {
            return List.of(t);
        }
    }


    //TODO -> ImplicitConversionHelper
    public List<ImplicitConversionDefinition> implicitConversionPath(
        IJadescriptType start,
        IJadescriptType end
    ) {
        if (start.typeEquals(end)) {
            return Arrays.asList(new ImplicitConversionDefinition(
                start,
                end,
                (e) -> e
            ));
        }
        final Map<String, Vertex> map = new HashMap<>();
        for (ImplicitConversionDefinition edge : implicitConversions) {
            IJadescriptType from = edge.getFrom();
            IJadescriptType to = edge.getTo();

            final Vertex fromV = map.computeIfAbsent(
                edge.getFrom().getID(),
                (__) -> new Vertex(from)
            );
            final Vertex toV = map.computeIfAbsent(
                edge.getTo().getID(),
                (__) -> new Vertex(to)
            );

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
        for (
            Edge edge = endVertex.getLinkToPredecessor();
            edge != null;
            edge = edge.getFrom().getLinkToPredecessor()
        ) {
            path.add(edge);
        }

        Collections.reverse(path);

        return path.stream()
            .map(Edge::getDefinition)
            .collect(Collectors.toList());
    }


    //TODO -> JvmTypeHelper
    public JvmTypeReference objectTypeRef() {
        return module.get(JvmTypeReferenceBuilder.class).typeRef(Object.class);
    }

    //TODO -> ImplicitConversionHelper
    public boolean isPrimitiveWideningViable(
        JvmTypeReference from,
        JvmTypeReference to
    ) {
        if (from == null || to == null) {
            return false;
        } else {
            if (JvmTypeHelper.typeReferenceEquals(from, to)) {
                return true;
            } else if (JvmTypeHelper.typeReferenceEquals(
                to,
                typeRef(Double.class)
            )) {
                return isPrimitiveWideningViable(from, typeRef(Float.class));
            } else if (JvmTypeHelper.typeReferenceEquals(
                to,
                typeRef(Float.class)
            )) {
                return isPrimitiveWideningViable(from, typeRef(Long.class));
            } else if (JvmTypeHelper.typeReferenceEquals(
                to,
                typeRef(Long.class)
            )) {
                return isPrimitiveWideningViable(from, typeRef(Integer.class));
            } else if (JvmTypeHelper.typeReferenceEquals(
                to,
                typeRef(Integer.class)
            )) {
                return isPrimitiveWideningViable(from, typeRef(Short.class));
            } else if (JvmTypeHelper.typeReferenceEquals(
                to,
                typeRef(Short.class)
            )) {
                return isPrimitiveWideningViable(from, typeRef(Byte.class));
            }
        }

        return false;
    }


    //TODO -> ImplicitConversionHelper
    public JvmTypeReference boxedReferenceIfPrimitive(JvmTypeReference ref) {
        final JvmType type = ref.getType();
        if (type instanceof JvmPrimitiveType) {
            return boxedReference((JvmPrimitiveType) type);
        }
        return ref;
    }


    //TODO -> ImplicitConversionHelper
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





    //TODO -> TypeLatticeComputer
    @SuppressWarnings("unused")
    public IJadescriptType getLUB(IJadescriptType t0, IJadescriptType... ts) {
        if (ts.length == 0) {
            return t0;
        } else if (ts.length == 1) {
            return getLUB(t0, ts[0]);
        } else {
            return Arrays.stream(ts).reduce(t0, this::getLUB);
        }
    }


    //TODO -> TypeLatticeComputer
    @SuppressWarnings("unused")
    public IJadescriptType getGLB(IJadescriptType t1, IJadescriptType t2) {
        if (t1.isSupertypeOrEqualTo(t2)) {
            return t2;
        } else if (t2.isSupertypeOrEqualTo(t1)) {
            return t1;
        } else {
            return NOTHING;
        }
    }


    //TODO -> TypeLatticeComputer
    public IJadescriptType getGLB(IJadescriptType t0, IJadescriptType... ts) {
        if (ts.length == 0) {
            return t0;
        } else if (ts.length == 1) {
            return getGLB(t0, ts[0]);
        } else {
            return Arrays.stream(ts).reduce(t0, this::getGLB);
        }
    }


    //TODO -> TypeLatticeComputer
    @SuppressWarnings("unused")
    public Maybe<JvmTypeReference> getGLB(
        Maybe<JvmTypeReference> t1,
        Maybe<JvmTypeReference> t2
    ) {
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


    //TODO -> TypeLatticeComputer
    public IJadescriptType getLUB(IJadescriptType t1, IJadescriptType t2) {
        if (t1.isSupertypeOrEqualTo(t2)) {
            return t1;
        } else if (t2.isSupertypeOrEqualTo(t1)) {
            return t2;
        }
        if (t1.asJvmTypeReference().getType() instanceof JvmDeclaredType
            && t2.asJvmTypeReference().getType() instanceof JvmDeclaredType) {
            List<JvmTypeReference> parentChainOfA =
                getParentChain(t1.asJvmTypeReference());
            for (JvmTypeReference candidateCommonParent : parentChainOfA) {
                if (isAssignable(
                    candidateCommonParent,
                    t2.asJvmTypeReference()
                )) {
                    return jtFromJvmTypeRef(candidateCommonParent);
                }
            }
        }
        return TOP.apply("Could not compute LUB between " + t1 + " and " + t2);
    }



    //TODO keep
    public boolean isTypeWithPrimitiveOntologySchema(IJadescriptType type) {
        TypeHelper typeHelper = this;
        return Stream.of(
                typeHelper.INTEGER,
                typeHelper.BOOLEAN,
                typeHelper.TEXT,
                typeHelper.REAL
            )
            .anyMatch(t1 -> isAssignable(t1, type));
    }


    //TODO -> ImplicitConversionHelper
    private static class Vertex implements Comparable<Vertex> {

        private final List<Edge> adjacents = new ArrayList<>();
        private final IJadescriptType type;
        private boolean visited = false;
        private Edge linkToPredecessor = null;
        private int distance = Integer.MAX_VALUE;


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

    //TODO -> ImplicitConversionHelper
    private static class Edge {

        private final Vertex from;
        private final Vertex to;
        private final ImplicitConversionDefinition definition;


        public Edge(
            Vertex from,
            Vertex to,
            ImplicitConversionDefinition definition
        ) {
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

    //TODO -> ImplicitConversionHelper
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

        private final Map<Integer, IJadescriptType> argumentToDefault =
            new HashMap<>();
        private final Map<Integer, BiFunction<TypeArgument, String, String>>
            argumentToCompile = new HashMap<>();

        private final int targetCount;


        private MessageContentTupleDefaultElements(int targetCount) {
            this.targetCount = targetCount;
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


        public String compile(
            int argumentPosition,
            TypeArgument inputType,
            String inputExpression
        ) {
            return argumentToCompile.getOrDefault(
                argumentPosition,
                (t, s) -> s
            ).apply(inputType, inputExpression);
        }


        public int getTargetCount() {
            return targetCount;
        }


        public int getDefaultCount() {
            return Math.min(argumentToDefault.size(), argumentToCompile.size());
        }

    }


}
