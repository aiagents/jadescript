package it.unipr.ailab.jadescript.semantics.utils;

import it.unipr.ailab.jadescript.jadescript.FeatureContainer;
import it.unipr.ailab.jadescript.jadescript.NamedFeature;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.ProxyEObject;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.util.ITextRegionWithLineInformation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;

public class SemanticsUtils implements SemanticsConsts {

    private SemanticsUtils() {
    } // do not instantiate


    public static <T, X> java.util.function.Predicate<T> dinstinctBy(
        Function<? super T, ? extends X> extractor
    ) {
        HashSet<X> hashFlag = new HashSet<>();
        return t -> hashFlag.add(extractor.apply(t));
    }


    public static <T> java.util.function.Predicate<T> truePredicate() {
        return (__) -> true;
    }


    public static <T> java.util.function.Predicate<T> falsePredicate() {
        return (__) -> false;
    }


    public static <T> Maybe<? extends EObject> extractEObject(Maybe<T> object) {
        final T t = object.toNullable();
        if (t instanceof ProxyEObject) {
            return some(((ProxyEObject) t).getProxyEObject());
        } else if (t instanceof EObject) {
            return some(((EObject) t));
        } else {
            return nothing();
        }
    }


    public static <T, R> boolean allElementsMatch(
        List<T> a,
        List<R> b,
        BiPredicate<? super T, ? super R> predicate
    ) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!predicate.test(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }


    public static <T, R> boolean listEquals(List<T> a, List<R> b) {
        return allElementsMatch(a, b, Objects::equals);
    }


    public static <T, R> boolean endsWith(List<T> a, List<R> b) {
        if (b.isEmpty()) {
            return true;
        }
        return listEquals(
            a.subList(a.size() - 1 - b.size(), a.size()),
            b
        );
    }


    public static <T, R> boolean startsWith(List<T> a, List<R> b) {
        if (b.isEmpty()) {
            return true;
        }
        return listEquals(
            a.subList(0, b.size()),
            b
        );
    }


    public static <T> Stream<T> safeFilter(
        Stream<T> stream,
        @Nullable  Predicate<T> predicate
    ) {
        if (predicate != null) {
            return stream.filter(predicate);
        } else {
            return stream;
        }
    }


    public static <T1, T2> Stream<T1> safeFilter(
        Stream<T1> stream,
        Function<T1, T2> function,
        Predicate<T2> predicate
    ) {
        if (predicate != null && function != null) {
            return stream.filter(x -> predicate.test(function.apply(x)));
        } else {
            return stream;
        }
    }


    public static <T1, T2, T3> Stream<T1> safeFilter(
        Stream<T1> stream,
        Function<T1, T2> function1,
        Function<T1, T3> function2,
        BiPredicate<T2, T3> predicate
    ) {
        if (predicate != null && function1 != null && function2 != null) {
            return stream.filter(x -> predicate.test(
                function1.apply(x),
                function2.apply(x)
            ));
        } else {
            return stream;
        }
    }


    public static boolean implication(boolean a, boolean b) {
        return !a || b;
    }


    public static boolean xor(boolean a, boolean b) {
        return (a || b) && !(a && b);
    }


    public static Maybe<String> getOuterClassThisReference(
        Maybe<? extends EObject> input
    ) {
        input = SemanticsUtils.extractEObject(input);

        if (input.isNothing()) {
            return nothing();
        }

        EObject inputSafe = input.toNullable();
        NamedFeature memberContainer = EcoreUtil2.getContainerOfType(
            inputSafe,
            NamedFeature.class
        );
        if (memberContainer != null) {
            return some(memberContainer.getName() + "." + THIS);
        }

        FeatureContainer container = EcoreUtil2.getContainerOfType(
            inputSafe,
            FeatureContainer.class
        );
        if (container != null && !(container instanceof NamedFeature)) {
            return some(container.getName() + "." + THIS);
        } else {
            return some(THIS);
        }
    }


