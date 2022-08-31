package it.unipr.ailab.jadescript.semantics.context.flowtyping;

import com.google.common.collect.Streams;

import java.util.List;
import java.util.Objects;

/**
 * Created on 08/11/2019.
 */
public class ExpressionTypeTruth {
    private final List<String> propertyChain;
    private final FlowTypeInferringTerm inferredType;

    public ExpressionTypeTruth(List<String> propertyChain, FlowTypeInferringTerm inferringTerm) {
        this.propertyChain = propertyChain;
        this.inferredType = inferringTerm;
    }

    public List<String> getPropertyChain() {
        return propertyChain;
    }

    public FlowTypeInferringTerm getInferredType() {
        return inferredType;
    }

    public boolean matchesPropertyChain(List<String> strings) {
        if (propertyChain.size() != strings.size()) {
            return false;
        }

        return Streams.zip(propertyChain.stream(), strings.stream(), Objects::equals).reduce(true, Boolean::logicalAnd);
    }
}
