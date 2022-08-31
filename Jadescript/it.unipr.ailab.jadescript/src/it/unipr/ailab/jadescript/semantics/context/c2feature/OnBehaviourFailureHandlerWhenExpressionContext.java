package it.unipr.ailab.jadescript.semantics.context.c2feature;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class OnBehaviourFailureHandlerWhenExpressionContext
        extends HandlerWhenExpressionContext
implements NamedSymbol.Searcher, OnBehaviourFailureHandledContext {
    private final IJadescriptType failedBehaviourType;
    private final IJadescriptType behaviourFailureReasonType;

    public OnBehaviourFailureHandlerWhenExpressionContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer,
            IJadescriptType failedBehaviourType, IJadescriptType behaviourFailureReasonType
    ) {
        super(module, outer);
        this.failedBehaviourType = failedBehaviourType;
        this.behaviourFailureReasonType = behaviourFailureReasonType;
    }

    @Override
    public String getCurrentOperationLogName() {
        return "<evaluating when-expression>";
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
    public Stream<? extends NamedSymbol> searchName(Predicate<String> name, Predicate<IJadescriptType> readingType, Predicate<Boolean> canWrite) {
        return Streams.concat(
                getFailedBehaviourStream(name, readingType, canWrite),
                getFailureReasonStream(name, readingType, canWrite)
        );
    }
}
