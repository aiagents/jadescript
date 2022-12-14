package it.unipr.ailab.jadescript.semantics.context.c2feature;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.Context;
import it.unipr.ailab.jadescript.semantics.context.associations.*;
import it.unipr.ailab.jadescript.semantics.context.c1toplevel.TopLevelDeclarationContext;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedReference;
import it.unipr.ailab.jadescript.semantics.context.symbol.SymbolUtils;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.emf.ecore.EObject;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public class ProceduralFeatureContainerContext
        extends Context
        implements SelfAssociated, CallableSymbol.Searcher, NamedSymbol.Searcher {
    private final TopLevelDeclarationContext outer;
    private final Maybe<IJadescriptType> thisReferenceType;
    private final Maybe<? extends EObject> featureContainer;
    private final LazyValue<Maybe<TypeNamespace>> thisReferenceNamespace;
    private final LazyValue<Maybe<NamedSymbol>> thisReferenceElement;

    public ProceduralFeatureContainerContext(
            SemanticsModule module,
            TopLevelDeclarationContext outer,
            IJadescriptType thisReferenceType,
            Maybe<? extends EObject> featureContainer
    ) {
        super(module);
        this.outer = outer;
        this.thisReferenceType = Maybe.of(thisReferenceType);
        this.featureContainer = featureContainer;
        this.thisReferenceNamespace = new LazyValue<>(() -> Maybe.of(thisReferenceType.namespace()));

        this.thisReferenceElement = new LazyValue<>(() -> Maybe.of(new ContextGeneratedReference(
                THIS,
                thisReferenceType,
                (__) -> Util.getOuterClassThisReference(featureContainer).orElse(THIS)
        )));

    }

    public TopLevelDeclarationContext getOuterContextTopLevelDeclaration(){
        return outer;
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

    @Override
    public Stream<? extends CallableSymbol> searchCallable(
            String name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        return searchAs(
                Associated.class,
                x -> AnyAssociationComputer.computeAllAssociations(x)
                        .flatMap(a -> a.getAssociatedType()
                                .namespace()
                                .searchCallable(name, returnType, parameterNames, parameterTypes)
                                .flatMap(c -> dereference(a, c)))

        );
    }

    private Stream<CallableSymbol> dereference(Association a, CallableSymbol c) {
        if (a instanceof SelfAssociation || a instanceof BehaviourAssociation) {
            return Stream.of(SymbolUtils.setDereferenceByVariable(
                    c,
                    Util.getOuterClassThisReference(featureContainer).orElse(THIS)
            ));
        } else if (a instanceof AgentAssociation) {
            return Stream.of(SymbolUtils.setDereferenceByVariable(
                    c,
                    Util.getOuterClassThisReference(featureContainer).orElse(THIS)
                            + "." + THE_AGENT + "()"
            ));
        } else if (a instanceof OntologyAssociation) {
            return Stream.of(SymbolUtils.setDereferenceByExternalClass(
                    c,
                    ((OntologyAssociation) a).getOntology()
            ));
        } else {
            return Stream.empty();
        }
    }

    private Stream<NamedSymbol> dereference(Association a, NamedSymbol c) {
        if (a instanceof SelfAssociation || a instanceof BehaviourAssociation) {
            return Stream.of(SymbolUtils.setDereferenceByVariable(
                    c,
                    Util.getOuterClassThisReference(featureContainer).orElse(THIS)
            ));
        } else if (a instanceof AgentAssociation) {
            return Stream.of(SymbolUtils.setDereferenceByVariable(
                    c,
                    Util.getOuterClassThisReference(featureContainer).orElse(THIS)
                            + "." + THE_AGENT + "()"
            ));
        } else {
            return Stream.empty();
        }
    }

    @Override
    public Stream<? extends CallableSymbol> searchCallable(
            Predicate<String> name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        return searchAs(
                Associated.class,
                x -> AnyAssociationComputer.computeAllAssociations(x)
                        .flatMap(a -> a.getAssociatedType()
                                .namespace()
                                .searchCallable(name, returnType, parameterNames, parameterTypes)
                                .flatMap(c -> dereference(a, c)))

        );
    }

    @Override
    public Stream<? extends NamedSymbol> searchName(
            Predicate<String> name,
            Predicate<IJadescriptType> readingType,
            Predicate<Boolean> canWrite
    ) {
        IJadescriptType thisReferenceType;
        NamedSymbol thisElement;
        final Stream<? extends NamedSymbol> thisStream;
        if (this.thisReferenceType.isNothing()) {
            thisStream = Stream.empty();
        }else if (this.thisReferenceElement.get().isNothing()) {
            thisStream = Stream.empty();
        }else {
            thisReferenceType = this.thisReferenceType.toNullable();
            thisElement = this.thisReferenceElement.get().toNullable();
            thisStream = thisReferenceNamespace.get().__(thisNamespace -> {
                Stream<Integer> thisStream2 = Stream.of(0);
                thisStream2 = safeFilter(thisStream2, (__) -> THIS, name);
                thisStream2 = safeFilter(thisStream2, (__) -> thisReferenceType, readingType);
                thisStream2 = safeFilter(thisStream2, (__) -> false, canWrite);
                return thisStream2.map((__) -> thisElement);
            }).orElseGet(Stream::empty);
        }
        final Stream<? extends NamedSymbol> associationsStream
                = searchAs(
                Associated.class,
                x -> AnyAssociationComputer.computeAllAssociations(x)
                        .flatMap(a -> a.getAssociatedType()
                                .namespace()
                                .searchName(name, readingType, canWrite)
                                .flatMap(c -> dereference(a, c)))
        );
        return Streams.concat(thisStream, associationsStream);
    }

    @Override
    public Maybe<? extends Searcheable> superSearcheable() {
        return Maybe.of(outer);
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        scb.open("--> is ProceduralFeatureContainerContext {");
        scb.line("thisReferenceType = " + thisReferenceType.__(IJadescriptType::getDebugPrint).orElse("/*Maybe::nothing*/"));
        scb.open("thisReferenceNamespace = {");
        thisReferenceNamespace.get().safeDo(n -> n.debugDump(scb), (/*else*/) -> scb.line("/*Maybe::nothing*/"));
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
        final Maybe<Maybe<? extends TypeNamespace>> maybeMaybe = thisReferenceNamespace.get()
                .__(TypeNamespace::getSuperTypeNamespace);
        return Maybe.flatten(maybeMaybe.__(x -> x.__(y -> y)));
    }

    @Override
    public Stream<SelfAssociation> computeCurrentSelfAssociations() {
        return thisReferenceType.__(it -> Stream.of(new SelfAssociation(it, SelfAssociation.T.INSTANCE)))
                .orElseGet(Stream::empty);
    }


}
