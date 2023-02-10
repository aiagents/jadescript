package jadescript.util;


import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by Giuseppe on 31/03/18.
 *
 * @author Giuseppe Petrosino - giuseppe.petrosino@studenti.unipr.it
 */
public class JadescriptCollections {

    private JadescriptCollections() {
    }


    @SafeVarargs
    public static <K, V> JadescriptMap<K, V> getRest(
        JadescriptMap<K, V> map,
        K... excludeKeys
    ) {
        return new JadescriptMapRest<>(map, excludeKeys);
    }


    public static <T> JadescriptListRest<T> getRest(
        JadescriptList<T> list,
        int excludeHead
    ) {
        return new JadescriptListRest<>(list, excludeHead);
    }


    public static <T> JadescriptListRest<T> getRest(JadescriptList<T> list) {
        return new JadescriptListRest<>(list, 0);
    }


    @SafeVarargs
    public static <T> JadescriptSet<T> getRest(
        JadescriptSet<T> set,
        T... excludeElements
    ) {
        return new JadescriptSetRest<>(set, excludeElements);
    }


    public static <T> JadescriptList<T> createList(
        Collection<? extends T> c
    ) {
        return new JadescriptList<>(c);
    }


    public static <T> JadescriptList<T> createList(
        Collection<? extends T> c,
        Collection<? extends T> rest
    ) {
        return new JadescriptList<>(c, rest);
    }


    public static <K, V> JadescriptMap<K, V> createMap(
        List<K> keys,
        List<V> values
    ) {
        JadescriptMap<K, V> result = new JadescriptMap<>();
        result.setKeys(keys);
        result.setValues(values);
        return result;
    }


    public static <K, V> JadescriptMap<K, V> createMap(
        List<K> keys,
        List<V> values,
        Map<K, V> rest
    ) {
        final JadescriptMap<K, V> map = createMap(keys, values);
        map.putAll(rest);
        return map;
    }


    public static <T> JadescriptSet<T> createSet(List<T> elements) {
        JadescriptSet<T> result = new JadescriptSet<>();
        result.setElements(elements);
        return result;
    }


    public static <T> JadescriptSet<T> createSet(
        List<T> elements,
        Collection<? extends T> rest
    ) {
        final JadescriptSet<T> result = createSet(elements);
        result.addAll(rest);
        return result;
    }


    @SafeVarargs
    public static <T> JadescriptSet<T> createSet(T... elements) {
        return createSet(Arrays.asList(elements));
    }


}
