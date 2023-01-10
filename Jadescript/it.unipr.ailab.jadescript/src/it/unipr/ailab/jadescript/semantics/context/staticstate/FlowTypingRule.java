package it.unipr.ailab.jadescript.semantics.context.staticstate;

import java.util.function.Function;

public class FlowTypingRule {
    private final EvaluationResult ruleCondition;
    private final Function<StaticState, StaticState> consequence;

    public FlowTypingRule(
        EvaluationResult ruleCondition,
        Function<StaticState, StaticState> consequence
    ) {
        this.ruleCondition = ruleCondition;
        this.consequence = consequence;
    }


    public EvaluationResult getRuleCondition() {
        return ruleCondition;
    }

    public Function<StaticState, StaticState> getConsequence() {
        return consequence;
    }
}

