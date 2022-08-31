package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.search.UnknownLocation;
import it.unipr.ailab.maybe.Maybe;

public class EmptyNamespace extends Namespace{
    public EmptyNamespace(SemanticsModule module) {
        super(module);
    }

    @Override
    public Maybe<? extends Searcheable> superSearcheable() {
        return Maybe.nothing();
    }

    @Override
    public SearchLocation currentLocation() {
        return UnknownLocation.getInstance();
    }
}
