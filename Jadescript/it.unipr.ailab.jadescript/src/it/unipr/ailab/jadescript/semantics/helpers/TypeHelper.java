package it.unipr.ailab.jadescript.semantics.helpers;


import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agent.UserDefinedAgentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.TupleType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.BoundedTypeArgument;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.utils.LazyInit;
import jadescript.lang.Performative;
import org.eclipse.xtext.common.types.JvmDeclaredType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static jadescript.lang.Performative.UNKNOWN;
import static jadescript.lang.Performative.performativeByName;


public class TypeHelper implements SemanticsConsts {


    public static final String builtinPrefix = "BUILTIN#";
    public static final String VOID_TYPEID = builtinPrefix + "JAVAVOID";


    private final Map<Performative, MessageContentTupleDefaultElements>
        defaultContentElementsMap = new HashMap<>();

    private final SemanticsModule module;
    private final LazyInit<JvmTypeHelper> jvm;
    private final LazyInit<BuiltinTypeProvider> builtins;
    private final LazyInit<TypeComparator> comparator;


    public TypeHelper(SemanticsModule module) {
        this.module = module;
        this.jvm = LazyInit.lazyInit(() -> module.get(JvmTypeHelper.class));
        this.builtins = LazyInit.lazyInit(
            () -> module.get(BuiltinTypeProvider.class)
        );
        this.comparator = LazyInit.lazyInit(
            () -> module.get(TypeComparator.class)
        );
    }


    public BoundedTypeArgument covariant(IJadescriptType jadescriptType) {
        return new BoundedTypeArgument(
            module,
            jadescriptType,
            BoundedTypeArgument.Variance.EXTENDS
        );
    }


    public BoundedTypeArgument contravariant(IJadescriptType jadescriptType) {
        return new BoundedTypeArgument(
            module,
            jadescriptType,
            BoundedTypeArgument.Variance.SUPER
        );
    }


    public IJadescriptType beingDeclaredAgentType(
        JvmDeclaredType itClass,
        Maybe<IJadescriptType> superType
    ) {
        return new UserDefinedAgentType(
            module,
            jvm.get().typeRef(itClass),
            superType,
            builtins.get().agent()
        );
    }


    //TODO reimplement in TypeSolver
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
                            mctde.getDefaultType(i)
                                .orElse(builtins.get().any(""))
                        ));
                    }
                }
                return builtins.get().tuple(elements);
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
                            .orElse(builtins.get().any(""))));
                    }
                }
                return builtins.get().tuple(elements);
            } else {
                return inputContentType;
            }
        }
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
    public List<? extends TypeArgument> unpackTuple(TypeArgument t) {
        if (t.ignoreBound().category().isTuple() && t instanceof TupleType) {
            return ((TupleType) t).typeArguments();
        }
        return List.of(t);
    }


    public boolean isTypeWithPrimitiveOntologySchema(IJadescriptType type) {
        final BuiltinTypeProvider builtins = this.builtins.get();
        return Stream.of(
            builtins.integer(),
            builtins.boolean_(),
            builtins.text(),
            builtins.real()
        ).anyMatch(t1 ->
            TypeRelationshipQuery.superTypeOrEqual().matches(
                comparator.get().compare(t1, type)
            )
        );
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
