package it.unipr.ailab.jadescript.semantics.context.search;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;

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
            final TypeSolver typeSolver = module.get(TypeSolver.class);
            final BuiltinTypeProvider builtins =
                module.get(BuiltinTypeProvider.class);
        if(fullyQualifiedName!=null) {
            return typeSolver.fromFullyQualifiedName(fullyQualifiedName);
        }else{
            return builtins.any("Failed to extract type from " +
                "null qualified name.");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FQNameLocation) {
            return this.fullyQualifiedName.equals(
                ((FQNameLocation) obj).fullyQualifiedName
            );
        }else{
            return false;
        }
    }

    @Override
    public int hashCode() {
        return fullyQualifiedName!=null ? fullyQualifiedName.hashCode() : 0;
    }
}
