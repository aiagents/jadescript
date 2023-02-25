package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.UserLocalDefinition;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.FlowSensitiveSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.LocalName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

public class PatternMatchUnifiedVariable
    implements LocalName, FlowSensitiveSymbol {

    private final String name;
    private final IJadescriptType type;
    private final String rootPatternMatchVariableName;
    private final ExpressionDescriptor descriptor;


    public PatternMatchUnifiedVariable(
        String name,
        IJadescriptType type,
        String rootPatternMatchVariableName,
        ExpressionDescriptor descriptor
    ) {
        this.name = name;
        this.type = type;
        this.rootPatternMatchVariableName = rootPatternMatchVariableName;
        this.descriptor = descriptor;
    }


    @Override
    public String compileRead(BlockElementAcceptor acceptor) {
        return rootPatternMatchVariableName + "." + name;
    }


    @Override
    public void compileWrite(String rexpr, BlockElementAcceptor acceptor) {
        acceptor.accept(
            w.assign("/*internal error*/" + name, w.expr(rexpr))
        );
    }


    @Override
    public ExpressionDescriptor descriptor() {
        return descriptor;
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
        return false;
    }

}
