package it.unipr.ailab.jadescript.semantics.context.search;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import org.jetbrains.annotations.NotNull;

public class FQNameLocation extends SearchLocation {
    private final String fullyQualifiedName;

    public FQNameLocation(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }

    @Override
    public String toString() {
        return "(Fully-qualified name: '" + fullyQualifiedName + "')";
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public IJadescriptType extractType(SemanticsModule module){
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if(fullyQualifiedName!=null) {
            return typeHelper
                .jtFromFullyQualifiedName(fullyQualifiedName);
        }else{
            return typeHelper.ANY;
        }
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
