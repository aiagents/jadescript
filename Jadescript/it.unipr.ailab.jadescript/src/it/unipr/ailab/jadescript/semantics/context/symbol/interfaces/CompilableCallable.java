package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface CompilableCallable extends Compilable, Callable {

    String compileInvokeByArity(
        List<String> compiledRexprs,
        BlockElementAcceptor acceptor
    );

    String compileInvokeByName(
        Map<String, String> compiledRexprs,
        BlockElementAcceptor acceptor
    );

    public interface Namespace {

        Stream<? extends CompilableCallable> compilableCallables(
            @Nullable String name
        );

    }

}
