package it.unipr.ailab.sonneteer.statement.controlflow;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.statement.LocalVarBindingProvider;
import it.unipr.ailab.sonneteer.statement.StatementWriter;

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


}
