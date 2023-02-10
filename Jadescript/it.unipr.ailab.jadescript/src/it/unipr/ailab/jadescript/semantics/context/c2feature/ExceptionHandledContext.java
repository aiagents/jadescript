package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedReference;
import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.NameMember;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public interface ExceptionHandledContext extends SemanticsConsts {

    @NotNull
    static ContextGeneratedReference reasonContextGeneratedReference(
        IJadescriptType exceptionReasonType
    ) {
        return new ContextGeneratedReference(
            "exception",
            exceptionReasonType,
            (__) -> EXCEPTION_REASON_VAR_NAME
        );
    }

    IJadescriptType getExceptionReasonType();

    default Stream<NameMember> getExceptionReasonStream(
        Predicate<String> name,
        Predicate<IJadescriptType> readingType,
        Predicate<Boolean> canWrite
    ) {
        Stream<Integer> stream = Stream.of(0);
        stream = safeFilter(
            stream,
            __ -> "exception",
            name
        );
        stream = safeFilter(
            stream,
            __ -> getExceptionReasonType(),
            readingType
        );
        stream = safeFilter(
            stream,
            __ -> true,
            canWrite
        );
        return stream.map(__ -> reasonContextGeneratedReference(
            getExceptionReasonType()
        ));
    }

    default void debugDumpExceptionHandled(SourceCodeBuilder scb) {
        scb.open("--> is ExceptionHandledContext {");
        scb.line("exceptionReasonType = " +
            getExceptionReasonType().getDebugPrint());
        scb.close("}");
    }

}
