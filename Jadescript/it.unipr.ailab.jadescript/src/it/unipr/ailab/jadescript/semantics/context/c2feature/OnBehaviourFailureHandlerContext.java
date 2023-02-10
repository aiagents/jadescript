package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.NameMember;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class OnBehaviourFailureHandlerContext
        extends HandlerWithWhenExpressionContext
        implements NameMember.Namespace, BehaviourFailureHandledContext {

    private final IJadescriptType failedBehaviourType;
    private final IJadescriptType behaviourFailureReasonType;

    public OnBehaviourFailureHandlerContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer,
            IJadescriptType failedBehaviourType,
            IJadescriptType behaviourFailureReasonType
    ) {
        super(module, outer, "behaviour failure");
        this.failedBehaviourType = failedBehaviourType;
        this.behaviourFailureReasonType = behaviourFailureReasonType;
    }

    @Override
    public IJadescriptType getFailureReasonType() {
        return behaviourFailureReasonType;
    }

    @Override
    public IJadescriptType getFailedBehaviourType() {
        return failedBehaviourType;
    }


    @Override
    public Stream<? extends NameMember> searchName(
        Predicate<String> name,
        Predicate<IJadescriptType> readingType,
        Predicate<Boolean> canWrite
    ) {
        return Stream.concat(
            getFailureReasonStream(
                name,
                readingType,
                canWrite
            ),
            getFailedBehaviourStream(
                name,
                readingType,
                canWrite
            )
        );
    }



    @Override
    public String getCurrentOperationLogName() {
        return "on behaviour failure";
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is OnBehaviourFailureHandlerContext");
        debugDumpBehaviourFailureHandled(scb);
    }
}
