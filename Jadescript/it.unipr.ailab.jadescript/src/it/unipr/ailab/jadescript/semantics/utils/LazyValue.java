package it.unipr.ailab.jadescript.semantics.utils;

import java.util.Objects;
import java.util.function.Supplier;

public final class LazyValue<T> implements Supplier<T> {
    private T value = null;
    private boolean evaluated = false;
    private Supplier<T> supplier = null;

    public LazyValue(Supplier<T> supplier){
        Objects.requireNonNull(supplier);
        this.supplier = supplier;
    }

    public LazyValue(T element){
        this.value = element;
        this.evaluated = true;
    }


    @Override
    public T get() {
        if(!this.evaluated){
            this.value = this.supplier.get();
            this.evaluated = true;
        }
        return this.value;
    }
}
