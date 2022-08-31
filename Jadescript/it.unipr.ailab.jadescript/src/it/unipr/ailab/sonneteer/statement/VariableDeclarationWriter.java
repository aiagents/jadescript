package it.unipr.ailab.sonneteer.statement;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;

public class VariableDeclarationWriter extends StatementWriter {

    private boolean isFinal = false;
    private final String type;
    private final String name;
    private ExpressionWriter initExpression = null;

    public VariableDeclarationWriter(String type, String name){
        this.type = type;
        this.name = name;
    }

    public VariableDeclarationWriter(String type, String name, ExpressionWriter initExpression){
        this.type = type;
        this.name = name;
        this.initExpression = initExpression;
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        if(isFinal){
            s.spaced("final");
        }
        s.spaced(type).add(name);
        if(initExpression!=null){
            s.spaced(" =");
            initExpression.writeSonnet(s);
        }
        s.line(";");
    }

    public ExpressionWriter getInitExpression() {
        return initExpression;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }


    public VariableDeclarationWriter setFinal(boolean isFinal){
        this.isFinal = isFinal;
        return this;
    }

    public VariableDeclarationWriter setFinal(){
        this.isFinal = true;
        return this;
    }

    @Override
    public StatementWriter bindLocalVarUsages(LocalVarBindingProvider bindingProvider) {
        if(initExpression == null) {
            return this;
        }else{
            return w.variable(type, name, initExpression.bindVariableUsages(bindingProvider));
        }
    }
}
