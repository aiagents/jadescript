package it.unipr.ailab.jadescript.semantics.utils;

import it.unipr.ailab.jadescript.jadescript.FeatureContainer;
import it.unipr.ailab.jadescript.jadescript.NamedFeature;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.util.ITextRegionWithLineInformation;

import java.util.HashSet;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.of;

public class Util implements SemanticsConsts {
    private Util() {
    } // do not instantiate

    public static <T, X> java.util.function.Predicate<T> dinstinctBy(Function<? super T, ? extends X> extractor) {
        HashSet<X> hashFlag = new HashSet<>();
        return t -> hashFlag.add(extractor.apply(t));
    }


    public static <T> java.util.function.Predicate<T> truePredicate() {
        return (__) -> true;
    }

    public static <T> java.util.function.Predicate<T> falsePredicate() {
        return (__) -> false;
    }


    @SuppressWarnings("unused")
	private <T> Stream<T> safeFilter(Stream<T> stream, Predicate<T> predicate) {
        if (predicate != null) {
            return stream.filter(predicate);
        } else {
            return stream;
        }
    }

    public static <T1, T2> Stream<T1> safeFilter(Stream<T1> stream, Function<T1, T2> function, Predicate<T2> predicate) {
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
            return stream.filter(x -> predicate.test(function1.apply(x), function2.apply(x)));
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

    public static Maybe<String> getOuterClassThisReference(Maybe<? extends EObject> input) {
        if (input.isNothing()) {
            return nothing();
        }
        EObject inputSafe = input.toNullable();
        NamedFeature memberContainer = EcoreUtil2.getContainerOfType(inputSafe, NamedFeature.class);
        if (memberContainer != null) {
            return of(memberContainer.getName() + "." + THIS);
        }

        FeatureContainer container = EcoreUtil2.getContainerOfType(inputSafe, FeatureContainer.class);
        if (container != null && !(container instanceof NamedFeature)) {
            return of(container.getName() + "." + THIS);
        } else {
            return of(THIS);
        }
    }


    public static String getSignature(String name, int arity) {
        return name + "(arity = " + arity + ")";
    }

    public static String getSignature(String name, List<IJadescriptType> paramTypes, List<String> paramNames) {
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


    public static ITextRegionWithLineInformation getLocationForEObject(EObject inputSafe) {
        return NodeModelUtils.getNode(inputSafe).getTextRegionWithLineInformation();
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

            if (get_1() != null ? !get_1().equals(tuple2.get_1()) : tuple2.get_1() != null) return false;
            return get_2() != null ? get_2().equals(tuple2.get_2()) : tuple2.get_2() == null;
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

        public <R1> Tuple2<R1, T2> mapLeft(Function<? super T1, ? extends R1> mapper){
            return new Tuple2<>(mapper.apply(_1), _2);
        }

        public <R2> Tuple2<T1, R2> mapRight(Function<? super T2, ? extends R2> mapper){
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

            if (get_1() != null ? !get_1().equals(tuple3.get_1()) : tuple3.get_1() != null) return false;
            if (get_2() != null ? !get_2().equals(tuple3.get_2()) : tuple3.get_2() != null) return false;
            return get_3() != null ? get_3().equals(tuple3.get_3()) : tuple3.get_3() == null;
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
            return "Tuple3[" +
                    "" + _1 +
                    ", " + _2 +
                    ", " + _3 +
                    ']';
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

            if (get_1() != null ? !get_1().equals(tuple4.get_1()) : tuple4.get_1() != null) return false;
            if (get_2() != null ? !get_2().equals(tuple4.get_2()) : tuple4.get_2() != null) return false;
            if (get_3() != null ? !get_3().equals(tuple4.get_3()) : tuple4.get_3() != null) return false;
            return get_4() != null ? get_4().equals(tuple4.get_4()) : tuple4.get_4() == null;
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
            return "Tuple4[" +
                    "" + _1 +
                    ", " + _2 +
                    ", " + _3 +
                    ", " + _4 +
                    ']';
        }
    }

    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> tuple(T1 _1, T2 _2, T3 _3, T4 _4) {
        return new Tuple4<>(_1, _2, _3, _4);
    }


    public static <T1, T2, T3> Tuple3<T1, T2, T3> tuple(T1 _1, T2 _2, T3 _3) {
        return new Tuple3<>(_1, _2, _3);
    }

    public static <T1, T2> Tuple2<T1, T2> tuple(T1 _1, T2 _2) {
        return new Tuple2<>(_1, _2);
    }
}
