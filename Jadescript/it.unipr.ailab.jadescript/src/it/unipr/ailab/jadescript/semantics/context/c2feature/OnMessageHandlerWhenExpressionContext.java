package it.unipr.ailab.jadescript.semantics.context.c2feature;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.CallableMember;
import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.NameMember;
import it.unipr.ailab.jadescript.semantics.context.symbol.SymbolUtils;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.NamespaceWithMembers;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import jadescript.lang.Performative;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class OnMessageHandlerWhenExpressionContext
    extends HandlerWhenExpressionContext
    implements NameMember.Namespace, CallableMember.Namespace,
    MessageReceivedContext {


    private final Maybe<Performative> performative;
    private final LazyValue<NamespaceWithMembers> messageNamespace;


    public OnMessageHandlerWhenExpressionContext(
        SemanticsModule module,
        Maybe<Performative> performative,
        ProceduralFeatureContainerContext outer
    ) {
        super(module, outer);
        this.messageNamespace =
            new LazyValue<>(() -> getMessageType().namespace());
        this.performative = performative;
    }


    @Override
    public IJadescriptType getMessageContentType() {
        return getPerformative()
            .__(module.get(TypeHelper.class)::getContentBound)
            .orElseGet(() -> module.get(TypeHelper.class).ANY);
    }


    @Override
    public IJadescriptType getMessageType() {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (performative.isPresent()) {
            final List<TypeArgument> a = new ArrayList<>(
                typeHelper.unpackTuple(getMessageContentType())
            );
            return typeHelper
                .getMessageType(performative.toNullable()).apply(a);
        } else {
            return typeHelper.ANYMESSAGE;
        }
    }


    @Override
    public Maybe<Performative> getPerformative() {
        return performative;
    }


    @Override
    public Stream<? extends NameMember> searchName(
        Predicate<String> name,
        Predicate<IJadescriptType> readingType,
        Predicate<Boolean> canWrite
    ) {


        return Streams.concat(
            getContentStream(
                name,
                readingType,
                canWrite
            ),
            getMessageStream(
                name,
                readingType,
                canWrite
            ),
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
        scb.line("--> is OnMessageHandlerWhenExpressionContext");
        debugDumpReceivedMessage(scb);
    }


    @Override
    public String getCurrentOperationLogName() {
        return "<evaluating when-expression>";
    }

}
