package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class OnExceptionHandlerWhenExpressionContext
    extends HandlerWhenExpressionContext
    implements NamedSymbol.Searcher, ExceptionHandledContext {


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
    public Stream<? extends NamedSymbol> searchName(
        Predicate<String> name,
        Predicate<IJadescriptType> readingType,
        Predicate<Boolean> canWrite
    ) {
        return getExceptionReasonStream(name, readingType, canWrite);
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
