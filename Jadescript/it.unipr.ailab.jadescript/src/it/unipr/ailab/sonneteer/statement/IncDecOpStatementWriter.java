package it.unipr.ailab.sonneteer.statement;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public class IncDecOpStatementWriter extends StatementWriter {


    @Override
    public StatementWriter bindLocalVarUsages(LocalVarBindingProvider bindingProvider) {
        switch (mode){
            case DEC:
                return bindingProvider.bindWrite(varName, w.expr(bindingProvider.bindRead(varName) + "- 1"));
            case INC:
            default:
                return bindingProvider.bindWrite(varName, w.expr(bindingProvider.bindRead(varName) + "+ 1"));
        }
    }

    public enum Mode{
        INC, DEC
    }

    private final Mode mode;
    private final String varName;


    public IncDecOpStatementWriter(Mode mode, String varName){

        this.mode = mode;
        this.varName = varName;
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        switch (mode){
            case INC:
                s.add("++").add(varName).line(";");
                break;
            case DEC:
                s.add("--").add(varName).line(";");
                break;
        }
    }

    public Mode getMode() {
        return mode;
    }

    public String getVarName() {
        return varName;
    }
}
