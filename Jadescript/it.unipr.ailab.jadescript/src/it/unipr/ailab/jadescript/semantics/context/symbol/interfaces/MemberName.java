package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public interface MemberName extends Dereferenceable, Name {

    @Override
    DereferencedName dereference(String compiledOwner);

    @Override
    default Signature getSignature() {
        return Name.super.getSignature();
    }

    default void debugDumpMemberName(SourceCodeBuilder scb){
        dereference("<owner>").debugDumpDereferencedName(scb);
    }

    public interface Namespace{
        Stream<? extends MemberName> memberNames(
            @Nullable String name
        );
    }
}
