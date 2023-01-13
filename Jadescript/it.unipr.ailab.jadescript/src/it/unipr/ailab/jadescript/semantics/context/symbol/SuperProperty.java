package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

public class SuperProperty implements NamedSymbol {

    private final String name;
    private final IJadescriptType type;
    private final SearchLocation location;


    public SuperProperty(
        String name,
        IJadescriptType type,
        SearchLocation location
    ) {
        this.name = name;
        this.type = type;
        this.location = location;
    }


    @Override
    public SearchLocation sourceLocation() {
        return location;
    }


    @Override
    public String name() {
        return name;
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
        return true;
    }


    @Override
    public String compileWrite(
        String dereferencePrefix,
        String rexpr
    ) {
        return dereferencePrefix + name() + " = " + rexpr;
    }

}
