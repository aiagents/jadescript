package it.unipr.ailab.jadescript.semantics.context.associations;

import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedName;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

public interface AgentAssociated
    extends AgentAssociationComputer, Associated {
    static ContextGeneratedName contextGeneratedAgentReference(
        IJadescriptType agentType
    ) {
        return new ContextGeneratedName(
            "agent",
            agentType,
            CompilationHelper::compileAgentReference
        );
    }
}
