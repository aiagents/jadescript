package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.ImportedMembersNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.NamespaceWithMembers;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import jadescript.lang.Performative;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class OnMessageHandlerContext
    extends HandlerWithWhenExpressionContext
    implements CompilableName.Namespace,
    CompilableCallable.Namespace,
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
    public Stream<? extends CompilableName> compilableNames(
        @Nullable String name
    ) {
        return Stream.concat(
            Util.buildStream(
                this::getMessageName,
                this::getContentName
            ).filter(n -> name == null || name.equals(n.name())),
            ImportedMembersNamespace.importMembersNamespace(
                module,
                (__) -> MESSAGE_VAR_NAME,
                ExpressionDescriptor.messageReference,
                messageNamespace.get()
            ).compilableNames(name)
        );
    }

    @Override
    public Stream<? extends CompilableCallable> compilableCallables(
        @Nullable String name
    ) {
        return ImportedMembersNamespace.importMembersNamespace(
            module,
            (__) -> MESSAGE_VAR_NAME,
            ExpressionDescriptor.messageReference,
            messageNamespace.get()
        ).compilableCallables(name);
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
