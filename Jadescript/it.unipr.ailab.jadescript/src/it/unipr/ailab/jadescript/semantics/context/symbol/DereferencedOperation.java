package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.DereferencedCallable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DereferencedOperation
    extends Operation
    implements DereferencedCallable {

    private final Function<BlockElementAcceptor, String> ownerCompiler;


    public DereferencedOperation(
        Function<BlockElementAcceptor, String> ownerCompiler,
        Operation memberCallable
    ) {
        super(
            memberCallable.returnType(), memberCallable.name(),
            memberCallable.parameterTypesByName(),
            memberCallable.parameterNames(),
            memberCallable.sourceLocation(),
            memberCallable.isWithoutSideEffects(),
            memberCallable.invokeByArityCustom,
            memberCallable.invokeByNameCustom
        );
        this.ownerCompiler = ownerCompiler;
    }


    @Override
    public String compileInvokeByArity(
        List<String> compiledRexprs,
        BlockElementAcceptor acceptor
    ) {
        return invokeByArityCustom.apply(
            getOwnerCompiler().apply(acceptor),
            compiledRexprs
        );
    }


    @Override
    public String compileInvokeByName(
        Map<String, String> compiledRexprs,
        BlockElementAcceptor acceptor
    ) {
        return invokeByNameCustom.apply(
            getOwnerCompiler().apply(acceptor),
            compiledRexprs
        );
    }


    @Override
    public Function<BlockElementAcceptor, String> getOwnerCompiler() {
        return ownerCompiler;
    }


}
