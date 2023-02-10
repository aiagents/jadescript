package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberNamedCell;
import it.unipr.ailab.maybe.Maybe;

import java.util.List;
import java.util.stream.Stream;


public class BuiltinOpsNamespace extends JadescriptTypeNamespace {
    private final Maybe<? extends TypeNamespace> superTypeNamespace;
    private final List<MemberNamedCell> properties;
    private final List<? extends MemberCallable> callables;
    private final SearchLocation location;


    public BuiltinOpsNamespace(
            SemanticsModule module,
            Maybe<? extends TypeNamespace> superTypeNamespace,
            List<MemberNamedCell> properties,
            List<? extends MemberCallable> callables,
            SearchLocation location
    ) {
        super(module);
        this.superTypeNamespace = superTypeNamespace;
        this.properties = properties;
        this.callables = callables;
        this.location = location;
    }


    @Override
    public Stream<? extends MemberCallable> memberCallables() {
        return callables.stream();
    }


    @Override
    public Stream<? extends MemberNamedCell> memberNamedCells() {
        return properties.stream();
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
