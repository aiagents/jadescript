package jadescript.java;

import java.util.HashMap;
import java.util.Map;

class SingletonsManager {

    private final RuntimeBindingsManager bindingsManager;
    private final Map<String, Object> singletons = new HashMap<>();


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
