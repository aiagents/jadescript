package it.unipr.ailab.jadescript.semantics.jadescripttypes.implicit;

class ImplicitConversionsGraphEdge {

    private final ImplicitConversionsGraphNode from;
    private final ImplicitConversionsGraphNode to;
    private final ImplicitConversionDefinition definition;


    public ImplicitConversionsGraphEdge(
        ImplicitConversionsGraphNode from,
        ImplicitConversionsGraphNode to,
        ImplicitConversionDefinition definition
    ) {
        this.from = from;
        this.to = to;
        this.definition = definition;
    }


    public ImplicitConversionsGraphNode getFrom() {
        return from;
    }


    public ImplicitConversionsGraphNode getTo() {
        return to;
    }


    public ImplicitConversionDefinition getDefinition() {
        return definition;
    }

}
