package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.ImportedMembersNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.NamespaceWithMembers;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import jadescript.lang.Performative;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class OnMessageHandlerWhenExpressionContext
    extends HandlerWhenExpressionContext
    implements CompilableName.Namespace, CompilableCallable.Namespace,
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
