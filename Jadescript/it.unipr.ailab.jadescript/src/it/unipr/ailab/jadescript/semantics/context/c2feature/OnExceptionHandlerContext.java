package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class OnExceptionHandlerContext
    extends HandlerWithWhenExpressionContext
    implements CompilableName.Namespace, ExceptionHandledContext {

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
    public Stream<? extends CompilableName> compilableNames(
        @Nullable String name
    ) {
        return Util.buildStream(
            this::getExceptionReasonName
        ).filter(n -> name == null || name.equals(n.name()));
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
