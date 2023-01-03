package it.unipr.ailab.jadescript.semantics.context.c1toplevel;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.Context;
import it.unipr.ailab.jadescript.semantics.context.MightUseAgentReference;
import it.unipr.ailab.jadescript.semantics.context.c0outer.FileContext;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public abstract class TopLevelDeclarationContext
        extends Context
        implements MightUseAgentReference {
    private final FileContext outer;

    public TopLevelDeclarationContext(SemanticsModule module, FileContext outer) {
        super(module);
        this.outer = outer;
    }

    public FileContext getOuterContextFile() {
        return outer;
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        scb.line("--> is TopLevelDeclarationContext");
    }

    @Override
    public Maybe<? extends Searcheable> superSearcheable() {
        return Maybe.some(outer);
    }
}
