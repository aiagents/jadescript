package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.MethodInvocationSemantics;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class Operation implements CallableSymbol {
    private final String name;
    private final IJadescriptType returnType;
    private final Map<String, IJadescriptType> parameterNamesToTypes;
    private final List<String> parameterNames;
    private final SearchLocation location;

    protected final BiFunction<String, Map<String, String>, String> invokeByNameCustom;
    protected final BiFunction<String, List<String>, String> invokeByArityCustom;

    private final boolean pure;

    public Operation(
            boolean pure,
            String name,
            IJadescriptType returnType,
            List<Util.Tuple2<String, IJadescriptType>> params,
            SearchLocation location
    ) {
        this.pure = pure;
        this.name = name;
        this.returnType = returnType;
        this.location = location;
        this.parameterNames = new ArrayList<>();
        this.parameterNamesToTypes = new HashMap<>();
        for (Util.Tuple2<String, IJadescriptType> param : params) {
            parameterNames.add(param.get_1());
            parameterNamesToTypes.put(param.get_1(), param.get_2());
        }
        this.invokeByArityCustom = this::defaultInvokeByArity;
        this.invokeByNameCustom = this::defaultInvokeByName;
    }

    public Operation(
            boolean pure,
            String name,
            IJadescriptType returnType,
            List<Util.Tuple2<String, IJadescriptType>> params,
            SearchLocation location,
            BiFunction<String, Map<String, String>, String> invokeByNameCustom,
            BiFunction<String, List<String>, String> invokeByArityCustom

    ) {
        this.pure = pure;
        this.name = name;
        this.returnType = returnType;
        this.location = location;
        this.parameterNames = new ArrayList<>();
        this.parameterNamesToTypes = new HashMap<>();
        for (Util.Tuple2<String, IJadescriptType> param : params) {
            parameterNames.add(param.get_1());
            parameterNamesToTypes.put(param.get_1(), param.get_2());
        }
        this.invokeByArityCustom = invokeByArityCustom;
        this.invokeByNameCustom = invokeByNameCustom;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public IJadescriptType returnType() {
        return this.returnType;
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
        return parameterNames.stream()
                .map(parameterNamesToTypes::get)
                .collect(Collectors.toList());
    }

    public String defaultInvokeByName(
            String dereferencePrefix,
            Map<String, String> compiledRexprs
    ) {
        return dereferencePrefix + name() + "(" + String.join(
                " ,",
                MethodInvocationSemantics.sortToMatchParamNames(
                        compiledRexprs,
                        parameterNames
                )
        ) + ")";
    }

    public String defaultInvokeByArity(
            String dereferencePrefix,
            List<String> compiledRexprs
    ) {
        return dereferencePrefix + name() + "(" + String.join(
                " ,",
                compiledRexprs
        ) + ")";
    }

    @Override
    public String compileInvokeByName(String dereferencePrefix, Map<String, String> compiledRexprs) {
        return this.invokeByNameCustom.apply(dereferencePrefix, compiledRexprs);
    }

    @Override
    public boolean isPure() {
        return pure;
    }

    @Override
    public String compileInvokeByArity(String dereferencePrefix, List<String> compiledRexprs) {
        return this.invokeByArityCustom.apply(dereferencePrefix, compiledRexprs);
    }

    @Override
    public SearchLocation sourceLocation() {
        return location;
    }
}
