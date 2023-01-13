package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public class NamedSymbolWrapper implements NamedSymbol {

    protected final NamedSymbol wrapped;


    protected NamedSymbolWrapper(NamedSymbol wrapped) {
        this.wrapped = wrapped;
    }


    @Override
    public String name() {
        return wrapped.name();
    }


    @Override
    public IJadescriptType readingType() {
        return wrapped.readingType();
    }


    @Override
    public IJadescriptType writingType() {
        return wrapped.writingType();
    }


    @Override
    public boolean canWrite() {
        return wrapped.canWrite();
    }


    @Override
    public String compileRead(String dereferencePrefix) {
        return wrapped.compileRead(dereferencePrefix);
    }


    @Override
    public String compileWrite(String dereferencePrefix, String rexpr) {
        return wrapped.compileWrite(dereferencePrefix, rexpr);
    }


    @Override
    public NamedSymbolSignature getSignature() {
        return wrapped.getSignature();
    }


    @Override
    public void debugDumpNamedSymbol(SourceCodeBuilder scb) {
        wrapped.debugDumpNamedSymbol(scb);
    }


    @Override
    public SearchLocation sourceLocation() {
        return wrapped.sourceLocation();
    }

}
