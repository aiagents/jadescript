package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.jadescript.ExtendingFeature;
import it.unipr.ailab.jadescript.jadescript.NamedElement;
import it.unipr.ailab.jadescript.jadescript.Ontology;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.c0outer.FileContext;
import it.unipr.ailab.jadescript.semantics.context.c1toplevel.TopLevelDeclarationContext;
import it.unipr.ailab.jadescript.semantics.context.symbol.OntologyElementConstructor;
import it.unipr.ailab.jadescript.semantics.context.symbol.OntologyElementStructuralPattern;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalPattern;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsEmptyString;
import static it.unipr.ailab.maybe.Maybe.nullAsFalse;

public class OntologyDeclarationSupportContext
    extends TopLevelDeclarationContext
    implements GlobalCallable.Namespace, GlobalPattern.Namespace {

    private final Maybe<Ontology> input;
    private final String ontoFQName;


    public OntologyDeclarationSupportContext(
        SemanticsModule module,
        FileContext outer,
        Maybe<Ontology> input,
        String ontoFQName
    ) {
        super(module, outer);
        this.input = input;
        this.ontoFQName = ontoFQName;
    }


    @Override
    public Stream<? extends GlobalCallable> globalCallables(
        @Nullable String name
    ) {
        return Maybe.toListOfMaybes(input.__(Ontology::getFeatures)).stream()
            .filter(f -> f.__(ff -> ff instanceof ExtendingFeature)
                .extract(nullAsFalse))
            .map(f -> f.__(ff -> (ExtendingFeature) ff))
            .filter(f -> f.__(ff -> ff.getName().equals(name))
                .extract(nullAsFalse))
            .map(f -> OntologyElementConstructor.fromFeature(
                module, f, ontoFQName, currentLocation()
            )).filter(Maybe::isPresent)
            .map(Maybe::toNullable);
    }


    @Override
    public Stream<? extends GlobalPattern> globalPatterns(
        @Nullable String name
    ) {
        return Maybe.toListOfMaybes(input.__(Ontology::getFeatures)).stream()
            .filter(f -> f.__(ff -> ff instanceof ExtendingFeature)
                .extract(nullAsFalse))
            .map(f -> f.__(ff -> (ExtendingFeature) ff))
            .filter(f -> f.__(ff -> ff.getName().equals(name))
                .extract(nullAsFalse))
            .map(f -> OntologyElementStructuralPattern.fromFeature(
                module, f, currentLocation()
            )).filter(Maybe::isPresent)
            .map(Maybe::toNullable);
    }


    public String getOntologyName() {
        return input.__(NamedElement::getName).extract(nullAsEmptyString);
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.open("--> is OntologyDeclarationSupportContext {");
        scb.line("input = " + getOntologyName());
        scb.close("}");
    }


    @Override
    public String getCurrentOperationLogName() {
        return "<init ontology " + getOntologyName() + ">";
    }


    @Override
    public boolean canUseAgentReference() {
        return false;
    }

}
