package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;

public abstract class BaseNamespace implements Searcheable {

    protected final SemanticsModule module;


    protected BaseNamespace(SemanticsModule module) {
        this.module = module;
    }

}
