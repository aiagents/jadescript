package it.unipr.ailab.sonneteer.statement;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.expression.SimpleExpressionWriter;

public class SimpleStatementWriter extends StatementWriter {

    private final String stmt;

    public SimpleStatementWriter(String stmt){
        this.stmt = stmt;
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        s.add(stmt).line(";");
    }



    @Override
    public StatementWriter bindLocalVarUsages(LocalVarBindingProvider bindingProvider) {
        return w.simpleStmt(SimpleExpressionWriter.replacePlaceholderInString(stmt, bindingProvider::bindRead));
    }
}
