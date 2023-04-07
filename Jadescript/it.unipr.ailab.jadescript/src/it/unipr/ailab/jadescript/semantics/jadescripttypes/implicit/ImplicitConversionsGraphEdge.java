package it.unipr.ailab.jadescript.semantics.jadescripttypes.implicit;

class ImplicitConversionsGraphEdge {

    private final ImplicitConversionsGraphVertex from;
    private final ImplicitConversionsGraphVertex to;
    private final ImplicitConversionDefinition definition;


    public ImplicitConversionsGraphEdge(
        ImplicitConversionsGraphVertex from,
        ImplicitConversionsGraphVertex to,
        ImplicitConversionDefinition definition
    ) {
        this.from = from;
        this.to = to;
        this.definition = definition;
    }


    public ImplicitConversionsGraphVertex getFrom() {
        return from;
    }


    public ImplicitConversionsGraphVertex getTo() {
        return to;
    }


    public ImplicitConversionDefinition getDefinition() {
        return definition;
    }

}
