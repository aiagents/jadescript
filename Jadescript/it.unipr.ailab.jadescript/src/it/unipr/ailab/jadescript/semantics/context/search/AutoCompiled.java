package it.unipr.ailab.jadescript.semantics.context.search;

public class AutoCompiled extends SearchLocation{

    private static AutoCompiled INSTANCE = null;
    public static AutoCompiled getInstance() {
        if(INSTANCE == null){
            INSTANCE = new AutoCompiled();
        }
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "(autocompiled)";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AutoCompiled;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
