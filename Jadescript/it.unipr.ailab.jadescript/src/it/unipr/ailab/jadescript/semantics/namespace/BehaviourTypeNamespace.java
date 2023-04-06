package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.*;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberName;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agent.AgentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour.BaseBehaviourType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour.BehaviourType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour.UserDefinedBehaviourType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.maybe.utils.LazyInit;
import it.unipr.ailab.maybe.Maybe;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;

public class BehaviourTypeNamespace
    extends JadescriptTypeNamespace
    implements BehaviourAssociated, AgentAssociated, OntologyAssociated {

    private final BehaviourType behaviourType;
    private final LazyInit<JvmTypeNamespace> jvmNamespace;
    private final boolean useJvmIndex;
    private final List<Property> builtinProperties;


    public BehaviourTypeNamespace(
        SemanticsModule module,
        BehaviourType behaviourType,
        List<Property> builtinProperties
    ) {
        super(module);
        this.behaviourType = behaviourType;
        useJvmIndex = !(behaviourType instanceof BaseBehaviourType);
        this.builtinProperties = builtinProperties;
        this.jvmNamespace =
            new LazyInit<>(this.behaviourType::jvmNamespace);
    }


    @Override
    public Stream<? extends MemberCallable> memberCallables(
        @Nullable String name
    ) {
        if (useJvmIndex) {
            return callablesFromJvm(jvmNamespace.get())
                .memberCallables(name);
        } else {
            return Stream.empty();
        }
    }


    @Override
    public Stream<? extends MemberName> memberNames(@Nullable String name) {
        if (useJvmIndex) {
            return namesFromJvm(jvmNamespace.get())
                .memberNames(name);
        } else {
            return Stream.empty();
        }
    }


    @Override
    public Maybe<? extends TypeNamespace> getSuperTypeNamespace() {
        if (behaviourType instanceof UserDefinedBehaviourType) {
            return some(((UserDefinedBehaviourType) behaviourType)
                .getSuperBehaviourType().namespace());
        }
        return nothing();
    }


    @Override
    public SearchLocation currentLocation() {
        return behaviourType.getLocation();
    }


    @Override
    public Stream<AgentAssociation> computeCurrentAgentAssociations() {
        return Stream.empty(); // not an agent
    }


    @Override
    public Stream<AgentAssociation> computeForClauseAgentAssociations() {
        return behaviourType.getDirectlyForAgentType()
            .__(AgentType::namespace)
            .__(namspc -> namspc.computeAllAgentAssociations()
                .map(AgentAssociation::applyForAgent)
            )
            .orElseGet(Stream::empty);
    }


    @Override
    public Stream<OntologyAssociation> computeUsingOntologyAssociations() {
        return behaviourType.getDirectlyUsedOntology()
            .map(OntologyType::namespace)
            .flatMap(namspc -> namspc.computeAllOntologyAssociations()
                .map(OntologyAssociation::applyUsesOntology)
            );
    }


    @Override
    public Stream<OntologyAssociation> computeCurrentOntologyAssociations() {
        return Stream.empty(); //not an ontology
    }


    @Override
    public Stream<OntologyAssociation> computeForClauseOntologyAssociations() {
        return behaviourType.getDirectlyForAgentType()
            .__(AgentType::namespace)
            .__(namspc -> namspc.computeAllOntologyAssociations()
                .map(OntologyAssociation::applyForClause)
            )
            .orElseGet(Stream::empty);
    }


    @Override
    public Stream<BehaviourAssociation>
    computeCurrentBehaviourAssociations() {
        return Stream.of(new BehaviourAssociation(
            behaviourType,
            BehaviourAssociation.B.INSTANCE
        ));
    }

}
