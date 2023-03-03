package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.*;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class ImportedGlobalsNamespace
extends NamespaceWithCompilables
implements GlobalCallable.Namespace, GlobalName.Namespace {


    private final NamespaceWithGlobals namespace;


    public ImportedGlobalsNamespace(
        SemanticsModule module,
        NamespaceWithGlobals namespace
    ) {
        super(module);
        this.namespace = namespace;
    }


    public static ImportedGlobalsNamespace importedGlobalsNamespace(
        SemanticsModule module,
        NamespaceWithGlobals namespace
    ) {
        return new ImportedGlobalsNamespace(module, namespace);
    }


    @Override
    public Stream<? extends GlobalPattern> globalPatterns(
        @Nullable String name
    ) {
        return namespace.globalPatterns(name);
    }


    @Override
    public Stream<? extends GlobalCallable> globalCallables(@Nullable String name) {
        return namespace.globalCallables(name);
    }


    @Override
    public Stream<? extends GlobalName> globalNames(@Nullable String name) {
        return namespace.globalNames(name);
    }


    @Override
    public Maybe<? extends Searcheable> superSearcheable() {
        return namespace.superSearcheable().__(superNamespace ->
            importedGlobalsNamespace(
                module,
                superNamespace
            )
        );
    }


    @Override
    public SearchLocation currentLocation() {
        return namespace.currentLocation();
    }



}
