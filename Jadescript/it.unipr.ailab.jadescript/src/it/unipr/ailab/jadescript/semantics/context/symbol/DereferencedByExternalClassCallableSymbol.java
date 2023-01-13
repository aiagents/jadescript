package it.unipr.ailab.jadescript.semantics.context.symbol;

import java.util.List;
import java.util.Map;

public class DereferencedByExternalClassCallableSymbol
    extends CallableSymbolWrapper {

    private final String className;


    public DereferencedByExternalClassCallableSymbol(
        CallableSymbol wrapped,
        String className
    ) {
        super(wrapped);
        this.className = className;
    }


    @Override
    public String compileInvokeByArity(
        String dereferencePrefix,
        List<String> compiledRexprs
    ) {
        return super.compileInvokeByArity(
            className + ".",
            compiledRexprs
        );
    }


    @Override
    public String compileInvokeByName(
        String dereferencePrefix,
        Map<String, String> compiledRexprs
    ) {
        return super.compileInvokeByName(
            className + ".",
            compiledRexprs
        );
    }

}
