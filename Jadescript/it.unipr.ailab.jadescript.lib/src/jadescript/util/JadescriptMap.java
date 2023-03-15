package jadescript.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jade.content.Concept;

public class JadescriptMap<K, V> implements Map<K, V>, Concept {

	private final List<K> keys = new ArrayList<>();
	private final List<V> values = new ArrayList<>();
	private final Map<K, V> hashMap = new HashMap<>();
	private boolean zipped = false;
	
	public JadescriptMap() {
		//empty ctor for JADE
	}
	

	public List<K> getKeys(){
		unzipIfNeeded();
		return keys;
	}
	
	/**
	 * NOTE: this should be used only in conjunction with setValues by the
	 * ContentManager extractor.
	 */
	public void setKeys(List<K> keys) {
		unzipIfNeeded();
		this.keys.clear();
		this.keys.addAll(keys);
	}
	
	public List<V> getValues(){
		unzipIfNeeded();
		return values;
	}
	
	/**
	 * NOTE: this should be used only in conjunction with setKeys by the
	 * ContentManager extractor.
	 */
	public void setValues(List<V> values) {
		unzipIfNeeded();
		this.values.clear();
		this.values.addAll(values);
	}
	
	@Override
	public int size() {
		zipIfNeeded();
		return hashMap.size();
	}

	@Override
	public boolean isEmpty() {
		zipIfNeeded();
		return hashMap.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		zipIfNeeded();
		return hashMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		zipIfNeeded();
		return hashMap.containsValue(value);
	}

	@Override
	public V get(Object key) {
		zipIfNeeded();
		final V v = hashMap.get(key);
		if(v == null) {
			throw new MissingEntryException(key);
		}
		return v;
	}

	@Override
	public V put(K key, V value) {
		zipIfNeeded();
		return hashMap.put(key, value);
	}

	@Override
	public V remove(Object key) {
		zipIfNeeded();
		return hashMap.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		zipIfNeeded();
		hashMap.putAll(m);
	}

	@Override
	public void clear() {
		zipIfNeeded();
		hashMap.clear();
	}

	@Override
	public Set<K> keySet() {
		zipIfNeeded();
		return hashMap.keySet();
	}

	@Override
	public Collection<V> values() {
		zipIfNeeded();
		return hashMap.values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		zipIfNeeded();
		return hashMap.entrySet();
	}
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{");
		zipIfNeeded();
		Set<K> keyset= hashMap.keySet();
		boolean first = true;
		for(K key:keyset) {
			V value = hashMap.get(key);
			if(first) {
				first = false;
			}else {
				sb.append(", ");
			}
			sb.append(Util.quoteIfString(key));
			sb.append(":");
			sb.append(Util.quoteIfString(value));
		}
		sb.append("}");
		return sb.toString();
	}


	protected void zip() {
		int size = Math.min(keys.size(), values.size());
		for(int i = 0; i < size; i++) {
			hashMap.put(keys.get(i), values.get(i));
		}
		
		keys.clear();
		values.clear();
		zipped = true;
	}
	
	protected void unzip() {
		hashMap.forEach((k, v) -> {
			keys.add(k);
			values.add(v);
		});
		
		hashMap.clear();
		zipped = false;
	}
	
	protected void zipIfNeeded() {
		if(!zipped) {
			zip();
		}
	}
	
	protected void unzipIfNeeded() {
		if(zipped) {
			unzip();
		}
	}

	public static class MissingEntryException extends RuntimeException{
		public MissingEntryException(Object key) {
			super("Missing entry with key: '" + key + "'.");
		}
	}
}
