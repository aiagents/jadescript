package it.unipr.ailab.jadescript.semantics.effectanalysis;

import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;

import java.util.Collections;
import java.util.List;

public interface EffectfulOperationSemantics {
    default List<Effect> computeEffects(Maybe<? extends EObject> input) {
        return Collections.emptyList();
    }
}
