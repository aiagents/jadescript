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
