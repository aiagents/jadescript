package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import it.unipr.ailab.sonneteer.statement.VariableDeclarationWriter;

import java.util.function.Consumer;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;

public class UserVariable extends LocalVariable {

    private VariableCompilationMode compilationMode =
        VariableCompilationMode.LOCALVAR;
    private boolean neverOverwritten = true;
    private boolean neverRead = true;
    private Maybe<UserVariable> capturedVariable = nothing();


    public UserVariable(String name, IJadescriptType type, boolean canWrite) {
        super(name, type, canWrite);
    }


    public static UserVariable asCaptured(UserVariable captured) {
        UserVariable variable = new UserVariable(
                captured.name(),
                captured.readingType(),
                captured.canWrite()
        );
        variable.capturedVariable = some(captured);
        return variable;
    }

    private VariableCompilationMode getCompilationMode() {
        return capturedVariable.__(UserVariable::getCompilationMode)
            .orElse(compilationMode);
    }

    private void changeCompilationMode(VariableCompilationMode newMode) {
        if (capturedVariable.isPresent()) {
            var cv = capturedVariable.toNullable();
            cv.changeCompilationMode(newMode);
        } else {
            compilationMode = newMode;
        }
    }

    public VariableDeclarationWriter bindDeclaration(
            ExpressionWriter nullableInitExpression
    ) {
        return getCompilationMode().bindDeclaration(this, nullableInitExpression);
    }

    public StatementWriter bindWrite(ExpressionWriter expression) {
        return getCompilationMode().bindWrite(this, expression);
    }

    public String bindRead() {
        return getCompilationMode().bindRead(this);
    }

    public String bindReadInClosure() {
        return getCompilationMode().bindReadInClosure(this);
    }

    public void prepareEnteringInClosure(Consumer<StatementWriter> statementAcceptor) {
        getCompilationMode().prepareEnteringClosure(this, statementAcceptor);
    }

    public StatementWriter bindWriteInClosure(
            ExpressionWriter expressionWriter
    ) {
        return getCompilationMode().bindWriteInClosure(this, expressionWriter);
    }


    public boolean isCapturedInAClosure() {
        return capturedVariable.isPresent();
    }

    public void notifyReadUsage() {
        neverRead = false;
        if (canWrite() && isCapturedInAClosure()) {
            promoteCompilationMode(VariableCompilationMode.FINALIZEDCOPY);
        }

        capturedVariable.safeDo(UserVariable::notifyReadUsage);
    }

    public void notifyWriteUsage() {
        neverOverwritten = false;
        if (canWrite() && isCapturedInAClosure()) {
            promoteCompilationMode(VariableCompilationMode.ATOMICREF);
        }

        capturedVariable.safeDo(UserVariable::notifyWriteUsage);
    }

    public void promoteCompilationMode(VariableCompilationMode targetMode) {
        if (targetMode.isMoreRequiringThan(getCompilationMode())) {
            changeCompilationMode(targetMode);
        }
    }

    public boolean isNeverOverwritten() {
        return neverOverwritten;
    }

    public boolean isNeverRead() {
        return neverRead;
    }

}
