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

    default void debugDump(SourceCodeBuilder scb) {

        BlockElementAcceptor debugAcceptor = (e) -> e.writeSonnet(scb);

        scb.open("NamedSymbol(concrete class=" +
            this.getClass().getName() + ") {");
        {
            scb.line("sourceLocation = " + sourceLocation());
            scb.line("name =" + name());
            scb.line("readingType = " + readingType().getDebugPrint());
            scb.line("writingType = " + writingType().getDebugPrint());
            scb.line("canWrite = " + canWrite());
            scb.open("compileRead  -> {");
            {
                scb.line(compileRead(debugAcceptor));
            }
            scb.close("}");
            scb.open("compileWrite -> {");
            {
                compileWrite("<setvalue>", debugAcceptor);
            }
            scb.close("}");
        }
        scb.close("}");

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
