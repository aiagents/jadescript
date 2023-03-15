package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import jadescript.lang.Performative;
import org.jetbrains.annotations.NotNull;

public interface MessageReceivedContext extends SemanticsConsts {


    @NotNull
    private static ContextGeneratedName
    messageContentContextGeneratedName(
        IJadescriptType messageType,
        IJadescriptType contentType
    ) {
        return new ContextGeneratedName(
            "content",
            contentType,
            () -> "(" + messageType.compileAsJavaCast() + " "
                + MESSAGE_VAR_NAME + ")" +
                ".getContent(" +
                CompilationHelper.compileAgentReference() +
                ".getContentManager())"
        );
    }

    @NotNull
    private static ContextGeneratedName messageContextGeneratedName(
        IJadescriptType messageType
    ) {
        return new ContextGeneratedName(
            "message",
            messageType,
            () -> "(" + messageType.compileAsJavaCast() +
                " " + MESSAGE_VAR_NAME + ")"
        );
    }


    Maybe<Performative> getPerformative();

    IJadescriptType getMessageContentType();

    IJadescriptType getMessageType();

    default CompilableName getMessageName() {
        return messageContextGeneratedName(getMessageType());
    }

    default CompilableName getContentName() {
        return messageContentContextGeneratedName(
            getMessageType(),
            getMessageContentType()
        );
    }

    default void debugDumpReceivedMessage(SourceCodeBuilder scb) {
        scb.open("--> is MessageReceivedContext {");
        scb.line("performative = " + getPerformative());
        scb.line("messageContentType = " + getMessageContentType()
            .getDebugPrint());
        scb.line("messageType = " + getMessageType().getDebugPrint());
        scb.close("}");
    }

}
