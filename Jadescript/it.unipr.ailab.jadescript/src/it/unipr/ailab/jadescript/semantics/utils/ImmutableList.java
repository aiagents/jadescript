package it.unipr.ailab.jadescript.semantics.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.stream.Stream;

public class ImmutableList<E>
    implements Iterable<E> {
    //TODO optimize
    private final LinkedList<E> inner = new LinkedList<>();

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
}
