package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import java.util.stream.Stream;

public interface MemberCallable extends Dereferenceable, Callable {

    @Override
    DereferencedCallable dereference(String compiledOwner);

    @Override
    default Signature getSignature() {
        return Callable.super.getSignature();
    }

    public interface Namespace{
        Stream<? extends MemberCallable> memberCallables();
    }
}
