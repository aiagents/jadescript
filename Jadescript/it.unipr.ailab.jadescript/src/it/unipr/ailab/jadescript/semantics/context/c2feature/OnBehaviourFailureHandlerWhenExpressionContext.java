package it.unipr.ailab.jadescript.semantics.context.c2feature;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class OnBehaviourFailureHandlerWhenExpressionContext
    extends HandlerWhenExpressionContext
    implements NamedSymbol.Searcher, BehaviourFailureHandledContext {



    public OnBehaviourFailureHandlerWhenExpressionContext(
        SemanticsModule module,
        ProceduralFeatureContainerContext outer
    ) {
        super(module, outer);
    }


    @Override
    public String getCurrentOperationLogName() {
        return "<evaluating when-expression>";
    }


    @Override
    public IJadescriptType getFailureReasonType() {
        return module.get(TypeHelper.class).PROPOSITION;
    }


    @Override
    public IJadescriptType getFailedBehaviourType() {
        return module.get(TypeHelper.class).ANYBEHAVIOUR;
    }


    @Override
    public Stream<? extends NamedSymbol> searchName(
        Predicate<String> name,
        Predicate<IJadescriptType> readingType,
        Predicate<Boolean> canWrite
    ) {
        return Streams.concat(
            getFailedBehaviourStream(name, readingType, canWrite),
            getFailureReasonStream(name, readingType, canWrite)
        );
    }

}
