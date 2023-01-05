package it.unipr.ailab.jadescript.semantics.utils;

import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import org.jetbrains.annotations.Contract;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
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


    public V getUnsafe(K key) {
        return inner.get(key);
    }


    @Contract(pure = true)
    public ImmutableMap<K, V> put(K key, V value) {
        final ImmutableMap<K, V> result = new ImmutableMap<>();
        result.inner.putAll(this.inner);
        result.inner.put(key, value);
        return result;
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
        final ImmutableMap<K, V> result = new ImmutableMap<>();
        result.inner.putAll(this.inner);
        if (result.inner.containsKey(key)) {
            result.inner.put(key, resolveConflict.apply(
                result.inner.get(key),
                value
            ));
        } else {
            result.inner.put(key, value);
        }
        return result;
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


    public Stream<? extends V> streamValues() {
        return this.inner.values().stream();
    }

}
