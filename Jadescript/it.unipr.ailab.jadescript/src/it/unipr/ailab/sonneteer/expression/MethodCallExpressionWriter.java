package it.unipr.ailab.sonneteer.expression;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.LocalVarBindingProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MethodCallExpressionWriter extends ExpressionWriter {

    private final String methodName;
    private final List<ExpressionWriter> parameters;

    public MethodCallExpressionWriter(
        String methodName,
        ExpressionWriter... parameters
    ){
        this.methodName = methodName;
        this.parameters = new ArrayList<>();
        this.parameters.addAll(Arrays.asList(parameters));
    }

    public MethodCallExpressionWriter(
        String methodName,
        List<ExpressionWriter> parameters
    ){
        this.methodName = methodName;
        this.parameters = new ArrayList<>(parameters);
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        s.add(methodName).add("(");
        for(int i = 0; i < parameters.size(); i++){
            ExpressionWriter p = parameters.get(i);
            p.writeSonnet(s);
            if(i != parameters.size()-1) {
                s.spaced(",");
            }
        }
        s.add(")");
    }

    public String getMethodName() {
        return methodName;
    }

    public List<ExpressionWriter> getParameters() {
        return parameters;
    }

    @Override
    public ExpressionWriter bindVariableUsages(
            LocalVarBindingProvider bindingProvider
    ) {
        return w.callExpr(methodName, parameters.stream()
                .map(ew -> ew.bindVariableUsages(bindingProvider))
                .toArray(ExpressionWriter[]::new)
        );
    }
}
