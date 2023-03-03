package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public interface LocalName extends Local, CompilableName {

    @Override
    default Signature getSignature() {
        return CompilableName.super.getSignature();
    }



    public interface Namespace extends CompilableName.Namespace{
        Stream<? extends LocalName> localNames(
            @Nullable String name
        );

        @Override
        default Stream<? extends CompilableName> compilableNames(
            @Nullable String name
        ){
            return localNames(name);
        }

    }
}
