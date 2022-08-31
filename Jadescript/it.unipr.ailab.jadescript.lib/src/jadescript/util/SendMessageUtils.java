package jadescript.util;

import jade.content.onto.Ontology;
import jadescript.content.JadescriptOntoElement;
import jadescript.lang.Performative;

public class SendMessageUtils {
    private SendMessageUtils(){}

    public static Ontology getDeclaringOntology(
            Object o,
            Ontology defaultValue,
            Ontology usedOntology
    ){
        Ontology result;
        if(o instanceof JadescriptOntoElement){
            result = ((JadescriptOntoElement) o).__getDeclaringOntology();
        }else if(defaultValue!=null){
            result = defaultValue;
        }else{
            result = jadescript.content.onto.Ontology.getInstance();
        }
        if(Ontology.isBaseOntology(new Ontology[]{usedOntology}, result.getName())){
            return usedOntology;
        }else{
            return result;
        }
    }

    public static void validatePerformative(String name) {
        validatePerformative(Performative.performativeByName.get(name));
    }

    public static void validatePerformative(Performative performative){
        if(performative==null || performative==Performative.UNKNOWN){
            throw new RuntimeException("Invalid performative '" + performative + "'.");
        }
    }

    public static void validatePerformative(int code){
        validatePerformative(Performative.fromCode(code));
    }
}
