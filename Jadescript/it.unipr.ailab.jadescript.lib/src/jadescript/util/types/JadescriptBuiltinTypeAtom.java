package jadescript.util.types;

public enum JadescriptBuiltinTypeAtom {
    INTEGER("integer"),
    BOOLEAN("boolean"),
    REAL("real"),
    TEXT("text"),
    DURATION("duration"),
    TIMESTAMP("timestamp"),
    PERFORMATIVE("performative"),
    AID("aid"),
    ONTOLOGY("Ontology"),
    CONCEPT("Concept"),
    ACTION("Action"),
    PROPOSITION("Proposition"),
    BEHAVIOUR("Behaviour"),
    AGENT("Agent"),
    MESSAGE("Message"),
    LIST("list"),
    MAP("map"),
    SET("set"),
    TUPLE("tuple"),
    OTHER("unknown");

    private final String typeName;

    JadescriptBuiltinTypeAtom(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }
}
