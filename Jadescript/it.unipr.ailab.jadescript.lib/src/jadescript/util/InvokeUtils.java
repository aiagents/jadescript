package jadescript.util;


import jadescript.content.onto.basic.InvalidNativeOperationInvocation;
import jadescript.core.exception.JadescriptException;

import javax.lang.model.SourceVersion;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class InvokeUtils {


    public static Object executeNative(
        String fqMethodName,
        Object... arguments
    ) {
        if (fqMethodName == null) {
            throw new JadescriptException(
                new InvalidNativeOperationInvocation(
                    "The fully-qualified Java method name cannot be null.",
                    "(null)"
                )
            );
        }

        if (fqMethodName.isBlank()) {
            throw new JadescriptException(
                new InvalidNativeOperationInvocation(
                    "The fully-qualified Java method name cannot be blank.",
                    fqMethodName
                )
            );
        }


        List<String> segments = Arrays.asList(fqMethodName.split("\\."));
        if (segments.size() < 2) {
            throw new JadescriptException(
                new InvalidNativeOperationInvocation(
                    "The fully-qualified Java method requires at least 2 " +
                        "segments separated by '.'.",
                    fqMethodName)
            );
        }
        for (String segment : segments) {
            if (segment.isBlank()) {
                throw new JadescriptException(
                    new InvalidNativeOperationInvocation(
                        "The segments of the fully-qualified Java method name" +
                            " " +
                            "cannot be blank.",
                        fqMethodName
                    )
                );
            }
        }
        String classFQName = String.join(
            ".",
            segments.subList(0, segments.size() - 1)
        );
        String methodName = segments.get(segments.size() - 1);
        if (!SourceVersion.isName(classFQName)) {
            throw new JadescriptException(
                new InvalidNativeOperationInvocation(
                    "The provided fully-qualified class name is not valid.",
                    fqMethodName
                )
            );
        }
        if (!SourceVersion.isIdentifier(methodName)) {
            throw new JadescriptException(
                new InvalidNativeOperationInvocation(
                    "The provided method name (last segment) is not a valid" +
                        " identifier.",
                    fqMethodName
                )
            );
        }

        try {
            Class<?> aClass = Class.forName(classFQName);
            final Method[] declaredMethods = aClass.getDeclaredMethods();

            final Optional<Method> method = Arrays.stream(declaredMethods)
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                // The method must have the same name
                .filter(m -> Objects.equals(m.getName(), methodName))
                // The method must have the same arity
                .filter(m -> arguments != null
                    && m.getParameterTypes().length == arguments.length)
                // The method must have compatible types
                .filter(m -> {
                    Class<?>[] types = m.getParameterTypes();
                    // Parameter type array and argument array are equal size
                    for (int i = 0; i < types.length; i++) {
                        // cannot pass null to a primitive parameter
                        if (arguments[i] == null && types[i].isPrimitive()) {
                            return false;
                        }
                        // cannot pass an incompatible type (null is always ok
                        // for reference parameters)
                        if (arguments[i] != null &&
                            !types[i].isAssignableFrom(
                                arguments[i].getClass()
                            )) {
                            return false;
                        }
                    }
                    return true;
                }).min(Comparator.comparing(
                    Method::getReturnType,
                    //select the method with most specific return type
                    (r1, r2) -> {
                        if (r1.isAssignableFrom(r2)) {
                            return -1;
                        } else if (r2.isAssignableFrom(r1)) {
                            return +1;
                        } else {
                            return 0;
                        }
                    }
                ));

            if (method.isPresent()) {
                return method.get().invoke(null, arguments);
            }

            throw new JadescriptException(
                new InvalidNativeOperationInvocation(
                    "Could not find method with name '" + methodName + "', " +
                        "with parameters compatible with the ones provided, " +
                        "in class '" + classFQName + "'",
                    fqMethodName
                )
            );
        } catch (ClassNotFoundException e) {
            throw new JadescriptException(
                new InvalidNativeOperationInvocation(
                    "The class " + classFQName + " could not be found.",
                    fqMethodName
                )
            );
        } catch (Throwable e) {
            throw JadescriptException.wrap(e);
        }


    }


    public static Object invokeStatic(
        String className,
        String methodName,
        List<Class<?>> argsTypes,
        Object... args
    ) {
        Object result = null;
        try {
            Class<?> aClass = Class.forName(className);
            Method declaredMethod = aClass.getDeclaredMethod(methodName,
                argsTypes.toArray(new Class[0]));
            result = declaredMethod.invoke(null, args);
        } catch (ClassNotFoundException | IllegalAccessException
                 | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return result;
    }


    public static Object invokeOnInstance(
        Object instance,
        String methodName,
        List<Class<?>> argsTypes,
        Object... args
    ) {
        Object result = null;
        try {
            Method declaredMethod = instance.getClass()
                .getDeclaredMethod(methodName, argsTypes.toArray(new Class[0]));
            result = declaredMethod.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException
                 | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return result;
    }

}
