package it.unipr.ailab.maybe.utils;

import org.jetbrains.annotations.Contract;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class ImmutableMap<K, V> {

    private static final ImmutableMap<?, ?> EMPTY = new ImmutableMap<>();


    private final HashMap<K, V> inner = new HashMap<>();


    @SuppressWarnings("unchecked")
    public static <T1, T2> ImmutableMap<T1, T2> empty() {
        return (ImmutableMap<T1, T2>) EMPTY;
    }


    public static <T1, T2> ImmutableMap<T1, T2> from(Map<T1, T2> map) {
        var result = new ImmutableMap<T1, T2>();
        result.inner.putAll(map);
        return result;
    }


    public static <K, E> Collector<K, Map<K, E>, ImmutableMap<K, E>>
    toImmutableMap(Function<K, E> transformKeys) {
        return new Collector<>() {

            @Override
            public Supplier<Map<K, E>> supplier() {
                return HashMap::new;
            }


            @Override
            public BiConsumer<Map<K, E>, K> accumulator() {
                return (m, k) -> m.put(k, transformKeys.apply(k));
            }


            @Override
            public BinaryOperator<Map<K, E>> combiner() {
                return (m1, m2) -> {
                    m2.putAll(m1);
                    return m2;
                };
            }


            @Override
            public Function<Map<K, E>, ImmutableMap<K, E>> finisher() {
                return ImmutableMap::from;
            }


            @Override
            public Set<Characteristics> characteristics() {
                return Set.of(Characteristics.UNORDERED);
            }
        };
    }


    @Contract(pure = true)
    public Optional<V> get(K key) {
        return Optional.ofNullable(getUnsafe(key));
    }


    @Contract(pure = true)
    public V getUnsafe(K key) {
        return inner.get(key);
    }


    /**
     * Provides means to perform mutable changes to a mutable {@link Map}
     * containing all the entries of this immutable map, which is then used to
     * initialize a new ImmutableMap.
     */
    @Contract(pure = true)
    public ImmutableMap<K, V> change(
        Consumer<Map<K, V>> doOnMutableMap
    ) {
        final ImmutableMap<K, V> result = new ImmutableMap<>();
        result.inner.putAll(this.inner);
        doOnMutableMap.accept(result.inner);
        return result;
    }


    @Contract(pure = true)
    public ImmutableMap<K, V> change(
        Predicate<ImmutableMap<K, V>> changeCondition,
        Consumer<Map<K, V>> doOnMutableMap
    ) {
        if (changeCondition.test(this)) {
            return this.change(doOnMutableMap);
        } else {
            return this;
        }
    }


    @Contract(pure = true)
    public ImmutableMap<K, V> put(K key, V value) {
        return change(
            m -> !m.containsKey(key) || !m.get(key).equals(value),
            m -> m.put(key, value)
        );
    }


    /**
     * Like put, but if the entry is already contained in the map, it adds the
     * result of {@code resolveConflict}, giving {@code value} and the value
     * already present in the map as arguments to it.
     */
    @Contract(pure = true)
    public ImmutableMap<K, V> mergeAdd(
        K key, V value, BinaryOperator<V> resolveConflict
    ) {
        return change(m -> {
            if (m.containsKey(key)) {
                m.put(key, resolveConflict.apply(
                    m.get(key),
                    value
                ));
            } else {
                m.put(key, value);
            }
        });
    }


    @Contract(pure = true)
    public ImmutableMap<K, V> mergeAddAll(
        Collection<? extends V> values,
        Function<V, K> associateKey,
        BinaryOperator<V> solveConflicts
    ) {
        return change(m -> {
            for (V v : values) {
                K k = associateKey.apply(v);
                if (m.containsKey(k)) {
                    m.put(k, solveConflicts.apply(
                        m.get(k),
                        v
                    ));
                } else {
                    m.put(k, v);
                }
            }
        });
    }


    @Contract(pure = true)
    public boolean containsKey(K key) {
        return this.inner.containsKey(key);
    }


    @Contract(pure = true)
    public Stream<K> streamKeys() {
        return this.inner.keySet().stream();
    }


    @Contract(pure = true)
    public ImmutableSet<K> getKeys() {
        return ImmutableSet.from(this.inner.keySet());
    }


    @Contract(pure = true)
    public Iterable<V> values() {
        return this.inner.values();
    }


    @Contract(pure = true)
    public Stream<? extends V> streamValues() {
        return this.inner.values().stream();
    }


    public ImmutableMap<K, V> remove(K ed) {
        if (containsKey(ed)) {
            return change(m -> {
                m.remove(ed);
            });
        } else {
            return this;
        }
    }


    public ImmutableMap<K, V> putAll(
        ImmutableMap<K, V> other
    ) {
        return putAll(other.inner);
    }


    public ImmutableMap<K, V> putAll(
        Map<K, V> other
    ) {
        return this.change(m -> m.putAll(other));
    }

}
