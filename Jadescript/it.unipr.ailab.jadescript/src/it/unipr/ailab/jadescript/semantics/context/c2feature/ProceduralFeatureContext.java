package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.Context;
import it.unipr.ailab.jadescript.semantics.context.scope.ProceduralScope;
import it.unipr.ailab.jadescript.semantics.context.scope.ScopeManager;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public abstract class ProceduralFeatureContext extends Context implements ScopedContext {
    protected final ProceduralFeatureContainerContext outer;
    private final LazyValue<ScopeManager> scopeManager;

    public ProceduralFeatureContext(SemanticsModule module, ProceduralFeatureContainerContext outer) {
        super(module);
        this.outer = outer;
        this.scopeManager = new LazyValue<>(ScopeManager::new);
    }

    @Override
    public ScopeManager getScopeManager() {
        return scopeManager.get();
    }

    @Override
    public ProceduralScope getCurrentScope() {
        return getScopeManager().getCurrentScope();
    }

    @Override
    public Maybe<? extends Searcheable> superSearcheable() {
        return Maybe.of(outer);
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        scb.line("--> is ProceduralFeatureContext");
        debugDumpScopedContext(scb);
    }


}
