package it.unipr.ailab.jadescript.semantics.context;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.clashing.AutoCallableClashValidator;
import it.unipr.ailab.jadescript.semantics.context.clashing.AutoNameClashValidator;
import it.unipr.ailab.jadescript.semantics.context.search.ContextLocation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public abstract class Context
        implements SemanticsConsts, Searcheable, AutoNameClashValidator, AutoCallableClashValidator {
    protected final SemanticsModule module;

    public Context(SemanticsModule module) {
        this.module = module;
    }

    public SearchLocation currentLocation(){
        return new ContextLocation(this);
    }


    public abstract void debugDump(SourceCodeBuilder scb);

    public abstract String getCurrentOperationLogName();
}