    public static String getSignature(String name, int arity) {
        return name + "(arity = " + arity + ")";
    }


    public static String getSignature(
        String name,
        List<IJadescriptType> paramTypes,
        List<String> paramNames
    ) {
        StringBuilder sb = new StringBuilder(name + "(");
        for (int i = 0; i < paramTypes.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(paramNames.get(i)).append(" as ");
            sb.append(paramTypes.get(i).getJadescriptName());
        }
        sb.append(")");
        return sb.toString();
    }


    public static String getSignature(
        String name,
        List<IJadescriptType> paramTypes
    ) {
        StringBuilder sb = new StringBuilder(name + "(");
        for (int i = 0; i < paramTypes.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(paramTypes.get(i).getJadescriptName());
        }
        sb.append(")");
        return sb.toString();
    }




    public static ITextRegionWithLineInformation getLocationForEObject(
        EObject inputSafe
    ) {
        return NodeModelUtils.getNode(inputSafe)
            .getTextRegionWithLineInformation();
    }




    public static int min(int... args) {
        int min = Integer.MAX_VALUE;
        for (int arg : args) {
            if (arg < min) {
                min = arg;
            }
        }
        return min;
    }


    @SafeVarargs
    public static <T> Stream<T> buildStream(Supplier<? extends T>... suppliers) {
        return Arrays.stream(suppliers)
            .map(Supplier::get);
    }


    /**
     * Intermediate operation on streams.
     * Please note that this may change semantics depending on the encounter
     * order (it uses a stateful map operation).
     * Internally, the stream is set as sequential for this reason.
     * It can be used to transform the elements of the stream using a
     * transformation ({@code mapResults}) that depends on a value that is
     * computed and accumulated at each iteration using {@code mapAccumulator}
     * and {@code reduceAccumulator}.
     *
     * @param stream             the input stream (which is going to be
     *                           terminated).
     * @param accumulatorInitial the initial value of the accumulator.
     * @param mapResults         function used to compute the results using
     *                           the accumulator and the input values.
     * @param mapAccumulator     function used to compute a value for to be
     *                           combined with the accumulator.
     * @param reduceAccumulator  operator that combines values to compute the
     *                           new accumulator value.
     * @param <T>                the type of the input elements
     * @param <A>                the type of the accumulator value
     * @param <R>                the type of the output elements
     * @return a stream of all the results given from {@code mapResults}
     */
    public static <T, A, R> Stream<R> accumulateAndMap(
        Stream<T> stream,
        A accumulatorInitial,
        BiFunction<T, A, R> mapResults,
        BiFunction<T, A, A> mapAccumulator,
        BinaryOperator<A> reduceAccumulator
    ) {
        AtomicReference<A> running = new AtomicReference<>(accumulatorInitial);
        return stream.sequential().map(t -> {
            final R result = mapResults.apply(t, running.get());
            running.set(reduceAccumulator.apply(
                running.get(),
                mapAccumulator.apply(t, running.get())
            ));
            return result;
        });
    }


    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> tuple(
        T1 _1,
        T2 _2,
        T3 _3,
        T4 _4
    ) {
        return new Tuple4<>(_1, _2, _3, _4);
    }


    public static <T1, T2, T3> Tuple3<T1, T2, T3> tuple(T1 _1, T2 _2, T3 _3) {
        return new Tuple3<>(_1, _2, _3);
    }


    public static <T1, T2> Tuple2<T1, T2> tuple(T1 _1, T2 _2) {
        return new Tuple2<>(_1, _2);
    }





    public static class Tuple2<T1, T2> {

        private final T1 _1;
        private final T2 _2;


        public Tuple2(T1 _1, T2 _2) {
            this._1 = _1;
            this._2 = _2;
        }


        public T1 get_1() {
            return _1;
        }


