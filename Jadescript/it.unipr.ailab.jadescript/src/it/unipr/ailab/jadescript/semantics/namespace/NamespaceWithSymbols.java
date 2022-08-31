package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class NamespaceWithSymbols extends Namespace
        implements CallableSymbol.Searcher, NamedSymbol.Searcher {
    public NamespaceWithSymbols(SemanticsModule module) {
        super(module);
    }

    @Override
    public abstract Stream<? extends CallableSymbol> searchCallable(
            String name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    );

    @Override
    public abstract Stream<? extends CallableSymbol> searchCallable(
            Predicate<String> name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    );

    @Override
    public abstract Stream<? extends NamedSymbol> searchName(
            Predicate<String> name,
            Predicate<IJadescriptType> readingType,
            Predicate<Boolean> canWrite
    );

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        scb.open("--> is NamespaceWithSymbols {");
        scb.open("names     = [");
        searchName((Predicate<String>) null, null, null)
                .forEach(ns->ns.debugDumpNamedSymbol(scb));
        scb.close("]");
        scb.open("callables = [");
        searchCallable((Predicate<String>) null, null, null, null)
                .forEach(ns->ns.debugDumpCallableSymbol(scb));
        scb.close("]");
        scb.close("}");

    }
}
