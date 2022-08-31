package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.UserLocalDefinition;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

public abstract class LocalVariable implements NamedSymbol {

    private final String name;
    private final IJadescriptType type;
    private final boolean canWrite;

    public LocalVariable(String name, IJadescriptType type, boolean canWrite) {
        this.name = name;
        this.type = type;
        this.canWrite = canWrite;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String compileRead(String dereferencePrefix) {
        return dereferencePrefix + name();
    }

    @Override
    public IJadescriptType readingType() {
        return type;
    }

    @Override
    public boolean canWrite() {
        return canWrite;
    }

    @Override
    public String compileWrite(String dereferencePrefix, String rexpr) {
        return dereferencePrefix + name() + " = " + rexpr;
    }

    @Override
    public SearchLocation sourceLocation() {
        return UserLocalDefinition.getInstance();
    }
}
