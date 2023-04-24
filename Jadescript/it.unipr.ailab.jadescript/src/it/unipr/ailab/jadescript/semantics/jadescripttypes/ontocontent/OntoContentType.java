package it.unipr.ailab.jadescript.semantics.jadescripttypes.ontocontent;

import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.EmptyCreatable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategory;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategoryAdapter;

public interface OntoContentType
    extends SemanticsConsts, IJadescriptType, EmptyCreatable {

    enum OntoContentKind {
        Concept, Action, Predicate, Proposition, AtomicProposition
    }

    final TypeCategory CATEGORY = new TypeCategoryAdapter() {
        @Override
        public boolean isOntoContent() {
            return true;
        }
    };

    boolean isNative();

    @Override
    default TypeCategory category(){
        return CATEGORY;
    }

    static String getTypeNameForKind(OntoContentKind ontoContentKind) {
        return ontoContentKind.name();
    }

    static String getCategoryName(OntoContentKind ontoContentKind){
        switch (ontoContentKind){
            case Predicate:
            case Proposition:
            case AtomicProposition:
                return "PROPOSITION";
            case Concept:
            case Action:
            default:
                return ontoContentKind.name().toUpperCase();
        }
    }

}
