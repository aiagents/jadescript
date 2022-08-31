package it.unipr.ailab.sonneteer.statement;

import it.unipr.ailab.sonneteer.expression.ExpressionWriter;

public class VariableAssignmentPlaceholder implements BlockWriterElement {
    private final String varName;
    private final ExpressionWriter expression;


    public VariableAssignmentPlaceholder(String varName, ExpressionWriter expression) {
        this.varName = varName;
        this.expression = expression;
    }

    public String getVarName() {
        return varName;
    }

    public ExpressionWriter getExpression() {
        return expression;
    }
}
