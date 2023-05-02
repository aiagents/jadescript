package it.unipr.ailab.sonneteer.statement;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public class BreakStatementWriter extends StatementWriter {

    private final boolean isContinue;


    public BreakStatementWriter() {
        isContinue = false;
    }


    public BreakStatementWriter(boolean isContinue) {
        this.isContinue = isContinue;
    }


    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        if (isContinue) {
            s.line("continue;");
        } else {
            s.line("break;");
        }
    }


    public boolean isContinue() {
        return isContinue;
    }

}
