package it.unipr.ailab.maybe.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils.safeFilter;

public class ImmutableMultiMap<K, V> {

    private static final ImmutableMultiMap<?, ?> EMPTY =
        new ImmutableMultiMap<>();

    private final Map<K, Set<V>> inner = new HashMap<>();


    ImmutableMultiMap() {
        //package-default visibility
    }


    @SuppressWarnings("unchecked")
    public static <KK, VV> ImmutableMultiMap<KK, VV> empty() {
        return (ImmutableMultiMap<KK, VV>) EMPTY;
    }


    public static <KK, VV> ImmutableMultiMap<KK, VV> from(
        Map<? extends KK, ? extends Collection<? extends VV>> map
    ) {
        var result = new ImmutableMultiMap<KK, VV>();
        result.mutPutAllMany(map);
        return result;
    }

    @SafeVarargs
    public static <KK, VV> ImmutableMultiMap<KK, VV> ofSet(
        KK k, VV... v) {
        var result = new ImmutableMultiMap<KK, VV>();
        result.mutPutMany(k, List.of(v));
        return result;
    }


    @NotNull
    Set<V> getOrNewSet(K key) {
        return this.inner.computeIfAbsent(
            key,
            __ -> new HashSet<>()
        );
    }


    void mutPut(K key, V val) {
        getOrNewSet(key).add(val);
    }


    void mutPutMany(K key, Collection<? extends V> vals) {
        getOrNewSet(key).addAll(vals);
    }


    void mutPutMany(K key, Iterable<? extends V> vals) {
        final Set<V> innerSet = getOrNewSet(key);
        for (V val : vals) {
            innerSet.add(val);
        }
    }


    void mutPutAll(Map<? extends K, ? extends V> otherMap) {
        otherMap.forEach((k, v) -> getOrNewSet(k).add(v));
    }


    void mutReplaceSet(K key, Set<V> set) {
        this.inner.put(key, set);
    }


    private void mutPutAllMany(
        Map<? extends K, ? extends Collection<? extends V>> otherMap
    ) {
        otherMap.forEach((k, vv) -> getOrNewSet(k).addAll(vv));
    }


    public ImmutableSet<V> getValues(K key) {
        final Set<V> vs = this.inner.get(key);
        if (vs == null) {
            return ImmutableSet.empty();
        } else {
            return ImmutableSet.from(vs);
        }
    }


    public ImmutableSet<K> getKeys() {
        return ImmutableSet.from(this.inner.keySet());
    }


    public Stream<K> streamKeys() {
        return inner.keySet().stream();
    }


    public Stream<V> streamValues() {
        return inner.values().stream().flatMap(Collection::stream);
    }


    public Stream<V> streamValuesMatchingKey(
        @Nullable Predicate<K> keyPred
    ) {
        Stream<K> kStream = streamKeys();
        kStream = safeFilter(kStream, keyPred);

        return kStream.flatMap(k -> this.inner.get(k).stream());
    }


    public ImmutableMultiMap<K, V> put(K key, V val) {
        ImmutableMultiMap<K, V> result = new ImmutableMultiMap<>();
        result.mutPut(key, val);
        return result;
    }


    public ImmutableMultiMap<K, V> putMany(
        K key,
        Collection<? extends V> vals
    ) {
        ImmutableMultiMap<K, V> result = new ImmutableMultiMap<>();
        result.mutPutMany(key, vals);
        return result;
    }


    public ImmutableMultiMap<K, V> putMany(
        K key,
        Iterable<? extends V> vals
    ) {
        ImmutableMultiMap<K, V> result = new ImmutableMultiMap<>();
        result.mutPutMany(key, vals);
        return result;
    }


    public boolean containsKey(K key) {
        return this.inner.containsKey(key) && !this.inner.get(key).isEmpty();
    }


    public ImmutableMultiMap<K, V> putAll(
        ImmutableMultiMap<? extends K, ? extends V> other
    ) {
        ImmutableMultiMap<K, V> result = new ImmutableMultiMap<>();
        result.mutPutAllMany(other.inner);
        return result;
    }


    public ImmutableMultiMap<K, V> foldMergeSet(
        K key,
        ImmutableSet<V> values,
        BinaryOperator<ImmutableSet<V>> resolveConflicts
    ) {
        if (this.containsKey(key)) {
            final ImmutableSet<V> previously = getValues(key);
            return putMany(key, resolveConflicts.apply(previously, values));
        } else {
            return putMany(key, values);
        }
    }


    public ImmutableMultiMap<K, V> foldMergeAllSets(
        ImmutableMultiMap<K, V> other,
        BinaryOperator<ImmutableSet<V>> resolveConflicts
    ) {
        ImmutableMultiMap<K, V> result = this;
        for (K key : other.getKeys()) {
            result = result.foldMergeSet(
                key,
                other.getValues(key),
                resolveConflicts
            );
        }
        return result;
    }

}
