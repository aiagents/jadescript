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
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;


public class TypeHelper implements SemanticsConsts {


    public static final String builtinPrefix = "BUILTIN#";
    public static final String VOID_TYPEID = builtinPrefix + "JAVAVOID";


    private final SemanticsModule module;
    private final LazyInit<JvmTypeHelper> jvm;
    private final LazyInit<BuiltinTypeProvider> builtins;
    private final LazyInit<TypeComparator> comparator;


    @NotNull
    public static String getNarrowedContentErrorMsg(
        IJadescriptType pattNarrowedContentType,
        IJadescriptType wexpNarrowedBehaviourType
    ) {
        return "Could not compute content type: cannot find common " +
            "subtype of type (inferred from pattern) '"
            + pattNarrowedContentType + "' and type (inferred from " +
            "when-expression) '" + wexpNarrowedBehaviourType + "'.";
    }


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


}
