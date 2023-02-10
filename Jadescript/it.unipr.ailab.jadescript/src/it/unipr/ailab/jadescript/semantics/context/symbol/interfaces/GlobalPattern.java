package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import java.util.stream.Stream;

public interface GlobalPattern extends Global, Pattern {

    public interface Namespace {
        Stream<? extends GlobalPattern> globalPatterns();
    }

}
