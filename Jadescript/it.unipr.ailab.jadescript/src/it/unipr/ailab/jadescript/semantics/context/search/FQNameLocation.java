package it.unipr.ailab.jadescript.semantics.context.search;

import org.jetbrains.annotations.NotNull;

public class FQNameLocation extends SearchLocation {
    private final String fullyQualifiedName;

    public FQNameLocation(@NotNull String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }

    @Override
    public String toString() {
        return "(Fully-qualified name: '" + fullyQualifiedName + "')";
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FQNameLocation) {
            return fullyQualifiedName.equals(((FQNameLocation) obj).fullyQualifiedName);
        }else{
            return false;
        }
    }

    @Override
    public int hashCode() {
        return fullyQualifiedName!=null ? fullyQualifiedName.hashCode() : 0;
    }
}
