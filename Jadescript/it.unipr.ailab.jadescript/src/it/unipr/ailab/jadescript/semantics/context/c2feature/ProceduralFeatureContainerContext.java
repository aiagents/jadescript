package it.unipr.ailab.jadescript.semantics.context.c2feature;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.Context;
import it.unipr.ailab.jadescript.semantics.context.associations.*;
import it.unipr.ailab.jadescript.semantics.context.c1toplevel.TopLevelDeclarationContext;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.symbol.*;
import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.CallableMember;
import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.NameMember;
import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.PatternMember;
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
import static it.unipr.ailab.maybe.Maybe.some;

public class ProceduralFeatureContainerContext
    extends Context
    implements SelfAssociated,
    CallableMember.Namespace,
    NameMember.Namespace,
    PatternMember.Searcher {

    private final TopLevelDeclarationContext outer;
    private final Maybe<IJadescriptType> thisReferenceType;
    private final Maybe<? extends EObject> featureContainer;
    private final LazyValue<Maybe<TypeNamespace>> thisReferenceNamespace;
    private final LazyValue<Maybe<NameMember>> thisReferenceElement;


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
            some(new ContextGeneratedReference(
                THIS,
                thisReferenceType,
                (__) -> Util.getOuterClassThisReference(featureContainer)
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


    private Stream<CallableMember> dereference(
        Association a,
        CallableMember c
    ) {
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


    private Stream<NameMember> dereference(
        Association a,
        NameMember c
    ) {
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
    public Stream<? extends CallableMember> searchCallable(
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
                    .searchCallable(
                        name,
                        returnType,
                        parameterNames,
                        parameterTypes
                    ).flatMap(c -> dereference(a, c)))

        );
    }


    @Override
    public Stream<? extends CallableMember> searchCallable(
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
                    .searchCallable(
                        name,
                        returnType,
                        parameterNames,
                        parameterTypes
                    ).flatMap(c -> dereference(a, c)))

        );
    }


    @Override
    public Stream<? extends PatternMember> searchPattern(
        String name,
        Predicate<IJadescriptType> inputType,
        BiPredicate<Integer, Function<Integer, String>> termNames,
        BiPredicate<Integer, Function<Integer, IJadescriptType>> termTypes
    ) {
        return searchAs(
            Associated.class,
            x -> AnyAssociationComputer.computeAllAssociations(x)
                .flatMap(a -> {
                    final TypeNamespace associatedNamespace =
                        a.getAssociatedType().namespace();
                    if (associatedNamespace instanceof PatternMember.Searcher) {
                        return ((PatternMember.Searcher) associatedNamespace)
                            .searchPattern(
                                name,
                                inputType,
                                termNames,
                                termTypes
                            );
                    } else {
                        return Stream.empty();
                    }
                })
        );
    }


    //TODO: new generation of procedural contexts
    // - produce a scope by mixing in the contexts
    @Override
    public Stream<? extends PatternMember> searchPattern(
        Predicate<String> name,
        Predicate<IJadescriptType> inputType,
        BiPredicate<Integer, Function<Integer, String>> termNames,
        BiPredicate<Integer, Function<Integer, IJadescriptType>> termTypes
    ) {
        return searchAs(
            Associated.class,
            x -> AnyAssociationComputer.computeAllAssociations(x)
                .flatMap(a -> {
                    final TypeNamespace associatedNamespace =
                        a.getAssociatedType().namespace();
                    if (associatedNamespace instanceof PatternMember.Searcher) {
                        return ((PatternMember.Searcher) associatedNamespace)
                            .searchPattern(
                                name,
                                inputType,
                                termNames,
                                termTypes
                            );
                    } else {
                        return Stream.empty();
                    }
                })
        );
    }


    @Override
    public Stream<? extends NameMember> searchName(
        Predicate<String> name,
        Predicate<IJadescriptType> readingType,
        Predicate<Boolean> canWrite
    ) {
        IJadescriptType thisReferenceType;
        NameMember thisElement;
        final Stream<? extends NameMember> thisStream;
        if (this.thisReferenceType.isNothing()) {
            thisStream = Stream.empty();
        } else if (this.thisReferenceElement.get().isNothing()) {
            thisStream = Stream.empty();
        } else {
            thisReferenceType = this.thisReferenceType.toNullable();
            thisElement = this.thisReferenceElement.get().toNullable();
            thisStream = thisReferenceNamespace.get().__(thisNamespace -> {
                Stream<Integer> thisStream2 = Stream.of(0);
                thisStream2 = safeFilter(thisStream2, (__) -> THIS, name);
                thisStream2 = safeFilter(
                    thisStream2,
                    (__) -> thisReferenceType,
                    readingType
                );
                thisStream2 = safeFilter(thisStream2, (__) -> false, canWrite);
                return thisStream2.map((__) -> thisElement);
            }).orElseGet(Stream::empty);
        }
        final Stream<? extends NameMember> associationsStream
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
