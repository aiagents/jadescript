package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public interface PerceptPerceivedContext extends SemanticsConsts {

    static ContextGeneratedName perceptContentContextGeneratedReference(
        IJadescriptType contentType
    ) {
        return new ContextGeneratedName(
            "percept",
            contentType,
            () -> PERCEPT_CONTENT_VAR_NAME
        );
    }

    IJadescriptType getPerceptContentType();

    default CompilableName getPerceptContentName() {
        return perceptContentContextGeneratedReference(getPerceptContentType());
    }

    default void debugDumpPerception(SourceCodeBuilder scb) {
        scb.open("--> is PerceptPerceivedContext {");
        scb.line("perceptContentType = " + getPerceptContentType()
            .getDebugPrint());
        scb.close("}");
    }

}
