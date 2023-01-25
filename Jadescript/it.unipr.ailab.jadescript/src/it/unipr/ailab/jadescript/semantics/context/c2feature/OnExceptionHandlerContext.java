package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public class OnExceptionHandlerContext
    extends HandlerWithWhenExpressionContext
    implements ExceptionHandledContext {

    private final IJadescriptType exceptionReasonType;


    public OnExceptionHandlerContext(
        SemanticsModule module,
        ProceduralFeatureContainerContext outer,
        IJadescriptType exceptionReasonType
    ) {
        super(module, outer, "exception");
        this.exceptionReasonType = exceptionReasonType;
    }


    @Override
    public String getCurrentOperationLogName() {
        return "on exception";
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is OnExceptionHandlerContext");
        debugDumpExceptionHandled(scb);
    }


    @Override
    public IJadescriptType getExceptionReasonType() {
        return exceptionReasonType;
    }

}
