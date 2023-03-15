package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;


import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;

import java.util.function.Function;

public interface Dereferenced extends Member, Compilable {

    Function<BlockElementAcceptor, String> getOwnerCompiler();

}
