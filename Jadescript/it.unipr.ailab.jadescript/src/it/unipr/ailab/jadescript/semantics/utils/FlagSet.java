package it.unipr.ailab.jadescript.semantics.utils;

import java.util.*;

public class FlagSet<E extends Enum<E>> {
    private final Set<E> flagSet = new HashSet<>();

    @SafeVarargs
    public static <E extends Enum<E>> FlagSet<E> flags(E... flags){
        return flags(Arrays.asList(flags));
    }

    public static <E extends Enum<E>> FlagSet<E> flags(List<E> flags){
        return new FlagSet<>(flags);
    }


    protected FlagSet(Iterable<E> flags){
        for (E flag : flags) {
            flagSet.add(flag);
        }
    }

    public boolean isSet(E flag){
        return flagSet.contains(flag);
    }

    public Set<E> toJavaSet(){
        return flagSet;
    }

    public FlagSet<E> intersect(FlagSet<E> other){
        List<E> result = new ArrayList<>();
        for (E e : this.flagSet) {
            if(other.isSet(e)){
                result.add(e);
            }
        }
        return flags(result);
    }

    public FlagSet<E> union(FlagSet<E> other){
        List<E> result = new ArrayList<>();
        result.addAll(this.flagSet);
        result.addAll(other.flagSet);
        return flags(result);
    }

    public FlagSet<E> complement(Class<? extends E> enumClass){
        List<E> result = Arrays.asList(enumClass.getEnumConstants());
        result.removeIf(this::isSet);
        return flags(result);
    }

    public FlagSet<E> subtract(FlagSet<E> other){
        List<E> result = new ArrayList<>(this.flagSet);
        result.removeIf(other::isSet);
        return flags(result);
    }
}
