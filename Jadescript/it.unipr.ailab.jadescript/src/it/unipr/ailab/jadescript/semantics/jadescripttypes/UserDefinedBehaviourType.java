package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.BehaviourTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.Collections;
import java.util.List;

public class UserDefinedBehaviourType
    extends UserDefinedType<BaseBehaviourType>
    implements EmptyCreatable, BehaviourType {

    private final Maybe<IJadescriptType> superType;

    public UserDefinedBehaviourType(
        SemanticsModule module,
        JvmTypeReference jvmType,
        BaseBehaviourType rootCategoryType
    ) {
        super(module, jvmType, rootCategoryType);
        this.superType = Maybe.nothing();
    }

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
    public IJadescriptType getForAgentType() {
        return getRootCategoryType().getForAgentType();
    }


    public BaseBehaviourType.Kind getBehaviourKind() {
        return getRootCategoryType().getBehaviourKind();
    }


    @Override
    public String compileNewEmptyInstance() {
        return compileToJavaTypeReference() +
            ".__createEmpty(" + AGENT_ENV + ")";
    }


    @Override
    public boolean requiresAgentEnvParameter() {
        return true;
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
    public boolean isCollection() {
        return false;
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
        if(superType.isPresent()
            && superType.toNullable() instanceof BehaviourType){
            return ((BehaviourType) superType.toNullable());
        }

        final JvmType type = asJvmTypeReference().getType();
        if (type instanceof JvmDeclaredType) {
            final IJadescriptType result = module.get(TypeHelper.class)
                .jtFromJvmTypeRef(((JvmDeclaredType) type).getExtendedClass());
            if (result instanceof BehaviourType) {
                return ((BehaviourType) result);
            }
        }
        return module.get(TypeHelper.class).BEHAVIOUR.apply(List.of(
            getForAgentType()));
    }

}
