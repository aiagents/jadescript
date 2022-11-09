package it.unipr.ailab.jadescript.semantics.context.clashing;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;

import java.util.stream.Stream;

public interface CallableClashValidator {
    Stream<DefinitionClash> checkCallableClash(SemanticsModule module, CallableSymbol toBeAdded);
}
