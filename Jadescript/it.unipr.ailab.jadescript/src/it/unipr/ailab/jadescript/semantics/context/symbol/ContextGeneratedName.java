package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.context.location.ContextGenerated;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.LocalNamedCell;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.function.Supplier;

public class ContextGeneratedName implements LocalNamedCell {

    private final String name;
    private final IJadescriptType type;
    private final Supplier<String> customCompileRead;


    public ContextGeneratedName(
        String name,
        IJadescriptType type,
        Supplier<String> customCompileRead
    ) {
        this.name = name;
        this.type = type;
        this.customCompileRead = customCompileRead;
    }


    @Override
    public String compileRead() {
        return customCompileRead.get();
    }


    @Override
    public String compileWrite(String rexpr) {
        return "/*Error:" +
            " attempted to write on context-generated name '"+name+"' */" +
            name + " = " + rexpr;
    }


    @Override
    public SearchLocation sourceLocation() {
        return ContextGenerated.getInstance();
    }


    @Override
    public String name() {
        return name;
    }


    @Override
    public IJadescriptType readingType() {
        return type;
    }


    @Override
    public boolean canWrite() {
        return false;
    }

}
