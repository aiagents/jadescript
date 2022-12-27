package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.c1toplevel.TopLevelDeclarationContext;
import it.unipr.ailab.jadescript.semantics.context.c0outer.FileContext;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.OntologyElementConstructorSymbol;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;
import static it.unipr.ailab.maybe.Maybe.nullAsEmptyString;
import static it.unipr.ailab.maybe.Maybe.nullAsFalse;

public class OntologyDeclarationSupportContext extends TopLevelDeclarationContext
        implements CallableSymbol.Searcher {
    private final Maybe<Ontology> input;

    public OntologyDeclarationSupportContext(SemanticsModule module, FileContext outer, Maybe<Ontology> input) {
        super(module, outer);
        this.input = input;
    }


    @Override
    public Stream<? extends CallableSymbol> searchCallable(
            String name,
            Predicate<IJadescriptType> returnType,
            BiPredicate<Integer, Function<Integer, String>> parameterNames,
            BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        Stream<? extends CallableSymbol> stream = Maybe.toListOfMaybes(input.__(Ontology::getFeatures)).stream()
                .filter(f -> f.__(ff -> ff instanceof ExtendingFeature).extract(nullAsFalse))
                .map(f -> f.__(ff -> (ExtendingFeature) ff))
                .filter(f -> f.__(ff -> ff.getName().equals(name)).extract(nullAsFalse))
                .map(f -> new OntologyElementConstructorSymbol(module, f, currentLocation()));
        stream = safeFilter(stream, CallableSymbol::returnType, returnType);
        stream = safeFilter(
                stream,
                c -> c.parameterNames().size(),
                c -> i -> c.parameterNames().get(i),
                parameterNames
        );
        stream = safeFilter(
                stream,
                c -> c.parameterTypes().size(),
                c -> i -> c.parameterTypes().get(i),
                parameterTypes
        );
        return stream;
    }

    @Override
    public Stream<? extends CallableSymbol> searchCallable(Predicate<String> name, Predicate<IJadescriptType> returnType, BiPredicate<Integer, Function<Integer, String>> parameterNames, BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes) {
        Stream<? extends CallableSymbol> stream = Maybe.toListOfMaybes(input.__(Ontology::getFeatures)).stream()
                .filter(f -> f.__(ff -> ff instanceof ExtendingFeature).extract(nullAsFalse))
                .map(f -> f.__(ff -> (ExtendingFeature) ff))
                .map(f -> new OntologyElementConstructorSymbol(module, f, currentLocation()));
        stream = safeFilter(stream, CallableSymbol::name, name);
        stream = safeFilter(stream, CallableSymbol::returnType, returnType);
        stream = safeFilter(
                stream,
                c -> c.parameterNames().size(),
                c -> i -> c.parameterNames().get(i),
                parameterNames
        );
        stream = safeFilter(
                stream,
                c -> c.parameterTypes().size(),
                c -> i -> c.parameterTypes().get(i),
                parameterTypes
        );
        return stream;
    }

    public String getOntologyName(){
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
