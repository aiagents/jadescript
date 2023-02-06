package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedReference;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import jadescript.lang.Performative;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public interface MessageReceivedContext extends SemanticsConsts {


    @NotNull
    private static ContextGeneratedReference
    messageContentContextGeneratedReference(
        IJadescriptType messageType,
        IJadescriptType contentType
    ) {
        return new ContextGeneratedReference(
            "content",
            contentType,
            (__) -> "(" + messageType.compileAsJavaCast() + " "
                + MESSAGE_VAR_NAME + ")" +
                ".getContent(" + THE_AGENT + "().getContentManager())"
        );
    }

    @NotNull
    private static ContextGeneratedReference messageContextGeneratedReference(
         IJadescriptType messageType
    ){
        return new ContextGeneratedReference(
            "message",
            messageType,
            (__) -> "(" + messageType.compileAsJavaCast() +
                " " + MESSAGE_VAR_NAME + ")"
        );
    }


    Maybe<Performative> getPerformative();

    IJadescriptType getMessageContentType();

    IJadescriptType getMessageType();

    default Stream<NamedSymbol> getMessageStream(
        Predicate<String> name,
        Predicate<IJadescriptType> readingType,
        Predicate<Boolean> canWrite
    ) {
        Stream<Integer> mess = Stream.of(0);
        mess = safeFilter(mess, __ -> "message", name);
        final IJadescriptType messageType = getMessageType();
        mess = safeFilter(mess, __ -> messageType, readingType);
        mess = safeFilter(mess, __ -> true, canWrite);
        return mess.map(__ -> messageContextGeneratedReference(messageType));
    }

    default Stream<NamedSymbol> getContentStream(
        Predicate<String> name,
        Predicate<IJadescriptType> readingType,
        Predicate<Boolean> canWrite
    ) {
        Stream<Integer> cont = Stream.of(0);
        cont = safeFilter(cont, __ -> "content", name);
        cont = safeFilter(cont, __ -> getMessageContentType(), readingType);
        cont = safeFilter(cont, __ -> true, canWrite);
        return cont.map(__ -> messageContentContextGeneratedReference(
            getMessageType(),
            getMessageContentType()
        ));
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
