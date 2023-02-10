package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import java.util.stream.Stream;

public interface MemberNamedCell extends Dereferenceable, NamedCell {

    @Override
    DereferencedNamedCell dereference(String compiledOwner);

    @Override
    default Signature getSignature() {
        return NamedCell.super.getSignature();
    }

    public interface Namespace{
        Stream<? extends MemberNamedCell> memberNamedCells();
    }
}
