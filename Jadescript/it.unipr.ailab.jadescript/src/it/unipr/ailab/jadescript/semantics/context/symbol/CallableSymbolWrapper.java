package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.List;
import java.util.Map;

public class CallableSymbolWrapper implements CallableSymbol {

    private final CallableSymbol wrapped;


    public CallableSymbolWrapper(CallableSymbol wrapped) {
        this.wrapped = wrapped;
    }


    @Override
    public String name() {
        return wrapped.name();
    }


    @Override
    public IJadescriptType returnType() {
        return wrapped.returnType();
    }


    @Override
    public Map<String, IJadescriptType> parameterTypesByName() {
        return wrapped.parameterTypesByName();
    }


    @Override
    public List<String> parameterNames() {
        return wrapped.parameterNames();
    }


    @Override
    public List<IJadescriptType> parameterTypes() {
        return wrapped.parameterTypes();
    }


    @Override
    public String compileInvokeByArity(
        String dereferencePrefix,
        List<String> compiledRexprs
    ) {
        return wrapped.compileInvokeByArity(
            dereferencePrefix,
            compiledRexprs
        );
    }


    @Override
    public String compileInvokeByName(
        String dereferencePrefix,
        Map<String, String> compiledRexprs
    ) {
        return wrapped.compileInvokeByName(
            dereferencePrefix,
            compiledRexprs
        );
    }


    @Override
    public boolean isWithoutSideEffects() {
        return wrapped.isWithoutSideEffects();
    }


    @Override
    public int arity() {
        return wrapped.arity();
    }


    @Override
    public CallableSymbolSignature getSignature() {
        return wrapped.getSignature();
    }


    @Override
    public void debugDumpCallableSymbol(SourceCodeBuilder scb) {
        wrapped.debugDumpCallableSymbol(scb);
    }


    @Override
    public SearchLocation sourceLocation() {
        return wrapped.sourceLocation();
    }

}
