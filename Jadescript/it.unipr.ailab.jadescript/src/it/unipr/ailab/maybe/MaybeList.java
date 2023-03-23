package it.unipr.ailab.maybe;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A representation of list of maybes designed with a more efficient use of
 * space resources. This is an immutable data structure.
 */
public class MaybeList<OfType> implements Iterable<Maybe<OfType>> {

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final MaybeList EMPTY = new MaybeList(List.of());


    @NotNull
    private final List<Object> wrappedList;


    @Contract(pure = true)
    private MaybeList(@NotNull Collection<?> source) {
        this.wrappedList = new ArrayList<>(source);
    }


    @SuppressWarnings("unchecked")
    private MaybeList(
        @NotNull List<?> source,
        boolean copyRef
    ) {
        if (copyRef) {
            this.wrappedList = (List<Object>) source;
        } else {
            this.wrappedList = new ArrayList<>(source);
        }
    }


    /**
     * Returns a (singleton) empty maybe-list.
     */
    @SuppressWarnings("unchecked")
    @Contract(pure = true)
    public static <T> @NotNull MaybeList<T> empty() {
        return EMPTY;
    }


    /**
     * Produces a maybe-list by wrapping a nullable. If {@code c} is null,
     * then a new empty maybe-list is returned.
     */
    @Contract("!null -> new")
    public static <T> MaybeList<T> someList(@Nullable Collection<T> c) {
        if (c == null) {
            return empty();
        }

        return new MaybeList<>(c);
    }


    public static <T> MaybeList<T> someListNullsRemoved(
        @Nullable Collection<T> c
    ){
        if(c == null){
            return empty();
        }

        final MaybeList<T> result = new MaybeList<>(c);
        result.removeNulls();
        return result;
    }

    /**
     * Produces a maybe-list by pushing down to the elements the maybe-ness of
     * the overall list. If {@code maybeAList} is nothing, the result is an
     * empty maybe-list.
     */
    public static <T> MaybeList<T> fromMaybeList(
        @NotNull
        Maybe<? extends List<T>> maybeAList
    ) {
        if (maybeAList.isNothing()) {
            return empty();
        }

        return new MaybeList<>(maybeAList.toNullable());
    }


    public static <T> MaybeList<T> fromMaybeListNullsRemoved(
        @NotNull
        Maybe<? extends List<T>> maybeAList
    ){
        if(maybeAList.isNothing()){
            return empty();
        }

        MaybeList<T> result = new MaybeList<>(maybeAList.toNullable());
        result.removeNulls();
        return result;
    }


    public static <T> MaybeList<T> fromListOfMaybesFinalize(
        @NotNull
        @Unmodifiable
        List<Maybe<T>> listOfMaybes
    ) {
        return new MaybeList<>(listOfMaybes, true);
    }


    public static <T> Collector<Maybe<T>, List<Maybe<T>>, MaybeList<T>>
    collectFromStreamOfMaybes() {
        return new Collector<>() {


            @Override
            public Supplier<List<Maybe<T>>> supplier() {
                return ArrayList::new;
            }


            @Override
            public BiConsumer<List<Maybe<T>>, Maybe<T>> accumulator() {
                return List::add;
            }


            @Override
            public BinaryOperator<List<Maybe<T>>> combiner() {
                return (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                };
            }


            @Override
            public Function<List<Maybe<T>>, MaybeList<T>> finisher() {
                return l -> new MaybeList<>(l, true);
            }


            @Override
            public Set<Characteristics> characteristics() {
                return Set.of();
            }
        };
    }


    public static <T> Collector<T, List<T>, MaybeList<T>>
    collectFromStreamOfNullables() {
        return new Collector<>() {


            @Override
            public Supplier<List<T>> supplier() {
                return ArrayList::new;
            }


            @Override
            public BiConsumer<List<T>, T> accumulator() {
                return List::add;
            }


            @Override
            public BinaryOperator<List<T>> combiner() {
                return (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                };
            }


            @Override
            public Function<List<T>, MaybeList<T>> finisher() {
                return l -> new MaybeList<>(l, true);
            }


            @Override
            public Set<Characteristics> characteristics() {
                return Set.of();
            }
        };
    }

    private void removeNulls(){
        this.wrappedList.removeIf(Objects::isNull);
    }


    private boolean isWrapped(int i) {
        if (i < 0 || i > this.wrappedList.size() - 1) {
            // Elements out of bounds are assumed to be Maybe.nothing()
            return true;
        }

        Object x = this.wrappedList.get(i);

        return x instanceof Maybe;
    }


