package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.stream.Stream;

public interface MemberName extends Dereferenceable, Name {

    @Override
    DereferencedName dereference(
        Function<BlockElementAcceptor, String> ownerCompiler
    );


    default void debugDumpMemberName(SourceCodeBuilder scb) {
        dereference((__) -> "<owner>").debugDumpDereferencedName(scb);
    }

    public interface Namespace {

        Stream<? extends MemberName> memberNames(
            @Nullable String name
        );

    }

}
