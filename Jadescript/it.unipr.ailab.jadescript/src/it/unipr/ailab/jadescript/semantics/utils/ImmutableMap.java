package it.unipr.ailab.jadescript.semantics.utils;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

public class ImmutableMap<K, V> {
    private final HashMap<K, V> inner = new HashMap<>();

    public Optional<V> get(K key) {
        return Optional.ofNullable(inner.get(key));
    }

    public ImmutableMap<K, V> put(K key, V value) {
        final ImmutableMap<K, V> result = new ImmutableMap<>();
        result.inner.putAll(this.inner);
        result.inner.put(key, value);
        return result;
    }

    public ImmutableMap<K, V> merge(
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

    public boolean containsKey(K key) {
        return this.inner.containsKey(key);
    }

    public Stream<K> streamKeys() {
        return this.inner.keySet().stream();
    }

    public Iterable<K> keys() {
        return this.inner.keySet();
    }

    public Iterable<V> values() {
        return this.inner.values();
    }
}
