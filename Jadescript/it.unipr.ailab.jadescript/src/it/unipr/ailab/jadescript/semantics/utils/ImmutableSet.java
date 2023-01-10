package it.unipr.ailab.jadescript.semantics.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ImmutableSet<E> implements Iterable<E> {

    private static final ImmutableSet<?> EMPTY = new ImmutableSet<>();
    private final Set<E> wrapped = new HashSet<>();


    private ImmutableSet() {

    }


    @SuppressWarnings("unchecked")
    public static <T> ImmutableSet<T> empty() {
        return (ImmutableSet<T>) EMPTY;
    }


    @SafeVarargs
    public static <T> ImmutableSet<T> of(T... elements) {
        ImmutableSet<T> result = new ImmutableSet<>();
        result.wrapped.addAll(Arrays.asList(elements));
        return result;
    }


    public static <T> ImmutableSet<T> from(Iterable<? extends T> elements) {
        ImmutableSet<T> result = new ImmutableSet<>();
        for (T element : elements) {
            result.wrapped.add(element);
        }
        return result;
    }


    @Contract(pure = true)
    public ImmutableSet<E> add(E e) {
        if (contains(e)) {
            return this;
        } else {
            ImmutableSet<E> result = new ImmutableSet<>();
            result.wrapped.add(e);
            return result;
        }
    }


    @Contract(pure = true)
    @SuppressWarnings("unchecked")
    public ImmutableSet<E> addAll(Iterable<? extends E> other) {
        if (containsAll(other)) {
            return this;
        } else {
            ImmutableSet<E> result = new ImmutableSet<>();
            if (other instanceof Collection) {
                result.wrapped.addAll(((Collection<? extends E>) other));
            } else {
                for (E e : other) {
                    result.wrapped.add(e);
                }
            }
            return result;
        }
    }


    @Contract(pure = true)
    @SuppressWarnings("unchecked")
    public boolean containsAll(Iterable<? extends E> other) {
        if (other instanceof Collection) {
            return this.wrapped.containsAll(((Collection<? extends E>) other));
        } else {
            for (E e : other) {
                if (!this.contains(e)) {
                    return false;
                }
            }
            return true;
        }
    }

    public boolean isEmpty(){
        return this.wrapped.isEmpty();
    }


    @Contract(pure = true)
    public ImmutableSet<E> remove(E e) {
        if (!contains(e)) {
            return this;
        } else {
            var result = new ImmutableSet<E>();
            result.wrapped.addAll(this.wrapped);
            result.wrapped.remove(e);
            return result;
        }
    }


    @Contract(pure = true)
    public Stream<E> stream() {
        return wrapped.stream();
    }


    @Contract(pure = true)
    public ImmutableSet<E> removeIf(Predicate<? super E> pred) {
        var result = new ImmutableSet<E>();
        result.wrapped.addAll(this.wrapped);
        result.wrapped.removeIf(pred);
        return result;
    }


    @Contract(pure = true)
    public ImmutableSet<E> retainIf(Predicate<? super E> pred) {
        var result = new ImmutableSet<E>();
        result.wrapped.addAll(this.wrapped);
        result.wrapped.removeIf(pred.negate());
        return result;
    }


    @Contract(pure = true)
    public boolean contains(E e) {
        return wrapped.contains(e);
    }


    @Contract(pure = true)
    public ImmutableSet<E> union(ImmutableSet<E> other) {
        return this.addAll(other);
    }


    @Contract(pure = true)
    public ImmutableSet<E> intersection(ImmutableSet<E> other) {
        return this.retainIf(other::contains);
    }


    @Contract(pure = true)
    @NotNull
    @Override
    public Iterator<E> iterator() {
        return wrapped.iterator();
    }


    @Contract(pure = true)
    public <V> ImmutableMap<E, V> associate(
        Function<? super E, ? extends V> associator
    ) {
        Map<E, V> result = new HashMap<>();
        for (E e : this) {
            result.put(e, associator.apply(e));
        }
        return ImmutableMap.from(result);
    }


    /**
     * Like {@link ImmutableSet#associate(Function)}, but the value is not added
     * if the {@code associator}  function returns {@link Optional#empty()}.
     */
    public <V> ImmutableMap<E, V> associateOpt(
        Function<? super E, Optional<? extends V>> associator
    ){
        Map<E, V> result = new HashMap<>();
        for(E e: this){
            final Optional<? extends V> opt = associator.apply(e);
            opt.ifPresent(o -> result.put(e, o));
        }
        return ImmutableMap.from(result);
    }


    public <K> ImmutableMultiMap<K, E> associateKey(
        Function<? super E, ? extends K> computeKey
    ) {
        ImmutableMultiMap<K, E> result = new ImmutableMultiMap<>();
        for(E e: this){
            result.mutPut(computeKey.apply(e), e);
        }
        return result;
    }


    public Set<E> mutableCopy() {
        return new HashSet<>(this.wrapped);
    }

}
