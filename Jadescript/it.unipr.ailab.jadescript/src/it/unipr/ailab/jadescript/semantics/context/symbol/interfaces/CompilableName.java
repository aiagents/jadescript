package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public interface CompilableName extends Compilable, Name {

    String compileRead(BlockElementAcceptor acceptor);

    void compileWrite(String rexpr, BlockElementAcceptor acceptor);


    public interface Namespace {

        Stream<? extends CompilableName> compilableNames(
            @Nullable String name
        );

    }

}
