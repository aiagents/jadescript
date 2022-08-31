package it.unipr.ailab.jadescript.semantics.context.c2feature;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.SymbolUtils;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.NamespaceWithSymbols;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MessageHandlerWhenExpressionContext
        extends HandlerWhenExpressionContext
        implements NamedSymbol.Searcher, CallableSymbol.Searcher, MessageReceivedContext {
    private final IJadescriptType messageType;
    private final IJadescriptType messageContentType;
    private final LazyValue<NamespaceWithSymbols> messageNamespace;

    public MessageHandlerWhenExpressionContext(
            SemanticsModule module,
            ProceduralFeatureContainerContext outer,
            IJadescriptType messageType,
            IJadescriptType messageContentType
    ) {
        super(module, outer);
        this.messageType = messageType;
        this.messageContentType = messageContentType;
        this.messageNamespace = new LazyValue<>(messageType::namespace);
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
    public Stream<? extends NamedSymbol> searchName(Predicate<String> name, Predicate<IJadescriptType> readingType, Predicate<Boolean> canWrite) {

        final Stream<NamedSymbol> contentStream = getContentStream(name, readingType, canWrite);


        final Stream<NamedSymbol> messageStream = getMessageStream(name, readingType, canWrite);

        return Streams.concat(
                contentStream,
                messageStream,
                messageNamespace.get().searchName(name, readingType, canWrite)
                        .map(ne -> SymbolUtils.setDereferenceByVariable(ne, MESSAGE_VAR_NAME))
        );
    }



    @Override
    public Stream<? extends CallableSymbol> searchCallable(
            String name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        return messageNamespace.get().searchCallable(name, returnType, parameterNames, parameterTypes)
                .map(ce -> SymbolUtils.setDereferenceByVariable(ce, MESSAGE_VAR_NAME));
    }

    @Override
    public Stream<? extends CallableSymbol> searchCallable(
            Predicate<String> name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        return messageNamespace.get().searchCallable(name, returnType, parameterNames, parameterTypes)
                .map(ce -> SymbolUtils.setDereferenceByVariable(ce, MESSAGE_VAR_NAME));
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is MessageHandlerWhenExpressionContext");
        debugDumpReceivedMessage(scb);
    }

    @Override
    public String getCurrentOperationLogName() {
        return "<evaluating when-expression>";
    }
}
