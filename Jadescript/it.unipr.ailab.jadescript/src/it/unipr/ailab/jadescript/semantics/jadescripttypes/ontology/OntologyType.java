package it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.EmptyCreatable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategory;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategoryAdapter;
import it.unipr.ailab.jadescript.semantics.namespace.OntologyTypeNamespace;

public interface OntologyType
    extends IJadescriptType, EmptyCreatable {

    SearchLocation getLocation();

    boolean isSuperOrEqualOntology(OntologyType other);

    @Override
    OntologyTypeNamespace namespace();

    final TypeCategory CATEGORY = new TypeCategoryAdapter() {
        @Override
        public boolean isOntology() {
            return true;
        }
    };

    @Override
    default TypeCategory category() {
        return CATEGORY;
    }

}
