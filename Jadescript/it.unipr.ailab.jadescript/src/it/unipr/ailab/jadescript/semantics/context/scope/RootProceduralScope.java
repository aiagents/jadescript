package it.unipr.ailab.jadescript.semantics.context.scope;

import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.maybe.Maybe;

public class RootProceduralScope extends ProceduralScope {


    @Override
    public Maybe<? extends Searcheable> superSearcheable() {
        return Maybe.nothing();
    }
}
