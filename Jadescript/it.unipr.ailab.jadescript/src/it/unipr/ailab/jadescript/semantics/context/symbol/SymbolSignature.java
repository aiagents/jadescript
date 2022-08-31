package it.unipr.ailab.jadescript.semantics.context.symbol;

public abstract class SymbolSignature {

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract String toString();
}
