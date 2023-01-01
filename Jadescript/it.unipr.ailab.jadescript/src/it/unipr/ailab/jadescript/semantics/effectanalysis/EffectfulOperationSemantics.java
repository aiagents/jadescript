package it.unipr.ailab.jadescript.semantics.effectanalysis;

import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.maybe.Maybe;

import java.util.Collections;
import java.util.List;

public interface EffectfulOperationSemantics<T> {
    default List<Effect> computeEffects(Maybe<T> input, StaticState state) {
        return computeEffectsInternal(input, state);
    }

    default List<Effect> computeEffectsInternal(
        Maybe<T> input,
        StaticState state
    ) {
        return Collections.emptyList();
    }
}
