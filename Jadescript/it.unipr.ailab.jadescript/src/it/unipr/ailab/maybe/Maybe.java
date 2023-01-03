package it.unipr.ailab.maybe;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created on 2019-06-10.
 */
public class Maybe<OfType> {

    private final OfType o;
    private static final Maybe<?> EMPTY = new Maybe<>(null);

    private Maybe(OfType o) {
        this.o = o;
    }


    public static <T> Maybe<T> some(T x) {
        if (x == null) {
            return nothing();
        }
        return new Maybe<>(x);
    }

    public static <T> Maybe<T> nothing() {
        @SuppressWarnings("unchecked")
        Maybe<T> t = (Maybe<T>) EMPTY;
        return t;
    }

    public static <T> Maybe<T> fromOpt(
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<T> x
    ) {
        return x.map(Maybe::some).orElseGet(Maybe::nothing);
    }

    public static <T1, T2 extends List<T1>> List<T1> nullAsEmptyList(Maybe<T2> maybeList) {
        if (maybeList.isPresent()) {
            return maybeList.o;
        }
        return Collections.emptyList();
    }

    public static <T1, T2 extends Iterable<T1>> Iterable<T1> nullAsEmpty(Maybe<T2> maybeCollection) {
        if (maybeCollection.isPresent()) {
            return maybeCollection.o;
        }

        return Collections.emptyList();
    }

    public static <T> Maybe<T> paddedGet(List<Maybe<T>> maybeList, int index) {
        if (index < 0 || index >= maybeList.size()) {
            return nothing();
        } else {
            return maybeList.get(index);
        }
    }

    public static <T> void paddedSet(List<Maybe<T>> maybeList, int index, Maybe<T> element) {
        if (index >= maybeList.size()) {
            while (maybeList.size() <= index) {
                maybeList.add(nothing());
            }
        }
        maybeList.set(index, element);
    }

    public static Boolean nullAsFalse(Maybe<Boolean> maybeABoolean) {
        if (maybeABoolean.isPresent()) {
            return maybeABoolean.o;
        }

        return Boolean.FALSE;
    }

    public static final Function<Maybe<Boolean>, Boolean> nullAsFalse = Maybe::nullAsFalse;


    public static Boolean nullAsTrue(Maybe<Boolean> maybeABoolean) {
        if (maybeABoolean.isPresent()) {
            return maybeABoolean.o;
        }

        return Boolean.TRUE;
    }

    public static final Function<Maybe<Boolean>, Boolean> nullAsTrue = Maybe::nullAsTrue;


    public static String nullAsEmptyString(Maybe<String> maybeAString) {
        if (maybeAString.isPresent()) {
            return maybeAString.o;
        }

        return "";
    }

    public static Function<Maybe<String>, String> nullAsDefaultString(String defaultValue) {
        return ms -> {
            if (ms.isPresent()) {
                return ms.o;
            }
            return defaultValue;
        };
    }

    public static final Function<Maybe<String>, String> nullAsEmptyString = Maybe::nullAsEmptyString;

    public static final Function<Boolean, Boolean> not = (b) -> !b;

    public static <T> Maybe<T> flatten(Maybe<Maybe<T>> input) {
        if (input.isPresent()) {
            return input.o;
        }

        return nothing();
    }