    @SuppressWarnings("unchecked")
    private Maybe<OfType> wrapGet(int i) {
        if (i < 0 || i >= this.wrappedList.size()) {
            return Maybe.nothing();
        }

        if (this.isWrapped(i)) {
            return (Maybe<OfType>) this.wrappedList.get(i);
        }

        Object x = this.wrappedList.get(i);
        Maybe<OfType> result;

        try {
            OfType xCasted = (OfType) x;
            result = Maybe.some(xCasted);
        } catch (ClassCastException ignored) {
            result = Maybe.nothing();
        }

        this.wrappedList.set(i, result);

        return result;
    }


    private void wrapAll() {
        for (int i = 0; i < this.size(); i++) {
            wrapGet(i);
        }
    }


    public int size() {
        return wrappedList.size();
    }


    public boolean isEmpty() {
        return wrappedList.isEmpty();
    }


    /**
     * Checks that the list contains the value wrapped by {@code m}.
     * If {@code m} is nothing, then this returns true.
     *
     * @see MaybeList#containsOrFalse(Maybe)
     */
    public boolean containsOrTrue(Maybe<OfType> m) {
        if (m.isNothing()) {
            return true;
        }

        return this.contains(m.toNullable());
    }


    /**
     * Checks that the list contains the value wrapped by {@code m}.
     * If {@code m} is nothing, then this returns false.
     *
     * @see MaybeList#containsOrTrue(Maybe)
     */
    public boolean containsOrFalse(Maybe<OfType> m) {
        if (m.isNothing()) {
            return false;
        }

        return this.contains(m.toNullable());
    }


    public boolean contains(Object o) {
        return wrappedList.stream().anyMatch(e -> {
            if (e instanceof Maybe) {
                return ((Maybe<?>) e).wrappedEquals(o);
            }

            return Objects.equals(e, o);
        });
    }


    @NotNull
    @Override
    public Iterator<Maybe<OfType>> iterator() {
        return new Iterator<>() {
            private int i = 0;


            @Override
            public boolean hasNext() {
                return this.i < wrappedList.size();
            }


            @Override
            public Maybe<OfType> next() {
                final Maybe<OfType> result = wrapGet(i);
                i++;
                return result;
            }
        };
    }


    public boolean addSome(OfType someElement) {
        // Wrapped only if re-extracted back
        return wrappedList.add(someElement);
    }


    public boolean addNothing() {
        return wrappedList.add(Maybe.nothing());
    }


    public boolean addSeveral(Collection<?> c) {
        return wrappedList.addAll(c);
    }


    public boolean addSeveral(int index, Collection<?> c) {
        return wrappedList.addAll(index, c);
    }


    @NotNull
    public Iterator<OfType> nonNullIterator() {
        return new Iterator<>() {
            private int i = 0;

            @Nullable
            private OfType extractedValue = null;


            @Override
            public boolean hasNext() {
                if (this.extractedValue != null) {
                    return true;
                }

                while (this.i < wrappedList.size()) {
                    Object o = wrappedList.get(i);
                    i++;

                    if (o instanceof Maybe) {
                        o = ((Maybe<?>) o).toNullable();
                    }

                    try {
                        //noinspection unchecked
                        this.extractedValue = (OfType) o;

                        if (this.extractedValue != null) {
                            return true;
                        }
                    } catch (ClassCastException ignored) {
                    }
                }

                return false;
            }


            @Override
            public OfType next() {
                final OfType result = this.extractedValue;
                if (result == null) {
                    throw new NoSuchElementException();
                }
                this.extractedValue = null;
                return result;
            }
        };
    }


    @Override
    public void forEach(Consumer<? super Maybe<OfType>> action) {
        for (int i = 0; i < this.wrappedList.size(); i++) {
            action.accept(wrapGet(i));
        }
    }


    public Maybe<OfType> get(int index) {
        return wrapGet(index);
    }


    public Object setSome(int index, Object element) {
        return this.wrappedList.set(index, element);
    }


    public void removeNoWrap(int index) {
        this.wrappedList.remove(index);
    }


    @NotNull
    public MaybeList<OfType> subList(int fromIndex, int toIndex) {
        return new MaybeList<>(
            this.wrappedList.subList(fromIndex, toIndex),
            true
        );

    }



    // TODO analyze usages
    public Stream<Maybe<OfType>> stream() {
        return IntStream.range(0, wrappedList.size())
            .mapToObj(this::wrapGet);
    }


    public List<Maybe<OfType>> toListOfMaybes() {
        return stream().collect(Collectors.toList());
    }

}
