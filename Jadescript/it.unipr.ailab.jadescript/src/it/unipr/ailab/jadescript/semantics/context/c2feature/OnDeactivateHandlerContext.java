package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public class OnDeactivateHandlerContext
    extends EventHandlerContext {

    public OnDeactivateHandlerContext(
        SemanticsModule module,
        ProceduralFeatureContainerContext outer
    ) {
        super(module, outer, "deactivate");
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is OnDeactivateHandlerContext");
    }


    @Override
    public String getCurrentOperationLogName() {
        return "on deactivate";
    }

}
