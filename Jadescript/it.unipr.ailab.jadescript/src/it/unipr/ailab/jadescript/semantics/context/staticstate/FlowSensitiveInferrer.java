package it.unipr.ailab.jadescript.semantics.context.staticstate;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.stream.Stream;

public interface FlowSensitiveInferrer {

    Stream<IJadescriptType> inferUpperBound(
        ExpressionDescriptor forExpression
    );


}
