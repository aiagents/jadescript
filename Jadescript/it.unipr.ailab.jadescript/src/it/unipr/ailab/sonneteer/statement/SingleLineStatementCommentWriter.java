package it.unipr.ailab.sonneteer.statement;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public class SingleLineStatementCommentWriter extends StatementWriter {

    private final String comment;

    public SingleLineStatementCommentWriter(String comment){
        this.comment = comment;
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        s.line("//"+comment);
    }

}
