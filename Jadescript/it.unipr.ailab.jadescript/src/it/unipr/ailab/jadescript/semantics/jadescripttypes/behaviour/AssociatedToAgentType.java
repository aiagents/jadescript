package it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.agent.AgentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmField;

import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts.THE_AGENT;

public interface AssociatedToAgentType extends IJadescriptType {

    default Maybe<AgentType> getDirectlyForAgentType() {
        final JvmTypeNamespace jvmNamespace = jvmNamespace();
        return Maybe.fromOpt((jvmNamespace == null
            ? Stream.<IJadescriptType>empty()
            : jvmNamespace.searchJvmField()
            .filter(f -> THE_AGENT.equals(f.getSimpleName()))
            .map(JvmField::getType)
            .map(jvmNamespace::resolveType))
            .filter(i -> i instanceof AgentType)
            .map(i -> (AgentType) i)
            .findAny());
    }

}
