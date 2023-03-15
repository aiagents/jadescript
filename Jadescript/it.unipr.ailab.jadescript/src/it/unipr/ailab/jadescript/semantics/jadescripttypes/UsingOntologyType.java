package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberName;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import org.eclipse.xtext.common.types.JvmField;

import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts.ONTOLOGY_VAR_NAME;

public interface UsingOntologyType extends IJadescriptType {

    default Stream<OntologyType> getDirectlyUsedOntology() {
        final JvmTypeNamespace jvmNamespace = jvmNamespace();
        return (
            jvmNamespace == null
                ? Stream.<MemberName>empty()
                : jvmNamespace.searchJvmField()
                .filter(f -> f.getSimpleName() != null
                    && f.getSimpleName().startsWith(ONTOLOGY_VAR_NAME))
                .map(JvmField::getType)
                .map(jvmNamespace::resolveType)
        ).filter(i -> i instanceof OntologyType)
            .map(i -> (OntologyType) i);
    }

}
