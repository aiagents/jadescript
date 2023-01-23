package it.unipr.ailab.sonneteer.statement;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;


public class AssignmentWriter extends StatementWriter {

    private final String leftSide;
    private final ExpressionWriter rightSide;

    public AssignmentWriter(String leftSide, ExpressionWriter rightSide){
        this.leftSide = leftSide;
        this.rightSide = rightSide;
    }

    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        getComments().forEach(x -> x.writeSonnet(s));
        s.spaced(leftSide).spaced("=");
        rightSide.writeSonnet(s);
        s.line(";");
    }

}
