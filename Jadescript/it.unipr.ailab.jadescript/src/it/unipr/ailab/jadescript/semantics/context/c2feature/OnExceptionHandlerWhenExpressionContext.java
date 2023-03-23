package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class OnExceptionHandlerWhenExpressionContext
    extends HandlerWhenExpressionContext
    implements CompilableName.Namespace, ExceptionHandledContext {


    public OnExceptionHandlerWhenExpressionContext(
        SemanticsModule module,
        ProceduralFeatureContainerContext outer
    ) {
        super(module, outer);
    }


    @Override
    public String getCurrentOperationLogName() {
        return "<evaluating when-expression>";
    }


    @Override
    public IJadescriptType getExceptionReasonType() {
        return module.get(TypeHelper.class).PROPOSITION;
    }


    @Override
    public Stream<? extends CompilableName> compilableNames(
        @Nullable String name
    ) {
        return SemanticsUtils.buildStream(
            this::getExceptionReasonName
        ).filter(n -> name == null || name.equals(n.name()));
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.open("-->  is OnExceptionHandlerWhenExpressionContext {");
        scb.line("exceptionReasonType = " +
            getExceptionReasonType().getDebugPrint());
        scb.close("}");
        debugDumpExceptionHandled(scb);
    }

}
