package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public interface LocalName extends Local, CompilableName {


    public interface Namespace extends CompilableName.Namespace {

        Stream<? extends LocalName> localNames(
            @Nullable String name
        );

        @Override
        default Stream<? extends CompilableName> compilableNames(
            @Nullable String name
        ) {
            return localNames(name);
        }

    }

}
