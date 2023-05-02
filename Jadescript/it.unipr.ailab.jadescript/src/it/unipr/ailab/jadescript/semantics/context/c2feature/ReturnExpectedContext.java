package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public interface ReturnExpectedContext {

    IJadescriptType expectedReturnType();

    default void debugDumpReturnExpectation(SourceCodeBuilder scb) {
        scb.open("--> is ReturnExpectedContext {");
        scb.line("expectedReturnType = " + expectedReturnType()
            .getDebugPrint()
        );
        scb.close("}");
    }

}
