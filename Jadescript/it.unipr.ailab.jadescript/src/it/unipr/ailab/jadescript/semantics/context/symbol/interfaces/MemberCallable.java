package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.stream.Stream;

public interface MemberCallable extends Dereferenceable, Callable {

    @Override
    DereferencedCallable dereference(
        Function<BlockElementAcceptor, String> ownerCompiler
    );


    default void debugDumpMemberCallable(SourceCodeBuilder scb) {
        dereference((__) -> "<owner>").debugDumpDereferencedCallable(scb);
    }

    public interface Namespace {

        Stream<? extends MemberCallable> memberCallables(
            @Nullable String name
        );

    }

}
