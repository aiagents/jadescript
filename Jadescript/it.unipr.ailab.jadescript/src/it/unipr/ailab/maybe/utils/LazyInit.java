package it.unipr.ailab.maybe.utils;

import java.util.Objects;
import java.util.function.Supplier;

public final class LazyInit<T> implements Supplier<T> {

    private T value = null;
    private boolean evaluated = false;
    private Supplier<? extends T> supplier = null;


    public LazyInit(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier);
        this.supplier = supplier;
    }


    public LazyInit(T element) {
        this.value = element;
        this.evaluated = true;
    }


    public static <S> LazyInit<S> lazyInit(Supplier<? extends S> supplier) {
        return new LazyInit<>(supplier);
    }


    public static <S> LazyInit<S> init(S value) {
        return new LazyInit<>(value);
    }


    @Override
    public T get() {
        if (!this.evaluated) {
            this.value = this.supplier.get();
            this.evaluated = true;
        }
        return this.value;
    }

}
