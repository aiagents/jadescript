package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public class BuiltinOpsNamespace extends JadescriptTypeNamespace {
    private final Maybe<? extends TypeNamespace> superTypeNamespace;
    private final List<NamedSymbol> properties;
    private final List<? extends CallableSymbol> callables;
    private final SearchLocation location;


    public BuiltinOpsNamespace(
            SemanticsModule module,
            Maybe<? extends TypeNamespace> superTypeNamespace,
            List<NamedSymbol> properties,
            List<? extends CallableSymbol> callables,
            SearchLocation location
    ) {
        super(module);
        this.superTypeNamespace = superTypeNamespace;
        this.properties = properties;
        this.callables = callables;
        this.location = location;
    }

    @Override
    public Stream<? extends CallableSymbol> searchCallable(
            String name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        Stream<? extends CallableSymbol> stream = callables.stream().filter(ce -> ce.name().equals(name));
        stream = safeFilter(stream, CallableSymbol::returnType, returnType);
        stream = safeFilter(
                stream,
                cs -> cs.parameterNames().size(),
                cs -> i -> cs.parameterNames().get(i),
                parameterNames
        );
        stream = safeFilter(
                stream,
                cs -> cs.parameterTypes().size(),
                cs -> i -> cs.parameterTypes().get(i),
                parameterTypes
        );
        return stream;
    }

    @Override
    public Stream<? extends CallableSymbol> searchCallable(
            Predicate<String> name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        Stream<? extends CallableSymbol> stream = callables.stream();
        stream = safeFilter(stream, CallableSymbol::name, name);
        stream = safeFilter(stream, CallableSymbol::returnType, returnType);
        stream = safeFilter(
                stream,
                cs -> cs.parameterNames().size(),
                cs -> i -> cs.parameterNames().get(i),
                parameterNames
        );
        stream = safeFilter(
                stream,
                cs -> cs.parameterTypes().size(),
                cs -> i -> cs.parameterTypes().get(i),
                parameterTypes
        );
        return stream;
    }

    @Override
    public Stream<? extends NamedSymbol> searchName(
            Predicate<String> name,
            Predicate<IJadescriptType> readingType,
            Predicate<Boolean> canWrite
    ) {
        Stream<NamedSymbol> stream = properties.stream();
        stream = safeFilter(stream, NamedSymbol::name, name);
        stream = safeFilter(stream, NamedSymbol::readingType, readingType);
        stream = safeFilter(stream, NamedSymbol::canWrite, canWrite);
        return stream;
    }

    @Override
    public Maybe<? extends TypeNamespace> getSuperTypeNamespace() {
        return superTypeNamespace;
    }

    @Override
    public SearchLocation currentLocation() {
        return location;
    }
}
