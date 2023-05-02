package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalPattern;
import it.unipr.ailab.maybe.Maybe;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public abstract class NamespaceWithCompilables
    extends BaseNamespace
    implements CompilableName.Namespace, CompilableCallable.Namespace,
    GlobalPattern.Namespace {

    public NamespaceWithCompilables(SemanticsModule module) {
        super(module);
    }

    public static NamespaceWithCompilables empty(
        SemanticsModule module,
        SearchLocation location
    ) {
        return new NamespaceWithCompilables(module) {
            @Override
            public Maybe<? extends Searcheable> superSearcheable() {
                return Maybe.nothing();
            }


            @Override
            public SearchLocation currentLocation() {
                return location;
            }


            @Override
            public Stream<? extends CompilableCallable> compilableCallables(
                @Nullable String name
            ) {
                return Stream.empty();
            }


            @Override
            public Stream<? extends CompilableName> compilableNames(
                @Nullable String name
            ) {
                return Stream.empty();
            }


            @Override
            public Stream<? extends GlobalPattern> globalPatterns(
                @Nullable String name
            ) {
                return Stream.empty();
            }
        };
    }

}
