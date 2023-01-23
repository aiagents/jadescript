package it.unipr.ailab.sonneteer.statement;

import it.unipr.ailab.sonneteer.Writer;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;

public interface LocalVarBindingProvider {
    LocalVarBindingProvider DEFAULT_VAR_BINDING_PROVIDER = new LocalVarBindingProvider() {
        @Override
        public VariableDeclarationWriter bindDeclaration(
                String chosenType,
                String varName,
                ExpressionWriter nullableInitExpression
        ) {
            return nullableInitExpression != null
                    ? Writer.w.variable(chosenType, varName, nullableInitExpression)
                    : Writer.w.variable(chosenType, varName);
        }

        @Override
        public StatementWriter bindWrite(String varName, ExpressionWriter expression) {
            return Writer.w.assign(varName, expression);
        }

        @Override
        public String bindRead(String varName) {
            return "/*defaultbinding*/"+varName;
        }
    };

    VariableDeclarationWriter bindDeclaration(
            String chosenType,
            String varName,
            ExpressionWriter nullableInitExpression
    );

    StatementWriter bindWrite(String varName, ExpressionWriter expression);

    String bindRead(String varName);


}
