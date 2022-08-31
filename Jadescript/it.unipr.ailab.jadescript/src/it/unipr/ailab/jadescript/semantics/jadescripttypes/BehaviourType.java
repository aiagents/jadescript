package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.*;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.namespace.JadescriptTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
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
import static it.unipr.ailab.maybe.Maybe.of;

public interface BehaviourType extends IJadescriptType, UsingOntologyType, ForAgentClausedType {
    SearchLocation getLocation();

    @Override
    BehaviourTypeNamespace namespace();

    class BehaviourTypeNamespace
            extends JadescriptTypeNamespace
            implements BehaviourAssociated, AgentAssociated, OntologyAssociated {
        private final BehaviourType behaviourType;
        private final LazyValue<JvmTypeNamespace> jvmNamespace;
        private final boolean useJvm;
        private final Map<String, Property> builtinProperties;

        public BehaviourTypeNamespace(
                SemanticsModule module,
                BehaviourType behaviourType,
                Map<String, Property> builtinProperties
        ) {
            super(module);
            this.behaviourType = behaviourType;
            useJvm = !(behaviourType instanceof BaseBehaviourType);
            this.builtinProperties = builtinProperties;
            this.jvmNamespace = new LazyValue<>(() -> JvmTypeNamespace.fromTypeReference(
                    this.module,
                    this.behaviourType.asJvmTypeReference()
            ));
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
            if (behaviourType instanceof UserDefinedBehaviourType) {
                return of(((UserDefinedBehaviourType) behaviourType).getSuperBehaviourType().namespace());
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
            return Stream.of(new BehaviourAssociation(behaviourType, BehaviourAssociation.B.INSTANCE));
        }
    }
}
