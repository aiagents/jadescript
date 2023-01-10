package it.unipr.ailab.jadescript.semantics.context.staticstate;

public interface EvaluationResult {

    interface Returned extends EvaluationResult {
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
}
