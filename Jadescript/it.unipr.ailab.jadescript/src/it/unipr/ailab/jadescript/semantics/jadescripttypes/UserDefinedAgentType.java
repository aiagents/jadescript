package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.Collections;

public class UserDefinedAgentType
        extends UserDefinedType<BaseAgentType>
        implements AgentType {
    public UserDefinedAgentType(
            SemanticsModule module,
            JvmTypeReference jvmType,
            BaseAgentType rootCategoryType
    ) {
        super(module, jvmType, rootCategoryType);
    }

    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        return Maybe.nothing();
    }

    @Override
    public AgentTypeNamespace namespace() {
        return new AgentTypeNamespace(module, this, Collections.emptyMap());
    }

    public AgentType getSuperAgentType() {
        final JvmType type = asJvmTypeReference().getType();
        if(type instanceof JvmDeclaredType){
            final IJadescriptType result = module.get(TypeHelper.class).jtFromJvmTypeRef(((JvmDeclaredType) type).getExtendedClass());
            if(result instanceof AgentType){
                return ((AgentType) result);
            }
        }
        return module.get(TypeHelper.class).AGENT;
    }

}
