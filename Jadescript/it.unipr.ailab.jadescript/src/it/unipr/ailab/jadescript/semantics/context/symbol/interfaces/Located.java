package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;

public interface Located extends SemanticsConsts {

    BaseSignature getSignature();
    SearchLocation sourceLocation();
}
