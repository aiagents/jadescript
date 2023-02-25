package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
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

    //TODO 25/02/23:
    /*  - create actual parameters symbols
        - create way to automatically import all the members of something into a
            new scope/context, with correct coreferent descriptor
        - proceed to fix all the other contexts with context-generated references
     */

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is ProcedureContext");
    }
}
