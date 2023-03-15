package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;

import java.util.function.Function;

public interface Dereferenceable extends Member {

    Dereferenced dereference(
        Function<BlockElementAcceptor, String> ownerCompiler
    );

}
