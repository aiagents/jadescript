package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.sonneteer.WriterFactory;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import it.unipr.ailab.sonneteer.statement.VariableDeclarationWriter;

import java.util.function.Consumer;

public enum VariableCompilationMode {
    LOCALVAR {
        @Override
        public VariableDeclarationWriter bindDeclaration(UserVariable variable, ExpressionWriter nullableInitExpression) {
            return w.variable(
                    variable.readingType().compileToJavaTypeReference(),
                    variable.compileRead(""),
                    nullableInitExpression
            ).setFinal(!variable.canWrite());
        }

        @Override
        public StatementWriter bindWrite(UserVariable variable, ExpressionWriter expression) {
            return w.assign(variable.compileRead(""), expression);
        }

        @Override
        public String bindRead(UserVariable variable) {
            return variable.compileRead("");
        }

        @Override
        public String bindReadInClosure(UserVariable variable) {
            return "/*captured*/" + bindRead(variable);
        }

        @Override
        public StatementWriter bindWriteInClosure(UserVariable variable, ExpressionWriter expressionWriter) {
            final StatementWriter statementWriter = bindWrite(variable, expressionWriter);
            statementWriter.addComment(" - captured:");
            return statementWriter;
        }

        @Override
        public void prepareEnteringClosure(
                UserVariable variable,
                Consumer<StatementWriter> prepStatements
        ) {
            // do nothing
        }
    },

    FINALIZEDCOPY {
        @Override
        public VariableDeclarationWriter bindDeclaration(
                UserVariable variable,
                ExpressionWriter nullableInitExpression
        ) {
            return LOCALVAR.bindDeclaration(variable, nullableInitExpression);
        }

        @Override
        public StatementWriter bindWrite(UserVariable variable, ExpressionWriter expression) {
            return LOCALVAR.bindWrite(variable, expression);
        }

        @Override
        public String bindRead(UserVariable variable) {
            return LOCALVAR.bindRead(variable);
        }

        @Override
        public String bindReadInClosure(UserVariable variable) {
            return "__finalized_" + variable.compileRead("");
        }

        @Override
        public StatementWriter bindWriteInClosure(
                UserVariable variable,
                ExpressionWriter expressionWriter
        ) {
            return LOCALVAR.bindWriteInClosure(variable, expressionWriter);
        }

        @Override
        public void prepareEnteringClosure(
                UserVariable variable,
                Consumer<StatementWriter> prepStatements
        ) {
            prepStatements.accept(w.variable(
                    variable.readingType().compileToJavaTypeReference(),
                    "__finalized_" + variable.name(),
                    w.expr(variable.name())
            ).setFinal(!variable.canWrite()));

        }
    },

    ATOMICREF {
        @Override
        public VariableDeclarationWriter bindDeclaration(UserVariable variable, ExpressionWriter nullableInitExpression) {
            return w.variable(
                    "java.util.concurrent.atomic.AtomicReference<"
                            + variable.readingType().compileToJavaTypeReference() + ">",
                    variable.name(),
                    w.callExpr("new java.util.concurrent.atomic.AtomicReference<>", nullableInitExpression)
            ).setFinal();
        }

        @Override
        public StatementWriter bindWrite(UserVariable variable, ExpressionWriter expression) {
            return w.callStmnt(variable.name() + ".set", expression);
        }

        @Override
        public String bindRead(UserVariable variable) {
            return variable.name() + ".get()";
        }

        @Override
        public String bindReadInClosure(UserVariable variable) {
            return "/*captured*/" + bindRead(variable);
        }

        @Override
        public StatementWriter bindWriteInClosure(UserVariable variable, ExpressionWriter expressionWriter) {
            final StatementWriter statementWriter = bindWrite(variable, expressionWriter);
            statementWriter.addComment(" - captured:");
            return statementWriter;
        }

        @Override
        public void prepareEnteringClosure(UserVariable variable, Consumer<StatementWriter> prepStatements) {
            // do nothing
        }
    },

    ;


    private static final WriterFactory w = WriterFactory.getInstance();

    public abstract VariableDeclarationWriter bindDeclaration(
            UserVariable variable,
            ExpressionWriter nullableInitExpression
    );

    public abstract StatementWriter bindWrite(
            UserVariable variable,
            ExpressionWriter expression
    );

    public abstract String bindRead(UserVariable variable);

    public abstract String bindReadInClosure(UserVariable variable);

    public abstract StatementWriter bindWriteInClosure(
            UserVariable variable,
            ExpressionWriter expressionWriter
    );

    public abstract void prepareEnteringClosure(
            UserVariable variable,
            Consumer<StatementWriter> prepStatements
    );

    public boolean isMoreRequiringThan(VariableCompilationMode other) {
        return this.ordinal() - other.ordinal() > 0;
    }

}
