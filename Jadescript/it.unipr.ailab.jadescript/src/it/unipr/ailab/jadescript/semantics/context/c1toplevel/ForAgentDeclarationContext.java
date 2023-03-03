package it.unipr.ailab.jadescript.semantics.context.c1toplevel;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.c0outer.FileContext;
import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

public abstract class ForAgentDeclarationContext
    extends UsingOntologyDeclarationContext
    implements CompilableName.Namespace, AgentAssociated {

    private final IJadescriptType agentType;
    private final LazyValue<TypeNamespace> agentNamespace;
    private final LazyValue<CompilableName> agentSymbol;


    public ForAgentDeclarationContext(
        SemanticsModule module,
        FileContext outer,
        List<IJadescriptType> ontologyTypes,
        IJadescriptType agentType
    ) {
        super(module, outer, ontologyTypes);
        this.agentType = agentType;
        this.agentNamespace = new LazyValue<>(agentType::namespace);
        this.agentSymbol = new LazyValue<>(() ->
            new ContextGeneratedName(
                "agent",
                agentType,
                () -> THE_AGENT + "()"
            )
        );
    }


    @Override
    public Stream<? extends CompilableName> compilableNames(
        @Nullable String name
    ) {
        if (name != null && !name.equals("agent")) {
            return Stream.of();
        }
        return Util.buildStream(agentSymbol);
    }



    @Override
    public Stream<AgentAssociation> computeCurrentAgentAssociations() {
        return Stream.empty();
    }


    @Override
    public Stream<AgentAssociation> computeForClauseAgentAssociations() {
        return Stream.of(new AgentAssociation(
            agentType,
            AgentAssociation.F_A.INSTANCE
        ));
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
