package it.unipr.ailab.jadescript.semantics.jadescripttypes.index;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agent.BaseAgentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agentenv.AgentEnvType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agentenv.SEMode;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agentenv.SideEffectFlagInternalType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.basic.*;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour.BaseBehaviourType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour.BehaviourType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.ListType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.MapType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.SetType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.TupleType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.message.BaseMessageType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.message.MessageSubType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.message.MessageType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontocontent.BaseOntoContentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontocontent.OntoContentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontocontent.UserDefinedOntoContentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.BaseOntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.*;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.util.AnyOntologyElementType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.util.AnyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.util.JavaVoidType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.util.NothingType;
import it.unipr.ailab.maybe.utils.LazyInit;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jadescript.content.JadescriptOntoElement;
import jadescript.core.message.*;
import jadescript.java.AgentEnv;
import jadescript.java.SideEffectsFlag;
import jadescript.lang.Duration;
import jadescript.lang.Performative;
import jadescript.lang.Timestamp;
import jadescript.util.JadescriptList;
import jadescript.util.JadescriptMap;
import jadescript.util.JadescriptSet;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import static it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts.ONTOLOGY_TRUE_VALUE;
import static it.unipr.ailab.maybe.utils.LazyInit.lazyInit;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class BuiltinTypeProvider {

    private final SemanticsModule module;

    @BuiltinType(Object.class)
    /*package-private*/ final LazyInit<AnyType> tAny =
        lazyInit(() -> new AnyType(getModule(), ""));

    @BuiltinType
    /*package-private*/ final LazyInit<NothingType> tNothing
        = lazyInit(() -> new NothingType(getModule(), ""));

    @BuiltinType({Void.class, void.class})
    /*package-private*/ final LazyInit<JavaVoidType> tVoid =
        fromModule(JavaVoidType.class);

    @BuiltinType(JadescriptOntoElement.class)
    /*package-private*/ final LazyInit<AnyOntologyElementType>
        tAnyOntologyElement = fromModule(AnyOntologyElementType.class);

    @BuiltinType
    /*package-private*/ final LazyInit<BaseMessageType> tAnyMessage =
        lazyInit(() -> message(covariant(anyOntologyElement())));

    @BuiltinType({Integer.class, int.class, Long.class, long.class})
    /*package-private*/ final LazyInit<IntegerType> tInteger =
        fromModule(IntegerType.class);

    @BuiltinType(String.class)
    /*package-private*/ final LazyInit<TextType> tText =
        fromModule(TextType.class);

    @BuiltinType({Boolean.class, boolean.class})
    /*package-private*/ final LazyInit<BooleanType> tBoolean =
        fromModule(BooleanType.class);

    @BuiltinType({Float.class, float.class, Double.class, double.class})
    /*package-private*/ final LazyInit<RealType> tReal =
        fromModule(RealType.class);

    @BuiltinType(Performative.class)
    /*package-private*/ final LazyInit<PerformativeType> tPerformative =
        fromModule(PerformativeType.class);

    @BuiltinType(AID.class)
    /*package-private*/ final LazyInit<AIDType> tAID =
        fromModule(AIDType.class);

    @BuiltinType(Duration.class)
    /*package-private*/ final LazyInit<DurationType> tDuration =
        fromModule(DurationType.class);

    @BuiltinType(Timestamp.class)
    /*package-private*/ final LazyInit<TimestampType> tTimestamp =
        fromModule(TimestampType.class);

    @BuiltinType(jadescript.core.Agent.class)
    /*package-private*/ final LazyInit<BaseAgentType> tAgent =
        fromModule(BaseAgentType.class);


    @BuiltinType(SideEffectsFlag.class)
    /*package-private*/ final LazyInit<IJadescriptType> tSEModeTop =
        lazyInit(() -> new SideEffectFlagInternalType(
            getModule(),
            SEMode.TOP
        ));

    @BuiltinType(SideEffectsFlag.AnySideEffectFlag.class)
    /*package-private*/ final LazyInit<IJadescriptType> tSEModeBottom =
        lazyInit(() -> new SideEffectFlagInternalType(
            getModule(),
            SEMode.BOTTOM
        ));

    @BuiltinType(SideEffectsFlag.WithSideEffects.class)
    /*package-private*/ final LazyInit<IJadescriptType> tSEModeWithSE =
        lazyInit(() -> new SideEffectFlagInternalType(
            getModule(),
            SEMode.WITH_SE
        ));

    @BuiltinType(SideEffectsFlag.NoSideEffects.class)
    /*package-private*/ final LazyInit<IJadescriptType> tSEModeNoSE =
        lazyInit(() -> new SideEffectFlagInternalType(
            getModule(),
            SEMode.NO_SE
        ));


    @BuiltinType({jadescript.content.onto.Ontology.class,
        jade.content.onto.Ontology.class})
    /*package-private*/ final LazyInit<BaseOntologyType> tOntology =
        fromModule(BaseOntologyType.class);

    @BuiltinType(jadescript.content.JadescriptConcept.class)
    /*package-private*/ final LazyInit<BaseOntoContentType>
        tConcept = lazyInit(() -> new BaseOntoContentType(
        getModule(),
        OntoContentType.OntoContentKind.Concept
    ));

    @BuiltinType(jadescript.content.JadescriptProposition.class)
    /*package-private*/ final LazyInit<BaseOntoContentType>
        tProposition = lazyInit(() -> new BaseOntoContentType(
        getModule(),
        OntoContentType.OntoContentKind.Proposition
    ));

    @BuiltinType(jadescript.content.JadescriptPredicate.class)
    /*package-private*/ final LazyInit<BaseOntoContentType>
        tPredicate = lazyInit(() -> new BaseOntoContentType(
        getModule(),
        OntoContentType.OntoContentKind.Predicate
    ));

    @BuiltinType(jadescript.content.onto.basic.InternalException.class)
    /*package-private*/ final LazyInit<OntoContentType> tInternalException =
        lazyInit(() -> new UserDefinedOntoContentType(
            getModule(),
            getModule()
                .get(JvmTypeHelper.class)
                .typeRef(jadescript.content.onto.basic.InternalException.class),
            predicate()
        ));

    @BuiltinType(jadescript.content.JadescriptAtomicProposition.class)
    /*package-private*/ final LazyInit<BaseOntoContentType>
        tAtomicProposition = lazyInit(() -> new BaseOntoContentType(
        getModule(),
        OntoContentType.OntoContentKind.AtomicProposition
    ));

    @BuiltinType(jadescript.content.JadescriptAction.class)
    /*package-private*/ final LazyInit<BaseOntoContentType>
        tAction = lazyInit(() -> new BaseOntoContentType(
        getModule(),
        OntoContentType.OntoContentKind.Action
    ));

    private final LazyInit<TypeSolver> solver =
        lazyInit(() -> getModule().get(TypeSolver.class));

    private final LazyInit<JvmTypeHelper> jvmTypeHelper =
        lazyInit(() -> getModule().get(JvmTypeHelper.class));

    @BuiltinType(jadescript.content.onto.basic.True.class)
    /*package-private*/ final LazyInit<OntoContentType>
        tTrueProposition = lazyInit(() -> {
        return new UserDefinedOntoContentType(
            getModule(),
            jvmTypeHelper.get()
                .typeRef(jadescript.content.onto.basic.True.class),
            atomicProposition()
        );
    });

    @BuiltinType(jadescript.content.onto.basic.False.class)
    /*package-private*/ final LazyInit<OntoContentType>
        tFalseProposition = lazyInit(() -> {
        return new UserDefinedOntoContentType(
            getModule(),
            jvmTypeHelper.get()
                .typeRef(jadescript.content.onto.basic.False.class),
            atomicProposition()
        );
    });

    // Factory for formal type parameters
    private final LazyInit<TypeParameterFactory> ftpFactory =
        lazyInit(() -> getModule().get(TypeParameterFactory.class));
    // Factory for parametric type schemas
    private final LazyInit<ParametricTypeSchemaFactory> ptFactory =
        lazyInit(() -> getModule().get(ParametricTypeSchemaFactory.class));

    @BuiltinType(
        value = JadescriptList.class,
        minArgs = 1,
        maxArgs = 1
    )
    /*package-private*/ final LazyInit<ParametricTypeSchema<ListType>> ptList =
        lazyInit(() -> {
            FormalTypeParameter ftpAny = ftpFactory.get().typeParameter();
            return ptFactory.get().<ListType>parametricType()
                .add(ftpAny)
                .seal(new ParametricMapBuilder<>() {
                    @Override
                    public ListType build()
                        throws InvalidTypeInstantiatonException {
                        return new ListType(
                            getModule(),
                            getArgument(ftpAny)
                        );
                    }


                    @Override
                    public ListType instantiateErroneous(
                        List<? extends TypeArgument> arguments,
                        InvalidTypeInstantiatonException error
                    ){
                        return new ListType(
                            getModule(),
                            any(error.getMessage())
                        );
                    }
                });
        });


    @BuiltinType(
        value = InformRefMessage.class,
        minArgs = 1,
        maxArgs = 1
    )
    @MessageBuiltinType(ACLMessage.INFORM_REF)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptInformRefMessage = lazyInit(() -> {
        final FormalTypeParameter concepts =
            ftpFactory.get().boundedTypeParameter(
                list(covariant(concept()))
            );

        return ptFactory.get().messageType()
            .add(concepts)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        InformRefMessage.class,
                        getArgument(concepts)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        InformRefMessage.class,
                        any(error.getMessage())
                    );
                }
            });
    });

    @BuiltinType(
        value = QueryRefMessage.class,
        minArgs = 1,
        maxArgs = 1
    )
    @MessageBuiltinType(ACLMessage.QUERY_REF)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptQueryRefMessage = lazyInit(() -> {
        final FormalTypeParameter concepts =
            ftpFactory.get().boundedTypeParameter(
                list(covariant(concept()))
            );

        return ptFactory.get().messageType()
            .add(concepts)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        QueryRefMessage.class,
                        getArgument(concepts)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        QueryRefMessage.class,
                        any(error.getMessage())
                    );
                }
            });
    });

    @BuiltinType(
        value = SubscribeMessage.class,
        minArgs = 1,
        maxArgs = 1
    )
    @MessageBuiltinType(ACLMessage.SUBSCRIBE)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptSubscribeMessage = lazyInit(() -> {

        final FormalTypeParameter concepts =
            ftpFactory.get().boundedTypeParameter(list(covariant(concept())));

        return ptFactory.get().messageType()
            .add(concepts)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        SubscribeMessage.class,
                        getArgument(concepts)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        SubscribeMessage.class,
                        any(error.getMessage())
                    );
                }
            });
    });

    @BuiltinType(
        value = ProxyMessage.class,
        minArgs = 2,
        maxArgs = 3
    )
    @MessageBuiltinType(ACLMessage.PROXY)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptProxyMesssage = lazyInit(() -> {
        final FormalTypeParameter aids =
            ftpFactory.get().boundedTypeParameter(list(aid()));

        final FormalTypeParameter message =
            ftpFactory.get().boundedTypeParameter(anyMessage());

        final FormalTypeParameter propositionOrTrue =
            ftpFactory.get().messageTypeParameter(
                proposition(),
                trueProposition(),
                addToTuple(
                    "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                    trueProposition()
                )
            );

        return ptFactory.get().messageType()
            .add(aids)
            .add(message)
            .add(propositionOrTrue)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        ProxyMessage.class,
                        getArgument(aids),
                        getArgument(message),
                        getArgument(propositionOrTrue)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        ProxyMessage.class,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = PropagateMessage.class,
        minArgs = 2,
        maxArgs = 3
    )
    @MessageBuiltinType(ACLMessage.PROPAGATE)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptPropagateMesssage = lazyInit(() -> {
        final FormalTypeParameter aids =
            ftpFactory.get().boundedTypeParameter(list(aid()));

        final FormalTypeParameter message =
            ftpFactory.get().boundedTypeParameter(anyMessage());

        final FormalTypeParameter propositionOrTrue =
            ftpFactory.get().messageTypeParameter(
                proposition(),
                trueProposition(),
                addToTuple(
                    "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                    trueProposition()
                )
            );

        return ptFactory.get().messageType()
            .add(aids)
            .add(message)
            .add(propositionOrTrue)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        PropagateMessage.class,
                        getArgument(aids),
                        getArgument(message),
                        getArgument(propositionOrTrue)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        PropagateMessage.class,
                        any(error)
                    );
                }
            });
    });


    @BuiltinType(
        value = JadescriptSet.class,
        minArgs = 1,
        maxArgs = 1
    )
    /*package-private*/ final LazyInit<ParametricTypeSchema<SetType>> ptSet =
        lazyInit(() -> {
            FormalTypeParameter ftpAny =
                ftpFactory.get().typeParameter();
            return ptFactory.get().<SetType>parametricType()
                .add(ftpAny)
                .seal(new ParametricMapBuilder<>() {
                    @Override
                    public SetType build()
                        throws InvalidTypeInstantiatonException {
                        return new SetType(
                            getModule(),
                            getArgument(ftpAny)
                        );
                    }


                    @Override
                    public SetType instantiateErroneous(
                        List<? extends TypeArgument> arguments,
                        InvalidTypeInstantiatonException error
                    ) {
                        return new SetType(
                            getModule(),
                            any(error)
                        );
                    }
                });
        });

    @BuiltinType(
        value = JadescriptMap.class,
        minArgs = 2,
        maxArgs = 2
    )
    /*package-private*/ final LazyInit<ParametricTypeSchema<MapType>> ptMap =
        lazyInit(() -> {
            FormalTypeParameter ftpAnyKey =
                ftpFactory.get().typeParameter();

            FormalTypeParameter ftpAnyValue =
                ftpFactory.get().typeParameter();
            return ptFactory.get().<MapType>parametricType()
                .add(ftpAnyKey)
                .add(ftpAnyValue)
                .seal(new ParametricMapBuilder<>() {
                    @Override
                    public MapType build()
                        throws InvalidTypeInstantiatonException {
                        return new MapType(
                            getModule(),
                            getArgument(ftpAnyKey),
                            getArgument(ftpAnyValue)
                        );
                    }


                    @Override
                    public MapType instantiateErroneous(
                        List<? extends TypeArgument> arguments,
                        InvalidTypeInstantiatonException error
                    ) {
                        if(arguments.size() == 1){
                            return new MapType(
                                getModule(),
                                arguments.get(0),
                                any(error)
                            );
                        }
                        return new MapType(
                            getModule(),
                            any(error),
                            any("")
                        );
                    }


                });
        });

    @BuiltinType(
        minArgs = 0,
        variadic = true
    )
    /*package-private*/ final LazyInit<ParametricTypeSchema<TupleType>>
        ptTuple = lazyInit(() -> {
        VariadicTypeParameter vtpAny = ftpFactory.get()
            .varadicTypeParameter();
        return ptFactory.get().<TupleType>parametricType()
            .add(vtpAny)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public TupleType build()
                    throws InvalidTypeInstantiatonException {
                    return new TupleType(
                        getModule(),
                        getVariadic(vtpAny)
                    );
                }


                @Override
                public TupleType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new TupleType(
                        getModule(),
                        List.of(any(error))
                    );
                }
            });
    });

    @BuiltinType(
        value = AgentEnv.class,
        minArgs = 2,
        maxArgs = 2
    )
    /*package-private*/ final LazyInit<ParametricTypeSchema<AgentEnvType>>
        ptAgentEnv = lazyInit(() -> {
        FormalTypeParameter ftpAgent =
            ftpFactory.get().boundedTypeParameter(agent());

        FormalTypeParameter ftpAnySE =
            ftpFactory.get().boundedTypeParameter(seModeTop());

        return ptFactory.get().<AgentEnvType>parametricType()
            .add(ftpAgent)
            .add(ftpAnySE)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public AgentEnvType build()
                    throws InvalidTypeInstantiatonException {
                    final TypeArgument agentType = getArgument(ftpAgent);
                    final TypeArgument sideEffect = getArgument(ftpAnySE);
                    return new AgentEnvType(
                        getModule(),
                        agentType,
                        sideEffect
                    );
                }


                @Override
                public AgentEnvType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new AgentEnvType(
                        getModule(),
                        any(error),
                        seModeTop()
                    );
                }
            });
    });

    @BuiltinType
    /*package-private*/ final LazyInit<AgentEnvType> tAnyAgentEnv =
        lazyInit(() -> agentEnv(covariant(agent()), covariant(seModeTop())));

    @BuiltinType(
        value = jadescript.core.message.Message.class,
        minArgs = 1,
        maxArgs = 1
    )
    /*package-private*/ final LazyInit<ParametricTypeSchema<BaseMessageType>>
        ptMessage = lazyInit(() -> {
        FormalTypeParameter ftpAny =
            ftpFactory.get().typeParameter();

        return ptFactory.get().<BaseMessageType>parametricType()
            .add(ftpAny)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public BaseMessageType build()
                    throws InvalidTypeInstantiatonException {
                    return new BaseMessageType(
                        getModule(),
                        getArgument(ftpAny)
                    );
                }


                @Override
                public BaseMessageType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new BaseMessageType(
                        getModule(),
                        any(error)
                    );
                }
            });

    });

    @BuiltinType(
        value = jadescript.core.behaviours.Behaviour.class,
        minArgs = 0,
        maxArgs = 1
    )
    /*package-private*/ final LazyInit<ParametricTypeSchema<BaseBehaviourType>>
        ptBehaviour = lazyInit(() -> {
        FormalTypeParameter ftpAnyAgent =
            ftpFactory.get()
                .boundedTypeParameterWithDefault(agent(), covariant(agent()));

        return ptFactory.get().<BaseBehaviourType>parametricType()
            .add(ftpAnyAgent)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public BaseBehaviourType build()
                    throws InvalidTypeInstantiatonException {
                    return new BaseBehaviourType(
                        getModule(),
                        BehaviourType.Kind.Base,
                        getArgument(ftpAnyAgent)
                    );
                }


                @Override
                public BaseBehaviourType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new BaseBehaviourType(
                        getModule(),
                        BehaviourType.Kind.Base,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType
    /*package-private*/ final LazyInit<BaseBehaviourType>
        tAnyBehaviour = lazyInit(() -> behaviour(covariant(agent())));

    @BuiltinType(
        value = jadescript.core.behaviours.CyclicBehaviour.class,
        minArgs = 0,
        maxArgs = 1
    )
    /*package-private*/ final LazyInit<ParametricTypeSchema<BaseBehaviourType>>
        ptCyclicBehaviour = lazyInit(() -> {
        FormalTypeParameter ftpAnyAgent =
            ftpFactory.get().boundedTypeParameterWithDefault(
                agent(), covariant(agent())
            );

        return ptFactory.get().<BaseBehaviourType>parametricType()
            .add(ftpAnyAgent)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public BaseBehaviourType build()
                    throws InvalidTypeInstantiatonException {
                    return new BaseBehaviourType(
                        getModule(),
                        BehaviourType.Kind.Cyclic,
                        getArgument(ftpAnyAgent)
                    );
                }


                @Override
                public BaseBehaviourType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new BaseBehaviourType(
                        getModule(),
                        BehaviourType.Kind.Cyclic,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = jadescript.core.behaviours.OneShotBehaviour.class,
        minArgs = 0,
        maxArgs = 1
    )
    /*package-private*/ final LazyInit<ParametricTypeSchema<BaseBehaviourType>>
        ptOneshotBehaviour = lazyInit(() -> {
        FormalTypeParameter ftpAnyAgent =
            ftpFactory.get().boundedTypeParameterWithDefault(
                agent(), covariant(agent())
            );

        return ptFactory.get().<BaseBehaviourType>parametricType()
            .add(ftpAnyAgent)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public BaseBehaviourType build()
                    throws InvalidTypeInstantiatonException {
                    return new BaseBehaviourType(
                        getModule(),
                        BehaviourType.Kind.OneShot,
                        getArgument(ftpAnyAgent)
                    );
                }


                @Override
                public BaseBehaviourType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new BaseBehaviourType(
                        getModule(),
                        BehaviourType.Kind.OneShot,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = AcceptProposalMessage.class,
        minArgs = 1,
        maxArgs = 2
    )
    @MessageBuiltinType(ACLMessage.ACCEPT_PROPOSAL)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptAcceptProposalMessage = lazyInit(() -> {
        final FormalTypeParameter action =
            ftpFactory.get().boundedTypeParameter(action());

        final FormalTypeParameter defaultProp =
            ftpFactory.get().messageTypeParameter(
                proposition(),
                trueProposition(),
                promoteToTuple2(
                    "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                    trueProposition()
                )
            );

        return ptFactory.get().messageType()
            .add(action)
            .add(defaultProp)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        AcceptProposalMessage.class,
                        getArgument(action),
                        getArgument(defaultProp)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        AcceptProposalMessage.class,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = AgreeMessage.class,
        minArgs = 1,
        maxArgs = 2
    )
    @MessageBuiltinType(ACLMessage.AGREE)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptAgreeMessage = lazyInit(() -> {
        final FormalTypeParameter action =
            ftpFactory.get().boundedTypeParameter(action());

        final FormalTypeParameter defaultProp =
            ftpFactory.get().messageTypeParameter(
                proposition(),
                trueProposition(),
                promoteToTuple2(
                    "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                    trueProposition()
                )
            );

        return ptFactory.get().messageType()
            .add(action)
            .add(defaultProp)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        AgreeMessage.class,
                        getArgument(action),
                        getArgument(defaultProp)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        AgreeMessage.class,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = CancelMessage.class,
        minArgs = 1,
        maxArgs = 1
    )
    @MessageBuiltinType(ACLMessage.CANCEL)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptCancelMessage = lazyInit(() -> {
        final FormalTypeParameter action =
            ftpFactory.get().boundedTypeParameter(action());

        return ptFactory.get().messageType()
            .add(action)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        CancelMessage.class,
                        getArgument(action)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        CancelMessage.class,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = CFPMessage.class,
        minArgs = 1,
        maxArgs = 2
    )
    @MessageBuiltinType(ACLMessage.CFP)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptCFPMessage = lazyInit(() -> {
        final FormalTypeParameter action =
            ftpFactory.get().boundedTypeParameter(action());

        final FormalTypeParameter defaultProp =
            ftpFactory.get().messageTypeParameter(
                proposition(),
                trueProposition(),
                promoteToTuple2(
                    "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                    trueProposition()
                )
            );

        return ptFactory.get().messageType()
            .add(action)
            .add(defaultProp)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        CFPMessage.class,
                        getArgument(action),
                        getArgument(defaultProp)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        CFPMessage.class,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = ConfirmMessage.class,
        minArgs = 1,
        maxArgs = 1
    )
    @MessageBuiltinType(ACLMessage.CONFIRM)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptConfirmMessage = lazyInit(() -> {
        final FormalTypeParameter proposition =
            ftpFactory.get().boundedTypeParameter(proposition());


        return ptFactory.get().messageType()
            .add(proposition)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        ConfirmMessage.class,
                        getArgument(proposition)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        ConfirmMessage.class,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = DisconfirmMessage.class,
        minArgs = 1,
        maxArgs = 1
    )
    @MessageBuiltinType(ACLMessage.DISCONFIRM)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptDisconfirmMessage = lazyInit(() -> {
        final FormalTypeParameter proposition =
            ftpFactory.get().boundedTypeParameter(proposition());


        return ptFactory.get().messageType()
            .add(proposition)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        DisconfirmMessage.class,
                        getArgument(proposition)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        DisconfirmMessage.class,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = FailureMessage.class,
        minArgs = 1,
        maxArgs = 2
    )
    @MessageBuiltinType(ACLMessage.FAILURE)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptFailureMessage = lazyInit(() -> {
        final FormalTypeParameter action =
            ftpFactory.get().boundedTypeParameter(action());

        final FormalTypeParameter defaultProp =
            ftpFactory.get().messageTypeParameter(
                proposition(),
                trueProposition(),
                promoteToTuple2(
                    "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                    trueProposition()
                )
            );


        return ptFactory.get().messageType()
            .add(action)
            .add(defaultProp)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        FailureMessage.class,
                        getArgument(action),
                        getArgument(defaultProp)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        FailureMessage.class,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = InformMessage.class,
        minArgs = 1,
        maxArgs = 1
    )
    @MessageBuiltinType(ACLMessage.INFORM)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptInformMessage = lazyInit(() -> {
        final FormalTypeParameter proposition =
            ftpFactory.get().boundedTypeParameter(proposition());

        return ptFactory.get().messageType()
            .add(proposition)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        InformMessage.class,
                        getArgument(proposition)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        InformMessage.class,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = InformIfMessage.class,
        minArgs = 1,
        maxArgs = 1
    )
    @MessageBuiltinType(ACLMessage.INFORM_IF)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptInformIfMessage = lazyInit(() -> {
        final FormalTypeParameter proposition =
            ftpFactory.get().boundedTypeParameter(proposition());

        return ptFactory.get().messageType()
            .add(proposition)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        InformIfMessage.class,
                        getArgument(proposition)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        InformIfMessage.class,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = NotUnderstoodMessage.class,
        minArgs = 1,
        maxArgs = 2
    )
    @MessageBuiltinType(ACLMessage.NOT_UNDERSTOOD)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptNotUnderstoodMessage = lazyInit(() -> {
        final FormalTypeParameter anyMessage =
            ftpFactory.get().boundedTypeParameter(anyMessage());

        final FormalTypeParameter propositionOrTrue =
            ftpFactory.get().messageTypeParameter(
                proposition(),
                trueProposition(),
                promoteToTuple2(
                    "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                    trueProposition()
                )
            );

        return ptFactory.get().messageType()
            .add(anyMessage)
            .add(propositionOrTrue)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        NotUnderstoodMessage.class,
                        getArgument(anyMessage),
                        getArgument(propositionOrTrue)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        NotUnderstoodMessage.class,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = ProposeMessage.class,
        minArgs = 1,
        maxArgs = 2
    )
    @MessageBuiltinType(ACLMessage.PROPOSE)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptProposeMessage = lazyInit(() -> {
        final FormalTypeParameter action =
            ftpFactory.get().boundedTypeParameter(action());

        final FormalTypeParameter propositionOrTrue =
            ftpFactory.get().messageTypeParameter(
                proposition(),
                trueProposition(),
                promoteToTuple2(
                    "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                    trueProposition()
                )
            );

        return ptFactory.get().messageType()
            .add(action)
            .add(propositionOrTrue)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        ProposeMessage.class,
                        getArgument(action),
                        getArgument(propositionOrTrue)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        ProposeMessage.class,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = QueryIfMessage.class,
        minArgs = 1,
        maxArgs = 1
    )
    @MessageBuiltinType(ACLMessage.QUERY_IF)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptQueryIfMessage = lazyInit(() -> {
        final FormalTypeParameter proposition =
            ftpFactory.get().boundedTypeParameter(proposition());

        return ptFactory.get().messageType()
            .add(proposition)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        QueryIfMessage.class,
                        getArgument(proposition)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        QueryIfMessage.class,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = RefuseMessage.class,
        minArgs = 1,
        maxArgs = 2
    )
    @MessageBuiltinType(ACLMessage.REFUSE)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptRefuseMessage = lazyInit(() -> {
        final FormalTypeParameter action =
            ftpFactory.get().boundedTypeParameter(action());

        final FormalTypeParameter propositionOrTrue =
            ftpFactory.get().messageTypeParameter(
                proposition(),
                trueProposition(),
                promoteToTuple2(
                    "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                    trueProposition()
                )
            );

        return ptFactory.get().messageType()
            .add(action)
            .add(propositionOrTrue)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        RefuseMessage.class,
                        getArgument(action),
                        getArgument(propositionOrTrue)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        RefuseMessage.class,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = RejectProposalMessage.class,
        minArgs = 1,
        maxArgs = 3
    )
    @MessageBuiltinType(ACLMessage.REJECT_PROPOSAL)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptRejectProposalMessage = lazyInit(() -> {
        final FormalTypeParameter action =
            ftpFactory.get().boundedTypeParameter(action());

        final FormalTypeParameter propositionOrTrue1 =
            ftpFactory.get().messageTypeParameter(
                proposition(),
                trueProposition(),
                promoteToTuple2(
                    "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                    trueProposition()
                )
            );

        final FormalTypeParameter propositionOrTrue2 =
            ftpFactory.get().messageTypeParameter(
                proposition(),
                trueProposition(),
                addToTuple(
                    "/*default value*/" + ONTOLOGY_TRUE_VALUE,
                    trueProposition()
                )
            );

        return ptFactory.get().messageType()
            .add(action)
            .add(propositionOrTrue1)
            .add(propositionOrTrue2)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        RejectProposalMessage.class,
                        getArgument(action),
                        getArgument(propositionOrTrue1),
                        getArgument(propositionOrTrue2)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        RejectProposalMessage.class,
                        any(error)
                    );
                }


            });
    });

    @BuiltinType(
        value = RequestMessage.class,
        minArgs = 1,
        maxArgs = 1
    )
    @MessageBuiltinType(ACLMessage.REQUEST)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptRequestMessage = lazyInit(() -> {
        final FormalTypeParameter action =
            ftpFactory.get().boundedTypeParameter(action());


        return ptFactory.get().messageType()
            .add(action)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        RequestMessage.class,
                        getArgument(action)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        RequestMessage.class,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = RequestWhenMessage.class,
        minArgs = 2,
        maxArgs = 2
    )
    @MessageBuiltinType(ACLMessage.REQUEST_WHEN)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptRequestWhenMessage = lazyInit(() -> {
        final FormalTypeParameter action =
            ftpFactory.get().boundedTypeParameter(action());

        final FormalTypeParameter proposition =
            ftpFactory.get().boundedTypeParameter(proposition());

        return ptFactory.get().messageType()
            .add(action)
            .add(proposition)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        RequestWhenMessage.class,
                        getArgument(action),
                        getArgument(proposition)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        RequestWheneverMessage.class,
                        any(error)
                    );
                }

            });
    });

    @BuiltinType(
        value = RequestWheneverMessage.class,
        minArgs = 2,
        maxArgs = 2
    )
    @MessageBuiltinType(ACLMessage.REQUEST_WHENEVER)
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptRequestWheneverMessage = lazyInit(() -> {
        final FormalTypeParameter action =
            ftpFactory.get().boundedTypeParameter(action());

        final FormalTypeParameter proposition =
            ftpFactory.get().boundedTypeParameter(proposition());

        return ptFactory.get().messageType()
            .add(action)
            .add(proposition)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        RequestWheneverMessage.class,
                        getArgument(action),
                        getArgument(proposition)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        RequestWheneverMessage.class,
                        any(error)
                    );
                }
            });
    });

    @BuiltinType(
        value = UnknownMessage.class,
        minArgs = 1,
        maxArgs = 1
    )
    @MessageBuiltinType()
    /*package-private*/ final LazyInit<MessageTypeSchema>
        ptUnknownMessage = lazyInit(() -> {
        final FormalTypeParameter anyElement =
            ftpFactory.get().boundedTypeParameter(anyOntologyElement());

        return ptFactory.get().messageType()
            .add(anyElement)
            .seal(new ParametricMapBuilder<>() {
                @Override
                public MessageSubType build()
                    throws InvalidTypeInstantiatonException {
                    return new MessageSubType(
                        getModule(),
                        UnknownMessage.class,
                        getArgument(anyElement)
                    );
                }


                @Override
                public MessageSubType instantiateErroneous(
                    List<? extends TypeArgument> arguments,
                    InvalidTypeInstantiatonException error
                ) {
                    return new MessageSubType(
                        getModule(),
                        UnknownMessage.class,
                        any(error)
                    );
                }
            });
    });


    public BuiltinTypeProvider(SemanticsModule module) {
        this.module = module;
    }


    public MessageSubType unknownMessage(OntoContentType anyOntoContentType) {
        return ptUnknownMessage.get().create(anyOntoContentType);
    }


    public MessageSubType propagateMessage(
        ListType aids,
        MessageType message,
        OntoContentType proposition
    ) {
        return ptPropagateMesssage.get().create(aids, message, proposition);
    }


    @SuppressWarnings("SameParameterValue")
    private static BiFunction<TypeArgument, String, String> promoteToTuple2(
        String defaultValue,
        TypeArgument defaultType
    ) {
        return (inputType, inputExpression) -> TupleType.compileNewInstance(
            List.of(inputExpression, defaultValue),
            List.of(inputType, defaultType)
        );
    }


    @SuppressWarnings("SameParameterValue")
    private static BiFunction<TypeArgument, String, String> addToTuple(
        String defaultValue,
        TypeArgument defaultType
    ) {
        return (inputType, inputExpression) -> TupleType.compileAddToTuple(
            inputExpression,
            defaultValue,
            defaultType
        );
    }


    public MessageSubType propagateMessage(
        ListType aids,
        MessageType message
    ) {
        return ptPropagateMesssage.get().create(aids, message);
    }


    public MessageSubType proxyMessage(
        ListType aids,
        MessageType message,
        OntoContentType proposition
    ) {
        return ptProxyMesssage.get().create(aids, message, proposition);
    }


    public MessageSubType proxyMessage(
        ListType aids,
        MessageType message
    ) {
        return ptProxyMesssage.get().create(aids, message);
    }


    public MessageSubType subscribeMessage(ListType concepts) {
        return ptSubscribeMessage.get().create(concepts);
    }


    public MessageSubType requestWhenMessage(
        OntoContentType action,
        OntoContentType proposition
    ) {
        return ptRequestWhenMessage.get().create(action, proposition);
    }


    public MessageSubType requestWheneverMessage(
        OntoContentType action,
        OntoContentType proposition
    ) {
        return ptRequestWheneverMessage.get().create(action, proposition);
    }


    public MessageSubType requestMessage(OntoContentType action) {
        return ptRequestMessage.get().create(action);
    }


    public MessageSubType rejectProposalMessage(
        OntoContentType action,
        OntoContentType proposition1,
        OntoContentType proposition2
    ) {
        return ptRejectProposalMessage.get().create(
            action,
            proposition1,
            proposition2
        );
    }


    public MessageSubType rejectProposalMessage(
        OntoContentType action,
        OntoContentType proposition
    ) {
        return ptRejectProposalMessage.get().create(
            action,
            proposition
        );
    }


    public MessageSubType rejectProposalMessage(OntoContentType action) {
        return ptRejectProposalMessage.get().create(action);
    }


    public MessageSubType refuseMessage(
        OntoContentType action,
        OntoContentType proposition
    ) {
        return ptRefuseMessage.get().create(action, proposition);
    }


    public MessageSubType refuseMessage(
        OntoContentType action
    ) {
        return ptRefuseMessage.get().create(action);
    }


    public MessageSubType notUnderstoodMessage(
        MessageType message,
        OntoContentType proposition
    ) {
        return ptNotUnderstoodMessage.get().create(message, proposition);
    }


    public MessageSubType notUnderstoodMessage(
        MessageType message
    ) {
        return ptNotUnderstoodMessage.get().create(message);
    }


    public MessageSubType proposeMessage(
        OntoContentType action,
        OntoContentType proposition
    ) {
        return ptProposeMessage.get().create(action, proposition);
    }


    public MessageSubType proposeMessage(
        OntoContentType action
    ) {
        return ptProposeMessage.get().create(action);
    }


    public MessageSubType queryIfMessage(
        OntoContentType proposition
    ) {
        return ptQueryIfMessage.get().create(proposition);
    }


    public MessageSubType informRefMessage(ListType concepts) {
        return ptInformRefMessage.get().create(concepts);
    }


    public MessageSubType queryRefMessage(ListType concepts) {
        return ptQueryRefMessage.get().create(concepts);
    }


    public MessageSubType informIfMessage(OntoContentType proposition) {
        return ptInformIfMessage.get().create(proposition);
    }


    public MessageSubType disconfirmMessage(
        OntoContentType proposition
    ) {
        return ptDisconfirmMessage.get().create(proposition);
    }


    public MessageSubType informMessage(OntoContentType proposition) {
        return ptInformMessage.get().create(proposition);
    }


    public MessageSubType confirmMessage(
        OntoContentType proposition
    ) {
        return ptConfirmMessage.get().create(proposition);
    }


    public MessageSubType cancelMessage(
        OntoContentType action
    ) {
        return ptCancelMessage.get().create(action);
    }


    public MessageSubType cfpMessage(
        OntoContentType action,
        OntoContentType predicate
    ) {
        return ptCFPMessage.get().create(action, predicate);
    }


    public MessageSubType cfpMessage(
        OntoContentType action
    ) {
        return ptCFPMessage.get().create(action);
    }


    public MessageSubType agreeMessage(
        OntoContentType action,
        OntoContentType predicate
    ) {
        return ptAgreeMessage.get().create(action, predicate);
    }


    public MessageSubType agreeMessage(
        OntoContentType action
    ) {
        return ptAgreeMessage.get().create(action);
    }


    public MessageSubType failureMessage(
        OntoContentType action,
        OntoContentType predicate
    ) {
        return ptFailureMessage.get().create(action, predicate);
    }


    public MessageSubType failureMessage(
        OntoContentType action
    ) {
        return ptFailureMessage.get().create(action);
    }


    public MessageSubType acceptProposalMessage(
        OntoContentType action,
        OntoContentType proposition
    ) {
        return ptAcceptProposalMessage.get().create(
            action, proposition
        );
    }


    public MessageSubType acceptProposalMessage(
        OntoContentType action
    ) {
        return ptAcceptProposalMessage.get().create(action);
    }


    private <T extends IJadescriptType> LazyInit<T> fromModule(
        Class<? extends T> clz
    ) {
        return lazyInit(() -> getModule().get(clz));
    }


    public AnyType any(String errorMessage) {
        return new AnyType(getModule(), errorMessage);
    }

    public AnyType any(Throwable t){
        return new AnyType(getModule(), t.getMessage());
    }


    public NothingType nothing(String errorMessage) {
        return new NothingType(getModule(), errorMessage);
    }


    public IntegerType integer() {
        return tInteger.get();
    }


    public BooleanType boolean_() {
        return tBoolean.get();
    }


    public TextType text() {
        return tText.get();
    }


    public PerformativeType performative() {
        return tPerformative.get();
    }


    public RealType real() {
        return tReal.get();
    }


    public AIDType aid() {
        return tAID.get();
    }


    public TimestampType timestamp() {
        return tTimestamp.get();
    }


    public DurationType duration() {
        return tDuration.get();
    }


    public ListType list(TypeArgument elementType) {
        return ptList.get().create(elementType);
    }


    public SetType set(TypeArgument elementType) {
        return ptSet.get().create(elementType);
    }


    public MapType map(TypeArgument keyType, TypeArgument valueType) {
        return ptMap.get().create(keyType, valueType);
    }


    public TupleType tuple(List<TypeArgument> typeArguments) {
        return ptTuple.get().create(typeArguments);
    }


    public TupleType tuple(TypeArgument... typeArguments) {
        return tuple(Arrays.asList(typeArguments));
    }


    public BaseAgentType agent() {
        return tAgent.get();
    }


    public IJadescriptType seModeBottom() {
        return tSEModeBottom.get();
    }


    public IJadescriptType seModeTop() {
        return tSEModeTop.get();
    }


    public IJadescriptType seModeWithSE() {
        return tSEModeWithSE.get();
    }


    public IJadescriptType seModeNoSE() {
        return tSEModeNoSE.get();
    }


    public BaseOntologyType ontology() {
        return tOntology.get();
    }


    public AgentEnvType anyAgentEnv() {
        return tAnyAgentEnv.get();
    }


    public BaseBehaviourType behaviour(TypeArgument agentType) {
        return ptBehaviour.get().create(agentType);
    }


    public BaseBehaviourType behaviour() {
        return ptBehaviour.get().create();
    }


    public BaseBehaviourType anyBehaviour() {
        return tAnyBehaviour.get();
    }


    public BaseBehaviourType cyclicBehaviour(TypeArgument agent) {
        return ptCyclicBehaviour.get().create(agent);
    }


    public BaseBehaviourType cyclicBehaviour() {
        return ptCyclicBehaviour.get().create();
    }


    public BaseBehaviourType oneshotBehaviour(TypeArgument agent) {
        return ptOneshotBehaviour.get().create(agent);
    }


    public BaseBehaviourType oneshotBehaviour() {
        return ptOneshotBehaviour.get().create();
    }


    public BaseOntoContentType concept() {
        return tConcept.get();
    }


    public BaseOntoContentType predicate() {
        return tPredicate.get();
    }


    public BaseOntoContentType atomicProposition() {
        return tAtomicProposition.get();
    }


    public BaseOntoContentType proposition() {
        return tProposition.get();
    }


    public BaseOntoContentType action() {
        return tAction.get();
    }


    public OntoContentType trueProposition() {
        return tTrueProposition.get();
    }


    public OntoContentType falseProposition() {
        return tFalseProposition.get();
    }


    public OntoContentType internalException() {
        return tInternalException.get();
    }


    public BaseMessageType message(TypeArgument contentType) {
        return ptMessage.get().create(contentType);
    }


    private BoundedTypeArgument contravariant(
        IJadescriptType type
    ) {
        return new BoundedTypeArgument(
            module,
            type,
            BoundedTypeArgument.Variance.SUPER
        );
    }


    private BoundedTypeArgument covariant(
        IJadescriptType type
    ) {
        return new BoundedTypeArgument(
            module,
            type,
            BoundedTypeArgument.Variance.EXTENDS
        );
    }


    public AgentEnvType agentEnv(
        TypeArgument agentType,
        TypeArgument seMode
    ) {
        return ptAgentEnv.get().create(List.of(agentType, seMode));
    }


    public JavaVoidType javaVoid() {
        return tVoid.get();
    }


    public AnyOntologyElementType anyOntologyElement() {
        return tAnyOntologyElement.get();
    }


    public BaseMessageType anyMessage() {
        return tAnyMessage.get();
    }


    private SemanticsModule getModule() {
        return this.module;
    }


}
