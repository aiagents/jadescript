package it.unipr.ailab.jadescript.semantics.jadescripttypes.agent;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.UsingOntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategory;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategoryAdapter;
import it.unipr.ailab.jadescript.semantics.namespace.AgentTypeNamespace;

public interface AgentType extends IJadescriptType, UsingOntologyType {

    final TypeCategory CATEGORY = new TypeCategoryAdapter() {
        @Override
        public boolean isAgent() {
            return true;
        }
    };

    @Override
    AgentTypeNamespace namespace();

    @Override
    default TypeCategory category(){
        return CATEGORY;
    }

}
