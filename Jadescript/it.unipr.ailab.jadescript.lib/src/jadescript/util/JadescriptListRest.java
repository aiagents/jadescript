package jadescript.util;

import java.util.*;

public class JadescriptListRest<T> implements List<T> {
    private final List<T> originalList;
    private final int headSkips;


    public JadescriptListRest(List<T> originalList, int headSkips) {
        this.originalList = originalList;
        this.headSkips = headSkips;
    }


    @Override
    public int size() {
        return Math.max(originalList.size() - headSkips, 0);
    }

    @Override
    public boolean isEmpty() {
        return size() > 0;
    }

    @SuppressWarnings("unchecked")
	@Override
    public boolean contains(Object o) {
        //noinspection unchecked
        return originalList.contains(o) && originalList.lastIndexOf((T)o) >= headSkips;
    }

    @Override
    public Iterator<T> iterator() {
        Iterator<T> iterator = originalList.iterator();
        for (int i = 0; i < headSkips && iterator.hasNext(); i++) {
            iterator.next();
        }
        return iterator;
    }

    @Override
    public Object[] toArray() {
        Object[] arr = new Object[size()];
        for (int i = headSkips; i < originalList.size(); i++) {
            arr[i - headSkips] = originalList.get(i);
        }
        return arr;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T1> T1[] toArray(T1[] a) {
        if (a.length < size()) {
            // Make a new array of a's runtime type, but my contents:
            return (T1[]) Arrays.copyOf(toArray(), size(), a.getClass());
        }

        //noinspection SuspiciousSystemArraycopy
        System.arraycopy(toArray(), 0, a, 0, size());
        if (a.length > size())
            a[size()] = null;
        return a;
    }

    @Override
    public boolean add(T t) {
        return originalList.add(t);
    }

    @Override
    public String toString() {
        return originalList.subList(headSkips, originalList.size()).toString();
    }

    @Override
    public Spliterator<T> spliterator() {
        return originalList.subList(headSkips, originalList.size()).spliterator();
    }

    @Override
    public boolean remove(Object o) {
        int toRemove = -1;
        for (int i = headSkips; i < originalList.size(); i++) {
            if (originalList.get(i).equals(o)) {
                toRemove = i;
                break;
            }
        }
        if (toRemove >= 0) {
            originalList.remove(toRemove);
            return true;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.stream().allMatch(this::contains);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return originalList.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return originalList.addAll(index + headSkips, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = false;
        for (Object o : c) {
            result = result || remove(o);
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        List<Integer> removeIndexes = new ArrayList<>();
        for (int i = headSkips; i < originalList.size(); i++) {
            if (!c.contains(originalList.get(i))) {
                removeIndexes.add(i);
            }
        }
        if (!removeIndexes.isEmpty()) {
            for (Integer removeIndex : removeIndexes) {
                originalList.remove((int) removeIndex);
            }
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        while (originalList.size() > headSkips) {
            originalList.remove(originalList.size() - 1);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof List) {
            return originalList.subList(headSkips, originalList.size()).equals(o);
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return originalList.subList(headSkips, originalList.size()).hashCode();
    }

    @Override
    public T get(int index) {
        return originalList.get(index + headSkips);
    }

    @Override
    public T set(int index, T element) {
        return originalList.set(index + headSkips, element);
    }

    @Override
    public void add(int index, T element) {
        originalList.add(index + headSkips, element);
    }

    @Override
    public T remove(int index) {
        return originalList.remove(index + headSkips);
    }

    @Override
    public int indexOf(Object o) {
        return Math.max(-1, originalList.indexOf(o) - headSkips);
    }

    @Override
    public int lastIndexOf(Object o) {
        return Math.max(-1, originalList.lastIndexOf(o) - headSkips);
    }

    @Override
    public ListIterator<T> listIterator() {
        return originalList.listIterator(headSkips);
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return originalList.listIterator(headSkips+index);
    }


    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return originalList.subList(fromIndex + headSkips, toIndex + headSkips);
    }

    public List<T> toNew(){
        return new ArrayList<>(this);
    }
}
