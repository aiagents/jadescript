package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public interface GlobalCallable extends Global, CompilableCallable {

    @Override
    default Signature getSignature() {
        return CompilableCallable.super.getSignature();
    }

    public interface Namespace extends CompilableCallable.Namespace {
        Stream<? extends GlobalCallable> globalCallables(
            @Nullable String name
        );

        @Override
        default Stream<? extends CompilableCallable> compilableCallables(
            @Nullable String name
        ){
            return globalCallables(name);
        }

    }
}
