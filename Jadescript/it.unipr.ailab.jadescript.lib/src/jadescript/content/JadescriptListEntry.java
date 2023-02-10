package jadescript.content;

import jade.content.Concept;
import jadescript.util.JadescriptList;

import java.util.List;
import java.util.stream.Collectors;

public class JadescriptListEntry<T> implements Concept {

    private T element;

    public JadescriptListEntry(){}

    public T getElement() {
        return element;
    }

    public void setElement(T element){
        this.element = element;
    }

    public static <Tt> JadescriptListEntry<Tt> of(Tt element){
        JadescriptListEntry<Tt> result = new JadescriptListEntry<>();
        result.setElement(element);
        return result;
    }

    public static <Tt> List<JadescriptListEntry<Tt>> toListOfEntries(
        List<Tt> input
    ){
        return input.stream()
            .map(JadescriptListEntry::of)
            .collect(Collectors.toList());
    }

    public static <Tt> List<Tt> fromListOfEntries(
        List<JadescriptListEntry<Tt>> input
    ){
        List<Tt> result = new JadescriptList<>();
        for(JadescriptListEntry<Tt> entry: input){
            result.add(entry.getElement());
        }
        return result;
    }


}
