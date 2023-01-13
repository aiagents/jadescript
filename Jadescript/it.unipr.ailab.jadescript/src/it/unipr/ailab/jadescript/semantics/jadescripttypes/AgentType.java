package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.namespace.JadescriptTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmModelBasedNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.maybe.Maybe;

import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
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
        private final Map<String, Property> builtinProperties;
        private final LazyValue<JvmModelBasedNamespace> jvmNamespace;


        public AgentTypeNamespace(
                SemanticsModule module,
                AgentType agentType,
                Map<String, Property> builtinProperties
        ) {
            super(module);
            this.agentType = agentType;
            useJvm = !(agentType instanceof BaseAgentType);
            this.builtinProperties = builtinProperties;
            this.jvmNamespace = new LazyValue<>(this.agentType::jvmNamespace);
        }


        @Override
        public Stream<? extends CallableSymbol> searchCallable(
                String name,
                Predicate<IJadescriptType> returnType,
                BiPredicate<Integer, Function<Integer, String>> parameterNames,
                BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
        ) {
            Stream<? extends CallableSymbol> jvmStream;
            if (useJvm) {
                jvmStream = computeUserDefinedSymbols(jvmNamespace.get()).searchCallable(
                        name, returnType, parameterNames, parameterTypes
                );
            } else {
                jvmStream = Stream.empty();
            }
            return jvmStream;
        }

        @Override
        public Stream<? extends CallableSymbol> searchCallable(
                Predicate<String> name,
                Predicate<IJadescriptType> returnType,
                BiPredicate<Integer, Function<Integer, String>> parameterNames,
                BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
        ) {
            Stream<? extends CallableSymbol> jvmStream;
            if (useJvm) {
                jvmStream = computeUserDefinedSymbols(jvmNamespace.get()).searchCallable(
                        name, returnType, parameterNames, parameterTypes
                );
            } else {
                jvmStream = Stream.empty();
            }
            return jvmStream;
        }

        @Override
        public Stream<? extends NamedSymbol> searchName(
                Predicate<String> name,
                Predicate<IJadescriptType> readingType,
                Predicate<Boolean> canWrite
        ) {
            Stream<Map.Entry<String, Property>> stream = builtinProperties.entrySet().stream();
            stream = safeFilter(stream, Map.Entry::getKey, name);
            stream = safeFilter(stream, x -> x.getValue().readingType(), readingType);
            stream = safeFilter(stream, x -> x.getValue().canWrite(), canWrite);

            Stream<? extends NamedSymbol> jvmStream;
            if (useJvm) {

                jvmStream = computeUserDefinedSymbols(jvmNamespace.get()).searchName(
                        name, readingType, canWrite
                );
            } else {
                jvmStream = Stream.empty();
            }

            return Streams.concat(stream.map(Map.Entry::getValue), jvmStream);
        }

        @Override
        public Maybe<? extends TypeNamespace> getSuperTypeNamespace() {
            if (agentType instanceof UserDefinedAgentType) {
                return some(((UserDefinedAgentType) agentType).getSuperAgentType().namespace());
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
            return Stream.of(new AgentAssociation(agentType, AgentAssociation.A.INSTANCE));
        }

        @Override
        public Stream<AgentAssociation> computeForClauseAgentAssociations() {
            return Stream.empty();
        }
    }
}
