package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.DereferencedName;

import java.util.function.Function;

public class DereferencedProperty
    extends Property
    implements DereferencedName {

    private final Function<BlockElementAcceptor, String> ownerCompiler;



    public DereferencedProperty(
        Function<BlockElementAcceptor, String> ownerCompiler,
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
        this.ownerCompiler = ownerCompiler;
    }


    @Override
    public String compileRead(BlockElementAcceptor acceptor) {
        return this.read.apply(getOwnerCompiler().apply(acceptor), acceptor);
    }


    @Override
    public void compileWrite(String rexpr, BlockElementAcceptor acceptor) {
        this.write.accept(getOwnerCompiler().apply(acceptor), rexpr, acceptor);
    }


    @Override
    public Function<BlockElementAcceptor, String> getOwnerCompiler() {
        return ownerCompiler;
    }

}
