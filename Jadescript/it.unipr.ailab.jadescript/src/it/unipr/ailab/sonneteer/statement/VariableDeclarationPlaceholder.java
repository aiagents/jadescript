package it.unipr.ailab.sonneteer.statement;

import it.unipr.ailab.sonneteer.expression.ExpressionWriter;

public class VariableDeclarationPlaceholder implements BlockWriterElement {
    private final String chosenType;
    private final String chosenName;
    private final ExpressionWriter expressionWriter;

    public VariableDeclarationPlaceholder(
            String chosenType,
            String chosenName,
            ExpressionWriter expressionWriter
    ) {
        this.chosenType = chosenType;
        this.chosenName = chosenName;
        this.expressionWriter = expressionWriter;
    }

    public String getChosenType() {
        return chosenType;
    }

    public String getChosenName() {
        return chosenName;
    }


    public ExpressionWriter getExpressionWriter() {
        return expressionWriter;
    }
}
