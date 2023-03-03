package it.unipr.ailab.jadescript.semantics.context.clashing;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.Callable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableCallable;

import java.util.stream.Stream;

public interface CallableClashValidator {

    Stream<DefinitionClash> checkCallableClash(
        SemanticsModule module,
        Callable toBeAdded
    );

}
