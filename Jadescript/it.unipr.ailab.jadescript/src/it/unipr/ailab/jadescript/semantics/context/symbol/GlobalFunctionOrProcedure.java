package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.CallSemantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalCallable;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agentenv.AgentEnvType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GlobalFunctionOrProcedure implements GlobalCallable {


    protected final Function<List<String>, String>
        invokeByArityCustom;
    protected final Function<Map<String, String>, String>
        invokeByNameCustom;
    private final IJadescriptType returnType;
    private final String name;
    private final Map<String, IJadescriptType> parameterNamesToTypes;
    private final List<String> parameterNames;
    private final SearchLocation location;
    private final boolean withoutSideEffects;


    private GlobalFunctionOrProcedure(
        IJadescriptType returnType,
        String name,
        Map<String, IJadescriptType> parameterNamesToTypes,
        List<String> parameterNames,
        SearchLocation location,
        boolean withoutSideEffects,
        Function<List<String>, String> invokeByArityCustom,
        Function<Map<String, String>, String> invokeByNameCustom
    ) {
        this.returnType = returnType;
        this.name = name;
        this.parameterNamesToTypes = parameterNamesToTypes;
        this.parameterNames = parameterNames;
        this.location = location;
        this.withoutSideEffects = withoutSideEffects;
        this.invokeByArityCustom = invokeByArityCustom;
        this.invokeByNameCustom = invokeByNameCustom;
    }


    public static GlobalFunctionOrProcedure fromJvmStaticOperation(
        SemanticsModule module,
        JvmTypeNamespace namespace,
        JvmOperation operation
    ) {
        return fromJvmStaticOperation(module, namespace, operation, null);
    }


    public static GlobalFunctionOrProcedure fromJvmStaticOperation(
        SemanticsModule module,
        JvmTypeNamespace namespace,
        JvmOperation operation,
        @Nullable SearchLocation location
    ) {
        List<JvmFormalParameter> parameters = operation.getParameters();
        if (parameters == null) {
            parameters = List.of();
        }

        List<String> paramNames = new ArrayList<>();
        Map<String, IJadescriptType> paramNamesToTypes = new HashMap<>();

        final IJadescriptType anyAE =
            module.get(BuiltinTypeProvider.class).anyAgentEnv();

        boolean withoutSideEffects = false;

        for (JvmFormalParameter parameter : parameters) {
            if (parameter == null) {
                continue;
            }

            final String paramName = parameter.getName();
            final JvmTypeReference paramTypeRef =
                parameter.getParameterType();
            if (paramName == null || paramTypeRef == null) {
                continue;
            }

            if (paramName.equals(SemanticsConsts.AGENT_ENV)) {
                final IJadescriptType envType =
                    namespace.resolveType(paramTypeRef)
                        .ignoreBound();

                if (envType instanceof AgentEnvType) {
                    withoutSideEffects =
                        ((AgentEnvType) envType).isWithoutSideEffects();
                }

                continue;
            }

            final IJadescriptType solvedType =
                namespace.resolveType(paramTypeRef)
                    .ignoreBound();

            final TypeComparator comparator = module.get(TypeComparator.class);
            if (TypeRelationshipQuery.superTypeOrEqual().matches(
                comparator.compare(anyAE, solvedType)
            )) {
                continue;
            }

            paramNames.add(paramName);
            paramNamesToTypes.put(paramName, solvedType);
        }


        String fullyQualifiedName = operation.getQualifiedName('.');
        return new GlobalFunctionOrProcedure(
            namespace.resolveType(operation.getReturnType()).ignoreBound(),
            operation.getSimpleName(),
            paramNamesToTypes,
            paramNames,
            location == null
                ? namespace.currentLocation()
                : location,
            withoutSideEffects,
            CompilationHelper.addEnvParameterByArity(
                invokeGlobalByArity(fullyQualifiedName)),
            CompilationHelper.addEnvParameterByName(
                invokeGlobalByName(fullyQualifiedName, paramNames))
        );
    }


    @NotNull
    private static Function<Map<String, String>, String> invokeGlobalByName(
        String fullyQualifiedName,
        List<String> paramNames
    ) {
        return (args1) -> fullyQualifiedName + "(" + String.join(
            " ,",
            CallSemantics.sortToMatchParamNames(
                args1,
                paramNames
            )
        ) + ")";
    }


    @NotNull
    private static Function<List<String>, String> invokeGlobalByArity(
        String fullyQualifiedName
    ) {
        return (args) -> fullyQualifiedName + "(" +
            String.join(" ,", args) +
            ")";
    }


    @Override
    public String name() {
        return name;
    }


    @Override
    public IJadescriptType returnType() {
        return returnType;
    }


    @Override
    public Map<String, IJadescriptType> parameterTypesByName() {
        return parameterNamesToTypes;
    }


    @Override
    public List<String> parameterNames() {
        return parameterNames;
    }


    @Override
    public List<IJadescriptType> parameterTypes() {
        final Map<String, IJadescriptType> map = parameterTypesByName();
        return parameterNames.stream()
            .filter(map::containsKey)
            .map(map::get)
            .collect(Collectors.toList());
    }


    @Override
    public boolean isWithoutSideEffects() {
        return withoutSideEffects;
    }


    @Override
    public String compileInvokeByArity(
        List<String> compiledRexprs,
        BlockElementAcceptor acceptor
    ) {
        return invokeByArityCustom.apply(compiledRexprs);
    }


    @Override
    public String compileInvokeByName(
        Map<String, String> compiledRexprs,
        BlockElementAcceptor acceptor
    ) {
        return invokeByNameCustom.apply(compiledRexprs);
    }


    @Override
    public SearchLocation sourceLocation() {
        return location;
    }

}
