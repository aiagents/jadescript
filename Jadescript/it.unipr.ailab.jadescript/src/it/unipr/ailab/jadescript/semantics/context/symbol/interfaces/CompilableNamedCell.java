package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import java.util.stream.Stream;

public interface CompilableNamedCell extends Compilable, NamedCell {

    String compileRead();

    String compileWrite(String rexpr);


    public interface Namespace {

        Stream<? extends CompilableNamedCell> compilableNamedCells();

    }

}
