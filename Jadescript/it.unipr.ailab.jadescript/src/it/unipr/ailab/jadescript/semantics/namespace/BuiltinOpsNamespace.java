package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberName;
import it.unipr.ailab.maybe.Maybe;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;


public class BuiltinOpsNamespace extends JadescriptTypeNamespace {

    private final Maybe<? extends TypeNamespace> superTypeNamespace;
    private final List<MemberName> properties;
    private final List<? extends MemberCallable> callables;
    private final SearchLocation location;


    public BuiltinOpsNamespace(
        SemanticsModule module,
        Maybe<? extends TypeNamespace> superTypeNamespace,
        List<MemberName> properties,
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
    public Maybe<? extends TypeNamespace> getSuperTypeNamespace() {
        return superTypeNamespace;
    }


    @Override
    public SearchLocation currentLocation() {
        return location;
    }


    @Override
    public Stream<? extends MemberCallable> memberCallables(
        @Nullable String name
    ) {
        return callables.stream()
            .filter(c -> name==null || name.equals(c.name()));

    }


    @Override
    public Stream<? extends MemberName> memberNames(@Nullable String name) {
        return properties.stream()
            .filter(p -> name==null || name.equals(p.name()));
    }

}
