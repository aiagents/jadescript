package it.unipr.ailab.jadescript.semantics.context.staticstate;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.stream.Stream;

public interface FlowSensitiveInferrer {

    Stream<IJadescriptType> inferUpperBound(
        ExpressionDescriptor forExpression
    );


}
