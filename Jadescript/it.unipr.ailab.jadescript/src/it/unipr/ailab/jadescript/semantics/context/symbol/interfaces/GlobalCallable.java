package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import java.util.stream.Stream;

public interface GlobalCallable extends Global, CompilableCallable {

    @Override
    default Signature getSignature() {
        return CompilableCallable.super.getSignature();
    }

    public interface Namespace extends CompilableCallable.Namespace {
        Stream<? extends GlobalCallable> globalCallables();

        @Override
        default Stream<? extends CompilableCallable> compilableCallables(){
            return globalCallables();
        }

    }
}
