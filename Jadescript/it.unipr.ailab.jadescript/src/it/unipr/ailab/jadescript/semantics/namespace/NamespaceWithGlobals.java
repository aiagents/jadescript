package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalPattern;
import it.unipr.ailab.maybe.Maybe;

public interface NamespaceWithGlobals extends GlobalCallable.Namespace,
    GlobalName.Namespace, GlobalPattern.Namespace, Searcheable {

    @Override
    Maybe<? extends NamespaceWithGlobals> superSearcheable();

}
