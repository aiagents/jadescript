package jadescript.core.nativeevent;


import jade.content.Predicate;
import jade.content.onto.Ontology;
import jadescript.content.JadescriptPredicate;
import jadescript.content.JadescriptProposition;

public class NativeEvent implements Predicate {
    private JadescriptProposition content;
    private Ontology ontology;

    public NativeEvent() {
    }

    public NativeEvent(JadescriptProposition content, Ontology ontology) {
        this.content = content;
        this.ontology = ontology;
    }

    public JadescriptProposition getContent() {
        return content;
    }

    public void setContent(JadescriptProposition content) {
        this.content = content;
    }

    public Ontology getOntology() {
        return ontology;
    }

    public void setOntology(Ontology ontology) {
        this.ontology = ontology;
    }
}
