package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public interface DereferencedName
    extends Dereferenced,
    MemberName,
    CompilableName {

    default void debugDumpDereferencedName(SourceCodeBuilder scb){
        BlockElementAcceptor debugAcceptor = (e) -> e.writeSonnet(scb);
        scb.open("member name '"+name()+"' {");
        {
            scb.line("compiledOwner = " + getCompiledOwner());
            scb.line("readingType = " + readingType());
            scb.line("writingType = " + writingType());
            scb.line("canWrite = " + canWrite());
            scb.line("location = " + sourceLocation());
            scb.line("signature = " + getSignature());
            scb.open("compileRead -> {");
            {
                scb.line(compileRead(debugAcceptor));
            }
            scb.close("}");
            scb.open("compileWrite -> {");
            {
                compileWrite("<rexpr>", debugAcceptor);
            }
            scb.close("}");
        }
        scb.close("}");
    }

}
