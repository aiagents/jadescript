package it.unipr.ailab.jadescript.semantics.jadescripttypes.index;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.c0outer.RawTypeReferenceSolverContext;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.UnknownJVMType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agent.UserDefinedAgentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour.BaseBehaviourType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour.BehaviourType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour.UserDefinedBehaviourType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.message.MessageType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontocontent.UserDefinedOntoContentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.UserDefinedOntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.InvalidTypeInstantiatonException;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.MessageTypeSchema;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.ParametricTypeSchema;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.maybe.Maybe;
import jade.content.onto.Ontology;
import jadescript.content.*;
import jadescript.core.Agent;
import jadescript.core.behaviours.*;
import jadescript.lang.Performative;
import jadescript.lang.Tuple;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.*;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;
import static jadescript.lang.Performative.performativeByName;

public class TypeSolver {

    private static final boolean storeNonBuiltins = true;

    private final SemanticsModule module;
    private final TypeIndex index;
    private final BuiltinTypeProvider builtinTypes;
    private final JvmTypeHelper jvm;


    public TypeSolver(SemanticsModule module) {
        this.module = module;
        this.index = new TypeIndex(module);
        this.jvm = module.get(JvmTypeHelper.class);
        builtinTypes = module.get(BuiltinTypeProvider.class);
    }


