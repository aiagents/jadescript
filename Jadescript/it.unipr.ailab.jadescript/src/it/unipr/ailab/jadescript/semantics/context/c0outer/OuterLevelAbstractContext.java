package it.unipr.ailab.jadescript.semantics.context.c0outer;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.Context;
import it.unipr.ailab.jadescript.semantics.context.search.ModuleGlobalLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedOperation;
import it.unipr.ailab.jadescript.semantics.context.symbol.SymbolUtils;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import jadescript.lang.JadescriptGlobalFunction;
import jadescript.lang.JadescriptGlobalProcedure;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.namespace.JvmModelBasedNamespace.CTOR_INTERNAL_NAME;
import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public abstract class OuterLevelAbstractContext extends Context implements RawTypeReferenceSolverContext {

    public OuterLevelAbstractContext(SemanticsModule module) {
        super(module);
    }

    protected Stream<? extends CallableSymbol> getCallableStreamFromDeclaredType(
            JvmTypeReference methodJVMClassRef,
            JvmDeclaredType type,
            Predicate<String> name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        IJadescriptType jadescriptType = module.get(TypeHelper.class).jtFromJvmTypeRef(methodJVMClassRef);
        Stream<? extends CallableSymbol> result;
        if (module.get(TypeHelper.class).isAssignable(jade.core.behaviours.Behaviour.class, methodJVMClassRef)) {
            JvmTypeNamespace namespace = new JvmTypeNamespace(module, type);
            result = searchBehaviourCtorFunction(namespace, name, returnType, parameterNames, parameterTypes);
        } else if (module.get(TypeHelper.class).isAssignable(jade.content.onto.Ontology.class, methodJVMClassRef)) {
            Stream<Integer> ontoStream = Stream.of(0);
            ontoStream = safeFilter(ontoStream, (__) -> methodJVMClassRef.getSimpleName(), name);
            ontoStream = safeFilter(ontoStream, (__) -> jadescriptType, returnType);
            ontoStream = safeFilter(ontoStream, (__) -> 0, (__) -> i -> "", parameterNames);
            ontoStream = safeFilter(ontoStream, (__) -> 0,
                    (__) -> i -> module.get(TypeHelper.class).NOTHING, parameterTypes
            );
            result = ontoStream.map((__) -> ontoInstanceAsCallable(
                    methodJVMClassRef.getSimpleName(),
                    jadescriptType
            ));
        } else if (module.get(TypeHelper.class).isAssignable(JadescriptGlobalFunction.class, methodJVMClassRef)
                || module.get(TypeHelper.class).isAssignable(JadescriptGlobalProcedure.class, methodJVMClassRef)
        ) {
            JvmTypeNamespace namespace = new JvmTypeNamespace(module, type);
            result = namespace.searchCallable(name, returnType, parameterNames, parameterTypes)
                    .map(m -> SymbolUtils.setDereferenceByCtor(m, jadescriptType));
        } else {
            result = Stream.empty();
        }
        return result.map(cs -> SymbolUtils.changeLocation(cs, new ModuleGlobalLocation(
                type.getPackageName() != null ? type.getPackageName() : ""
        )));
    }

    protected Stream<? extends CallableSymbol> getCallableStreamFromDeclaredType(
            JvmTypeReference methodJVMClassRef,
            JvmDeclaredType type,
            String name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        IJadescriptType jadescriptType = module.get(TypeHelper.class).jtFromJvmTypeRef(methodJVMClassRef);
        Stream<? extends CallableSymbol> result;
        if (module.get(TypeHelper.class).isAssignable(jade.core.behaviours.Behaviour.class, methodJVMClassRef)) {
            JvmTypeNamespace namespace = new JvmTypeNamespace(module, type);
            result = searchBehaviourCtorFunction(namespace, name, returnType, parameterNames, parameterTypes);
        } else if (module.get(TypeHelper.class).isAssignable(jade.content.onto.Ontology.class, methodJVMClassRef)) {
            Stream<Integer> ontoStream = Stream.of(0);
            ontoStream = safeFilter(ontoStream, (__) -> methodJVMClassRef.getSimpleName(), name::equals);
            ontoStream = safeFilter(ontoStream, (__) -> jadescriptType, returnType);
            ontoStream = safeFilter(ontoStream, (__) -> 0, (__) -> i -> "", parameterNames);
            ontoStream = safeFilter(ontoStream, (__) -> 0,
                    (__) -> i -> module.get(TypeHelper.class).NOTHING, parameterTypes
            );
            result = ontoStream.map((__) -> ontoInstanceAsCallable(
                    methodJVMClassRef.getSimpleName(),
                    jadescriptType
            ));
        } else if (module.get(TypeHelper.class).isAssignable(JadescriptGlobalFunction.class, methodJVMClassRef)
                || module.get(TypeHelper.class).isAssignable(JadescriptGlobalProcedure.class, methodJVMClassRef)
        ) {
            JvmTypeNamespace namespace = new JvmTypeNamespace(module, type);
            result = namespace.searchCallable(name, returnType, parameterNames, parameterTypes)
                    .map(m -> SymbolUtils.setDereferenceByCtor(m, jadescriptType));
        } else {
            result = Stream.empty();
        }
        return result.map(cs -> SymbolUtils.changeLocation(cs, new ModuleGlobalLocation(
                type.getPackageName() != null ? type.getPackageName() : ""
        )));
    }

    protected Stream<? extends CallableSymbol> getCallableStreamFromFQName(
            String fqName,
            String name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        JvmTypeReference methodJVMClassRef = module.get(TypeHelper.class).typeRef(fqName);
        if (methodJVMClassRef.getType() instanceof JvmDeclaredType) {
            final JvmDeclaredType type = (JvmDeclaredType) methodJVMClassRef.getType();
            return getCallableStreamFromDeclaredType(
                    methodJVMClassRef,
                    type,
                    name,
                    returnType,
                    parameterNames,
                    parameterTypes
            );
        }
        return Stream.empty();
    }

    private CallableSymbol ontoInstanceAsCallable(String name, IJadescriptType ontoType) {
        final String ontoTypeCompiled = ontoType.compileToJavaTypeReference();
        return new ContextGeneratedOperation(
                name,
                ontoType,
                List.of(),
                (__1, __2) -> "((" + ontoTypeCompiled + ") " + ontoTypeCompiled + ".getInstance())",
                (__1, __2) -> "((" + ontoTypeCompiled + ") " + ontoTypeCompiled + ".getInstance())"
        );
    }

    private Stream<? extends CallableSymbol> searchBehaviourCtorFunction(
            CallableSymbol.Searcher namespace,
            String name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        return searchBehaviourCtorFunction(
                namespace,
                name::equals,
                returnType,
                parameterNames,
                parameterTypes
        );
    }

    private Stream<? extends CallableSymbol> searchBehaviourCtorFunction(
            CallableSymbol.Searcher namespace,
            Predicate<String> name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        return namespace.searchCallable(//Searching locally (not in supertypes)
                CTOR_INTERNAL_NAME,
                returnType,
                parameterNames,
                parameterTypes
        ).filter(callable ->
                name.test(callable.returnType().asJvmTypeReference().getSimpleName())
        );
    }
}
