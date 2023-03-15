package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;

/**
 * Common interface for all those symbols whose actual type could be narrowed
 * using information based on code flow.
 */
public interface FlowSensitiveSymbol {

    /**
     * The descriptor matching this symbol.
     * Please note that coreferent symbols/expressions should have equal
     * descriptors.
     */
    public ExpressionDescriptor descriptor();

}
