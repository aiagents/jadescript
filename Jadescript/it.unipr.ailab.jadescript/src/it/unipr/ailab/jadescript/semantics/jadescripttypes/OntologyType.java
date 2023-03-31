package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.namespace.OntologyTypeNamespace;

public interface OntologyType
    extends IJadescriptType, EmptyCreatable {

    SearchLocation getLocation();

    boolean isSuperOrEqualOntology(OntologyType other);

    @Override
    OntologyTypeNamespace namespace();

}
