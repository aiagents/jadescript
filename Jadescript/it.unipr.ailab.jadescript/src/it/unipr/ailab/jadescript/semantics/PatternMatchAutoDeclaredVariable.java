package it.unipr.ailab.jadescript.semantics;

import it.unipr.ailab.jadescript.semantics.context.symbol.LocalVariable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

public class PatternMatchAutoDeclaredVariable extends LocalVariable {
    private final String prefixDereference;

    public PatternMatchAutoDeclaredVariable(String name, IJadescriptType type, String prefixDereference) {
        super(name, type, false);
        this.prefixDereference = prefixDereference;
    }

    @Override
    public String compileRead(String dereferencePrefix) {
        return super.compileRead(prefixDereference);
    }

    @Override
    public String compileWrite(String dereferencePrefix, String rexpr) {
        return super.compileWrite(prefixDereference, rexpr);
    }
}
