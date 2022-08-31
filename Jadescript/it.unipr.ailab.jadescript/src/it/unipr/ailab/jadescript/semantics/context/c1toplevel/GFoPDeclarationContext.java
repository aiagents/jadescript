package it.unipr.ailab.jadescript.semantics.context.c1toplevel;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation;
import it.unipr.ailab.jadescript.semantics.context.c0outer.FileContext;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.List;
import java.util.stream.Stream;

public class GFoPDeclarationContext
        extends UsingOntologyDeclarationContext
implements AgentAssociated {

    private final String GFoPName;

    public GFoPDeclarationContext(
            SemanticsModule module,
            FileContext outer,
            String GFoPName,
            List<IJadescriptType> ontologyTypes
    ) {
        super(module, outer, ontologyTypes);
        this.GFoPName = GFoPName;
    }




    @Override
    public Maybe<Searcheable> superTypeSearcheable() {
        return Maybe.nothing();
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is GFoPDeclarationContext");
        debugDumpAgentAssociations(scb);
        debugDumpOntologyAssociations(scb);
    }

    @Override
    public String getCurrentOperationLogName() {
        return GFoPName;
    }

    @Override
    public Stream<AgentAssociation> computeCurrentAgentAssociations() {
        return Stream.empty();
    }

    @Override
    public Stream<AgentAssociation> computeForClauseAgentAssociations() {
        return Stream.of(new AgentAssociation(module.get(TypeHelper.class).AGENT, AgentAssociation.F_A.INSTANCE));
    }

    @Override
    public Stream<OntologyAssociation> computeCurrentOntologyAssociations() {
        return Stream.empty();
    }

    @Override
    public Stream<OntologyAssociation> computeForClauseOntologyAssociations() {
        return Stream.of(new OntologyAssociation(module.get(TypeHelper.class).ONTOLOGY, OntologyAssociation.F_U_O.INSTANCE));
    }

    @Override
    public boolean canUseAgentReference() {
        return false;
    }
}
