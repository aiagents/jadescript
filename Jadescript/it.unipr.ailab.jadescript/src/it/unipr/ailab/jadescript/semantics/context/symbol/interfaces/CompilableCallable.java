package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface CompilableCallable extends Compilable, Callable {

    String compileInvokeByArity(List<String> compiledRexprs);

    String compileInvokeByName(Map<String, String> compiledRexprs);

    public interface Namespace {

        Stream<? extends CompilableCallable> compilableCallables();

    }

}
