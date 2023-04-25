package jadescript.java;


import jadescript.content.onto.basic.InvalidNativeOperationInvocation;
import jadescript.core.exception.JadescriptException;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RuntimeBindingsManager {

    RuntimeBindingsManager() {
        //Only Jadescript class can create it
    }


    private final Map<String, Class<?>> implementations
        = new ConcurrentHashMap<>();

    void bindNativeType(
        Class<?> interface_,
        Class<?> implementation
    ) {
        this.implementations.put(interface_.getName(), implementation);
    }

    void bindNativeType(
        String interfaceName,
        Class<?> implementation
    ) {
        this.implementations.put(interfaceName, implementation);
    }


    @SuppressWarnings("unchecked")
    <T> T createOrNull(String interfaceName){
        try {
            return (T) this.implementations.get(interfaceName)
                .getDeclaredConstructor()
                .newInstance();
        } catch (NoSuchMethodException | InvocationTargetException |
                 InstantiationException | IllegalAccessException e) {
            throw JadescriptException.wrap(e);
        }
    }

    <T> T createOrNull(Class<? extends T> interface_) {
        return createOrNull(interface_.getName());
    }

    <T> T create(String interfaceName){
        final T implemented = createOrNull(interfaceName);
        if(implemented == null){
            throw new JadescriptException(
                new InvalidNativeOperationInvocation(
                    "Could not find implementation class for interface '" +
                        interfaceName + "'. Use Jadescript.bindNative(...) to" +
                        " " +
                        "register the implementation class.",
                    ""
                )
            );
        }
        return implemented;
    }

    <T> T create(Class<? extends T> interface_){
        return create(interface_.getName());
    }


    Class<?> getImplementationClassOrNull(
        String interfaceName
    ) {
        return this.implementations.get(interfaceName);
    }


    Class<?> getImplementationClassOrNull(
        Class<?> interface_
    ) {
        return this.getImplementationClassOrNull(interface_.getName());
    }

    Class<?> getImplementationClass(
        String interfaceName
    ){
        final Class<?> implementation =
            getImplementationClassOrNull(interfaceName);
        if(implementation == null){
            throw new JadescriptException(
                new InvalidNativeOperationInvocation(
                    "Could not find implementation class for interface '" +
                        interfaceName + "'. Use Jadescript.bindNative(...) to" +
                        " " +
                        "register the implementation class.",
                    ""
                )
            );
        }
        return implementation;
    }

    Class<?> getImplementationClass(
        Class<?> interface_
    ) {
        return getImplementationClass(interface_.getName());
    }


}
