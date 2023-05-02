package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class OnBehaviourFailureHandlerContext
    extends HandlerWithWhenExpressionContext
    implements CompilableName.Namespace, BehaviourFailureHandledContext {

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
    public Stream<? extends CompilableName> compilableNames(
        @Nullable String name
    ) {
        return SemanticsUtils.buildStream(
            this::getFailureReasonName,
            this::getFailedBehaviourName
        ).filter(n -> name == null || name.equals(n.name()));
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
