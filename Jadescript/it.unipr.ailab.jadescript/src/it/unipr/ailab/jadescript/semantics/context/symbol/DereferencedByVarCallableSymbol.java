package it.unipr.ailab.jadescript.semantics.context.symbol;

import java.util.List;
import java.util.Map;

public class DereferencedByVarCallableSymbol
    extends CallableSymbolWrapper {

    private final String var;


    public DereferencedByVarCallableSymbol(
        CallableSymbol wrapped,
        String var
    ) {
        super(wrapped);
        this.var = var;
    }


    @Override
    public String compileInvokeByArity(
        String dereferencePrefix,
        List<String> compiledRexprs
    ) {
        return super.compileInvokeByArity(
            var + ".",
            compiledRexprs
        );
    }


    @Override
    public String compileInvokeByName(
        String dereferencePrefix,
        Map<String, String> compiledRexprs
    ) {
        return super.compileInvokeByName(
            var + ".",
            compiledRexprs
        );
    }

}
