package jadescript.java;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class SingletonsManager {

    private final RuntimeBindingsManager bindingsManager;
    private final Map<String, Object> singletons = new ConcurrentHashMap<>();


    SingletonsManager(RuntimeBindingsManager bindingsManager) {
        this.bindingsManager = bindingsManager;
    }


    @SuppressWarnings("unchecked")
    <T> T get(String interfaceName) {
        return (T) singletons.computeIfAbsent(
            interfaceName,
            bindingsManager::create
        );
    }

    <T> T get(Class<? extends T> interface_){
        return get(interface_.getName());
    }

}
