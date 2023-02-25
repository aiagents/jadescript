package it.unipr.ailab.jadescript.semantics.context.c1toplevel;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation;
import it.unipr.ailab.jadescript.semantics.context.c0outer.FileContext;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.search.WithSupertype;
import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.LocalName;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

public class AgentDeclarationContext extends UsingOntologyDeclarationContext
    implements AgentAssociated,
    WithSupertype,
    LocalName.Namespace {

    private final JvmDeclaredType agentJvmType;
    private final LazyValue<IJadescriptType> agentType;
    private final LazyValue<TypeNamespace> agentTypeNamespace;
    private final LazyValue<ContextGeneratedName> agentReference;


    public AgentDeclarationContext(
        SemanticsModule module,
        FileContext outer,
        List<IJadescriptType> ontologyTypes,
        JvmDeclaredType agentType
    ) {
        super(module, outer, ontologyTypes);
        this.agentJvmType = agentType;
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        this.agentType = new LazyValue<>(() ->
            typeHelper.jtFromJvmType(agentJvmType)
        );
        this.agentTypeNamespace = new LazyValue<>(() ->
            this.agentType.get().namespace()
        );
        this.agentReference = new LazyValue<>(() ->
            AgentAssociated.contextGeneratedAgentReference(this.agentType.get())
        );
    }



    public IJadescriptType getAgentType() {
        return agentType.get();
    }


    @Override
    public Maybe<Searcheable> superTypeSearcheable() {
        return agentTypeNamespace.get().superTypeSearcheable();
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.open("--> is AgentDeclarationContext {");
        scb.line("agentType = " + getAgentType().getDebugPrint());
        scb.close("}");
        debugDumpAgentAssociations(scb);
    }


    @Override
    public String getCurrentOperationLogName() {
        return "<init agent>";
    }


    @Override
    public Stream<AgentAssociation> computeCurrentAgentAssociations() {
        return Stream.of(new AgentAssociation(
            getAgentType(),
            AgentAssociation.A.INSTANCE
        ));
    }


    @Override
    public Stream<OntologyAssociation> computeCurrentOntologyAssociations() {
        return Stream.empty();
    }


    @Override
    public Stream<OntologyAssociation> computeForClauseOntologyAssociations() {
        return Stream.empty();
    }


    @Override
    public Stream<AgentAssociation> computeForClauseAgentAssociations() {
        return Stream.empty();
    }


    @Override
    public Stream<? extends LocalName> localNames(
        @Nullable String name
    ) {
        return Util.buildStream(agentReference)
            .filter((__) -> name == null || name.equals("agent"));
    }


    @Override
    public Stream<? extends CompilableName> compilableNames(
        @Nullable String name
    ) {
        return localNames(name);
    }


    @Override
    public boolean canUseAgentReference() {
        return true; // Can always use 'agent' and perform agent-related actions
        // in an agent declaration.
    }

}
