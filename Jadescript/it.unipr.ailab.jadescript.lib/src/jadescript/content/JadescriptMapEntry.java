package jadescript.content;

import jade.content.Concept;
import jadescript.util.JadescriptMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JadescriptMapEntry<K, V> implements Concept {
    private K key;
    private V value;

    public JadescriptMapEntry() {}

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }
    
    public static <Kt, Vt> JadescriptMapEntry<Kt, Vt> of(Kt key, Vt value){
    	JadescriptMapEntry<Kt, Vt> result = new JadescriptMapEntry<>();
    	result.setKey(key);
    	result.setValue(value);
    	return result;
    }
    
    public static <Kt, Vt> List<JadescriptMapEntry<Kt, Vt>> toListOfEntries(Map<Kt, Vt> input){
    	return input.entrySet().stream()
    			.map((entry) -> JadescriptMapEntry.of(entry.getKey(), entry.getValue()))
    			.collect(Collectors.toList());
    }
    
    public static <Kt, Vt> Map<Kt, Vt> fromListOfEntries(List<JadescriptMapEntry<Kt, Vt>> input){
    	Map<Kt, Vt> result = new JadescriptMap<>();
        for (JadescriptMapEntry<Kt, Vt> jadescriptMapEntry : input) {
            result.put(jadescriptMapEntry.getKey(), jadescriptMapEntry.getValue());
        }
    	return result;
    }
}
