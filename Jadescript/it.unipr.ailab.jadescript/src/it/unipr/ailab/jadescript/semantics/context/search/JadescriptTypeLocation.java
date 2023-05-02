package it.unipr.ailab.jadescript.semantics.context.search;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

public class JadescriptTypeLocation extends FQNameLocation {

    private final IJadescriptType jadescriptType;


    public JadescriptTypeLocation(IJadescriptType jadescriptType) {
        super(jadescriptType.getID());
        this.jadescriptType = jadescriptType;
    }


    @Override
    public IJadescriptType extractType(SemanticsModule module) {
        return jadescriptType;
    }

}
