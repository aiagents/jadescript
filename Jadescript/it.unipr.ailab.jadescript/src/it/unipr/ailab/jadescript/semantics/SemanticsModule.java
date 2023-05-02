package it.unipr.ailab.jadescript.semantics;

import it.unipr.ailab.jadescript.jvmmodel.JadescriptCompilerUtils;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.xbase.jvmmodel.JvmAnnotationReferenceBuilder;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypeReferenceBuilder;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class SemanticsModule {


    private final String phase;
    private final JvmTypesBuilder jvmTypesBuilder;
    private final JvmTypeReferenceBuilder jvmTypeReferenceBuilder;
    private final JvmAnnotationReferenceBuilder jvmAnnotationReferenceBuilder;
    private final IQualifiedNameProvider iQualifiedNameProvider;
    private final JadescriptCompilerUtils jadescriptCompilerUtils;
    private final Map<Class<?>, Object> singletonCache = new HashMap<>();


    public SemanticsModule(
        String phase,
        JvmTypesBuilder jvmTypesBuilder,
        JvmTypeReferenceBuilder jvmTypeReferenceBuilder,
        JvmAnnotationReferenceBuilder jvmAnnotationReferenceBuilder,
        IQualifiedNameProvider iQualifiedNameProvider,
        JadescriptCompilerUtils jadescriptCompilerUtils
    ) {
        this.phase = phase;
        this.jvmTypesBuilder = jvmTypesBuilder;
        this.jvmTypeReferenceBuilder = jvmTypeReferenceBuilder;
        this.jvmAnnotationReferenceBuilder = jvmAnnotationReferenceBuilder;
        this.iQualifiedNameProvider = iQualifiedNameProvider;
        this.jadescriptCompilerUtils = jadescriptCompilerUtils;
    }


    public <T> void bind(Class<? extends T> clazz, T instance) {
        singletonCache.put(clazz, instance);
    }


    @SuppressWarnings("unchecked")
    public <T> T get(@NotNull Class<? extends T> clazz) {
        if (clazz.isAssignableFrom(JvmTypesBuilder.class)) {
            return (T) jvmTypesBuilder;
        }
        if (clazz.isAssignableFrom(JvmTypeReferenceBuilder.class)) {
            return (T) jvmTypeReferenceBuilder;
        }
        if (clazz.isAssignableFrom(JvmAnnotationReferenceBuilder.class)) {
            return (T) jvmAnnotationReferenceBuilder;
        }
        if (clazz.isAssignableFrom(IQualifiedNameProvider.class)) {
            return (T) iQualifiedNameProvider;
        }
        if (clazz.isAssignableFrom(JadescriptCompilerUtils.class)) {
            return (T) jadescriptCompilerUtils;
        }
        if (clazz.isAssignableFrom(SemanticsModule.class)) {
            return (T) this;
        }

        if (singletonCache.containsKey(clazz)) {
            return (T) singletonCache.get(clazz);
        }

        try {
            final T t = clazz.getConstructor(SemanticsModule.class)
                .newInstance(this);
            singletonCache.put(clazz, t);
            return t;
        } catch (NoSuchMethodException e) {
            return getNonJadescript(clazz);
        } catch (InstantiationException | IllegalAccessException
                 | InvocationTargetException e) {
            System.err.println("Error while creating new " +
                clazz.getSimpleName()
            );
            e.printStackTrace();

            //noinspection DataFlowIssue
            throw Exceptions.sneakyThrow(e);
        }
    }


    public String getPhase() {
        return phase;
    }


    @SuppressWarnings("unchecked")
    private <T> T getNonJadescript(Class<? extends T> clazz) {
        if (singletonCache.containsKey(clazz)) {
            return (T) singletonCache.get(clazz);
        }

        try {
            final T t = clazz.getConstructor().newInstance();
            singletonCache.put(clazz, t);
            return t;
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException e) {
            System.err.println("Error while creating new " +
                clazz.getSimpleName()
            );
            e.printStackTrace();
            //noinspection DataFlowIssue
            throw Exceptions.sneakyThrow(e);
        }
    }

}
