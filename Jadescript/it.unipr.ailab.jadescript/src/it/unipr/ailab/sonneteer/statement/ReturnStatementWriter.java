package it.unipr.ailab.sonneteer.statement;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;

public class ReturnStatementWriter extends StatementWriter {

    private final ExpressionWriter expression;

    public ReturnStatementWriter(ExpressionWriter expression){
        this.expression = expression;
    }

    public ReturnStatementWriter(){
        expression = null;
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        s.spaced("return");
        if(expression != null) expression.writeSonnet(s);
        s.line(";");
    }

    public ExpressionWriter getExpression() {
        return expression;
    }



    @Override
    public StatementWriter bindLocalVarUsages(LocalVarBindingProvider bindingProvider) {
        if(expression!=null){
            return w.returnStmnt(expression.bindVariableUsages(bindingProvider));
        }else{
            return this;
        }
    }
}
