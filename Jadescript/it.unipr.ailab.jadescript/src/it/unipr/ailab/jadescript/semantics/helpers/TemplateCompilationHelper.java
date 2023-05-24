package it.unipr.ailab.jadescript.semantics.helpers;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.WriterFactory;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.expression.MethodCallExpressionWriter;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import jade.content.lang.leap.LEAPCodec;

public class TemplateCompilationHelper implements SemanticsConsts {

    private static final WriterFactory w = WriterFactory.getInstance();


    public static ExpressionWriter True() {
        return customMessage("__ignored", w.block()
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
            w.expr("jadescript.core.message.Message." +
                performativeSafe.toUpperCase())
        );
    }


    public static MethodCallExpressionWriter notNative() {
        return w.callExpr(
            "jadescript.core.nativeevent.NotNativeEventTemplate.MatchNotNative",
            w.expr(CompilationHelper.compileAgentReference() +
                ".getContentManager()")
        );
    }


    public static MethodCallExpressionWriter isNative() {
        return w.callExpr(
            "jadescript.lang.acl.ContentMessageTemplate.MatchClass",
            w.expr(CompilationHelper.compileAgentReference() +
                ".getContentManager()"),
            w.expr("jadescript.core.nativeevent.NativeEvent.class")
        );
    }


    public static MethodCallExpressionWriter isStale() {
        return w.callExpr(
            "jadescript.lang.acl.StaleMessageTemplate.matchStale",
            w.expr("() -> " + CompilationHelper.compileAgentReference())
        );
    }

    public static MethodCallExpressionWriter isRightCodec() {
        return w.callExpr(
            "jade.lang.acl.MessageTemplate.MatchLanguage",
            w.expr(CODEC_VAR_NAME + ".getName()")
        );
    }


    public static ExpressionWriter customMessage(
        String messageVarName,
        BlockWriter predicateBlock
    ) {
        SourceCodeBuilder scb = new SourceCodeBuilder();
        predicateBlock.writeSonnet(scb);
        return w.expr(
            "new jade.lang.acl.MessageTemplate(new jadescript.lang.acl" +
                ".CustomMessageTemplate(" +
                "((java.util.function.Predicate<jade.lang.acl.ACLMessage>) (" +
                messageVarName + ") -> {" + scb + "})))"
        );
    }


    public static ExpressionWriter customNativeEvent(
        BlockWriter predicateBlock
    ) {
        SourceCodeBuilder scb = new SourceCodeBuilder();
        predicateBlock.writeSonnet(scb);
        return w.expr(
            "new jade.lang.acl.MessageTemplate(new jadescript.lang.acl" +
                ".CustomMessageTemplate(" +
                "((java.util.function.Predicate<jade.lang.acl.ACLMessage>) (" +
                NATIVE_EVENT_VAR_NAME + ") -> {" + scb + "})))"
        );
    }

}
