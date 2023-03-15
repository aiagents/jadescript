package jadescript.util;

import jade.content.Concept;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JadescriptList<E> implements Collection<E>, Concept {

    private final List<E> list;


    public JadescriptList() {
        //empty ctor for JADE
        this.list = new ArrayList<>();
    }


    public JadescriptList(Collection<? extends E> c) {
        this.list = new ArrayList<>(c);
    }


    public JadescriptList(
        Collection<? extends E> c,
        Collection<? extends E> rest
    ) {
        this.list = new ArrayList<>(c);
        this.list.addAll(rest);
    }



    public static <T> JadescriptList<T> empty() {
        return new JadescriptList<>();
    }

    public static <T> JadescriptList<T> of(){
        return empty();
    }

    @SafeVarargs
    public static <T> JadescriptList<T> of(T... elements){
        return fromArray(elements);
    }

    public static <T> JadescriptList<T> fromArray(T[] elements){
        return new JadescriptList<>(Arrays.asList(elements));
    }


    public List<E> getElements() {
        return list;
    }


    public void setElements(List<E> elements) {
        this.list.clear();
        this.list.addAll(elements);
    }


    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }


    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }


    @Override
    public Object[] toArray() {
        return list.toArray();
    }


    @Override
    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }


    @Override
    public boolean add(E e) {
        return list.add(e);
    }


    @Override
    public boolean remove(Object o) {
        return list.remove(o);
    }


    @Override
    public boolean containsAll(Collection<?> c) {
        return new HashSet<>(list).containsAll(c);
    }


    @Override
    public boolean addAll(Collection<? extends E> c) {
        return list.addAll(c);
    }



    public boolean addAll(int index, Collection<? extends E> c) {
        return list.addAll(index, c);
    }


    @Override
    public boolean removeAll(Collection<?> c) {
        return list.removeAll(c);
    }


    @Override
    public boolean retainAll(Collection<?> c) {
        return list.retainAll(c);
    }



    public void replaceAll(UnaryOperator<E> operator) {
        list.replaceAll(operator);
    }



    public void sort(Comparator<? super E> c) {
        list.sort(c);
    }


    @Override
    public void clear() {
        list.clear();
    }


    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return list.equals(o);
    }


    @Override
    public int hashCode() {
        return list.hashCode();
    }



    public E get(int index) {
        return list.get(index);
    }



    public E set(int index, E element) {
        return list.set(index, element);
    }



    public void add(int index, E element) {
        list.add(index, element);
    }



    public E remove(int index) {
        return list.remove(index);
    }



    public int indexOf(Object o) {
        //noinspection SuspiciousMethodCalls
        return list.indexOf(o);
    }



    public int lastIndexOf(Object o) {
        //noinspection SuspiciousMethodCalls
        return list.lastIndexOf(o);
    }



    public ListIterator<E> listIterator() {
        return list.listIterator();
    }



    public ListIterator<E> listIterator(int index) {
        return list.listIterator(index);
    }



    public List<E> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }


    @Override
    public Spliterator<E> spliterator() {
        return list.spliterator();
    }


    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        return list.toArray(generator);
    }


    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return list.removeIf(filter);
    }


    @Override
    public Stream<E> stream() {
        return list.stream();
    }


    @Override
    public Stream<E> parallelStream() {
        return list.parallelStream();
    }


    @Override
    public void forEach(Consumer<? super E> action) {
        list.forEach(action);
    }


    @Override
    public int size() {
        return list.size();
    }


    @Override
    public String toString() {
        return "[" +
            this.list.stream()
                .map(Util::quoteIfString)
                .collect(Collectors.joining(", "))
            + "]";
    }


    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    //TODO jadescript's index out of bounds exception
}
