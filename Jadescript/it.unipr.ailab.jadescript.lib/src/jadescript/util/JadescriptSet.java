package jadescript.util;

import jade.content.Concept;

import java.util.*;

public class JadescriptSet<T> implements Set<T>, Concept {
    private final List<T> elements = new ArrayList<>();
    private final Set<T> hashSet = new HashSet<>();
    private boolean converted = false;

    public JadescriptSet() {
        //empty ctor for jade
    }


    public List<T> getElements(){
        unzipIfNeeded();
        return elements;
    }

    /**
     * NOTE: this should be used only in conjunction with setValues by the
     * ContentManager extractor.
     */
    public void setElements(List<T> elements) {
        unzipIfNeeded();
        this.elements.clear();
        this.elements.addAll(elements);
    }

    @Override
    public int size() {
        zipIfNeeded();
        return hashSet.size();
    }

    @Override
    public boolean isEmpty() {
        zipIfNeeded();
        return hashSet.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        zipIfNeeded();
        return hashSet.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        zipIfNeeded();
        return hashSet.iterator();
    }

    @Override
    public Object[] toArray() {
        zipIfNeeded();
        return hashSet.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        zipIfNeeded();
        //noinspection SuspiciousToArrayCall
        return hashSet.toArray(a);
    }

    @Override
    public boolean add(T t) {
        zipIfNeeded();
        return hashSet.add(t);
    }


    @Override
    public boolean remove(Object o) {
        zipIfNeeded();
        return hashSet.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        zipIfNeeded();
        return hashSet.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        zipIfNeeded();
        return hashSet.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        zipIfNeeded();
        return hashSet.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        zipIfNeeded();
        return hashSet.removeAll(c);
    }

    @Override
    public void clear() {
        zipIfNeeded();
        hashSet.clear();
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        zipIfNeeded();
        boolean first = true;
        for(T value:hashSet) {
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
