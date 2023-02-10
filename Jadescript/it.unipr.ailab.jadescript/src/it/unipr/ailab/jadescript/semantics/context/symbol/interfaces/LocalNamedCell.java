package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import java.util.stream.Stream;

public interface LocalNamedCell extends Local, CompilableNamedCell {

    @Override
    default Signature getSignature() {
        return CompilableNamedCell.super.getSignature();
    }

    public interface Namespace extends CompilableNamedCell.Namespace{
        Stream<? extends LocalNamedCell> localNamedCells();

        @Override
        default Stream<? extends CompilableNamedCell> compilableNamedCells(){
            return localNamedCells();
        }

    }
}
