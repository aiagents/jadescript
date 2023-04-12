package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.InvalidTypeInstantiatonException;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.ImportedMembersNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.NamespaceWithMembers;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.utils.LazyInit;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import jadescript.lang.Performative;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class OnMessageHandlerWhenExpressionContext
    extends HandlerWhenExpressionContext
    implements CompilableName.Namespace,
    CompilableCallable.Namespace,
    MessageReceivedContext {


    private final Maybe<Performative> performative;
    private final LazyInit<NamespaceWithMembers> messageNamespace;
    private final LazyInit<ImportedMembersNamespace> importedFromMessage;


    public OnMessageHandlerWhenExpressionContext(
        SemanticsModule module,
        Maybe<Performative> performative,
        ProceduralFeatureContainerContext outer
    ) {
        super(module, outer);
        this.messageNamespace =
            new LazyInit<>(() -> getMessageType().namespace());
        this.performative = performative;
        this.importedFromMessage = new LazyInit<>(() ->
            ImportedMembersNamespace.importMembersNamespace(
                module,
                (__) -> MESSAGE_VAR_NAME,
                ExpressionDescriptor.messageReference,
                messageNamespace.get()
            ));
    }


    @Override
    public IJadescriptType getMessageContentType() {
        return getPerformative()
            .__(module.get(TypeSolver.class)::getContentBoundForPerformative)
            .orElseGet(() -> module.get(BuiltinTypeProvider.class)
                .any("Could not compute content type without " +
                    "performative."));
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
    public IJadescriptType getMessageType() {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (performative.isPresent()) {
            final List<TypeArgument> a = new ArrayList<>(
                typeHelper.unpackTuple(getMessageContentType())
            );
            final TypeSolver typeSolver = module.get(TypeSolver.class);
            try {
                return typeSolver.getMessageTypeSchemaForPerformative(
                    performative.toNullable()
                ).create(a);
            } catch (InvalidTypeInstantiatonException e) {
                e.printStackTrace();
            }
        }

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        return builtins.anyMessage();
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
