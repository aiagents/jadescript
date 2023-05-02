package it.unipr.ailab.maybe;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created on 2019-06-13.
 */
public final class Functional {

    private Functional(){}//do not instantiate


    public interface TriFunction<T1, T2, T3, R>{
        R apply(T1 t1, T2 t2, T3 t3);
    }

    public interface QuadFunction<T1, T2, T3, T4, R>{
        R apply(T1 t1, T2 t2, T3 t3, T4 t4);
    }

    @FunctionalInterface
    public interface TriConsumer<T1, T2, T3>{
        void accept(T1 t1, T2 t2, T3 t3);
    }

    @FunctionalInterface
    public interface QuadConsumer<T1, T2, T3, T4>{
        void accept(T1 t1, T2 t2, T3 t3, T4 t4);
    }

    @FunctionalInterface
    public interface PentaConsumer<T1, T2, T3, T4, T5>{
        void accept(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
    }


    public static <T1,T2,R> Function<T2, R> partial1(
        BiFunction<T1, T2, R> function,
        T1 arg
    ){
        return (t2 -> function.apply(arg, t2));
    }

    public static <T1,T2,R> Function<T1, R> partial2(
        BiFunction<T1, T2, R> function,
        T2 arg
    ){
        return (t1 -> function.apply(t1, arg));
    }

    public static <T1, T2> Function<T1, Stream<T2>> filterAndCast(
        Class<T2> clazz
    ){
        return x -> Stream.of(x).filter(clazz::isInstance).map(clazz::cast);
    }

}
