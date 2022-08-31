package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;

public abstract class Namespace implements Searcheable {
    protected final SemanticsModule module;
    protected Namespace(SemanticsModule module) {
        this.module = module;
    }
}
