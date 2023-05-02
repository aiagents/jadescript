package it.unipr.ailab.jadescript.semantics.context;

public interface MightUseAgentReference {
    /**
     * Returns true if the current context can use 'agent' and perform
     * agent-related actions in an agent declaration.
     */
    boolean canUseAgentReference();

    /**
     * To be used in combination with actAs+findFirst, so the innermost
     * context is the one that gives the response.
     * If findFirst returns nothing, the answer is false.
     */
    static boolean canUseAgentReference(Context inner){
        return inner.actAs(MightUseAgentReference.class)
                .findFirst()
                .map(it -> it.canUseAgentReference())
                .orElse(false);
    }
}
