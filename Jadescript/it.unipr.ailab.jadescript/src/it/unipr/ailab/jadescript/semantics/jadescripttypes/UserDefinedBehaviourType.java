package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.Collections;
import java.util.List;

public class UserDefinedBehaviourType
        extends UserDefinedType<BaseBehaviourType>
        implements EmptyCreatable, BehaviourType {
    public UserDefinedBehaviourType(
            SemanticsModule module,
            JvmTypeReference jvmType,
            BaseBehaviourType rootCategoryType
    ) {
        super(module, jvmType, rootCategoryType);
    }

    public IJadescriptType getForAgentType(){
        return getRootCategoryType().getForAgentType();
    }

    public BaseBehaviourType.Kind getBehaviourKind(){
        return getRootCategoryType().getBehaviourKind();
    }

    @Override
    public String compileNewEmptyInstance() {
        return compileToJavaTypeReference() + ".__createEmpty()";
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
        return new BehaviourTypeNamespace(module, this, Collections.emptyMap());
    }

    public BehaviourType getSuperBehaviourType() {
        final JvmType type = asJvmTypeReference().getType();
        if(type instanceof JvmDeclaredType){
            final IJadescriptType result = module.get(TypeHelper.class)
                    .jtFromJvmTypeRef(((JvmDeclaredType) type).getExtendedClass());
            if(result instanceof BehaviourType){
                return ((BehaviourType) result);
            }
        }
        return module.get(TypeHelper.class).BEHAVIOUR.apply(List.of(getForAgentType()));
    }
}
