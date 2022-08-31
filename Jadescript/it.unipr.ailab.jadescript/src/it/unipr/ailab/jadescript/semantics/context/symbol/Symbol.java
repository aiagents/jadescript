package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;

public interface Symbol {
    SearchLocation sourceLocation();
    SymbolSignature getSignature();
}
