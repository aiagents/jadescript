package it.unipr.ailab.jadescript.semantics.proxyeobjects;

import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.expression.trailersexprchain.ReversedTrailerChain;
import it.unipr.ailab.maybe.Maybe;

public class Subscript extends ProxyEObject {

    private final RValueExpression key;
    private final ReversedTrailerChain rest;


    private Subscript(RValueExpression key, ReversedTrailerChain rest) {
        super(key);
        this.key = key;
        this.rest = rest;
    }


    public static Maybe<Subscript> subscript(
        Maybe<RValueExpression> key,
        ReversedTrailerChain rest
    ) {
        return key
            .nullIf(__ -> rest == null)
            .nullIf(__ -> rest.getElements().isEmpty())
            .__(k -> new Subscript(k, rest));
    }


    public RValueExpression getKey() {
        return key;
    }


    public ReversedTrailerChain getRest() {
        return rest;
    }

}
