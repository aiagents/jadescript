package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;

public interface Located {
    BaseSignature getSignature();
    SearchLocation sourceLocation();
}
