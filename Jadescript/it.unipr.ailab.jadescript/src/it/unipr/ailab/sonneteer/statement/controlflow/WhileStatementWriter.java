package it.unipr.ailab.sonneteer.statement.controlflow;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.LocalVarBindingProvider;
import it.unipr.ailab.sonneteer.statement.StatementWriter;

import java.util.function.Consumer;

public class WhileStatementWriter extends StatementWriter implements LoopWriter{
    private final ExpressionWriter condition;
    private final StatementWriter body;

    public WhileStatementWriter(ExpressionWriter condition, StatementWriter body){
        this.condition = condition;
        this.body = body;
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        s.add("while(");
        condition.writeSonnet(s);
        s.spaced(")");
        body.writeSonnet(s);
    }

    @Override
    public void getSubBlocks(Consumer<BlockWriter> statementAcceptor) {
        if(body instanceof BlockWriter) {
            statementAcceptor.accept(((BlockWriter) body));
        }
    }

    @Override
    public StatementWriter bindLocalVarUsages(LocalVarBindingProvider bindingProvider) {
        return w.whileStmnt(
                condition.bindVariableUsages(bindingProvider),
                body.bindLocalVarUsages(bindingProvider)
        );
    }
}
