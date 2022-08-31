package it.unipr.ailab.sonneteer.classmember;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.expression.SimpleExpressionWriter;
import it.unipr.ailab.sonneteer.qualifiers.Visibility;
import it.unipr.ailab.sonneteer.statement.ReturnStatementWriter;

/**
 * Writer for class fields. It has a type, a name and can have an expression
 * for initialization.
 */
public class FieldWriter extends ClassMemberWriter {


    private final String type;
    private final String name;
    private ExpressionWriter initExpression = null;

    public FieldWriter(Visibility visibility, boolean isStatic, boolean isFinal,
                       String type, String name){
        super(visibility, isFinal, isStatic);
        this.type = type;
        this.name = name;
    }

    public FieldWriter(Visibility visibility, boolean isStatic, boolean isFinal,
                       String type, String name, ExpressionWriter initExpression){
        this(visibility, isStatic, isFinal, type, name);
        this.initExpression = initExpression;
    }


    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        getAnnotations().forEach(s::line);
        getVisibility().writeSonnet(s);
        if(isFinal()) s.spaced("final");
        if(isStatic()) s.spaced("static");
        s.spaced(type).add(name);
        if(initExpression!=null) {
            s.spaced(" = ");
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

    public MethodWriter generateGetter(){
        String firstCapitalLetterName = name.substring(0,1).toUpperCase()+name.substring(1);
        MethodWriter getter = new MethodWriter(Visibility.PUBLIC, false, isStatic(), getType(), "get" + firstCapitalLetterName);
        getter.getBody().addStatement(new ReturnStatementWriter(new SimpleExpressionWriter(
                (isStatic() ? "" : "this.") + name)));
        return getter;
    }

}
