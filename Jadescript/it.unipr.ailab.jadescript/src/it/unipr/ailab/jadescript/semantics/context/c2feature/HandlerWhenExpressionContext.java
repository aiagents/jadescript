package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public abstract class HandlerWhenExpressionContext
        extends ProceduralFeatureContext
        implements WhenExpressionContext {

    public HandlerWhenExpressionContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer
    ) {
        super(module, outer);
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is HandlerWhenExpressionContext");
        debugDumpIsWhenExpressionContext(scb);
    }
}
