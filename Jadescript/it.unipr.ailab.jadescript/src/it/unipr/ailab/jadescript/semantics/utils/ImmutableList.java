package it.unipr.ailab.jadescript.semantics.utils;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

public class ImmutableList<E> implements Iterable<E> {

    private static final ImmutableList<?> EMPTY = new ImmutableList<>();

    //TODO optimize
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
        final ImmutableList<E> result = new ImmutableList<>();
        result.inner.addAll(this.inner);
        result.inner.add(e);
        return result;
    }


    @NotNull
    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            private int i = 0;


            @Override
            public boolean hasNext() {
                return i < inner.size();
            }


            @Override
            public E next() {
                final E e = inner.get(i);
                i++;
                return e;
            }
        };
    }


    public Stream<E> stream() {
        return inner.stream();
    }


    public boolean isEmpty() {
        return inner.isEmpty();
    }


    public ImmutableList<E> concat(ImmutableList<E> l2) {
        final ImmutableList<E> result = new ImmutableList<>();
        result.inner.addAll(this.inner);
        result.inner.addAll(l2.inner);
        return result;
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

}
