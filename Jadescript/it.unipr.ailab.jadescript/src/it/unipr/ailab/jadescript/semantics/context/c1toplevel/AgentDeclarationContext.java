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
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.utils.LazyInit;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

public class AgentDeclarationContext
    extends UsingOntologyDeclarationContext
    implements AgentAssociated, WithSupertype, CompilableName.Namespace {


    private final JvmDeclaredType agentJvmType;
    private final LazyInit<IJadescriptType> agentType;
    private final LazyInit<TypeNamespace> agentTypeNamespace;
    private final LazyInit<ContextGeneratedName> agentReference;


    public AgentDeclarationContext(
        SemanticsModule module,
        FileContext outer,
        List<IJadescriptType> ontologyTypes,
        JvmDeclaredType agentType
    ) {
        super(module, outer, ontologyTypes);
        this.agentJvmType = agentType;
        final TypeSolver typeSolver = module.get(TypeSolver.class);
        this.agentType = new LazyInit<>(() ->
            typeSolver.fromJvmTypePermissive(agentJvmType)
        );
        this.agentTypeNamespace = new LazyInit<>(() ->
            this.agentType.get().namespace()
        );
        this.agentReference = new LazyInit<>(() ->
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
    public Stream<? extends CompilableName> compilableNames(
        @Nullable String name
    ) {
        return SemanticsUtils.buildStream(agentReference)
            .filter((__) -> name == null || name.equals("agent"));
    }


    @Override
    public boolean canUseAgentReference() {
        return true; // Can always use 'agent' and perform agent-related actions
        // in an agent declaration.
    }

}
