package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public class OnDestroyHandlerContext extends EventHandlerContext {

    public OnDestroyHandlerContext(
        SemanticsModule module,
        ProceduralFeatureContainerContext outer
    ) {
        super(module, outer, "destroy");
    }


    @Override
    public String getCurrentOperationLogName() {
        return "on destroy";
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is OnDestroyHandlerContext");

    }

}
