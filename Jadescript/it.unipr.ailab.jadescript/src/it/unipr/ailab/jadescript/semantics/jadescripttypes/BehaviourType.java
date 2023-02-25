package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.*;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberName;
import it.unipr.ailab.jadescript.semantics.namespace.JadescriptTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmTypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.maybe.Maybe;

import java.util.List;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;
import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;

public interface BehaviourType extends IJadescriptType, UsingOntologyType,
    ForAgentClausedType {

    SearchLocation getLocation();

    @Override
    BehaviourTypeNamespace namespace();

    class BehaviourTypeNamespace
        extends JadescriptTypeNamespace
        implements BehaviourAssociated, AgentAssociated, OntologyAssociated {

        private final BehaviourType behaviourType;
        private final LazyValue<JvmTypeNamespace> jvmNamespace;
        private final boolean useJvm;
        private final List<Property> builtinProperties;


        public BehaviourTypeNamespace(
            SemanticsModule module,
            BehaviourType behaviourType,
            List<Property> builtinProperties
        ) {
            super(module);
            this.behaviourType = behaviourType;
            useJvm = !(behaviourType instanceof BaseBehaviourType);
            this.builtinProperties = builtinProperties;
            this.jvmNamespace =
                new LazyValue<>(this.behaviourType::jvmNamespace);
        }


        @Override
        public Stream<? extends MemberCallable> memberCallables() {
            if (useJvm) {
                return callablesFromJvm(jvmNamespace.get())
                    .memberCallables();
            } else {
                return Stream.empty();
            }
        }

        @Override
        public Stream<? extends MemberName> memberNamedCells() {
            if (useJvm) {
                return namesFromJvm(jvmNamespace.get())
                    .memberNames();
            } else {
                return Stream.empty();
            }
        }


        @Override
        public Maybe<? extends TypeNamespace> getSuperTypeNamespace() {
            if (behaviourType instanceof UserDefinedBehaviourType) {
                return some(((UserDefinedBehaviourType) behaviourType).getSuperBehaviourType().namespace());
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
        public Stream<BehaviourAssociation> computeCurrentBehaviourAssociations() {
            return Stream.of(new BehaviourAssociation(
                behaviourType,
                BehaviourAssociation.B.INSTANCE
            ));
        }

    }

}
