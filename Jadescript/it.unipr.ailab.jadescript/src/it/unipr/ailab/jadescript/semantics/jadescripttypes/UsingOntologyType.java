package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmFieldSymbol;
import it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmModelBasedNamespace;

import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts.ONTOLOGY_VAR_NAME;

public interface UsingOntologyType extends IJadescriptType {
    default Stream<OntologyType> getDirectlyUsedOntology() {
        final JvmModelBasedNamespace jvmNamespace = jvmNamespace();
        return (
                jvmNamespace == null
                        ? Stream.<NamedSymbol>empty()
                        : jvmNamespace.searchName((n) -> n.startsWith(ONTOLOGY_VAR_NAME), null, null)
        )
                .filter(i -> i instanceof JvmFieldSymbol)
                .map(NamedSymbol::readingType)
                .filter(i -> i instanceof OntologyType)
                .map(i -> (OntologyType) i);
    }
}
