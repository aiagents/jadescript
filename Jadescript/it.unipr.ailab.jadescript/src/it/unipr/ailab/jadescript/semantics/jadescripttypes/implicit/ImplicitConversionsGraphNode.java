package it.unipr.ailab.jadescript.semantics.jadescripttypes.implicit;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.ArrayList;
import java.util.List;

class ImplicitConversionsGraphNode
    implements Comparable<ImplicitConversionsGraphNode> {

    private final List<ImplicitConversionsGraphEdge> adjacents =
        new ArrayList<>();
    private final IJadescriptType type;
    private boolean visited = false;
    private ImplicitConversionsGraphEdge linkToPredecessor = null;
    private int distance = Integer.MAX_VALUE;


    ImplicitConversionsGraphNode(IJadescriptType type) {
        this.type = type;
    }


    public int getDistance() {
        return distance;
    }


    public void setDistance(int distance) {
        this.distance = distance;
    }


    public List<ImplicitConversionsGraphEdge> getAdjacents() {
        return adjacents;
    }


    public boolean isVisited() {
        return visited;
    }


    public void setVisited(boolean visited) {
        this.visited = visited;
    }


    public ImplicitConversionsGraphEdge getLinkToPredecessor() {
        return linkToPredecessor;
    }


    public void setLinkToPredecessor(ImplicitConversionsGraphEdge linkToPredecessor) {
        this.linkToPredecessor = linkToPredecessor;
    }


    @SuppressWarnings("unused")
    public IJadescriptType getType() {
        return type;
    }


    @Override
    public int compareTo(ImplicitConversionsGraphNode o) {
        return Integer.compare(this.distance, o.distance);
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof ImplicitConversionsGraphNode
            && compareTo((ImplicitConversionsGraphNode) o) == 0;
    }


    @Override
    public int hashCode() {
        int result = getAdjacents().hashCode();
        result = 31 * result + getType().hashCode();
        result = 31 * result + (isVisited() ? 1 : 0);
        result = 31 * result + (getLinkToPredecessor() != null ?
            getLinkToPredecessor().hashCode() : 0);
        result = 31 * result + getDistance();
        return result;
    }

}
