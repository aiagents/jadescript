package it.unipr.ailab.sonneteer.statement;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;

public class ThrowStatementWriter extends StatementWriter {

    private final ExpressionWriter expression;


    public ThrowStatementWriter(ExpressionWriter expression) {
        this.expression = expression;
    }


    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        s.spaced("throw");
        if (expression != null) {
            expression.writeSonnet(s);
        }
        s.line(";");
    }


    public ExpressionWriter getExpression() {
        return expression;
    }


}
