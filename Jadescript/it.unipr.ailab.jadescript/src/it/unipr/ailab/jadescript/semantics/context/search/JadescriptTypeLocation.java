package it.unipr.ailab.jadescript.semantics.context.search;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

public class JadescriptTypeLocation extends FQNameLocation{
    public JadescriptTypeLocation(IJadescriptType jadescriptType) {
        super(jadescriptType.getID());
    }
}
