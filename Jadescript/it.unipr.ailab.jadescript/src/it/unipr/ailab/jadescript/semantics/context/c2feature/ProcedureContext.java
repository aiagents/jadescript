package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.ActualParameter;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.List;

public class ProcedureContext extends FunctionOrProcedureContext {

    public ProcedureContext(
        SemanticsModule module,
        ProceduralFeatureContainerContext outer,
        String procedureName,
        List<ActualParameter> parameters
    ) {
        super(module, outer, procedureName, parameters);
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is ProcedureContext");
    }

}
