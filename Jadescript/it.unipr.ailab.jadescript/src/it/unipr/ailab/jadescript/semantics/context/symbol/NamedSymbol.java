package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.function.Predicate;
import java.util.stream.Stream;

public interface NamedSymbol extends Symbol {

    String name();

    IJadescriptType readingType();

    default IJadescriptType writingType() {
        return readingType();
    }

    boolean canWrite();

    default String compileRead(
        String dereferencePrefix
    ) {
        return dereferencePrefix + name();
    }

    default String compileWrite(String dereferencePrefix, String rexpr) {
        return dereferencePrefix + name() + " = " + rexpr;
    }

    @Override
    default NamedSymbolSignature getSignature(){
        return new NamedSymbolSignature(name(), readingType(), canWrite());
    }


    default void debugDumpNamedSymbol(SourceCodeBuilder scb) {
        scb.open("NamedSymbol(concrete class=" + this.getClass().getName() +
            ") {");
        scb.line("sourceLocation = " + sourceLocation());
        scb.line("name =" + name());
        scb.line("readingType = " + readingType().getDebugPrint());
        scb.line("writingType = " + writingType().getDebugPrint());
        scb.line("canWrite = " + canWrite());
        scb.line("compileRead  -> " + compileRead(
            "<dereferencePrefix>"
        ));
        scb.line("compileWrite -> " + compileWrite(
            "<dereferencePrefix>", "<setvalue>"
        ));
        scb.close("}");
    }

    interface Searcher {
        Stream<? extends NamedSymbol> searchName(
                Predicate<String> name,
                Predicate<IJadescriptType> readingType,
                Predicate<Boolean> canWrite
        );

        default Stream<? extends NamedSymbol> searchName(
                String name,
                Predicate<IJadescriptType> readingType,
                Predicate<Boolean> canWrite
        ) {
            return searchName(n -> n.equals(name), readingType, canWrite);
        }
    }
}
