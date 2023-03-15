package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.namespace.AgentTypeNamespace;

public interface AgentType extends IJadescriptType, UsingOntologyType {

    SearchLocation getLocation();

    @Override
    AgentTypeNamespace namespace();

}
