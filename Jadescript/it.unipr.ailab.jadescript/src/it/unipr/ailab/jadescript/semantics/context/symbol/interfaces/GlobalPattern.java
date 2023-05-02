package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public interface GlobalPattern extends Global, Pattern {

    public interface Namespace {

        Stream<? extends GlobalPattern> globalPatterns(
            @Nullable String name
        );

    }

}
