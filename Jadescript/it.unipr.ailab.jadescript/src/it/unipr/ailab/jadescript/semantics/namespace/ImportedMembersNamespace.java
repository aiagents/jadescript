package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.symbol.ImportedCoreferentMemberName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalPattern;
import it.unipr.ailab.maybe.Maybe;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.stream.Stream;

public class ImportedMembersNamespace
    extends NamespaceWithCompilables {

    private final Function<BlockElementAcceptor, String> ownerCompiler;
    private final ExpressionDescriptor ownerDescriptor;
    private final NamespaceWithMembers namespace;


    private ImportedMembersNamespace(
        SemanticsModule module,
        Function<BlockElementAcceptor, String> ownerCompiler,
        ExpressionDescriptor ownerDescriptor,
        NamespaceWithMembers namespace
    ) {
        super(module);
        this.ownerCompiler = ownerCompiler;
        this.ownerDescriptor = ownerDescriptor;
        this.namespace = namespace;
    }


    public static ImportedMembersNamespace importMembersNamespace(
        SemanticsModule module,
        Function<BlockElementAcceptor, String> ownerCompiler,
        ExpressionDescriptor ownerDescriptor,
        NamespaceWithMembers namespace
    ) {
        return new ImportedMembersNamespace(
            module,
            ownerCompiler,
            ownerDescriptor,
            namespace
        );
    }


    @Override
    public Stream<? extends CompilableName> compilableNames(
        @Nullable String name
    ) {
        return namespace.memberNames(name).map(n -> {
            return new ImportedCoreferentMemberName(
                ownerDescriptor,
                ownerCompiler,
                n
            );
        });
    }


    @Override
    public Stream<? extends CompilableCallable> compilableCallables(
        @Nullable String name
    ) {
        return namespace.memberCallables(name).map(c -> {
            return c.dereference(ownerCompiler);
        });
    }


    @Override
    public Stream<? extends GlobalPattern> globalPatterns(
        @Nullable String name
    ) {
        return Stream.empty();
    }


    @Override
    public Maybe<? extends Searcheable> superSearcheable() {
        return namespace.superSearcheable()
            .__(superNamespace -> importMembersNamespace(
                module,
                ownerCompiler,
                ownerDescriptor,
                superNamespace
            ));
    }


    @Override
    public SearchLocation currentLocation() {
        return namespace.currentLocation();
    }

}
