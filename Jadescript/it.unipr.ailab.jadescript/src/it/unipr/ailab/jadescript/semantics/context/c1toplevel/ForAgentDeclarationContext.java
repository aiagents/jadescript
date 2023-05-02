package it.unipr.ailab.jadescript.semantics.context.c1toplevel;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.c0outer.FileContext;
import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agent.AgentType;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.utils.LazyInit;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

public abstract class ForAgentDeclarationContext
    extends UsingOntologyDeclarationContext
    implements CompilableName.Namespace, AgentAssociated {

    private final IJadescriptType agentType;
    private final LazyInit<TypeNamespace> agentNamespace;
    private final LazyInit<CompilableName> agentSymbol;


    public ForAgentDeclarationContext(
        SemanticsModule module,
        FileContext outer,
        List<IJadescriptType> ontologyTypes,
        IJadescriptType agentType
    ) {
        super(module, outer, ontologyTypes);
        this.agentType = agentType;
        this.agentNamespace = new LazyInit<>(agentType::namespace);
        this.agentSymbol = new LazyInit<>(() ->
            new ContextGeneratedName(
                "agent",
                agentType,
                CompilationHelper::compileAgentReference
            )
        );
    }


    @Override
    public Stream<? extends CompilableName> compilableNames(
        @Nullable String name
    ) {
        if (name != null && !name.equals("agent")) {
            return Stream.empty();
        }
        return SemanticsUtils.buildStream(agentSymbol);
    }


    @Override
    public Stream<AgentAssociation> computeCurrentAgentAssociations() {
        return Stream.empty();
    }


    @Override
    public Stream<AgentAssociation> computeForClauseAgentAssociations() {
        if (agentType instanceof AgentType) {
            return ((AgentType) agentType).namespace()
                .computeAllAgentAssociations()
                .map(AgentAssociation::applyForAgent);
        } else {
            return Stream.of(
                new AgentAssociation(
                    agentType,
                    AgentAssociation.F_A.INSTANCE
                )
            );
        }
    }


    @Override
    public Stream<OntologyAssociation> computeCurrentOntologyAssociations() {
        return Stream.empty();
    }


    @Override
    public Stream<OntologyAssociation> computeForClauseOntologyAssociations() {
        if (agentNamespace.get() instanceof OntologyAssociationComputer) {
            return ((OntologyAssociationComputer) agentNamespace.get())
                .computeAllOntologyAssociations()
                .map(OntologyAssociation::applyForClause);
        } else {
            return Stream.empty();
        }
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.open("--> is ForAgentDeclarationContext {");
        scb.line("Agent type = " + agentType.getDebugPrint());
        scb.open("Agent namespace = {");
        agentNamespace.get().debugDump(scb);
        scb.close("}");
        scb.close("}");
        debugDumpAgentAssociations(scb);
    }

}
