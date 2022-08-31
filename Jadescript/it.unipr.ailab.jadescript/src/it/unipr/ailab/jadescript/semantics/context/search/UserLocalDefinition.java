package it.unipr.ailab.jadescript.semantics.context.search;

public class UserLocalDefinition extends SearchLocation{
    private static UserLocalDefinition INSTANCE = null;
    public static UserLocalDefinition getInstance(){
        if(INSTANCE == null){
            INSTANCE = new UserLocalDefinition();
        }
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "(local definition by user)";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof UserLocalDefinition;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
