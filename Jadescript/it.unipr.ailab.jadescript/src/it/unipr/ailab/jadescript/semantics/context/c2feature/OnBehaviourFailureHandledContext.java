package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedReference;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public interface OnBehaviourFailureHandledContext extends SemanticsConsts {
    IJadescriptType getFailureReasonType();
    IJadescriptType getFailedBehaviourType();

    default Stream<NamedSymbol> getFailureReasonStream(
            Predicate<String> name,
            Predicate<IJadescriptType> readingType,
            Predicate<Boolean> canWrite
    ) {
        Stream<String> stream = Stream.of(FAILURE_REASON_VAR_NAME, "failure");
        stream = safeFilter(stream, it -> it, name);
        stream = safeFilter(stream, __ -> getFailureReasonType(), readingType);
        stream = safeFilter(stream, __ -> true, canWrite);
        return stream.map(__ -> new ContextGeneratedReference(FAILURE_REASON_VAR_NAME, getFailureReasonType()));
    }

    default Stream<NamedSymbol> getFailedBehaviourStream(
            Predicate<String> name,
            Predicate<IJadescriptType> readingType,
            Predicate<Boolean> canWrite
    ) {
        Stream<Integer> stream = Stream.of(0);
        stream = safeFilter(stream, __ -> FAILED_BEHAVIOUR_VAR_NAME, name);
        stream = safeFilter(stream, __ -> getFailureReasonType(), readingType);
        stream = safeFilter(stream, __ -> true, canWrite);
        return stream.map(__ -> new ContextGeneratedReference(FAILED_BEHAVIOUR_VAR_NAME, getFailureReasonType()));
    }

    default void debugDumpBehaviourFailureHandled(SourceCodeBuilder scb) {
        scb.open("--> is OnBehaviourFailureHandledContext {");
        scb.line("failureReasonType = " + getFailureReasonType().getDebugPrint());
        scb.line("failedBehaviourType = " + getFailedBehaviourType().getDebugPrint());
        scb.close("}");
    }
}
