package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.UnknownLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberName;
import it.unipr.ailab.maybe.Maybe;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class EmptyTypeNamespace extends TypeNamespace {

    public EmptyTypeNamespace(SemanticsModule module) {
        super(module);
    }


    @Override
    public SearchLocation currentLocation() {
        return UnknownLocation.getInstance();
    }


    @Override
    public Stream<? extends MemberCallable> memberCallables(
        @Nullable String name
    ) {
        return Stream.empty();
    }


    @Override
    public Stream<? extends MemberName> memberNames(@Nullable String name) {
        return Stream.empty();
    }


    @Override
    public Maybe<? extends TypeNamespace> getSuperTypeNamespace() {
        return Maybe.nothing();
    }

}
