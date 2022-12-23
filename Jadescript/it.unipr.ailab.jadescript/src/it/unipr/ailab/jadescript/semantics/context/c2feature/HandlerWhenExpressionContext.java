package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.List;

public abstract class HandlerWhenExpressionContext
        extends ProceduralFeatureContext
        implements WhenExpressionContext {

    public HandlerWhenExpressionContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer
    ) {
        super(module, outer);
    }

    //TODO generalize "property chains" in "flow-typeable values"
    public abstract Maybe<IJadescriptType> upperBoundForInterestedChain(List<String> propertyChain);

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is HandlerWhenExpressionContext");
        debugDumpIsWhenExpressionContext(scb);
    }
}
