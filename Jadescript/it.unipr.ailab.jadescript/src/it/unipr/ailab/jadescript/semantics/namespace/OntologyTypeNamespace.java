package it.unipr.ailab.jadescript.semantics.namespace;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.OntologyElementConstructor;
import it.unipr.ailab.jadescript.semantics.context.symbol.OntologyElementStructuralPattern;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.*;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.UserDefinedOntologyType;
import it.unipr.ailab.maybe.utils.LazyInit;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;

public class OntologyTypeNamespace
    extends JadescriptTypeNamespace
    implements OntologyAssociated, NamespaceWithGlobals {

    private final OntologyType ontologyType;
    private final LazyInit<JvmTypeNamespace> jvmNamespace;


    public OntologyTypeNamespace(
        SemanticsModule module,
        OntologyType ontologyType
    ) {
        super(module);
        this.ontologyType = ontologyType;
        this.jvmNamespace = new LazyInit<>(ontologyType::jvmNamespace);
    }


    @Override
    public Stream<? extends GlobalPattern> globalPatterns(
        @Nullable String name
    ) {
        return staticCallablesFromJvm(
            JadescriptTypeNamespace.NO_ENV_PARAMETER,
            jvmNamespace.get()
        ).compilableCallables(name)
            .map(cs -> new OntologyElementStructuralPattern(
                cs.name(),
                cs.returnType(),
                cs.parameterNames(),
                cs.parameterTypesByName(),
                cs.sourceLocation()
            ));
    }


    @Override
    public Stream<? extends GlobalCallable> globalCallables(
        @Nullable String name
    ) {
        return staticCallablesFromJvm(
            JadescriptTypeNamespace.NO_ENV_PARAMETER,
            jvmNamespace.get(),
            (sm, namespace, op) -> OntologyElementConstructor
                .fromJvmStaticOperation(
                    module,
                    namespace,
                    op,
                    ontologyType.compileToJavaTypeReference(),
                    ontologyType.getLocation()
                )
        ).globalCallables(name);
    }


    @Override
    public Maybe<? extends OntologyTypeNamespace> superSearcheable() {
        return getSuperTypeNamespace();
    }


    @Override
    public Maybe<? extends OntologyTypeNamespace> getSuperTypeNamespace() {
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


    @Override
    public Stream<? extends MemberCallable> memberCallables(
        @Nullable String name
    ) {
        return Stream.empty();
    }


    @Override
    public Stream<? extends MemberName> memberNames(
        @Nullable String name
    ) {
        return Stream.empty();
    }


    @Override
    public Stream<? extends GlobalName> globalNames(@Nullable String name) {
        return null;
    }


    @Override
    public Stream<? extends CompilableName> compilableNames(
        @Nullable String name
    ) {
        return Stream.empty();
    }

}
