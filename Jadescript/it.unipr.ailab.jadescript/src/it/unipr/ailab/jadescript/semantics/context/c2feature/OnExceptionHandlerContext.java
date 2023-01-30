package it.unipr.ailab.jadescript.semantics.context.c2feature;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.SymbolUtils;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class OnExceptionHandlerContext
    extends HandlerWithWhenExpressionContext
    implements NamedSymbol.Searcher, ExceptionHandledContext {

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
    public Stream<? extends NamedSymbol> searchName(
        Predicate<String> name,
        Predicate<IJadescriptType> readingType,
        Predicate<Boolean> canWrite
    ) {
        return getExceptionReasonStream(
            name,
            readingType,
            canWrite
        );
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
