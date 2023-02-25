package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.DereferencedName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

public class SuperProperty implements DereferencedName {

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
    public String compileRead(BlockElementAcceptor acceptor) {
        return name;
    }


    @Override
    public void compileWrite(String rexpr, BlockElementAcceptor acceptor) {
        acceptor.accept(
            w.assign(name, w.expr(rexpr))
        );
    }


    @Override
    public String getCompiledOwner() {
        return "super";
    }


    @Override
    public SearchLocation sourceLocation() {
        return location;
    }


    @Override
    public DereferencedName dereference(String compiledOwner) {
        return new SuperProperty(name, type, location);
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
        return true;
    }

}
