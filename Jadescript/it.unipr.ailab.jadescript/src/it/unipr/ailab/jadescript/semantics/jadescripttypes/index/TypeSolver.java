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
import it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour.UserDefinedBehaviourType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.TupleType;
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
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;
import static jadescript.lang.Performative.performativeByName;

public class TypeSolver {

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


    private IJadescriptType solveJvmTypeReferenceWithoutReattempts(
        JvmTypeReference reference,
        boolean permissive
    ) {
        final Maybe<IJadescriptType> fromJVMTypeReference =
            this.solveJVMTypeReference(
                reference);

        IJadescriptType result;
        if (fromJVMTypeReference.isPresent()) {
            result = fromJVMTypeReference.toNullable();
        } else if (jvm.isAssignable(
            Tuple.class,
            reference
        ) && reference instanceof JvmParameterizedTypeReference) {
            result =
                solveTupleType(((JvmParameterizedTypeReference) reference));
        } else {
            result = resolveNonBuiltinType(reference, permissive);
            if (!result.category().isUnknownJVM()) {
                index.store(reference, result);
            }
        }
        return result;
    }


    private Maybe<IJadescriptType> solveJVMTypeReference(
        @NotNull JvmTypeReference typeReference
    ) {

        final String qualifiedName = typeReference.getQualifiedName('.');

        if (index.getTypeTable().containsKey(qualifiedName)) {
            return some(index.getTypeTable().get(qualifiedName).get());
        }

        final String noGenericsTypeName = JvmTypeHelper.noGenericsTypeName(
            qualifiedName);

        if (!index.getParametricTypeTable().containsKey(noGenericsTypeName) || !(typeReference instanceof JvmParameterizedTypeReference)) {
            return nothing();
        }

        List<TypeArgument> args = new ArrayList<>();

        final EList<JvmTypeReference> jvmPTRArgs =
            ((JvmParameterizedTypeReference) typeReference).getArguments();

        for (JvmTypeReference arg : jvmPTRArgs) {
            IJadescriptType argType = fromJvmTypeReference(arg);
            args.add(argType);
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


    public IJadescriptType fromJvmTypeReference(
        JvmTypeReference reference
    ) {
        return fromJvmTypeReference(reference, false);
    }


    public IJadescriptType fromJvmTypeReference(
        JvmTypeReference reference, boolean permissive
    ) {
        IJadescriptType result =
            solveJvmTypeReferenceWithoutReattempts(
                reference,
                permissive
            );

        if (result.category().isJavaVoid()) {
            ICompositeNode node = NodeModelUtils.getNode(reference);
            if (node == null) {
                node = NodeModelUtils.findActualNodeFor(reference);
            }

            if (node != null) {
                final INode finalNode = node;
                final JvmTypeReference reattempt =
                    module.get(ContextManager.class).currentContext().searchAs(
                        RawTypeReferenceSolverContext.class,
                        solver -> solver.rawResolveTypeReference(finalNode.getText().trim())
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
            IJadescriptType typeDescriptor = fromJvmTypeReference(arg);
            args.add(typeDescriptor);
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
            final List<JvmTypeReference> args =
                jvm.getTypeArgumentsOfParent(
                    reference,
                    jvm.typeRef(CyclicBehaviour.class)
                );
            BaseBehaviourType rootCategoryType;
            if (args.size() == 1) {
                rootCategoryType = builtinTypes.cyclicBehaviour(
                    fromJvmTypeReference(args.get(0)));
            } else {
                rootCategoryType = builtinTypes.cyclicBehaviour();
            }
            return new UserDefinedBehaviourType(
                module,
                reference,
                rootCategoryType
            );
        } else if (jvm.isAssignable(OneShot.class, reference)) {
            final List<JvmTypeReference> args =
                jvm.getTypeArgumentsOfParent(
                    reference,
                    jvm.typeRef(OneShotBehaviour.class)
                );
            BaseBehaviourType rootCategoryType;
            if (args.size() == 1) {
                rootCategoryType = builtinTypes.oneshotBehaviour(
                    fromJvmTypeReference(args.get(0)));
            } else {
                rootCategoryType = builtinTypes.oneshotBehaviour();
            }
            return new UserDefinedBehaviourType(
                module,
                reference,
                rootCategoryType
            );

        } else if (jvm.isAssignable(Base.class, reference)) {
            final List<JvmTypeReference> args =
                jvm.getTypeArgumentsOfParent(
                    reference,
                    jvm.typeRef(Behaviour.class)
                );
            BaseBehaviourType rootCategoryType;
            if (args.size() == 1) {
                rootCategoryType = builtinTypes.behaviour(fromJvmTypeReference(
                    args.get(0)));
            } else {
                rootCategoryType = builtinTypes.behaviour();
            }
            return new UserDefinedBehaviourType(
                module,
                reference,
                rootCategoryType
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


    public IJadescriptType fromJvmType(
        JvmDeclaredType itClass, JvmTypeReference... typeArguments
    ) {
        return fromJvmTypeReference(this.jvm.typeRef(itClass, typeArguments));
    }


    public IJadescriptType fromJvmTypePermissive(
        JvmDeclaredType itClass, JvmTypeReference... typeArguments
    ) {
        return fromJvmTypeReference(
            this.jvm.typeRef(itClass, typeArguments),
            /*permissive = */true
        );
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
    getMessageTypeSchemaForPerformative(
        String performative
    ) {
        return getMessageTypeSchemaForPerformative(
            index.getMessageClassToPerformativeMap().get(performative)
        );
    }


    @SuppressWarnings("unchecked")
    public List<TypeArgument> getDefaultTypeArguments(
        String name
    ) {
        if (name.equals("Message")) {
            return List.of(builtinTypes.any(
                "There are no type upper bounds for the content " +
                    "of a message of root type Message."
            ));

        }
        final Performative performative =
            index.getMessageClassToPerformativeMap().get(name);

        if (performative == null) {
            return List.of();
        }

        final IJadescriptType contentBound =
            getContentBoundForPerformative(performative);

        if (contentBound.isErroneous()) {
            return List.of();
        }

        return (List<TypeArgument>) module.get(TypeHelper.class)
            .unpackTuple(contentBound);
    }


    public IJadescriptType getContentBoundForPerformative(
        Performative performative
    ) {
        final List<IJadescriptType> upperBounds =
            index.getPerformativeToMessageSubtypeMap().get(performative)
                .get().getUpperBounds();

        if (upperBounds.isEmpty()) {
            return builtinTypes.any(""); //TODO error message
        }

        if (upperBounds.size() == 1) {
            return upperBounds.get(0);
        }

        return builtinTypes.tuple(new ArrayList<>(upperBounds));
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


    //TODO reimplement in TypeSolver
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


        String result = inputExpression;

        final int parameterCount = messageSchema.getParameterCount();
        for (int i = inputArgsCount; i < parameterCount; i++) {
            messageSchema.adaptContentExpression(
                i,
                inputContentType,
                result
            );
        }
        return result;
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
        Class<?> class_, List<IJadescriptType> arguments
    ) {
        return fromJvmTypeReference(this.jvm.typeRef(
            class_,
            arguments.stream().map(IJadescriptType::asJvmTypeReference).collect(
                Collectors.toList())
        ));
    }


    public IJadescriptType fromClass(
        Class<?> class_, IJadescriptType... arguments
    ) {
        return fromClass(class_, Arrays.asList(arguments));
    }


    public IJadescriptType fromFullyQualifiedName(String fullyQualifiedName) {
        return fromJvmTypeReference(jvm.typeRef(fullyQualifiedName));
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
