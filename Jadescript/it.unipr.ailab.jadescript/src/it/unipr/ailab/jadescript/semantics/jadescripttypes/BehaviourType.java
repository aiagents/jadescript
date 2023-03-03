package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.namespace.BehaviourTypeNamespace;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public interface BehaviourType
    extends IJadescriptType, UsingOntologyType, ForAgentClausedType {

    SearchLocation getLocation();

    @Override
    BehaviourTypeNamespace namespace();
}
