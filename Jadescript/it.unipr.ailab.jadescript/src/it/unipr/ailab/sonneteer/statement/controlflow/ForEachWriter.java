package it.unipr.ailab.sonneteer.statement.controlflow;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.LocalVarBindingProvider;
import it.unipr.ailab.sonneteer.statement.StatementWriter;

import java.util.function.Consumer;

public class ForEachWriter extends StatementWriter implements LoopWriter{

    private final String varType;
    private final String varName;
    private final ExpressionWriter iterable;
    private final StatementWriter body;

    public ForEachWriter(String varType, String varName, ExpressionWriter iterable, StatementWriter body){
        this.varType = varType;
        this.varName = varName;
        this.iterable = iterable;
        this.body = body;
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        s.add("for ( ").spaced(varType).spaced(varName).spaced(":");
        iterable.writeSonnet(s);
        s.spaced(")");
        body.writeSonnet(s);
    }

    public StatementWriter getBody() {
        return body;
    }

    public ExpressionWriter getIterable() {
        return iterable;
    }

    public String getVarName() {
        return varName;
    }

    public String getVarType() {
        return varType;
    }

    @Override
    public void getSubBlocks(Consumer<BlockWriter> statementAcceptor) {
        if(body instanceof BlockWriter) {
            statementAcceptor.accept(((BlockWriter) body));
        }
    }

    @Override
    public StatementWriter bindLocalVarUsages(LocalVarBindingProvider bindingProvider) {
        return w.foreach(
                varType,
                varName,
                iterable.bindVariableUsages(bindingProvider),
                body.bindLocalVarUsages(bindingProvider)
        );
    }
}
