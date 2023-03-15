package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.UserLocalDefinition;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.FlowSensitiveSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.LocalName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

public final class ActualParameter implements LocalName, FlowSensitiveSymbol {

    private final String name;
    private final IJadescriptType type;


    private ActualParameter(String name, IJadescriptType type) {
        this.name = name;
        this.type = type;
    }

    public static ActualParameter actualParameter(
        String name,
        IJadescriptType type
    ){
        return new ActualParameter(name, type);
    }


    @Override
    public String compileRead(BlockElementAcceptor acceptor) {
        return name;
    }


    @Override
    public void compileWrite(String rexpr, BlockElementAcceptor acceptor) {
        acceptor.accept(w.assign(name, w.expr(rexpr)));
    }


    @Override
    public ExpressionDescriptor descriptor() {
        return new ExpressionDescriptor.PropertyChain(name);
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
        return type;
    }


    @Override
    public boolean canWrite() {
        return true;
    }

}
