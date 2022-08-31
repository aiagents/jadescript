package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public class SimpleHandlerContext
        extends EventHandlerContext {
    public SimpleHandlerContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer,
            String eventType
    ) {
        super(module, outer, eventType);
    }
    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is SimpleHandlerContext");
    }

    @Override
    public String getCurrentOperationLogName() {
        return "on " + getEventType();
    }
}
