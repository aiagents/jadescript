package jadescript.content.onto.basic;

import jade.content.onto.Ontology;
import jadescript.content.JadescriptPredicate;

public class InternalException implements JadescriptPredicate {
    private String description;

    public InternalException() {
        description = "";
    }


    public InternalException(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Ontology __getDeclaringOntology() {
        return jadescript.content.onto.Ontology.getInstance();
    }

    @SuppressWarnings("SameReturnValue")
    public jadescript.content.onto.Ontology __metadata_jadescript_content_onto_basic_InternalException() {
        return null;
    }

    @Override
    public String toString() {
        return "InternalException(description=\"" + description + "\")";
    }
}
