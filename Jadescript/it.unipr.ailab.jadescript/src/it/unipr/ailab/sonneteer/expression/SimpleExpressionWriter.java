package it.unipr.ailab.sonneteer.expression;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.LocalVarBindingProvider;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SimpleExpressionWriter extends ExpressionWriter {

    private final String expression;

    public SimpleExpressionWriter(String expression){
        this.expression = expression;
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        s.add(expression);
    }

    public String getExpression() {
        return expression;
    }


    @Override
    public ExpressionWriter bindVariableUsages(LocalVarBindingProvider varBindingProvider) {
        return w.expr(replacePlaceholderInString(expression, varBindingProvider::bindRead));
    }


    public static String replacePlaceholderInString(String text, Function<String, String> replacer){
        Pattern contextPattern = Pattern.compile("#\\{[\\w.]+\\}");
        Matcher m = contextPattern .matcher(text);
        while(m.find()){
            String currentGroup = m.group();
            String currentPattern = currentGroup.replaceAll("^#\\{", "").replaceAll("\\}$", "").trim();
            String mapValue = replacer.apply(currentPattern);
            if (mapValue != null){
                text = text.replace(currentGroup, mapValue);
            }
        }
        return text;
    }
}
