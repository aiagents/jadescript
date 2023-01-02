package it.unipr.ailab.jadescript.semantics.context.staticstate;

import java.util.function.Predicate;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public interface FlowTypingInferrer {
    Stream<? extends IJadescriptType> getUpperBound(
        @Nullable Predicate<ExpressionDescriptor> forExpression,
        @Nullable Predicate<IJadescriptType> upperBound
    );

    Stream<? extends FlowTypingRule> getRule(
        @Nullable Predicate<ExpressionDescriptor> forExpression,
        @Nullable Predicate<FlowTypingRule> rule
    );
}
