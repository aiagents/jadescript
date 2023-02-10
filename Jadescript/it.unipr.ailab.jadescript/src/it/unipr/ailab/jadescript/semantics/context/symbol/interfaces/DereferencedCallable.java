package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

public interface DereferencedCallable
    extends Dereferenced,
    MemberCallable,
    CompilableCallable {

    @Override
    default Signature getSignature() {
        return MemberCallable.super.getSignature();
    }

}
