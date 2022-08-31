package it.unipr.ailab.jadescript.semantics.context.search;

import org.jetbrains.annotations.NotNull;

public class ModuleGlobalLocation extends FQNameLocation {

    public ModuleGlobalLocation(@NotNull String moduleName) {
        super(moduleName);
    }

    @Override
    public String toString() {
        return "(module: "+getModuleName()+")";
    }


    public String getModuleName() {
        return getFullyQualifiedName();
    }
}
