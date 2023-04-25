package it.unipr.ailab.jadescript.semantics.jadescripttypes.agentenv;

public enum SEMode {
    TOP, // <- for upper bound specifications in type parameters
    BOTTOM, // <- for the agent's agentEnv field (it has to be most assignable)
    NO_SE, // <- for agentEnv parameters in operations without side effects
    WITH_SE // <- for agentEnv parameters in operations with side effects
}
