package jadescript.content;

import jade.content.Concept;
import jadescript.util.JadescriptSet;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JadescriptSetEntry<T> implements Concept {
    private T element;

    public JadescriptSetEntry() {}

    public T getElement() {
        return element;
    }

    public void setElement(T element) {
        this.element = element;
    }

    public static <Tt> JadescriptSetEntry<Tt> of(Tt element){
        JadescriptSetEntry<Tt> result = new JadescriptSetEntry<>();
        result.setElement(element);
        return result;
    }

    public static <Tt> List<JadescriptSetEntry<Tt>> toListOfEntries(Set<Tt> input){
        return input.stream().map(JadescriptSetEntry::of).collect(Collectors.toList());
    }

    public static <Tt> JadescriptSet<Tt> fromListOfEntries(List<JadescriptSetEntry<Tt>> input){
        JadescriptSet<Tt> result = new JadescriptSet<>();
        for (JadescriptSetEntry<Tt> entry : input) {
            result.add(entry.getElement());
        }
        return result;
    }
}
