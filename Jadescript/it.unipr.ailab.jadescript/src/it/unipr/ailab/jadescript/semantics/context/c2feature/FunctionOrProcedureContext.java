package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.ActualParameter;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.List;

public abstract class FunctionOrProcedureContext
        extends ProceduralFeatureContext
        implements ParameterizedContext {
    private final String functionOrProcedureName;
    private final List<ActualParameter> parameters;

    public FunctionOrProcedureContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer,
            String functionOrProcedureName,
            List<ActualParameter> parameters
    ) {
        super(module, outer);
        this.functionOrProcedureName = functionOrProcedureName;
        this.parameters = parameters;
    }


    @Override
    public List<ActualParameter> getParameters() {
        return parameters;
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is FunctionOrProcedureContext");
        debugDumpParameters(scb);
    }

    public String getFunctionOrProcedureName() {
        return functionOrProcedureName;
    }

    @Override
    public String getCurrentOperationLogName() {
        return functionOrProcedureName;
    }
}
