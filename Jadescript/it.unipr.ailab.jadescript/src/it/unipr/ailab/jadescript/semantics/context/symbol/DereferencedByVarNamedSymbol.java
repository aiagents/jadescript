package it.unipr.ailab.jadescript.semantics.context.symbol;

public class DereferencedByVarNamedSymbol
    extends NamedSymbolWrapper {

    private final String var;


    protected DereferencedByVarNamedSymbol(
        NamedSymbol wrapped,
        String var
    ) {
        super(wrapped);
        this.var = var;
    }


    @Override
    public String compileRead(String dereferencePrefix) {
        return this.wrapped.compileRead(var + ".");
    }


    @Override
    public String compileWrite(String dereferencePrefix, String rexpr) {
        return super.compileWrite(var + ".", rexpr);
    }

}
