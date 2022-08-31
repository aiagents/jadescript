package it.unipr.ailab.jadescript.semantics.context.search;

import it.unipr.ailab.jadescript.semantics.context.Context;

public class ContextLocation extends SearchLocation{
    private final Context context;

    public ContextLocation(Context context) {
        this.context = context;
    }

    @Override
    public String toString() {
        return "(Context of type: "+context.getClass().getName()+")";
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
