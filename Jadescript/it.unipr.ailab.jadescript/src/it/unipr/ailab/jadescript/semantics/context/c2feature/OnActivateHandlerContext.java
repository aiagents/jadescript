package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public class OnActivateHandlerContext
        extends EventHandlerContext {
    public OnActivateHandlerContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer
    ) {
        super(module, outer, "activate");
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is OnActivateHandlerContext");
    }

    @Override
    public String getCurrentOperationLogName() {
        return "on activate";
    }
}
