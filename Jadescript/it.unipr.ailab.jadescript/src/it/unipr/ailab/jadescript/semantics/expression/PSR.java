package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;

/**
 * PSR is the acronym of Procedural Semantics Result.
 * Used as container for results of semantics operations for expressions and
 * statements that may affect the {@link StaticState}.
 * <p></p>
 * Using the acronym as class name to keep the method signatures short.
 * <p></p>
 * Another way to wrap the head around this is to think it this as a pair of
 * values ({@code T}, {@link StaticState}).
 */
public class PSR<T> {
    private final T result;
    private final StaticState newState;

    private PSR(T result, StaticState newState) {
        this.result = result;
        this.newState = newState;
    }


    public T result() {
        return result;
    }

    public StaticState state() {
        return newState;
    }

    @Override
    public String toString() {
        return result.toString();
    }


    public static <R> PSR<R> psr(R result, StaticState state) {
        return new PSR<>(result, state);
    }
}
