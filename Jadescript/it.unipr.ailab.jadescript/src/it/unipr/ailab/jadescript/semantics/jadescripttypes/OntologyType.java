package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.namespace.JadescriptTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmOperationSymbol;
import it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;

public interface OntologyType extends IJadescriptType {
    SearchLocation getLocation();

    boolean isSuperOrEqualOntology(OntologyType other);

    @Override
    OntologyTypeNamespace namespace();

    class OntologyTypeNamespace
            extends JadescriptTypeNamespace
            implements OntologyAssociated {

        private final OntologyType ontologyType;
        private final LazyValue<JvmTypeNamespace> jvmNamespace;

        public OntologyTypeNamespace(
                SemanticsModule module,
                OntologyType ontologyType
        ) {
            super(module);
            this.ontologyType = ontologyType;
            this.jvmNamespace = new LazyValue<>(() -> JvmTypeNamespace.fromTypeReference(
                    this.module,
                    this.ontologyType.asJvmTypeReference()
            ));
        }


        @Override
        public Stream<? extends CallableSymbol> searchCallable(
                String name,
                Predicate<IJadescriptType> returnType,
                BiPredicate<Integer, Function<Integer, String>> parameterNames,
                BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
        ) {
            return jvmNamespace.get().searchCallable(
                            name, returnType, parameterNames, parameterTypes
                    ).filter(f -> f instanceof JvmOperationSymbol
                            && ((JvmOperationSymbol) f).isStatic());
        }

        @Override
        public Stream<? extends CallableSymbol> searchCallable(
                Predicate<String> name,
                Predicate<IJadescriptType> returnType,
                BiPredicate<Integer, Function<Integer, String>> parameterNames,
                BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
        ) {
            return jvmNamespace.get().searchCallable(
                    name, returnType, parameterNames, parameterTypes
            ).filter(f -> f instanceof JvmOperationSymbol
                    && ((JvmOperationSymbol) f).isStatic());

        }

        @Override
        public Stream<? extends NamedSymbol> searchName(
                Predicate<String> name,
                Predicate<IJadescriptType> readingType,
                Predicate<Boolean> canWrite
        ) {
            return Stream.empty();
        }

        @Override
        public Maybe<? extends TypeNamespace> getSuperTypeNamespace() {
            if (ontologyType instanceof UserDefinedOntologyType) {
                return some(((UserDefinedOntologyType) ontologyType).getSuperOntologyType().namespace());//FUTURETODO multiple ontologies
            }
            return nothing();
        }

        @Override
        public SearchLocation currentLocation() {
            return ontologyType.getLocation();
        }

        @Override
        public void debugDump(SourceCodeBuilder scb) {
            super.debugDump(scb);
            scb.open("--> is OntologyTypeNamespace {");
            scb.open("Partially delegating to JvmTypeNamespace: {");
            jvmNamespace.get().debugDump(scb);
            scb.close("}");
            scb.close("}");
        }

        @Override
        public Stream<OntologyAssociation> computeCurrentOntologyAssociations() {
            return Stream.of(new OntologyAssociation(ontologyType, OntologyAssociation.O.INSTANCE));
        }

        @Override
        public Stream<OntologyAssociation> computeUsingOntologyAssociations() {
            return Stream.empty();
        }

        @Override
        public Stream<OntologyAssociation> computeForClauseOntologyAssociations() {
            return Stream.empty();
        }
    }
}
