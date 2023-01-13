package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.context.search.AutoCompiled;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.function.Function;

public class ContextGeneratedReference implements NamedSymbol{
    private final String name;
    private final IJadescriptType type;
    private final Function<String, String> customCompileRead;

    public ContextGeneratedReference(
        String name,
        IJadescriptType type,
        Function<String, String> customCompileRead
    ) {
        this.name = name;
        this.type = type;
        this.customCompileRead = customCompileRead;
    }

    public ContextGeneratedReference(String name, IJadescriptType type){
        this(name, type, null);
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public IJadescriptType readingType() {
        return this.type;
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public SearchLocation sourceLocation() {
        return AutoCompiled.getInstance();
    }

    @Override
    public String compileRead(String dereferencePrefix) {
        if(customCompileRead==null) {
            return NamedSymbol.super.compileRead(dereferencePrefix);
        }else {
            return customCompileRead.apply(dereferencePrefix);
        }
    }
}
