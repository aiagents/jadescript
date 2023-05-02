package it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.UserDefinedType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.BehaviourTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.Collections;

public class UserDefinedBehaviourType
    extends UserDefinedType<BaseBehaviourType>
    implements BehaviourType {

    private final Maybe<IJadescriptType> superType;


    public UserDefinedBehaviourType(
        SemanticsModule module,
        JvmTypeReference jvmType,
        Maybe<IJadescriptType> superType,
        BaseBehaviourType rootCategoryType
    ) {
        super(module, jvmType, rootCategoryType);
        this.superType = superType;
    }


    @Override
    public TypeArgument getForAgentType() {
        return getRootCategoryType().getForAgentType();
    }


    public BaseBehaviourType.Kind getBehaviourKind() {
        return getRootCategoryType().getBehaviourKind();
    }


    @Override
    public boolean isSendable() {
        return false;
    }


    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        return Maybe.nothing();
    }


    @Override
    public BehaviourTypeNamespace namespace() {
        return new BehaviourTypeNamespace(
            module,
            this,
            Collections.emptyList()
        );
    }


    public BehaviourType getSuperBehaviourType() {
        if (superType.isPresent()
            && superType.toNullable() instanceof BehaviourType) {
            return ((BehaviourType) superType.toNullable());
        }

        final JvmType type = asJvmTypeReference().getType();
        if (type instanceof JvmDeclaredType) {
            final IJadescriptType result = module.get(TypeSolver.class)
                .fromJvmTypeReference(
                    ((JvmDeclaredType) type).getExtendedClass()
                ).ignoreBound();
            if (result instanceof BehaviourType) {
                return ((BehaviourType) result);
            }
        }
        return module.get(BuiltinTypeProvider.class)
            .behaviour(getForAgentType());
    }

}
