package jadescript.util;

import java.util.*;

public class JadescriptSetRest<T> extends JadescriptSet<T> {
    private final JadescriptSet<T> originalSet;
    private final HashSet<T> excludedValues;
    private JadescriptSet<T> cachedSubtraction = null;


    @SafeVarargs
    public JadescriptSetRest(
        JadescriptSet<T> originalSet,
        T... excludedValues
    ) {
        super();
        this.originalSet = originalSet;
        this.excludedValues = new HashSet<>(Arrays.asList(excludedValues));
    }


    private JadescriptSet<T> getSubtraction() {
        if (cachedSubtraction == null) {
            cachedSubtraction = new JadescriptSet<>();
            cachedSubtraction.addAll(originalSet);
            cachedSubtraction.removeAll(excludedValues);
        }
        return cachedSubtraction;
    }

    @Override
    public List<T> getElements() {
        List<T> result = new ArrayList<>();
        for (T t : originalSet) {
            if (!excludedValues.contains(t)) {
                result.add(t);
            }
        }
        return result;
    }

    @Override
    public void setElements(List<T> elements) {
        originalSet.clear();
        originalSet.addAll(excludedValues);
        originalSet.addAll(elements);
        cachedSubtraction = null;
    }

    @Override
    public int size() {
        return Math.max(0, originalSet.size() - excludedValues.size());
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        return originalSet.contains(o) && !excludedValues.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return getSubtraction().iterator();
    }

    @Override
    public Object[] toArray() {
        return getSubtraction().toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return getSubtraction().toArray(a);
    }

    @Override
    public boolean add(T t) {
        if(excludedValues.contains(t)){
            cachedSubtraction = null;
            excludedValues.remove(t);
            originalSet.add(t);
            return true;
        }
        return originalSet.add(t);
    }

    @SuppressWarnings("unchecked")
	@Override
    public boolean remove(Object o) {
        cachedSubtraction = null;
        //noinspection unchecked
        return originalSet.remove(o) && !excludedValues.contains((T)o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return getSubtraction().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        cachedSubtraction = null;
        return c.stream().anyMatch(this::add);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        List<T> removeValues = new ArrayList<>();
        for (T t : getSubtraction()) {
            if (!c.contains(t)) {
                removeValues.add(t);
            }
        }
        if (!removeValues.isEmpty()) {
            cachedSubtraction = null;
            for (T remove : removeValues) {
                originalSet.remove(remove);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        cachedSubtraction = null;
        return c.stream().anyMatch(this::remove);
    }

    @Override
    public void clear() {
        originalSet.removeIf(e -> !excludedValues.contains(e));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for(T value:getSubtraction()) {
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

    public JadescriptSet<T> toNew(){
        return getSubtraction();
    }
}
