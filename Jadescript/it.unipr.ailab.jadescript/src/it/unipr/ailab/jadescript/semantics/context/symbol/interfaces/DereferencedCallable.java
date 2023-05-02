package it.unipr.ailab.jadescript.semantics.context.symbol.interfaces;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public interface DereferencedCallable
    extends Dereferenced,
    MemberCallable,
    CompilableCallable {




    default void debugDumpDereferencedCallable(SourceCodeBuilder scb){
        scb.open("member name '"+name()+"' {");
        {
            scb.line("compiledOwner = " + getOwnerCompiler());
            scb.line("returnType = " + returnType());
            scb.line("parameterTypesByName = " + parameterTypesByName());
            scb.line("parameterNames = " + parameterNames());
            scb.line("parameterTypes = " + parameterTypes());
            scb.line("signature = " + getSignature());
            scb.line("arity = " + arity());
            scb.line("isWithoutSideEffects = " + isWithoutSideEffects());
            scb.line("location = " + sourceLocation());
            scb.close("}");
        }
        scb.close("}");
    }

}
