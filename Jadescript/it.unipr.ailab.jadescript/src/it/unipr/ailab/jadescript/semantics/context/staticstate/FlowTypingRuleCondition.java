package it.unipr.ailab.jadescript.semantics.context.staticstate;

public interface FlowTypingRuleCondition {

    interface Returned extends FlowTypingRuleCondition {
        Returned INSTANCE = new Returned() {
        };
    }

    interface ReturnedFalse extends Returned {
        ReturnedFalse INSTANCE = new ReturnedFalse() {
        };
    }

    interface ReturnedTrue extends Returned {
        ReturnedTrue INSTANCE = new ReturnedTrue() {
        };
    }

    interface DidImpossible extends FlowTypingRuleCondition {
        DidImpossible INSTANCE = new DidImpossible() {
        };
    }
}
