package it.unipr.ailab.jadescript.semantics.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Cache<T> {

    @FunctionalInterface
    public interface CachedBodyAcceptor<T>  {
        T get(Supplier<T> supplier);
    }

    private final Map<String, Map<Object[], T>> cachedResults = new HashMap<>();

    public void invalidate(){
        this.cachedResults.clear();
    }

    public void invalidate(String opName){
        if (cachedResults.containsKey(opName)) {
            this.cachedResults.get(opName).clear();
        }
    }

    public void invalidate(String opName, Object... args){
        if( cachedResults.containsKey(opName)){
            this.cachedResults.get(opName).remove(args);
        }
    }

    public CachedBodyAcceptor<T> when(String opName, Object... args){
        return supplier -> {
            if (!cachedResults.containsKey(opName)) {
                final T result = supplier.get();
                final HashMap<Object[], T> map = new HashMap<>();
                map.put(args, result);
                cachedResults.put(opName, map);
                return result;
            }

            final Map<Object[], T> subMap = cachedResults.get(opName);

            if (!subMap.containsKey(args)) {
                final T result = supplier.get();
                subMap.put(args, result);
                return result;
            }

            return subMap.get(args);
        };
    }


}
