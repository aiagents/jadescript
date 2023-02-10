package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

public interface Dereferenceable extends Member {

    Dereferenced dereference(String compiledOwner);

}
