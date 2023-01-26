package it.unipr.ailab.jadescript.semantics.context.associations;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public interface OntologyAssociated extends OntologyAssociationComputer, Associated {



    default void debugDumpOntologyAssociations(SourceCodeBuilder scb) {
        scb.open("--> is OntologyAssociated {");
        scb.line("*** Ontology associations: ***");
        computeAllOntologyAssociations().forEach((OntologyAssociation o) -> o.debugDump(scb));
        scb.close("}");
    }
}
