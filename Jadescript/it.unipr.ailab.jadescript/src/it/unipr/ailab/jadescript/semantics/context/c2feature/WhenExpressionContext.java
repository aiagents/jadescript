package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public interface WhenExpressionContext {
    default void debugDumpIsWhenExpressionContext(SourceCodeBuilder scb){
        scb.line("--> is WhenExpressionContext");
    }
}
