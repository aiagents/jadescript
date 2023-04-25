package it.unipr.ailab.jadescript.semantics.jadescripttypes.id;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;

public interface TypeCategory {
    boolean isAny();

    boolean isNothing();

    /**
     * Whether this is a Jadescript Basic type (integer, real, aid...)
     */
    boolean isBasicType();

    boolean isCollection();

    boolean isAgent();

    boolean isBehaviour();

    boolean isMessage();

    boolean isOntoContent();

    boolean isAgentEnv();

    /**
     * If true, then this can be safely casted to {@link OntologyType}.
     */
    boolean isOntology();

    boolean isUnknownJVM();

    boolean isJavaVoid();

    boolean isTuple();

    boolean isList();

    boolean isMap();

    boolean isSet();

    boolean isSideEffectFlag();

}
