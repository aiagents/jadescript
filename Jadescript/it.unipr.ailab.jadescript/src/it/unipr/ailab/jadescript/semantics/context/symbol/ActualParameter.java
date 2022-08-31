package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

public class ActualParameter extends LocalVariable {
    public ActualParameter(String name, IJadescriptType type) {
        super(name, type, false);
    }
}
