package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface PatternSymbol extends Symbol {

    String name();

    //For now, all functional-notation-like patterns are NOT typely-holed,
    // meaning that the input type and the term types can be represented by
    // solved types, and not by potentially holed PatternTypes
    IJadescriptType inputType();

    Map<String, IJadescriptType> termTypesByName();

    List<String> termNames();

    List<IJadescriptType> termTypes();

    @SuppressWarnings("SameReturnValue")
    boolean isWithoutSideEffects();

    default int termCount() {
        return Math.min(termNames().size(), termTypesByName().size());
    }

    @Override
    default PatternSymbolSignature getSignature(){
        return new PatternSymbolSignature(
            name(),
            inputType(),
            termTypes()
        );
    }

    default void debugDumpPatternSymbol(SourceCodeBuilder scb){
        final String cn = this.getClass().getName();
        scb.open("PatternSymbol(concrete class=" + cn + ") {");
        {
            scb.line("sourceLocation = " + sourceLocation());
            scb.line("name =" + name());
            scb.line("inputType = " + inputType().getDebugPrint());

            scb.open("terms (arity=" + termCount() + ") = [");
            {
                termNames().stream()
                    .map(name -> name + ": " +
                        termTypesByName().get(name).getDebugPrint())
                    .forEach(scb::line);
            }
            scb.close("]");
        }
        scb.close("}");
    }


    interface Searcher {

        String ANY_PATTERN_NAME = null;
        Predicate<IJadescriptType> ANY_INPUT_TYPE = null;
        BiPredicate<Integer, Function<Integer, String>>
            ANY_TERM_NAMES = null;
        BiPredicate<Integer, Function<Integer, IJadescriptType>>
            ANY_TERM_TYPES = null;

        static <T> BiPredicate<Integer, Function<Integer, T>>
        termCountIs(int count) {
            return (i, __) -> i == count;
        }


        Stream<? extends PatternSymbol> searchPattern(
            String name,
            Predicate<IJadescriptType> inputType,
            BiPredicate<Integer, Function<Integer, String>> termNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>>
                termTypes
        );

        Stream<? extends PatternSymbol> searchPattern(
            Predicate<String> name,
            Predicate<IJadescriptType> inputType,
            BiPredicate<Integer, Function<Integer, String>> termNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>>
                termTypes
        );
    }
}
