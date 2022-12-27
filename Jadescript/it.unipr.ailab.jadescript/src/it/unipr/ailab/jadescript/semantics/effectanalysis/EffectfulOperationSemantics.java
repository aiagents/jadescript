package it.unipr.ailab.jadescript.semantics.effectanalysis;

import it.unipr.ailab.maybe.Maybe;

import java.util.Collections;
import java.util.List;

public interface EffectfulOperationSemantics<T> {
    default List<Effect> computeEffects(Maybe<T> input) {
        return computeEffectsInternal(input);
    }

    default List<Effect> computeEffectsInternal(Maybe<T> input) {
        return Collections.emptyList();
    }
}
