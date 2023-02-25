package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.DereferencedName;

public class DereferencedProperty
    extends Property
    implements DereferencedName {

    private final String compiledOwner;



    public DereferencedProperty(
        String compiledOwner,
        Property property
    ) {
        super(
            property.canWrite(),
            property.name(),
            property.readingType(),
            property.sourceLocation(),
            property.read,
            property.write
        );
        this.compiledOwner = compiledOwner;
    }


    @Override
    public String compileRead(BlockElementAcceptor acceptor) {
        return this.read.apply(getCompiledOwner(), acceptor);
    }


    @Override
    public void compileWrite(String rexpr, BlockElementAcceptor acceptor) {
        this.write.accept(getCompiledOwner(), rexpr, acceptor);
    }


    @Override
    public String getCompiledOwner() {
        return compiledOwner;
    }

}
