package it.unipr.ailab.jadescript.semantics.context.c1toplevel;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.c0outer.FileContext;
import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedReference;
import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.NameMember;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public abstract class ForAgentDeclarationContext
    extends UsingOntologyDeclarationContext
    implements NameMember.Namespace, AgentAssociated {

    private final IJadescriptType agentType;
    private final LazyValue<TypeNamespace> agentNamespace;
    private final LazyValue<NameMember> agentSymbol;


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
            new ContextGeneratedReference(
                "agent", agentType, (__) -> THE_AGENT + "()"
            )
        );
    }


    @Override
    public Stream<? extends NameMember> searchName(
        Predicate<String> name,
        Predicate<IJadescriptType> readingType,
        Predicate<Boolean> canWrite
    ) {

        Stream<Integer> agentRefStream = Stream.of(0);
        agentRefStream = safeFilter(agentRefStream, (__) -> "agent", name);
        agentRefStream = safeFilter(
            agentRefStream,
            (__) -> agentType,
            readingType
        );
        agentRefStream = safeFilter(agentRefStream, (__) -> false, canWrite);
        return agentRefStream.map((__) -> agentSymbol.get());
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
