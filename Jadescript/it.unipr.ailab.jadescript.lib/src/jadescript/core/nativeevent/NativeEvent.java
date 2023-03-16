package jadescript.core.nativeevent;


import jade.content.Predicate;
import jade.content.onto.Ontology;
import jadescript.content.JadescriptPredicate;

public class NativeEvent implements Predicate {
    private JadescriptPredicate content;
    private Ontology ontology;

    public NativeEvent() {
    }

    public NativeEvent(JadescriptPredicate content, Ontology ontology) {
        this.content = content;
        this.ontology = ontology;
    }

    public JadescriptPredicate getContent() {
        return content;
    }

    public void setContent(JadescriptPredicate content) {
        this.content = content;
    }

    public Ontology getOntology() {
        return ontology;
    }

    public void setOntology(Ontology ontology) {
        this.ontology = ontology;
    }
}
