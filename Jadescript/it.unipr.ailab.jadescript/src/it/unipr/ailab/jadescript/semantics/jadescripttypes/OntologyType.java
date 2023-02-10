package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.OntologyElementStructuralPattern;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalPattern;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberNamedCell;
import it.unipr.ailab.jadescript.semantics.namespace.JadescriptTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmTypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;

public interface OntologyType
    extends IJadescriptType {

    SearchLocation getLocation();

    boolean isSuperOrEqualOntology(OntologyType other);

    @Override
    OntologyTypeNamespace namespace();

    class OntologyTypeNamespace
        extends JadescriptTypeNamespace
        implements OntologyAssociated,
        GlobalPattern.Namespace,
        CompilableCallable.Namespace {

        private final OntologyType ontologyType;
        private final LazyValue<JvmTypeNamespace> jvmNamespace;


        public OntologyTypeNamespace(
            SemanticsModule module,
            OntologyType ontologyType
        ) {
            super(module);
            this.ontologyType = ontologyType;
            this.jvmNamespace = new LazyValue<>(ontologyType::jvmNamespace);
        }


        @Override
        public Stream<? extends GlobalPattern> globalPatterns() {
            return staticCallablesFromJvm(jvmNamespace.get())
                .compilableCallables()
                .map(cs -> new OntologyElementStructuralPattern(
                    cs.name(),
                    cs.returnType(),
                    cs.parameterNames(),
                    cs.parameterTypesByName(),
                    cs.sourceLocation()
                ));
        }


        @Override
        public Stream<? extends CompilableCallable> compilableCallables() {
            return staticCallablesFromJvm(jvmNamespace.get())
                .compilableCallables();
        }


        @Override
        public Stream<? extends MemberCallable> memberCallables() {
            return Stream.empty();
        }


        @Override
        public Stream<? extends MemberNamedCell> memberNamedCells() {
            return Stream.empty();
        }



        @Override
        public Maybe<? extends TypeNamespace> getSuperTypeNamespace() {
            if (ontologyType instanceof UserDefinedOntologyType) {
                return some(((UserDefinedOntologyType) ontologyType)
                    .getSuperOntologyType().namespace());
                //TODO multiple ontologies
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
        public Stream<OntologyAssociation>
        computeCurrentOntologyAssociations() {
            return Stream.of(new OntologyAssociation(
                ontologyType,
                OntologyAssociation.O.INSTANCE
            ));
        }


        @Override
        public Stream<OntologyAssociation> computeUsingOntologyAssociations() {
            return Stream.empty();
        }


        @Override
        public Stream<OntologyAssociation>
        computeForClauseOntologyAssociations() {
            return Stream.empty();
        }

    }

}
