package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.Context;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public class OntologyElementDeclarationContext extends Context {
    private final OntologyDeclarationSupportContext outer;


    public OntologyElementDeclarationContext(
            SemanticsModule module,
            OntologyDeclarationSupportContext outer
    ) {
        super(module);
        this.outer = outer;
    }

    @Override
    public Maybe<? extends Searcheable> superSearcheable() {
        return Maybe.some(outer);
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        scb.line("--> is OntologyElementDeclarationContext");

    }

    @Override
    public String getCurrentOperationLogName() {
        return "<init ontology element for " + outer.getOntologyName() + ">";
    }
}
