package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.ActualParameter;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.List;

public class FunctionContext extends FunctionOrProcedureContext implements ReturnExpectedContext {

    private final IJadescriptType returnType;

    public FunctionContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer,
            String functionName,
            List<ActualParameter> parameters,
            IJadescriptType returnType
    ) {
        super(module, outer, functionName, parameters);
        this.returnType = returnType;
    }

    @Override
    public IJadescriptType expectedReturnType() {
        return returnType;
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is FunctionContext");
        debugDumpReturnExpectation(scb);
    }



    public IJadescriptType getReturnType() {
        return returnType;
    }


}
