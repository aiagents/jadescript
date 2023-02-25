package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberName;
import it.unipr.ailab.jadescript.semantics.namespace.JadescriptTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.maybe.Maybe;

import java.util.List;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;
import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;

public interface AgentType extends IJadescriptType, UsingOntologyType {
    SearchLocation getLocation();

    @Override
    AgentTypeNamespace namespace();

    class AgentTypeNamespace
            extends JadescriptTypeNamespace
            implements AgentAssociated, OntologyAssociated {

        private final AgentType agentType;
        private final boolean useJvm;
        private final List<Property> builtinProperties;
        private final LazyValue<JvmTypeNamespace> jvmNamespace;


        public AgentTypeNamespace(
                SemanticsModule module,
                AgentType agentType,
                List<Property> builtinProperties
        ) {
            super(module);
            this.agentType = agentType;
            useJvm = !(agentType instanceof BaseAgentType);
            this.builtinProperties = builtinProperties;
            this.jvmNamespace = new LazyValue<>(this.agentType::jvmNamespace);
        }


        @Override
        public Stream<? extends MemberCallable> memberCallables() {
            if(useJvm){
                return callablesFromJvm(jvmNamespace.get())
                    .memberCallables();
            }else{
                return Stream.empty();
            }
        }


        @Override
        public Stream<? extends MemberName> memberNamedCells() {
            if (useJvm) {
                return namesFromJvm(jvmNamespace.get())
                    .memberNames();
            } else {
                return builtinProperties.stream();
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
}
