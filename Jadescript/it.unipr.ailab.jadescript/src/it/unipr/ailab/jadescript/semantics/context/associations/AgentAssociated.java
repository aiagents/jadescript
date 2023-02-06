package it.unipr.ailab.jadescript.semantics.context.associations;

import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedReference;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

public interface AgentAssociated extends AgentAssociationComputer, Associated {
    static ContextGeneratedReference contextGeneratedAgentReference(
        IJadescriptType agentType
    ) {
        return new ContextGeneratedReference(
            "agent",
            agentType,
            (__) -> SemanticsConsts.THE_AGENT + "()"
        );
    }
}
