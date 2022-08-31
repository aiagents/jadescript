package it.unipr.ailab.jadescript.javaapi;

import java.util.HashMap;
import java.util.Map;

public class RuntimeBindingsManager {

    RuntimeBindingsManager(){
        //Only Jadescript class can create it
    }

    private Map<String, NativeValueFactory> factories = new HashMap<>();


    void bindNativeType(
            Class<?> interface_,
            NativeValueFactory spec
    ) {
        factories.put(interface_.getName(), spec);
    }

    NativeValueFactory getFactoryOrNull(String interfaceName){
        return factories.get(interfaceName);
    }

    NativeValueFactory getFactoryOrNull(
            Class<?> interface_
    ){
        return getFactoryOrNull(interface_.getName());
    }

    NativeValueFactory getFactory(String interfaceName){
        final NativeValueFactory specification = getFactoryOrNull(interfaceName);
        if(specification == null){
            throw new MissingBindingException(interfaceName);
        }
        return specification;
    }

    NativeValueFactory getFactory(Class<?> interface_){
        return getFactory(interface_.getName());
    }

    Class<?> getImplementationClass(
            String interfaceName
    ){
        return this.getFactory(interfaceName).getImplementationClass();
    }
}
