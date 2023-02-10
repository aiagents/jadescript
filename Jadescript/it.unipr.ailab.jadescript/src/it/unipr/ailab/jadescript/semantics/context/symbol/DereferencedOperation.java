package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.DereferencedCallable;

import java.util.List;
import java.util.Map;

public class DereferencedOperation
    extends Operation
    implements DereferencedCallable {

    private final String compiledOwner;


    public DereferencedOperation(
        String compiledOwner,
        Operation memberCallable
    ) {
        super(
            memberCallable.returnType(), memberCallable.name(),
            memberCallable.parameterTypesByName(),
            memberCallable.parameterNames(),
            memberCallable.sourceLocation(),
            memberCallable.isWithoutSideEffects(),
            memberCallable.invokeByArityCustom, memberCallable.invokeByNameCustom
        );
        this.compiledOwner = compiledOwner;
    }



    @Override
    public String compileInvokeByArity(List<String> compiledRexprs) {
        return invokeByArityCustom.apply(
            getCompiledOwner(),
            compiledRexprs
        );
    }


    @Override
    public String compileInvokeByName(Map<String, String> compiledRexprs) {
        return invokeByNameCustom.apply(
            getCompiledOwner(),
            compiledRexprs
        );
    }


    @Override
    public String getCompiledOwner() {
        return compiledOwner;
    }



}
