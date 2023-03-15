package jadescript.util;

import jade.content.Concept;

import java.util.*;

public class JadescriptSet<E> implements Collection<E>, Concept {
    private final List<E> elements;
    private final Set<E> hashSet = new HashSet<>();
    private boolean converted = false;

    public JadescriptSet() {
        //empty ctor for JADE
        this.elements = new ArrayList<>();
    }

    public JadescriptSet(Collection<? extends E> elements){
        this.elements = new ArrayList<>(elements);
    }


    public static <T> JadescriptSet<T> empty() {
        return new JadescriptSet<>();
    }

    public static <T> JadescriptSet<T> of(){
        return empty();
    }

    @SafeVarargs
    public static <T> JadescriptSet<T> of(T... elements){
        return fromArray(elements);
    }

    public static <T> JadescriptSet<T> fromArray(T[] elements){
        return new JadescriptSet<>(Arrays.asList(elements));
    }


    public List<E> getElements(){
        unzipIfNeeded();
        return elements;
    }

    public void setElements(List<E> elements) {
        unzipIfNeeded();
        this.elements.clear();
        this.elements.addAll(elements);
    }


    public int size() {
        zipIfNeeded();
        return hashSet.size();
    }


    public boolean isEmpty() {
        zipIfNeeded();
        return hashSet.isEmpty();
    }


    public boolean contains(Object o) {
        zipIfNeeded();
        return hashSet.contains(o);
    }


    public Iterator<E> iterator() {
        zipIfNeeded();
        return hashSet.iterator();
    }


    public Object[] toArray() {
        zipIfNeeded();
        return hashSet.toArray();
    }


    public <T1> T1[] toArray(T1[] a) {
        zipIfNeeded();

        return hashSet.toArray(a);
    }


    public boolean add(E e) {
        zipIfNeeded();
        return hashSet.add(e);
    }



    public boolean remove(Object o) {
        zipIfNeeded();
        return hashSet.remove(o);
    }


    public boolean containsAll(Collection<?> c) {
        zipIfNeeded();
        return hashSet.containsAll(c);
    }


    public boolean addAll(Collection<? extends E> c) {
        zipIfNeeded();
        return hashSet.addAll(c);
    }


    public boolean retainAll(Collection<?> c) {
        zipIfNeeded();
        return hashSet.retainAll(c);
    }


    public boolean removeAll(Collection<?> c) {
        zipIfNeeded();
        return hashSet.removeAll(c);
    }


    public void clear() {
        zipIfNeeded();
        hashSet.clear();
    }



    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        zipIfNeeded();
        boolean first = true;
        for(E value:hashSet) {
            if(first) {
                first = false;
            }else {
                sb.append(", ");
            }
            sb.append(quoteIfString(value));
        }
        sb.append("}");
        return sb.toString();
    }

    protected static String quoteIfString(Object x) {
        if(x instanceof String) {
            return "\"" + x + "\"";
        }else {
            return String.valueOf(x);
        }
    }

    private void zip() {
        hashSet.addAll(elements);
        elements.clear();
        converted = true;
    }

    private void unzip() {
        elements.addAll(hashSet);

        hashSet.clear();
        converted = false;
    }

    private void zipIfNeeded() {
        if(!converted) {
            zip();
        }
    }

    private void unzipIfNeeded() {
        if(converted) {
            unzip();
        }
    }
}
