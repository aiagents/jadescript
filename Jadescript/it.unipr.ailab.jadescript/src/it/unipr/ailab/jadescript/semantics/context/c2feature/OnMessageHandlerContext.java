package it.unipr.ailab.jadescript.semantics.context.c2feature;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.CallableMember;
import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.NameMember;
import it.unipr.ailab.jadescript.semantics.context.symbol.SymbolUtils;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.NamespaceWithMembers;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import jadescript.lang.Performative;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class OnMessageHandlerContext
    extends HandlerWithWhenExpressionContext
    implements NameMember.Namespace,
    CallableMember.Namespace,
    MessageReceivedContext {

    private final Maybe<String> performative;
    private final IJadescriptType messageContentType;
    private final IJadescriptType messageType;
    private final LazyValue<NamespaceWithMembers> messageNamespace;


    public OnMessageHandlerContext(
        SemanticsModule module,
        ProceduralFeatureContainerContext outer,
        Maybe<String> performative,
        IJadescriptType messageType,
        IJadescriptType messageContentType
    ) {
        super(module, outer, "message");
        this.performative = performative;
        this.messageContentType = messageContentType;
        this.messageType = messageType;
        this.messageNamespace =
            new LazyValue<>(() -> getMessageType().namespace());
    }


    @Override
    public Maybe<Performative> getPerformative() {
        return performative.__(Performative.performativeByName::get);
    }


    @Override
    public IJadescriptType getMessageContentType() {
        return messageContentType;
    }


    @Override
    public IJadescriptType getMessageType() {
        return messageType;
    }


    @Override
    public Stream<? extends NameMember> searchName(
        Predicate<String> name,
        Predicate<IJadescriptType> readingType,
        Predicate<Boolean> canWrite
    ) {

        final Stream<NameMember> contentStream = getContentStream(
            name,
            readingType,
            canWrite
        );

        final Stream<NameMember> messageStream = getMessageStream(
            name,
            readingType,
            canWrite
        );

        return Streams.concat(
            contentStream,
            messageStream,
            messageNamespace.get().searchName(name, readingType, canWrite)
                .map(ne -> SymbolUtils.setDereferenceByVariable(
                    ne,
                    MESSAGE_VAR_NAME
                ))
        );
    }


    @Override
    public Stream<? extends CallableMember> searchCallable(
        String name,
        Predicate<IJadescriptType> returnType,
        BiPredicate<Integer, Function<Integer, String>> parameterNames,
        BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        return messageNamespace.get().searchCallable(
            name,
            returnType,
            parameterNames,
            parameterTypes
        ).map(ce -> SymbolUtils.setDereferenceByVariable(
            ce,
            MESSAGE_VAR_NAME
        ));
    }


    @Override
    public Stream<? extends CallableMember> searchCallable(
        Predicate<String> name,
        Predicate<IJadescriptType> returnType,
        BiPredicate<Integer, Function<Integer, String>> parameterNames,
        BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        return messageNamespace.get().searchCallable(
                name,
                returnType,
                parameterNames,
                parameterTypes
            )
            .map(ce -> SymbolUtils.setDereferenceByVariable(
                ce,
                MESSAGE_VAR_NAME
            ));
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is OnMessageHandlerContext");
        debugDumpReceivedMessage(scb);
    }


    @Override
    public String getCurrentOperationLogName() {
        return "on " + performative.orElse("message");
    }


}
