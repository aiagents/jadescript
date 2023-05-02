package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.ImportedMembersNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.NamespaceWithMembers;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.utils.LazyInit;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import jadescript.lang.Performative;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class OnMessageHandlerContext
    extends HandlerWithWhenExpressionContext
    implements CompilableName.Namespace,
    CompilableCallable.Namespace,
    MessageReceivedContext {

    private final Maybe<String> performative;
    private final IJadescriptType messageContentType;
    private final IJadescriptType messageType;
    private final LazyInit<NamespaceWithMembers> messageNamespace;
    private final LazyInit<ImportedMembersNamespace> importedFromMessage;


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
            new LazyInit<>(() -> getMessageType().namespace());
        this.importedFromMessage = new LazyInit<>(() ->
            ImportedMembersNamespace.importMembersNamespace(
                module,
                (__) -> MESSAGE_VAR_NAME,
                ExpressionDescriptor.messageReference,
                messageNamespace.get()
            )
        );
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
            SemanticsUtils.buildStream(
                this::getMessageName,
                this::getContentName
            ).filter(n -> name == null || name.equals(n.name())),
            importedFromMessage.get().compilableNames(name)
        );
    }


    @Override
    public Stream<? extends CompilableCallable> compilableCallables(
        @Nullable String name
    ) {
        return importedFromMessage.get().compilableCallables(name);
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
