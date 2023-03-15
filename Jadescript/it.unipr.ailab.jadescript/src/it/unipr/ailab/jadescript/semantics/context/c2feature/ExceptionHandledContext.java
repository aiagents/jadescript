package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.NotNull;

public interface ExceptionHandledContext extends SemanticsConsts {

    @NotNull
    static ContextGeneratedName reasonContextGeneratedName(
        IJadescriptType exceptionReasonType
    ) {
        return new ContextGeneratedName(
            "exception",
            exceptionReasonType,
            () -> EXCEPTION_REASON_VAR_NAME
        );
    }

    IJadescriptType getExceptionReasonType();

    default CompilableName getExceptionReasonName() {
        return reasonContextGeneratedName(getExceptionReasonType());
    }

    default void debugDumpExceptionHandled(SourceCodeBuilder scb) {
        scb.open("--> is ExceptionHandledContext {");
        scb.line("exceptionReasonType = " +
            getExceptionReasonType().getDebugPrint());
        scb.close("}");
    }

}
