package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedReference;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public interface OnExceptionHandledContext extends SemanticsConsts {
    IJadescriptType getExceptionReasonType();

    default Stream<NamedSymbol> getExceptionReasonStream(
            Predicate<String> name,
            Predicate<IJadescriptType> readingType,
            Predicate<Boolean> canWrite
    ) {
        Stream<Integer> stream = Stream.of(0);
        stream = safeFilter(stream, __ -> EXCEPTION_REASON_VAR_NAME, name);
        stream = safeFilter(stream, __ -> getExceptionReasonType(), readingType);
        stream = safeFilter(stream, __ -> true, canWrite);
        return stream.map(__ -> new ContextGeneratedReference(EXCEPTION_REASON_VAR_NAME, getExceptionReasonType()));
    }

    default void debugDumpExceptionHandled(SourceCodeBuilder scb) {
        scb.open("--> is OnExceptionHandledContext {");
        scb.line("exceptionReasonType = " + getExceptionReasonType().getDebugPrint());
        scb.close("}");
    }
}
