package it.unipr.ailab.jadescript.semantics.effectanalysis;

import java.util.Collections;
import java.util.List;

public interface Effect {

    default List<Effect> toList() {
        return Collections.singletonList(this);
    }

    interface JumpsAwayFromIteration extends Effect {
        JumpsAwayFromIteration INSTANCE = new JumpsAwayFromIteration() {
        };
    }

    interface JumpsAwayFromOperation extends JumpsAwayFromIteration {
        JumpsAwayFromOperation INSTANCE = new JumpsAwayFromOperation() {
        };
    }
}
