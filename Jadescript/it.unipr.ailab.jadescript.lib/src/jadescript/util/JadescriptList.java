package jadescript.util;

import jade.content.Concept;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class JadescriptList<E> implements List<E>, Concept {

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
        return list.containsAll(c);
    }


    @Override
    public boolean addAll(Collection<? extends E> c) {
        return list.addAll(c);
    }


    @Override
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


    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        list.replaceAll(operator);
    }


    @Override
    public void sort(Comparator<? super E> c) {
        list.sort(c);
    }


    @Override
    public void clear() {
        list.clear();
    }


    @Override
    public boolean equals(Object o) {
        return list.equals(o);
    }


    @Override
    public int hashCode() {
        return list.hashCode();
    }


    @Override
    public E get(int index) {
        return list.get(index);
    }


    @Override
    public E set(int index, E element) {
        return list.set(index, element);
    }


    @Override
    public void add(int index, E element) {
        list.add(index, element);
    }


    @Override
    public E remove(int index) {
        return list.remove(index);
    }


    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }


    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }


    @Override
    public ListIterator<E> listIterator() {
        return list.listIterator();
    }


    @Override
    public ListIterator<E> listIterator(int index) {
        return list.listIterator(index);
    }


    @Override
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


    public List<E> getElements() {
        return list;
    }


    public void setElements(List<E> elements) {
        this.list.clear();
        this.list.addAll(elements);
    }


    @Override
    public int size() {
        return list.size();
    }


    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }


}
