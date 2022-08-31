package it.unipr.ailab.jadescript.semantics.helpers;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.WriterFactory;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.expression.MethodCallExpressionWriter;
import it.unipr.ailab.sonneteer.statement.BlockWriter;

public class TemplateCompilationHelper implements SemanticsConsts {
    private static final WriterFactory w = WriterFactory.getInstance();

    public static ExpressionWriter True() {
        return customMessage(w.block()
                .addStatement(w.returnStmnt(w.True)));
    }

    public static MethodCallExpressionWriter and(
            ExpressionWriter op1,
            ExpressionWriter op2
    ) {
        return w.callExpr("jade.lang.acl.MessageTemplate.and", op1, op2);
    }

    public static MethodCallExpressionWriter or(
            ExpressionWriter op1,
            ExpressionWriter op2
    ) {
        return w.callExpr("jade.lang.acl.MessageTemplate.or", op1, op2);
    }

    public static MethodCallExpressionWriter performative(
            String performativeSafe
    ) {
        return w.callExpr(
                "jade.lang.acl.MessageTemplate.MatchPerformative",
                w.expr("jadescript.core.message.Message." + performativeSafe.toUpperCase())
        );
    }

    public static MethodCallExpressionWriter notPercept() {
        return w.callExpr(
                "jadescript.core.percept.NotPerceptMessageTemplate.MatchNotPercept",
                w.expr(THE_AGENT + "().getContentManager()")
        );
    }

    public static MethodCallExpressionWriter isPercept() {
        return w.callExpr(
                "jadescript.lang.acl.ContentMessageTemplate.MatchClass",
                w.expr(THE_AGENT + "().getContentManager()"),
                w.expr("jadescript.core.percept.Percept.class")
        );
    }

    public static MethodCallExpressionWriter isStale() {
        return w.callExpr(
                "jadescript.lang.acl.StaleMessageTemplate.matchStale",
                w.expr("() -> " + THE_AGENT + "()")
        );
    }

    public static ExpressionWriter customMessage(
            BlockWriter predicateBlock
    ) {
        SourceCodeBuilder scb = new SourceCodeBuilder();
        predicateBlock.writeSonnet(scb);
        return w.expr(
                "new jade.lang.acl.MessageTemplate(new jadescript.lang.acl.CustomMessageTemplate(" +
                        "((java.util.function.Predicate<jade.lang.acl.ACLMessage>) (" + MESSAGE_VAR_NAME + ") -> {" +
                        scb +
                        "})" +
                        "))"
        );
    }

    public static ExpressionWriter customPercept(
            BlockWriter predicateBlock
    ) {
        SourceCodeBuilder scb = new SourceCodeBuilder();
        predicateBlock.writeSonnet(scb);
        return w.expr(
                "new jade.lang.acl.MessageTemplate(new jadescript.lang.acl.CustomMessageTemplate(" +
                        "((java.util.function.Predicate<jade.lang.acl.ACLMessage>) (" + PERCEPT_VAR_NAME + ") -> {" +
                        scb +
                        "})" +
                        "))"
        );
    }
}
