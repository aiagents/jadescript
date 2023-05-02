package it.unipr.ailab.jadescript.semantics.utils;

import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SemanticsClassState<I extends EObject, T> {

    private final Map<I, T> stateMap = new HashMap<>();
    private final Supplier<T> newStateSupplier;
    private final T nullState;


    public SemanticsClassState(Supplier<T> newStateSupplier) {
        this.newStateSupplier = newStateSupplier;
        this.nullState = newStateSupplier.get();
    }


    public T getOrNew(Maybe<I> input) {
        if (input.isPresent()) {
            return stateMap.computeIfAbsent(
                input.toNullable(),
                (__) -> newStateSupplier.get()
            );
        }
        return nullState;
    }


    public Maybe<T> get(Maybe<I> input) {
        return input.__(stateMap::get);
    }


    public void set(Maybe<I> input, T newState) {
        if (input.isPresent()) {
            stateMap.put(input.toNullable(), newState);
        }
    }


}
