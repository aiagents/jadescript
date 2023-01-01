package it.unipr.ailab.jadescript.semantics.context.flowtyping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Created on 08/11/2019.
 */
public class ExpressionTypeKB {
    private final List<ExpressionTypeTruth> truths = new ArrayList<>();

    public ExpressionTypeKB copy(){
        final ExpressionTypeKB etkb = new ExpressionTypeKB();
        this.truths.forEach(etkb::add);
        return etkb;
    }
    public ExpressionTypeKB mergeWith(
            ExpressionTypeKB kb2,
            BiFunction<FlowTypeInferringTerm, FlowTypeInferringTerm, FlowTypeInferringTerm> conflictResolution
    ) {
        ExpressionTypeKB result = empty();
        result.truths.addAll(truths);
        for (ExpressionTypeTruth t2 : kb2.truths) {
            Optional<FlowTypeInferringTerm> query = result.query(t2.getPropertyChain());
            if (query.isPresent()) {
                FlowTypeInferringTerm r = conflictResolution.apply(query.get(), t2.getInferredType());
                result.remove(t2.getPropertyChain());
                result.add(r, t2.getPropertyChain());
            } else {
                result.add(t2);
            }
        }
        return result;
    }

    public void add(FlowTypeInferringTerm term, List<String> propertyChain) {
        truths.add(new ExpressionTypeTruth(propertyChain, term));
    }

    public void add(ExpressionTypeTruth t) {
        truths.add(t);
    }

    public boolean remove(List<String> propertyChain) {
        return truths.removeIf(ett -> ett.matchesPropertyChain(propertyChain));
    }

    public Optional<FlowTypeInferringTerm> query(List<String> propertyChain) {
        return truths.stream()
                .filter(ett -> ett.matchesPropertyChain(propertyChain))
                .map(ExpressionTypeTruth::getInferredType\)
                .findFirst();
    }

    public Optional<FlowTypeInferringTerm> query(String... propertyChain) {
        return query(Arrays.asList(propertyChain));
    }

    public static ExpressionTypeKB empty() {
        return new ExpressionTypeKB();
    }


    public static ExpressionTypeKB not(ExpressionTypeKB subKb) {
        ExpressionTypeKB result = empty();
        for (ExpressionTypeTruth truth : subKb.truths) {
            result.add(truth.getInferredType().negate(), truth.getPropertyChain());
        }
        return result;
    }
}
