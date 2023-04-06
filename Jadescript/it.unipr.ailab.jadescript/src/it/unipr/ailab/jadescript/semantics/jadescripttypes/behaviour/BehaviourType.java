package it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.UsingOntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategory;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategoryAdapter;
import it.unipr.ailab.jadescript.semantics.namespace.BehaviourTypeNamespace;

public interface BehaviourType
    extends IJadescriptType, UsingOntologyType, AssociatedToAgentType {

    final TypeCategory CATEGORY = new TypeCategoryAdapter() {
        @Override
        public boolean isBehaviour() {
            return true;
        }
    };

    public static String getTypeName(Kind kind) {
        switch (kind) {
            case Cyclic:
                return "CyclicBehaviour";
            case OneShot:
                return "OneShotBehaviour";
            case Base:
            default:
                return "Behaviour";
        }
    }

    SearchLocation getLocation();

    @Override
    BehaviourTypeNamespace namespace();

    IJadescriptType getForAgentType();

    @Override
    default TypeCategory category() {
        return CATEGORY;
    }

    public enum Kind {
        Cyclic, OneShot, Base
    }

}
