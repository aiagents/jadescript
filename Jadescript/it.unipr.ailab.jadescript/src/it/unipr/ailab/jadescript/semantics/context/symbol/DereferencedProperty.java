package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.DereferencedNamedCell;

public class DereferencedProperty
    extends Property
    implements DereferencedNamedCell {

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
            property.readCompile,
            property.writeCompile
        );
        this.compiledOwner = compiledOwner;
    }


    @Override
    public String compileRead() {
        return this.readCompile.apply(getCompiledOwner());
    }


    @Override
    public String compileWrite(String rexpr) {
        return this.writeCompile.apply(getCompiledOwner(), rexpr);
    }


    @Override
    public String getCompiledOwner() {
        return compiledOwner;
    }

}
