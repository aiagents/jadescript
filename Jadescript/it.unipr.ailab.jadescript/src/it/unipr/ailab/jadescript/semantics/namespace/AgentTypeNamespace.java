package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Operation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agent.AgentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agent.BaseAgentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agent.UserDefinedAgentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.utils.LazyInit;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;

public class AgentTypeNamespace
    extends JadescriptTypeNamespace
    implements AgentAssociated, OntologyAssociated {

    private final AgentType agentType;
    private final boolean useJvm;
    private final List<Property> builtinProperties;
    private final List<Operation> builtinOperations;
    private final LazyInit<JvmTypeNamespace> jvmNamespace;


    public AgentTypeNamespace(
        SemanticsModule module,
        AgentType agentType,
        List<Property> builtinProperties,
        List<Operation> builtinOperations
    ) {
        super(module);
        this.agentType = agentType;
        useJvm = !(agentType instanceof BaseAgentType);
        this.builtinProperties = builtinProperties;
        this.builtinOperations = builtinOperations;
        this.jvmNamespace = new LazyInit<>(this.agentType::jvmNamespace);
    }


    @Override
    public Stream<? extends MemberCallable> memberCallables(
        @Nullable String name
    ) {
        if (useJvm) {
            return callablesFromJvm(jvmNamespace.get())
                .memberCallables(name);
        } else {
            return builtinOperations.stream()
                .filter(op -> name == null || op.name().equals(name));
        }
    }


    @Override
    public Stream<? extends MemberName> memberNames(
        @Nullable String name
    ) {
        if (useJvm) {
            return namesFromJvm(jvmNamespace.get())
                .memberNames(name);
        } else {
            return builtinProperties.stream()
                .filter(p -> name == null || name.equals(p.name()));
        }
    }


    @Override
    public Maybe<? extends TypeNamespace> getSuperTypeNamespace() {
        if (agentType instanceof UserDefinedAgentType) {
            return some(((UserDefinedAgentType) agentType)
                .getSuperAgentType().namespace());
        }
        return nothing();
    }


    @Override
    public SearchLocation currentLocation() {
        return agentType.getLocation();
    }


    @Override
    public Stream<OntologyAssociation> computeUsingOntologyAssociations() {
        return agentType.getDirectlyUsedOntology()
            .map(OntologyType::namespace)
            .flatMap(namspc -> namspc.computeAllOntologyAssociations()
                .map(OntologyAssociation::applyUsesOntology)
            );
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
    public Stream<AgentAssociation> computeCurrentAgentAssociations() {
        return Stream.of(new AgentAssociation(
            agentType,
            AgentAssociation.A.INSTANCE
        ));
    }


    @Override
    public Stream<AgentAssociation> computeForClauseAgentAssociations() {
        return Stream.empty();
    }


}
