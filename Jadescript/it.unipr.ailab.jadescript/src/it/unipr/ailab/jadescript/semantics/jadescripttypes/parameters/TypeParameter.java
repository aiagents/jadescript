package it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

public interface TypeParameter {

    void registerParameter(int index);

    IJadescriptType getUpperBound();

}
