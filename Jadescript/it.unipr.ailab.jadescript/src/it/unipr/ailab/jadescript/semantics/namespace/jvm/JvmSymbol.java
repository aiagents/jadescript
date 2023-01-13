package it.unipr.ailab.jadescript.semantics.namespace.jvm;

import it.unipr.ailab.jadescript.semantics.context.search.JvmTypeLocation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.UnknownLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Symbol;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmType;

public interface JvmSymbol extends Symbol {

    boolean isStatic();

    Maybe<JvmType> declaringType();

    @Override
    default SearchLocation sourceLocation() {
        return declaringType().__(it -> new JvmTypeLocation(it))
            .__(x -> (SearchLocation) x)
            .orElse(UnknownLocation.getInstance());
    }

}