        public T2 get_2() {
            return _2;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Tuple2<?, ?> tuple2 = (Tuple2<?, ?>) o;

            if (get_1() != null
                ? !get_1().equals(tuple2.get_1())
                : tuple2.get_1() != null
            ) return false;
            return get_2() != null
                ? get_2().equals(tuple2.get_2())
                : tuple2.get_2() == null;
        }


        @Override
        public int hashCode() {
            int result = get_1() != null ? get_1().hashCode() : 0;
            result = 31 * result + (get_2() != null ? get_2().hashCode() : 0);
            return result;
        }


        @Override
        public String toString() {
            return "Tuple2[" + _1 + ", " + _2 + ']';
        }


        public <R1> Tuple2<R1, T2> mapLeft(
            Function<? super T1, ? extends R1> mapper
        ) {
            return new Tuple2<>(mapper.apply(_1), _2);
        }


        public <R2> Tuple2<T1, R2> mapRight(
            Function<? super T2, ? extends R2> mapper
        ) {
            return new Tuple2<>(_1, mapper.apply(_2));
        }


        public Tuple2<T2, T1> swap() {
            return new Tuple2<>(_2, _1);
        }

    }

    public static class Tuple3<T1, T2, T3> {

        private final T1 _1;
        private final T2 _2;
        private final T3 _3;


        public Tuple3(T1 _1, T2 _2, T3 _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }


        public T1 get_1() {
            return _1;
        }


        public T2 get_2() {
            return _2;
        }


        public T3 get_3() {
            return _3;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Tuple3<?, ?, ?> tuple3 = (Tuple3<?, ?, ?>) o;

            if (get_1() != null
                ? !get_1().equals(tuple3.get_1())
                : tuple3.get_1() != null
            ) {
                return false;
            }
            if (get_2() != null
                ? !get_2().equals(tuple3.get_2())
                : tuple3.get_2() != null
            ) {
                return false;
            }
            return get_3() != null
                ? get_3().equals(tuple3.get_3())
                : tuple3.get_3() == null;
        }


        @Override
        public int hashCode() {
            int result = get_1() != null ? get_1().hashCode() : 0;
            result = 31 * result + (get_2() != null ? get_2().hashCode() : 0);
            result = 31 * result + (get_3() != null ? get_3().hashCode() : 0);
            return result;
        }


        @Override
        public String toString() {
            return "Tuple3[" + _1 + ", " + _2 + ", " + _3 + ']';
        }

    }

    public static class Tuple4<T1, T2, T3, T4> {

        private final T1 _1;
        private final T2 _2;
        private final T3 _3;
        private final T4 _4;


        public Tuple4(T1 _1, T2 _2, T3 _3, T4 t4) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = t4;
        }


        public T1 get_1() {
            return _1;
        }


        public T2 get_2() {
            return _2;
        }


        public T3 get_3() {
            return _3;
        }


        public T4 get_4() {
            return _4;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Tuple4<?, ?, ?, ?> tuple4 = (Tuple4<?, ?, ?, ?>) o;

            if (get_1() != null
                ? !get_1().equals(tuple4.get_1())
                : tuple4.get_1() != null) {
                return false;
            }
            if (get_2() != null
                ? !get_2().equals(tuple4.get_2())
                : tuple4.get_2() != null) {
                return false;
            }
            if (get_3() != null
                ? !get_3().equals(tuple4.get_3())
                : tuple4.get_3() != null) {
                return false;
            }
            return get_4() != null
                ? get_4().equals(tuple4.get_4())
                : tuple4.get_4() == null;
        }


        @Override
        public int hashCode() {
            int result = get_1() != null ? get_1().hashCode() : 0;
            result = 31 * result + (get_2() != null ? get_2().hashCode() : 0);
            result = 31 * result + (get_3() != null ? get_3().hashCode() : 0);
            result = 31 * result + (get_4() != null ? get_4().hashCode() : 0);
            return result;
        }


        @Override
        public String toString() {
            return "Tuple4[" + _1 + ", " + _2 + ", " + _3 + ", " + _4 + ']';
        }

    }

}
