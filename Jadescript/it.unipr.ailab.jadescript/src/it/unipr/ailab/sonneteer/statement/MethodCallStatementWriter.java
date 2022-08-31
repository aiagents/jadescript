package it.unipr.ailab.sonneteer.statement;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static it.unipr.ailab.sonneteer.expression.SimpleExpressionWriter.replacePlaceholderInString;

public class MethodCallStatementWriter extends StatementWriter {

    private final String methodName;
    private final List<ExpressionWriter> parameters;

    public MethodCallStatementWriter(String methodName, ExpressionWriter... parameters){
        this.methodName = methodName;
        this.parameters = new ArrayList<>();
        this.parameters.addAll(Arrays.asList(parameters));
    }

    public MethodCallStatementWriter(String methodName, List<ExpressionWriter> params) {
        this.methodName = methodName;
        this.parameters = new ArrayList<>(params);
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        s.add(methodName).add("(");
        for(int i = 0; i < parameters.size(); i++){
            ExpressionWriter p = parameters.get(i);
            p.writeSonnet(s);
            if(i != parameters.size()-1)
                s.spaced(",");
        }
        s.add(")").line(";");
    }

    public String getMethodName() {
        return methodName;
    }

    public List<ExpressionWriter> getParameters() {
        return parameters;
    }



    @Override
    public StatementWriter bindLocalVarUsages(LocalVarBindingProvider bindingProvider) {
        return w.callStmnt(replacePlaceholderInString(methodName, bindingProvider::bindRead), parameters.stream()
                        .map(ew -> ew.bindVariableUsages(bindingProvider))
                        .toArray(ExpressionWriter[]::new)
        );
    }
}
