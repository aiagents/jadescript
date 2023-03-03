package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.NotNull;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public interface BehaviourFailureHandledContext extends SemanticsConsts {

    @NotNull
    static ContextGeneratedName failureReasonContextGeneratedName(
        IJadescriptType failureReasonType
    ) {
        return new ContextGeneratedName(
            "failureReason",
            failureReasonType,
            () -> FAILURE_REASON_VAR_NAME
        );
    }

    @NotNull
    static ContextGeneratedName behaviourContextGeneratedName(
        IJadescriptType failedBehaviourType
    ) {
        return new ContextGeneratedName(
            "behaviour",
            failedBehaviourType,
            () -> FAILED_BEHAVIOUR_VAR_NAME
        );
    }

    IJadescriptType getFailureReasonType();

    IJadescriptType getFailedBehaviourType();


    default CompilableName getFailureReasonName() {
        return failureReasonContextGeneratedName(getFailureReasonType());
    }


    default CompilableName getFailedBehaviourName() {
        return behaviourContextGeneratedName(getFailedBehaviourType());
    }

    default void debugDumpBehaviourFailureHandled(SourceCodeBuilder scb) {
        scb.open("--> is BehaviourFailureHandledContext {");
        scb.line("failureReasonType = " +
            getFailureReasonType().getDebugPrint());
        scb.line("failedBehaviourType = " +
            getFailedBehaviourType().getDebugPrint());
        scb.close("}");
    }

}
