package it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.EmptyCreatable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.UsingOntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategory;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategoryAdapter;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.BehaviourTypeNamespace;

public interface BehaviourType
    extends IJadescriptType, UsingOntologyType,
    AssociatedToAgentType, EmptyCreatable {

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

    @Override
    default String compileNewEmptyInstance() {
        return JvmTypeHelper.noGenericsTypeName(compileToJavaTypeReference()) +
            ".__createEmpty(" + SemanticsConsts.AGENT_ENV + ")";
    }

    @Override
    default boolean requiresAgentEnvParameter() {
        return true;
    }

    SearchLocation getLocation();

    @Override
    BehaviourTypeNamespace namespace();

    TypeArgument getForAgentType();

    @Override
    default TypeCategory category() {
        return CATEGORY;
    }

    public enum Kind {
        Cyclic, OneShot, Base
    }

}
