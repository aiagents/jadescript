package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberName;
import it.unipr.ailab.maybe.Maybe;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;
import java.util.stream.Stream;

public class PermissiveJvmBasedNamespace extends JadescriptTypeNamespace{


    private final JvmTypeNamespace jvmNamespace;
    private final Supplier<Maybe<? extends TypeNamespace>>
        superNamespaceSupplier;
    private final SearchLocation location;


    public PermissiveJvmBasedNamespace(
        SemanticsModule module,
        JvmTypeNamespace jvmNamespace,
        Supplier<Maybe<? extends TypeNamespace>> superNamespaceSupplier,
        SearchLocation location
    ) {
        super(module);
        this.jvmNamespace = jvmNamespace;
        this.superNamespaceSupplier = superNamespaceSupplier;
        this.location = location;
    }


    @Override
    public SearchLocation currentLocation() {
        return location;
    }


    @Override
    public Stream<? extends MemberCallable> memberCallables(
        @Nullable String name
    ) {
        return callablesFromJvm(jvmNamespace).memberCallables(name);
    }


    @Override
    public Stream<? extends MemberName> memberNames(
        @Nullable String name
    ) {
        return namesFromJvm(jvmNamespace).memberNames(name);
    }


    @Override
    public Maybe<? extends TypeNamespace> getSuperTypeNamespace() {
        return superNamespaceSupplier.get();
    }

}
