package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.UserLocalDefinition;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.FlowSensitiveSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.LocalName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

/**
 * A local variable, created explicitly by the programmer with an initial
 * assignment.
 */
public final class LocalVariable implements LocalName, FlowSensitiveSymbol {

    private final String name;
    private final IJadescriptType declaredType;


    private LocalVariable(String name, IJadescriptType declaredType) {
        this.name = name;
        this.declaredType = declaredType;
    }


    public static LocalVariable localVariable(
        String name,
        IJadescriptType declaredType
    ) {
        return new LocalVariable(name, declaredType);
    }


    @Override
    public String compileRead(BlockElementAcceptor acceptor) {
        return name;
    }


    @Override
    public void compileWrite(String rexpr, BlockElementAcceptor acceptor) {
        acceptor.accept(w.assign(
            name,
            w.expr(rexpr)
        ));
    }


    @Override
    public SearchLocation sourceLocation() {
        return UserLocalDefinition.getInstance();
    }


    @Override
    public String name() {
        return name;
    }


    @Override
    public IJadescriptType readingType() {
        return declaredType;
    }


    @Override
    public IJadescriptType writingType() {
        return declaredType;
    }


    @Override
    public boolean canWrite() {
        return true;
    }


    @Override
    public ExpressionDescriptor descriptor() {
        return new ExpressionDescriptor.PropertyChain(name);
    }




}
