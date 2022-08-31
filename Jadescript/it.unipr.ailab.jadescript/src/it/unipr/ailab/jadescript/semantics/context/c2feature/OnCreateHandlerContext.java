package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.ActualParameter;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.List;

public class OnCreateHandlerContext
        extends EventHandlerContext
        implements ParameterizedContext {
    private final List<ActualParameter> parameters;
    public OnCreateHandlerContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer,
            List<ActualParameter> parameters
    ) {
        super(module, outer, "create");
        this.parameters = parameters;
    }



    @Override
    public List<ActualParameter> getParameters() {
        return parameters;
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is OnCreateHandlerContext");
        debugDumpParameters(scb);
    }

    @Override
    public String getCurrentOperationLogName() {
        return "on "+getEventType();
    }


}
