package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedReference;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public interface PerceptPerceivedContext extends SemanticsConsts {
    IJadescriptType getPerceptContentType();

    default Stream<NamedSymbol> getPerceptContentStream(
            Predicate<String> name,
            Predicate<IJadescriptType> readingType,
            Predicate<Boolean> canWrite
    ) {
        Stream<Integer> stream = Stream.of(0);
        stream = safeFilter(stream, __ -> PERCEPT_CONTENT_VAR_NAME, name);
        stream = safeFilter(stream, __ -> getPerceptContentType(), readingType);
        stream = safeFilter(stream, __ -> true, canWrite);
        return stream.map(__ -> perceptContentContextGeneratedReference(getPerceptContentType()));
    }

    default void debugDumpPerception(SourceCodeBuilder scb) {
        scb.open("--> is PerceptPerceivedContext {");
        scb.line("perceptContentType = " + getPerceptContentType().getDebugPrint());
        scb.close("}");
    }


    static ContextGeneratedReference perceptContentContextGeneratedReference(
            IJadescriptType contentType
    ) {
        return new ContextGeneratedReference(
                PERCEPT_CONTENT_VAR_NAME,
                contentType
        );
    }
}
