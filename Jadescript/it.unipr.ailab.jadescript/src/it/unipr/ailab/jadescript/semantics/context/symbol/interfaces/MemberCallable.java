package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public interface MemberCallable extends Dereferenceable, Callable {

    @Override
    DereferencedCallable dereference(String compiledOwner);

    @Override
    default Signature getSignature() {
        return Callable.super.getSignature();
    }

    default void debugDumpMemberCallable(SourceCodeBuilder scb){
        dereference("<owner>").debugDumpDereferencedCallable(scb);
    }

    public interface Namespace{
        Stream<? extends MemberCallable> memberCallables(
            @Nullable String name
        );
    }
}
