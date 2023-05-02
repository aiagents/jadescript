package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public class OnExecuteHandlerContext
    extends EventHandlerContext {

    public OnExecuteHandlerContext(
        SemanticsModule module,
        ProceduralFeatureContainerContext outer
    ) {
        super(module, outer, "execute");
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.open("--> is OnExecuteHandlerContext");
    }


    @Override
    public String getCurrentOperationLogName() {
        return "on execute";
    }

}
