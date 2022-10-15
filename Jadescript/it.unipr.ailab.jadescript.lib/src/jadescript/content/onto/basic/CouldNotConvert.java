package jadescript.content.onto.basic;

import jade.content.onto.Ontology;
import jadescript.content.JadescriptPredicate;

public class CouldNotConvert implements JadescriptPredicate {
    private String value;
    private String fromTypeName;
    private String toTypeName;

    public CouldNotConvert() {
        value = "";
        fromTypeName = "";
        toTypeName = "";
    }

    public CouldNotConvert(String value, String fromTypeName, String toTypeName) {
        this.value = value;
        this.fromTypeName = fromTypeName;
        this.toTypeName = toTypeName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getFromTypeName() {
        return fromTypeName;
    }

    public void setFromTypeName(String fromTypeName) {
        this.fromTypeName = fromTypeName;
    }

    public String getToTypeName() {
        return toTypeName;
    }

    public void setToTypeName(String toTypeName) {
        this.toTypeName = toTypeName;
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
        return "CouldNotConvert(" +
                "value='" + value + '\'' +
                ", fromTypeName='" + fromTypeName + '\'' +
                ", toTypeName='" + toTypeName + '\'' +
                ')';
    }
}
