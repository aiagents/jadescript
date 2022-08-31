package jadescript.util;

import java.util.*;
import java.util.stream.Collectors;

public class JadescriptMapRest<K, V> extends JadescriptMap<K, V> {

    private final JadescriptMap<K, V> originalMap;
    private final Set<K> excludedKeys;

    @SafeVarargs
    public JadescriptMapRest(JadescriptMap<K, V> originalMap, K... excludedKey) {
        super();
        this.originalMap = originalMap;
        this.excludedKeys = new HashSet<>(Arrays.asList(excludedKey));
    }

    @Override
    public List<K> getKeys() {
        final List<K> keys = originalMap.getKeys();
        final List<K> result = new ArrayList<>();
        for (K key : keys) {
            if (!excludedKeys.contains(key)) {
                result.add(key);
            }
        }
        return result;
    }

    private List<K> settingKeys = null;
    private List<V> settingValues = null;

    @Override
    public void setKeys(List<K> keys) {
        settingKeys = keys;
        if (settingValues != null) {
            setBoth();
        }
    }


    private void setBoth() {
        if (settingKeys != null && settingValues != null) {
            Map<K, V> saved = new HashMap<>();
            for (K excludedKey : excludedKeys) {
                saved.put(excludedKey, originalMap.get(excludedKey));
            }
            originalMap.clear();
            originalMap.putAll(saved);
            for (int i = 0; i < Math.min(settingKeys.size(), settingValues.size()); i++) {
                originalMap.put(settingKeys.get(i), settingValues.get(i));
            }
            settingKeys = null;
            settingValues = null;
        }
    }


    @Override
    public List<V> getValues() {
        final List<K> keys = originalMap.getKeys();
        final List<V> values = originalMap.getValues();
        final List<V> result = new ArrayList<>();
        for (int i = 0; i < Math.min(keys.size(), values.size()); i++) {
            K key = keys.get(i);
            V value = values.get(i);
            if (!excludedKeys.contains(key)) {
                result.add(value);
            }
        }
        return result;
    }

    @Override
    public void setValues(List<V> values) {
        settingValues = values;
        if (settingKeys != null) {
            setBoth();
        }
    }

    @Override
    public int size() {
        return Math.max(0, originalMap.size() - excludedKeys.size());
    }

    @Override
    public boolean isEmpty() {
        return originalMap.isEmpty() || excludedKeys.containsAll(originalMap.getKeys());
    }

    @Override
    public boolean containsKey(Object key) {
        //noinspection SuspiciousMethodCalls
        if (excludedKeys.contains(key)) {
            return false;
        }
        return originalMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (!originalMap.containsValue(value)) {
            return false;
        }
        for (Entry<K, V> kvEntry : originalMap.entrySet()) {
            if (kvEntry.getValue().equals(value) && !excludedKeys.contains(kvEntry.getKey())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        V v;
        //noinspection SuspiciousMethodCalls
        if (!excludedKeys.contains(key)) {
            v = originalMap.get(key);
        } else {
            v = null;
        }
        if (v == null) {
            throw new MissingEntryException(key);
        }
        return v;
    }

    @Override
    public V put(K key, V value) {
        return originalMap.put(key, value);
    }

    @Override
    public V remove(Object key) {
        //noinspection SuspiciousMethodCalls
        if (excludedKeys.contains(key)) {
            return null;
        }
        return originalMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        originalMap.putAll(m);
    }

    @Override
    public void clear() {
        HashSet<K> keys = new HashSet<>(originalMap.getKeys());
        for (K key : keys) {
            if (!excludedKeys.contains(key)) {
                originalMap.remove(key);
            }
        }
    }

    @Override
    public Set<K> keySet() {
        HashSet<K> keys = new HashSet<>(originalMap.getKeys());
        for (K excludedKey : excludedKeys) {
            keys.remove(excludedKey);
        }
        return keys;
    }

    @Override
    public Collection<V> values() {
        HashSet<V> values = new HashSet<>();
        for (K k : keySet()) {
            values.add(originalMap.get(k));
        }
        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return originalMap.entrySet().stream()
                .filter((e) -> !excludedKeys.contains(e.getKey()))
                .collect(Collectors.toSet());
    }

    public JadescriptMap<K, V> toNew() {
        JadescriptMap<K, V> result = new JadescriptMap<>();
        for (Entry<K, V> e : entrySet()) {
            result.put(e.getKey(), e.getValue());
        }
        return result;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        Set<K> keyset = keySet();
        boolean first = true;
        for (K key : keyset) {
            V value = originalMap.get(key);
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(quoteIfString(key));
            sb.append(":");
            sb.append(quoteIfString(value));
        }
        sb.append("}");
        return sb.toString();
    }
}