    private TypeArgument solveWildcardTypeReference(
        JvmWildcardTypeReference reference,
        boolean permissive
    ) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);

        final EList<JvmTypeConstraint> constraints = reference.getConstraints();
        final String errorMessage = "TOP type produced from " +
            "JvmWildcardTypeReference without constraints";
        if (constraints == null) {
            return typeHelper.covariant(builtinTypes.any(errorMessage));
        }

        for (JvmTypeConstraint constraint : constraints) {
            if (constraint instanceof JvmLowerBound) {
                return typeHelper.contravariant(
                    solveJvmTypeReferenceWithoutReattempts(
                        constraint.getTypeReference(),
                        permissive
                    ).ignoreBound()
                );
            }

            if (constraint instanceof JvmUpperBound) {
                return typeHelper.covariant(
                    solveJvmTypeReferenceWithoutReattempts(
                        constraint.getTypeReference(),
                        permissive
                    ).ignoreBound()
                );
            }
        }

        return typeHelper.covariant(builtinTypes.any(errorMessage));

    }


    private TypeArgument solveJvmTypeReferenceWithoutReattempts(
        JvmTypeReference reference,
        boolean permissive
    ) {
        if (reference instanceof JvmWildcardTypeReference) {
            JvmWildcardTypeReference wildcard =
                (JvmWildcardTypeReference) reference;

            return solveWildcardTypeReference(wildcard, permissive);

        }

        final Maybe<IJadescriptType> fromJVMTypeReference =
            this.solveFromIndex(reference);

        IJadescriptType result;
        if (fromJVMTypeReference.isPresent()) {
            result = fromJVMTypeReference.toNullable();
        } else if (reference instanceof JvmParameterizedTypeReference
            && jvm.isAssignable(Tuple.class, reference)) {
            result = solveTupleType(
                ((JvmParameterizedTypeReference) reference)
            );
        } else {
            result = resolveNonBuiltinType(reference, permissive);
            if (storeNonBuiltins && !result.category().isUnknownJVM()) {
                index.store(reference, result);
            }
        }
        return result;
    }


    private Maybe<IJadescriptType> solveFromIndex(
        @NotNull JvmTypeReference typeReference
    ) {

        final String qualifiedName = typeReference.getQualifiedName('.');

        if (index.getTypeTable().containsKey(qualifiedName)) {
            return some(index.getTypeTable().get(qualifiedName).get());
        }

        final String noGenericsTypeName = JvmTypeHelper.noGenericsTypeName(
            qualifiedName);

        if (!index.getParametricTypeTable().containsKey(noGenericsTypeName)
            || !(typeReference instanceof JvmParameterizedTypeReference)) {
            return nothing();
        }

        List<TypeArgument> args = new ArrayList<>();

        final EList<JvmTypeReference> jvmPTRArgs =
            ((JvmParameterizedTypeReference) typeReference).getArguments();

        if (jvmPTRArgs == null) {
            return nothing();
        }

        for (JvmTypeReference arg : jvmPTRArgs) {
            args.add(fromJvmTypeReference(arg));
        }

        final ParametricTypeSchema<? extends IJadescriptType> schema =
            index.getParametricTypeTable().get(
                noGenericsTypeName).get();

        if (schema.isApplicable(args)) {
            try {
                return some(schema.create(args));
            } catch (InvalidTypeInstantiatonException e) {
                e.printStackTrace();
                return nothing();
            }
        }

        return nothing();
    }


    public TypeArgument fromJvmTypeReference(
        JvmTypeReference reference
    ) {
        return fromJvmTypeReference(reference, false);
    }


    public TypeArgument fromJvmTypeReference(
        JvmTypeReference reference, boolean permissive
    ) {
        TypeArgument result = solveJvmTypeReferenceWithoutReattempts(
            reference,
            permissive
        );

        if (!(result instanceof IJadescriptType)) {
            return result;
        }

        IJadescriptType resultType = (IJadescriptType) result;

        if (resultType.category().isJavaVoid()) {
            ICompositeNode node = NodeModelUtils.getNode(reference);
            if (node == null) {
                node = NodeModelUtils.findActualNodeFor(reference);
            }

            if (node != null) {
                final INode finalNode = node;
                final JvmTypeReference reattempt =
                    module.get(ContextManager.class).currentContext().searchAs(
                        RawTypeReferenceSolverContext.class,
                        solver -> solver.rawResolveTypeReference(
                            finalNode.getText().trim()
                        )
                    ).findAny().orElse(jvm.typeRef(finalNode.getText()));

                return solveJvmTypeReferenceWithoutReattempts(
                    reattempt,
                    permissive
                );
            }
        }
        return result;
    }


    private IJadescriptType solveTupleType(
        JvmParameterizedTypeReference reference
    ) {
        List<TypeArgument> args = new ArrayList<>();
        for (JvmTypeReference arg : reference.getArguments()) {
            args.add(fromJvmTypeReference(arg));
        }

        return builtinTypes.tuple(args);

    }


    private IJadescriptType resolveNonBuiltinType(
        JvmTypeReference reference, boolean permissive
    ) {
        if (jvm.isAssignable(JadescriptConcept.class, reference)) {
            return new UserDefinedOntoContentType(
                module,
                reference,
                builtinTypes.concept()
            );
        } else if (jvm.isAssignable(JadescriptAction.class, reference)) {
            return new UserDefinedOntoContentType(
                module,
                reference,
                builtinTypes.action()
            );
        } else if (jvm.isAssignable(JadescriptPredicate.class, reference)) {
            return new UserDefinedOntoContentType(
                module,
                reference,
                builtinTypes.predicate()
            );
        } else if (jvm.isAssignable(
            JadescriptAtomicProposition.class,
            reference
        )) {
            return new UserDefinedOntoContentType(
                module,
                reference,
                builtinTypes.atomicProposition()
            );
        } else if (jvm.isAssignable(JadescriptProposition.class, reference)) {
            return new UserDefinedOntoContentType(
                module,
                reference,
                builtinTypes.proposition()
            );
        } else if (jvm.isAssignable(Agent.class, reference)) {
            return new UserDefinedAgentType(
                module,
                reference,
                builtinTypes.agent()
            );
        } else if (jvm.isAssignable(Cyclic.class, reference)) {
            return resolveUserDefinedBehaviourType(
                reference,
                BehaviourType.Kind.Cyclic
            );
        } else if (jvm.isAssignable(OneShot.class, reference)) {
            return resolveUserDefinedBehaviourType(
                reference,
                BehaviourType.Kind.OneShot
            );
        } else if (jvm.isAssignable(Base.class, reference)) {
            return resolveUserDefinedBehaviourType(
                reference,
                BehaviourType.Kind.Base
            );
        } else if (jvm.isAssignable(Ontology.class, reference)) {
            return new UserDefinedOntologyType(
                module,
                reference,
                builtinTypes.ontology()
            );
        }

        return new UnknownJVMType(module, reference, permissive);

    }


    public IJadescriptType resolveUserDefinedBehaviourType(
        JvmTypeReference reference,
        BehaviourType.Kind kind
    ) {
        Class<?> rootClass;
        Function<TypeArgument, BaseBehaviourType> builtinWithArg;
        Supplier<BaseBehaviourType> builtinWithoutArg;
        switch (kind) {
            case Cyclic:
                rootClass = CyclicBehaviour.class;
                builtinWithArg = builtinTypes::cyclicBehaviour;
                builtinWithoutArg = builtinTypes::cyclicBehaviour;
                break;
            case OneShot:
                rootClass = OneShotBehaviour.class;
                builtinWithArg = builtinTypes::oneshotBehaviour;
                builtinWithoutArg = builtinTypes::oneshotBehaviour;
                break;
            default:
            case Base:
                rootClass = Behaviour.class;
                builtinWithArg = builtinTypes::behaviour;
                builtinWithoutArg = builtinTypes::behaviour;
                break;
        }
        final List<JvmTypeReference> args =
            jvm.getTypeArgumentsOfParent(
                reference,
                jvm.typeRef(rootClass)
            );
        BaseBehaviourType rootCategoryType;
        if (args.size() == 1) {
            rootCategoryType = builtinWithArg.apply(
                fromJvmTypeReference(args.get(0))
            );

        } else {
            rootCategoryType = builtinWithoutArg.get();
        }

        final Optional<JvmTypeReference> extendedBehaviour =
            jvm.getParentClasses(reference).findFirst();

        Maybe<IJadescriptType> superType;
        if (extendedBehaviour.isPresent()) {
            final TypeArgument typeArgument = fromJvmTypeReference(
                extendedBehaviour.get(),
                true
            );
            if (typeArgument instanceof IJadescriptType) {
                superType = some(((IJadescriptType) typeArgument));
            } else {
                superType = nothing();
            }
        } else {
            superType = nothing();
        }


        return new UserDefinedBehaviourType(
            module,
            reference,
            superType,
            rootCategoryType
        );
    }


    public IJadescriptType fromJvmType(
        JvmDeclaredType itClass, JvmTypeReference... typeArguments
    ) {
        return fromJvmTypeReference(this.jvm.typeRef(itClass, typeArguments))
            .ignoreBound();
    }


    public IJadescriptType fromJvmTypePermissive(
        JvmDeclaredType itClass, JvmTypeReference... typeArguments
    ) {
        final TypeArgument typeArgument = fromJvmTypeReference(
            this.jvm.typeRef(itClass, typeArguments),
            /*permissive = */true
        );

        return typeArgument.ignoreBound();
    }


    public ParametricTypeSchema<? extends MessageType>
    getMessageTypeSchemaForPerformative(
        @Nullable Performative performative
    ) {
        if (performative == null) {
            return builtinTypes.ptMessage.get();
        }
        return index.getPerformativeToMessageSubtypeMap()
            .get(performative).get();
    }


    public ParametricTypeSchema<? extends MessageType>
    getMessageTypeSchemaForTypeName(String messageTypeName) {
        return getMessageTypeSchemaForPerformative(
            index.getMessageClassToPerformativeMap().get(messageTypeName)
        );
    }


    @SuppressWarnings("unchecked")
    public List<TypeArgument> getDefaultTypeArguments(String messageTypeName) {

        final @Nullable Performative performative =
            index.getMessageClassToPerformativeMap().get(messageTypeName);

        final IJadescriptType contentBound =
            getContentBoundForPerformative(performative);

        if (contentBound.isErroneous()) {
            return List.of();
        }

        return (List<TypeArgument>) module.get(TypeHelper.class)
            .unpackTuple(contentBound);
    }


    public IJadescriptType getContentBoundForPerformative(
        @Nullable Performative performative
    ) {
        if(performative == null){
            return builtinTypes.any("");
        }

        final List<IJadescriptType> upperBounds =
            index.getPerformativeToMessageSubtypeMap().get(performative)
                .get().getUpperBounds();

        if (upperBounds.isEmpty()) {
            return builtinTypes.any(
                "No upper bounds form perfomrative " + performative
            );
        }

        if (upperBounds.size() == 1) {
            return upperBounds.get(0);
        }

        final TypeHelper typeHelper = module.get(TypeHelper.class);

        return builtinTypes.tuple(upperBounds.stream()
            .map(typeHelper::covariant)
            .collect(Collectors.toList()));
    }


    /**
     * Given an inferred input content type and an input performative,
     * this method might produce a "fixed" content type which includes
     * additional default type arguments.
     */
    public IJadescriptType adaptMessageContentDefaultTypes(
        Maybe<String> performative,
        IJadescriptType inputContentType
    ) {
        if (performative.isNothing() || performative.toNullable().isBlank()) {
            return inputContentType;
        }
        final @Nullable Performative perf = performativeByName.get(
            performative.toNullable()
        );

        if (perf == null) {
            return inputContentType;
        }

        final ParametricTypeSchema<? extends MessageType> schema =
            getMessageTypeSchemaForPerformative(perf);


        final TypeHelper typeHelper = module.get(TypeHelper.class);

        final List<? extends TypeArgument> typeArguments =
            typeHelper.unpackTuple(inputContentType);

        final MessageType messageType;
        try {
            messageType = schema.create(typeArguments);
        } catch (InvalidTypeInstantiatonException e) {
            e.printStackTrace();
            return inputContentType;
        }

        return messageType.getContentType();
    }


    public String adaptMessageContentDefaultCompile(
        Maybe<String> performative,
        IJadescriptType inputContentType,
        String inputExpression
    ) {
        if (performative.isNothing() || performative.toNullable().isBlank()) {
            return inputExpression;
        }
        final @Nullable Performative perf = performativeByName.get(
            performative.toNullable()
        );

        if (perf == null) {
            return inputExpression;
        }

        final ParametricTypeSchema<? extends MessageType> schema =
            getMessageTypeSchemaForPerformative(perf);


        if (!(schema instanceof MessageTypeSchema)) {
            return inputExpression;
        }


        MessageTypeSchema messageSchema = (MessageTypeSchema) schema;
        final TypeHelper typeHelper = module.get(TypeHelper.class);

        final int inputArgsCount =
            typeHelper.unpackTuple(inputContentType).size();


        final int parameterCount = messageSchema.getParameterCount();
        for (int i = inputArgsCount; i < parameterCount; i++) {
            messageSchema.adaptContentExpression(
                i,
                inputContentType,
                inputExpression
            );
        }
        return inputExpression;
    }


    public MessageType instantiateMessageType(
        Maybe<String> performative,
        IJadescriptType computedContentType,
        boolean normalizeToUpperBounds
    ) throws InvalidTypeInstantiatonException {
        Maybe<ParametricTypeSchema<? extends MessageType>> schemaMaybe =
            performative
                .__(performativeByName::get)
                .__(this::getMessageTypeSchemaForPerformative);

        List<TypeArgument> resultTypeArgs;
        if (computedContentType.category().isTuple()) {
            resultTypeArgs =
                new ArrayList<>(computedContentType.typeArguments());
        } else {
            resultTypeArgs = List.of(computedContentType);
        }

        if (normalizeToUpperBounds) {
            resultTypeArgs =
                limitMsgContentTypesToUpperBounds(schemaMaybe, resultTypeArgs);
        }

        if (schemaMaybe.isPresent()) {
            return schemaMaybe.toNullable().create(resultTypeArgs);
        } else {
            return builtinTypes.ptMessage.get().create(resultTypeArgs);
        }
    }


    private List<TypeArgument> limitMsgContentTypesToUpperBounds(
        Maybe<ParametricTypeSchema<? extends MessageType>> schema,
        List<TypeArgument> arguments
    ) {
        if (schema == null || schema.isNothing()
            || schema.wrappedEquals(builtinTypes.ptUnknownMessage)) {
            return arguments;
        }

        return schema.toNullable().limitToUpperBounds(arguments);
    }


    public IJadescriptType fromClass(
        Class<?> class_, List<TypeArgument> arguments
    ) {
        return fromJvmTypeReference(this.jvm.typeRef(
            class_,
            arguments.stream().map(TypeArgument::asJvmTypeReference).collect(
                Collectors.toList())
        )).ignoreBound();
    }


    public IJadescriptType fromClass(
        Class<?> class_, TypeArgument... arguments
    ) {
        return fromClass(class_, Arrays.asList(arguments));
    }


    public IJadescriptType fromFullyQualifiedName(String fullyQualifiedName) {
        return fromJvmTypeReference(jvm.typeRef(fullyQualifiedName))
            .ignoreBound();
    }


}
