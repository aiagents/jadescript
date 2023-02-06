package it.unipr.ailab.jadescript.semantics.utils;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ImmutableList<E> implements Iterable<E> {

    private static final ImmutableList<?> EMPTY = new ImmutableList<>();

    private final LinkedList<E> inner = new LinkedList<>();


    private ImmutableList() {

    }


    @SuppressWarnings("unchecked")
    public static <T> ImmutableList<T> empty() {
        return (ImmutableList<T>) EMPTY;
    }


    @SafeVarargs
    public static <T> ImmutableList<T> of(T... args) {
        return from(Arrays.asList(args));
    }


    public static <T> ImmutableList<T> from(Collection<? extends T> args) {
        final ImmutableList<T> ts = new ImmutableList<>();
        ts.inner.addAll(args);
        return ts;
    }


    public ImmutableList<E> add(E e) {
        return change(l -> l.add(e));
    }


    @NotNull
    @Override
    public Iterator<E> iterator() {
        return this.inner.iterator();
    }


    public Stream<E> stream() {
        return inner.stream();
    }


    public boolean isEmpty() {
        return inner.isEmpty();
    }

    public ImmutableList<E> concat(ImmutableList<E> l2) {
        return change(l1 -> l1.addAll(l2.inner));
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImmutableList)) return false;

        ImmutableList<?> that = (ImmutableList<?>) o;

        return inner.equals(that.inner);
    }


    @Override
    public int hashCode() {
        return inner.hashCode();
    }


    public List<E> toMutable() {
        return new LinkedList<>(inner);
    }

    public ImmutableList<E> change(Consumer<List<E>> change){
        final ImmutableList<E> result = new ImmutableList<>();
        result.inner.addAll(this.inner);
        change.accept(result.inner);
        return result;
    }

}
