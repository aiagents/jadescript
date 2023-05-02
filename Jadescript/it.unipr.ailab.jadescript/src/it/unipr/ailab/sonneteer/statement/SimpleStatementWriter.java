package it.unipr.ailab.sonneteer.statement;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public class SimpleStatementWriter extends StatementWriter {

    private final String stmt;


    public SimpleStatementWriter(String stmt) {
        this.stmt = stmt;
    }


    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        s.add(stmt).line(";");
    }

}
