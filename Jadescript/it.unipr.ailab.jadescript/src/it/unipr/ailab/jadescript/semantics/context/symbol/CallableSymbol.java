package it.unipr.ailab.jadescript.semantics.context.symbol;


import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface CallableSymbol extends Symbol {


    String name();

    IJadescriptType returnType();

    Map<String, IJadescriptType> parameterTypesByName();

    List<String> parameterNames();

    List<IJadescriptType> parameterTypes();

    String compileInvokeByArity(String dereferencePrefix, List<String> compiledRexprs);

    String compileInvokeByName(String dereferencePrefix, Map<String, String> compiledRexprs);

    boolean isPure();

    default int arity() {
        return Math.min(parameterNames().size(), parameterTypesByName().size());
    }

    @Override
    default CallableSymbolSignature getSignature() {
        return new CallableSymbolSignature(name(), returnType(), parameterTypes());
    }

    default void debugDumpCallableSymbol(SourceCodeBuilder scb) {
        scb.open("CallableSymbol(concrete class=" + this.getClass().getName() + ") {");
        scb.line("sourceLocation = " + sourceLocation());
        scb.line("name =" + name());
        scb.line("returnType = " + returnType().getDebugPrint());

        scb.open("parameters (arity="+arity()+") = [");
        parameterNames().stream()
                .map(name -> name + ": " + parameterTypesByName().get(name).getDebugPrint())
                .forEach(scb::line);
        scb.close("]");


        scb.line("compileInvokeByArity  -> " + compileInvokeByArity(
                "<dereferencePrefix>",
                IntStream.range(0, arity())
                        .mapToObj(i -> "<arg" + i + ">")
                        .collect(Collectors.toList())
        ));
        scb.line("compileInvokeByName -> " + compileInvokeByName(
                "<dereferencePrefix>",
                parameterNames().stream().collect(Collectors.toMap(
                        n -> n,
                        n -> "<arg-" + n + ">"
                ))
        ));
        scb.close("}");
    }

    interface Searcher {
        String ANY_NAME = null;
        Predicate<IJadescriptType> ANY_RETURN_TYPE = null;
        BiPredicate<Integer, Function<Integer, String>> ANY_PARAMETER_NAMES = null;
        BiPredicate<Integer, Function<Integer, IJadescriptType>> ANY_PARAMETER_TYPES = null;

        Stream<? extends CallableSymbol> searchCallable(
                String name,
                Predicate<IJadescriptType> returnType,
                BiPredicate<Integer, Function<Integer, String>> parameterNames,
                BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
        );

        Stream<? extends CallableSymbol> searchCallable(
                Predicate<String> name,
                Predicate<IJadescriptType> returnType,
                BiPredicate<Integer, Function<Integer, String>> parameterNames,
                BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
        );
    }

}
