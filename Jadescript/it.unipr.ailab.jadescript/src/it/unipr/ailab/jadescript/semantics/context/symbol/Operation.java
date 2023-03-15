package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.CallSemantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.DereferencedCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberCallable;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.AgentEnvType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Operation implements MemberCallable {

    protected final BiFunction<String, Map<String, String>, String>
        invokeByNameCustom;
    protected final BiFunction<String, List<String>, String>
        invokeByArityCustom;
    private final String name;
    private final IJadescriptType returnType;
    private final Map<String, IJadescriptType> parameterNamesToTypes;
    private final List<String> parameterNames;
    private final SearchLocation location;
    private final boolean withoutSideEffects;


    protected Operation(
        IJadescriptType returnType,
        String name,
        Map<String, IJadescriptType> parameterNamesToTypes,
        List<String> parameterNames,
        SearchLocation location,
        boolean withoutSideEffects,
        BiFunction<String, List<String>, String> invokeByArityCustom,
        BiFunction<String, Map<String, String>, String> invokeByNameCustom
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

    public static Operation operation(
        IJadescriptType returnType,
        String name,
        Map<String, IJadescriptType> parameterNamesToTypes,
        List<String> parameterNames,
        SearchLocation location,
        boolean withoutSideEffects,
        BiFunction<String, List<String>, String> invokeByArityCustom,
        BiFunction<String, Map<String, String>, String> invokeByNameCustom
    ){
        return new Operation(
            returnType,
            name,
            parameterNamesToTypes,
            parameterNames,
            location,
            withoutSideEffects,
            invokeByArityCustom,
            invokeByNameCustom
        );
    }

    public static Operation operation(
        IJadescriptType returnType,
        String name,
        Map<String, IJadescriptType> parameterNamesToTypes,
        List<String> parameterNames,
        SearchLocation location,
        boolean withoutSideEffects
    ){
        return new Operation(
            returnType,
            name,
            parameterNamesToTypes,
            parameterNames,
            location,
            withoutSideEffects,
            Operation.defaultInvokeByArity(name),
            Operation.defaultInvokeByName(name, parameterNames)
        );
    }


    public static Operation fromJvmOperation(
        SemanticsModule module,
        JvmTypeNamespace namespace,
        JvmOperation operation
    ) {

        List<JvmFormalParameter> parameters = operation.getParameters();
        if (parameters == null) {
            parameters = List.of();
        }

        List<String> paramNames = new ArrayList<>();
        Map<String, IJadescriptType> paramNamesToTypes = new HashMap<>();

        final IJadescriptType anyAE = module.get(TypeHelper.class).ANYAGENTENV;

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
                    namespace.resolveType(paramTypeRef);

                if(envType instanceof AgentEnvType){
                    withoutSideEffects =
                        ((AgentEnvType) envType).isWithoutSideEffects();
                }

                continue;
            }

            final IJadescriptType solvedType =
                namespace.resolveType(paramTypeRef);
            if (anyAE.isSupEqualTo(solvedType)) {
                continue;
            }

            paramNames.add(paramName);
            paramNamesToTypes.put(paramName, solvedType);
        }


        return new Operation(
            namespace.resolveType(operation.getReturnType()),
            operation.getSimpleName(),
            paramNamesToTypes,
            paramNames,
            namespace.currentLocation(),
            withoutSideEffects,
            CompilationHelper.addEnvParameterByArity(defaultInvokeByArity(
                operation.getSimpleName()
            )),
            CompilationHelper.addEnvParameterByName(defaultInvokeByName(
                operation.getSimpleName(),
                paramNames
            ))
        );
    }


    public static BiFunction<String, Map<String, String>, String>
    defaultInvokeByName(
        String name,
        List<String> parameterNames
    ) {
        return (
            String ownerName,
            Map<String, String> compiledRexprs
        ) -> ownerName + "." + name + "(" + String.join(
            " ,",
            CallSemantics.sortToMatchParamNames(
                compiledRexprs,
                parameterNames
            )
        ) + ")";
    }


    public static BiFunction<String, List<String>, String>
    defaultInvokeByArity(
        String name
    ) {
        return (
            String ownerName,
            List<String> compiledRexprs
        ) -> ownerName + "." + name + "(" + String.join(
            " ,",
            compiledRexprs
        ) + ")";
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
        final Map<String, IJadescriptType> map =
            parameterTypesByName();
        return parameterNames().stream()
            .filter(map::containsKey)
            .map(map::get)
            .collect(Collectors.toList());
    }


    @Override
    public boolean isWithoutSideEffects() {
        return withoutSideEffects;
    }


    @Override
    public SearchLocation sourceLocation() {
        return location;
    }


    @Override
    public DereferencedCallable dereference(
        Function<BlockElementAcceptor, String> ownerCompiler
    ) {
        return new DereferencedOperation(
            ownerCompiler,
            this
        );
    }

}
