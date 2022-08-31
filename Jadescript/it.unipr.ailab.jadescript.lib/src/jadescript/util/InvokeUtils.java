package jadescript.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class InvokeUtils {
    public static Object invokeStatic(String className, String methodName, List<Class<?>> argsTypes, Object... args) {
        Object result = null;
        try {
            Class<?> aClass = Class.forName(className);
            Method declaredMethod = aClass.getDeclaredMethod(methodName, argsTypes.toArray(new Class[0]));
            result = declaredMethod.invoke(null, args);
        } catch (ClassNotFoundException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Object invokeOnInstance(Object instance, String methodName, List<Class<?>> argsTypes, Object... args) {
        Object result = null;
        try {
            Method declaredMethod = instance.getClass().getDeclaredMethod(methodName, argsTypes.toArray(new Class[0]));
            result = declaredMethod.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return result;
    }
}
