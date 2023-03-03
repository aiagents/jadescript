package it.unipr.ailab.jadescript.semantics.context.c2feature;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.Context;
import it.unipr.ailab.jadescript.semantics.context.associations.AnyAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.associations.Associated;
import it.unipr.ailab.jadescript.semantics.context.associations.SelfAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.SelfAssociation;
import it.unipr.ailab.jadescript.semantics.context.c1toplevel.TopLevelDeclarationContext;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalPattern;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.emf.ecore.EObject;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;
import static it.unipr.ailab.maybe.Maybe.some;

public class ProceduralFeatureContainerContext
    extends Context
    implements SelfAssociated,
    CompilableCallable.Namespace,
    CompilableName.Namespace,
    GlobalPattern.Namespace {

    private final TopLevelDeclarationContext outer;
    private final Maybe<IJadescriptType> thisReferenceType;
    private final Maybe<? extends EObject> featureContainer;
    private final LazyValue<Maybe<TypeNamespace>> thisReferenceNamespace;
    private final LazyValue<Maybe<CompilableName>> thisReferenceElement;


    public ProceduralFeatureContainerContext(
        SemanticsModule module,
        TopLevelDeclarationContext outer,
        IJadescriptType thisReferenceType,
        Maybe<? extends EObject> featureContainer
    ) {
        super(module);
        this.outer = outer;
        this.thisReferenceType = some(thisReferenceType);
        this.featureContainer = featureContainer;
        this.thisReferenceNamespace = new LazyValue<>(() ->
            some(thisReferenceType.namespace())
        );

        this.thisReferenceElement = new LazyValue<>(() ->
            some(new ContextGeneratedName(
                THIS,
                thisReferenceType,
                () -> Util.getOuterClassThisReference(featureContainer)
                    .orElse(THIS)
            ))
        );

    }


    public ProceduralFeatureContainerContext(
        SemanticsModule module,
        TopLevelDeclarationContext outer,
        Maybe<? extends EObject> featureContainer
    ) {
        super(module);
        this.outer = outer;
        this.thisReferenceType = Maybe.nothing();
        this.featureContainer = featureContainer;
        this.thisReferenceNamespace = new LazyValue<>(Maybe::nothing);
        this.thisReferenceElement = new LazyValue<>(Maybe::nothing);
    }


    public TopLevelDeclarationContext getOuterContextTopLevelDeclaration() {
        return outer;
    }


    @Override
    public Stream<? extends CompilableCallable> compilableCallables(
        String name
    ) {
        return searchAs(
            Associated.class,
            (Associated x) -> AnyAssociationComputer.computeAllAssociations(x)
                .flatMap(a ->
                    a.importNamespace(module, featureContainer)
                        .compilableCallables(name)
                )
        );
    }


    public Stream<? extends CompilableName> compilableNames(
        String name
    ) {
        final Stream<? extends CompilableName> thisStream;
        if ((name != null && !name.equals(THIS))
            || this.thisReferenceElement.get().isNothing()) {
            thisStream = Stream.empty();
        } else {
            thisStream = Stream.of(
                this.thisReferenceElement.get().toNullable()
            );
        }

        final Stream<? extends CompilableName> associationsStream = searchAs(
            Associated.class,
            (Associated x) -> AnyAssociationComputer.computeAllAssociations(x)
                .flatMap(a ->
                    a.importNamespace(module, featureContainer)
                        .compilableNames(name)
                )
        );
        return Streams.concat(thisStream, associationsStream);
    }


    @Override
    public Stream<? extends GlobalPattern> globalPatterns(
        String name
    ) {
        return searchAs(
            Associated.class,
            x -> AnyAssociationComputer.computeAllAssociations(x)
                .flatMap(a ->
                    a.importNamespace(module, featureContainer)
                        .globalPatterns(name))
        );
    }



    @Override
    public Maybe<? extends Searcheable> superSearcheable() {
        return some(outer);
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        scb.open("--> is ProceduralFeatureContainerContext {");
        scb.line("thisReferenceType = " +
            thisReferenceType.__(IJadescriptType::getDebugPrint)
                .orElse("/*Maybe::nothing*/")
        );
        scb.open("thisReferenceNamespace = {");
        thisReferenceNamespace.get().safeDo(
            n -> n.debugDump(scb),
            (/*else*/) -> scb.line("/*Maybe::nothing*/")
        );
        scb.close("}");
        scb.close("}");
        debugDumpSelfAssociations(scb);

    }


    @Override
    public String getCurrentOperationLogName() {
        return outer.getCurrentOperationLogName();
    }


    @Override
    public Maybe<Searcheable> superTypeSearcheable() {
        final Maybe<Maybe<? extends TypeNamespace>> maybeMaybe =
            thisReferenceNamespace.get()
                .__(TypeNamespace::getSuperTypeNamespace);
        return Maybe.flatten(maybeMaybe.__(x -> x.__(y -> y)));
    }


    @Override
    public Stream<SelfAssociation> computeCurrentSelfAssociations() {
        return thisReferenceType.__(it -> Stream.of(new SelfAssociation(
                it,
                SelfAssociation.T.INSTANCE
            )))
            .orElseGet(Stream::empty);
    }


}