    public static <T1> Iterable<Maybe<T1>> iterate(Maybe<? extends Iterable<T1>> maybeCollection) {
        Iterable<T1> collection = nullAsEmpty(maybeCollection);
        Iterator<T1> iterator = collection.iterator();
        return () -> new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Maybe<T1> next() {
                return some(iterator.next());
            }
        };
    }

    public static <T> List<Maybe<T>> toListOfMaybes(Maybe<? extends List<T>> maybeAList) {
        if (maybeAList.isPresent()) {
            return maybeAList.o.stream()
                    .map(Maybe::some).collect(Collectors.toCollection(ArrayList::new));
        } else {
            return Collections.emptyList();
        }
    }

    public static <T> List<Maybe<T>> toListOfMaybes(List<T> list) {
        return list.stream()
                .map(Maybe::some)
                .collect(Collectors.toList());
    }

    public static <T1> Stream<Maybe<T1>> stream(Maybe<? extends Iterable<T1>> maybeCollection) {
        Spliterator<Maybe<T1>> spliterator = iterate(maybeCollection).spliterator();
        return StreamSupport.stream(spliterator, false);
    }

    @SafeVarargs
    public static Maybe<String> concat(Maybe<String>... x) {
        String result = "";
        for (Maybe<String> stringMaybe : x) {
            if (stringMaybe.isPresent()) {
                result += stringMaybe.o;
            } else {
                return nothing();
            }
        }
        return some(result);
    }

    public static <T1, T2> void eitherDo(
            Maybe<T1> j1,
            Maybe<T2> j2,
            Consumer<? super T1> c1,
            Consumer<? super T2> c2
    ) {
        if (j1.isPresent()) {
            c1.accept(j1.toNullable());
        } else if (j2.isPresent()) {
            c2.accept(j2.toNullable());
        }
    }

    public static <T1, T2, R> Maybe<R> eitherCall(
            Maybe<T1> j1, Maybe<T2> j2,
            Function<? super T1, ? extends R> c1,
            Function<? super T2, ? extends R> c2
    ) {
        if (j1.isPresent()) {
            return Maybe.some(c1.apply(j1.toNullable()));
        } else if (j2.isPresent()) {
            return Maybe.some(c2.apply(j2.toNullable()));
        } else {
            return nothing();
        }
    }

    public static <T> Maybe<? extends T> eitherGet(Maybe<? extends T> j1, Maybe<? extends T> j2) {
        return eitherCall(j1, j2, j -> j, j -> j);
    }


    public static <T1, T2> void safeDo(
            Maybe<T1> j1,
            Maybe<T2> j2,
            BiConsumer<T1, T2> consumer
    ) {
        j1.safeDo(j1safe ->
                j2.safeDo(j2safe -> {
                    consumer.accept(j1safe, j2safe);
                }));
    }

    public static <T1, T2, T3> void safeDo(
            Maybe<T1> j1,
            Maybe<T2> j2,
            Maybe<T3> j3,
            Functional.TriConsumer<T1, T2, T3> consumer
    ) {
        j1.safeDo(j1safe ->
                j2.safeDo(j2safe ->
                        j3.safeDo(j3safe ->
                                consumer.accept(j1safe, j2safe, j3safe)
                        )));
    }

    public static <T1, T2, T3, T4> void safeDo(
            Maybe<T1> j1,
            Maybe<T2> j2,
            Maybe<T3> j3,
            Maybe<T4> j4,
            Functional.QuadConsumer<T1, T2, T3, T4> consumer
    ) {
        j1.safeDo(j1safe ->
                j2.safeDo(j2safe ->
                        j3.safeDo(j3safe ->
                                j4.safeDo(j4safe ->
                                        consumer.accept(j1safe, j2safe, j3safe, j4safe)
                                ))));
    }


    public static <T> Stream<T> filterNulls(Maybe<T> maybe) {
        if (!maybe.isPresent()) {
            return Stream.empty();
        } else {
            return Stream.of(maybe.o);
        }
    }


    public boolean isPresent() {
        return o != null;
    }

    public <OfType2> Maybe<OfType2> __(
            Function<? super OfType, ? extends OfType2> function,
            Supplier<? extends OfType2> nullPolicy
    ) {
        Objects.requireNonNull(nullPolicy);
        Objects.requireNonNull(function);
        if (isPresent()) {
            return some(function.apply(o));
        } else {
            return some(nullPolicy.get());
        }
    }

    /**
     * Maybe's equivalent of {@link Optional#map(Function)}.
     * Named __ because while identifying the operation done, the focus of the programmer should be in the argument
     * of this method.
     */
    public <OfType2> Maybe<OfType2> __(
            Function<? super OfType, ? extends OfType2> function
    ) {
        Objects.requireNonNull(function);
        if (isPresent()) {
            return some(function.apply(o));
        } else {
            return nothing();
        }
    }


    public <OfType2, ParType> Maybe<OfType2> __(
            BiFunction<? super OfType, ? super ParType, ? extends OfType2> function,
            ParType arg1,
            Supplier<? extends OfType2> nullPolicy
    ) {
        Objects.requireNonNull(nullPolicy);
        if (isPresent()) {
            return some(function.apply(o, arg1));
        } else {
            return some(nullPolicy.get());
        }
    }

    public <OfType2, ParType> Maybe<OfType2> __(
            BiFunction<? super OfType, ? super ParType, ? extends OfType2> function,
            ParType arg1
    ) {
        if (isPresent()) {
            return some(function.apply(o, arg1));
        } else {
            return nothing();
        }
    }

    /**
     * Runs a consumer on the wrapped object, if present; otherwise, no action is taken.
     */
    public void safeDo(
            Consumer<? super OfType> function
    ) {
        if (isPresent()) {
            function.accept(o);
        }
    }

    /**
     * Runs a consumer on the wrapped object, if present; otherwise, applies the {@code nullPolicy}.
     */
    public void safeDo(
            Consumer<? super OfType> function,
            Runnable nullPolicy
    ) {
        Objects.requireNonNull(nullPolicy);
        if (isPresent()) {
            function.accept(o);
        } else {
            nullPolicy.run();
        }
    }


    /**
     * Runs a BiConsumer on the wrapped object and the passed {@code arg}, only if the
     * wrapped object is present; otherwise, applies the {@code nullPolicy}.
     */
    public <ParType> void safeDo(
            BiConsumer<OfType, ParType> function,
            ParType arg1,
            Runnable nullPolicy
    ) {
        Objects.requireNonNull(nullPolicy);
        if (isPresent()) {
            function.accept(o, arg1);
        } else {
            nullPolicy.run();
        }
    }


    /**
     * Runs a BiConsumer on the wrapped object and the passed {@code arg}, only if the
     * wrapped object is present; otherwise, no action is taken.
     */
    public <ParType> void safeDo(
            BiConsumer<OfType, ParType> function,
            ParType arg1
    ) {
        if (isPresent()) {
            function.accept(o, arg1);
        }
    }

    public List<OfType> toList() {
        return isPresent()
                ? Collections.singletonList(o)
                : Collections.emptyList();
    }


    /**
     * Returns the result of invocation of the {@link Object#equals(Object)}
     * method on the wrapped object, if present; otherwise, it returns true if
     * {@code o2} is null.
     */
    public boolean wrappedEquals(Object o2) {
        if (isPresent()) {
            return o.equals(o2);
        } else {
            return o2 == null;
        }
    }

    public static <R, T extends R> Maybe<R> wrappedSuperCast(Maybe<T> input) {
        return some(input.toNullable());
    }

    @SuppressWarnings("unchecked")
    public static <T, R extends T> Maybe<R> wrappedSubCast(Maybe<T> input) {
        //noinspection unchecked
        return some((R) input.toNullable());
    }

    @SuppressWarnings("unchecked")
    public <R extends OfType> Maybe<R> wrappedSubCast(){
        return some((R) this.toNullable());
    }


    public boolean isInstanceOf(Class<?> c) {
        Objects.requireNonNull(c);
        if (isPresent()) {
            return c.isInstance(o);
        } else {
            return false;
        }
    }


    /**
     * Returns a {@link java.util.Optional} with the wrapped object/null reference.
     */
    public Optional<OfType> toOpt() {
        return Optional.ofNullable(o);
    }

    /**
     * Simply returns the wrapped reference, <br>which can be null</br>.
     */
    public OfType toNullable() {
        return o;
    }

    /**
     * Returns the wrapped value if present; otherwise, it returns the value passed
     * as {@code defaulT}.
     */
    public OfType orElse(OfType defaulT) {
        Objects.requireNonNull(defaulT);
        if (isPresent()) {
            return o;
        } else {
            return defaulT;
        }
    }

    public OfType orElseGet(Supplier<? extends OfType> supplier) {
        Objects.requireNonNull(supplier);
        if (isPresent()) {
            return o;
        } else {
            return supplier.get();
        }
    }

    public <U> Maybe<U> flatMap(
            Function<? super OfType, ? extends Maybe<? extends U>> function
    ) {
        Objects.requireNonNull(function);
        if (!isPresent()) {
            return nothing();
        } else {
            @SuppressWarnings("unchecked")
            Maybe<U> r = (Maybe<U>) function.apply(o);
            return Objects.requireNonNull(r);
        }
    }


    public <T2> T2 extract(Function<Maybe<OfType>, T2> extractor) {
        return extractor.apply(this);
    }

    public <T2> T2 extractOrElse(Function<Maybe<OfType>, T2> extractor, T2 defaulT) {
        if (isPresent()) {
            return extractor.apply(this);
        } else {
            return defaulT;
        }
    }

    public Maybe<OfType> or(Maybe<OfType> alternative) {
        Objects.requireNonNull(alternative);
        if (isPresent()) {
            return this;
        } else {
            return alternative;
        }
    }

    public Maybe<OfType> or(Supplier<? extends Maybe<? extends OfType>> supplier) {
        Objects.requireNonNull(supplier);
        if (isPresent()) {
            return this;
        } else {
            @SuppressWarnings("unchecked")
            Maybe<OfType> r = (Maybe<OfType>) supplier.get();
            return Objects.requireNonNull(r);
        }
    }

    public Maybe<OfType> nullIf(Predicate<OfType> predicate) {
        if (isNothing()) return this;
        if (predicate.test(o)) {
            return nothing();
        } else {
            return this;
        }
    }

    public Maybe<OfType> require(Predicate<OfType> predicate) {
        if (isNothing()) return this;
        if (predicate.test(o)) {
            return this;
        } else {
            return nothing();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Maybe)) {
            return false;
        }

        return Objects.equals(o, ((Maybe<?>) obj).o);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(o);
    }

    @Override
    public String toString() {
        if (isPresent()) {
            return o.toString();
        } else {
            return "[<missing value>]";
        }
    }

    public boolean isNothing() {
        return !isPresent();
    }

    public Stream<OfType> stream() {
        if (isPresent()) {
            return Stream.of(o);
        } else {
            return Stream.empty();
        }
    }
}
