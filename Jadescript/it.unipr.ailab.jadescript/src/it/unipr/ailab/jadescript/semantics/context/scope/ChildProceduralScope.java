package it.unipr.ailab.jadescript.semantics.context.scope;

import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.maybe.Maybe;

public class ChildProceduralScope extends ProceduralScope {
    private final ProceduralScope outerScope;

    public ChildProceduralScope(ProceduralScope outerScope) {
        this.outerScope = outerScope;
    }

    public ProceduralScope getOuterScope() {
        return outerScope;
    }


    @Override
    public Maybe<? extends Searcheable> superSearcheable() {
        return Maybe.some(getOuterScope());
    }
}
