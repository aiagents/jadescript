package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.MethodInvocationSemantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.c1toplevel.TopLevelDeclarationContext;
import it.unipr.ailab.jadescript.semantics.context.c0outer.FileContext;
import it.unipr.ailab.jadescript.semantics.context.search.*;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
                .map(this::fromExtendingFeature);
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

    private CallableSymbol fromExtendingFeature(Maybe<ExtendingFeature> input) {
        return new CallableSymbol() {
            private final LazyValue<String> lazyName = new LazyValue<>(() ->
                    input.__(ExtendingFeature::getName).extract(nullAsEmptyString)
            );
            private final LazyValue<IJadescriptType> lazyReturnType = new LazyValue<>(() ->
                    input.__(inputSafe -> module.get(TypeHelper.class).jtFromJvmTypeRef(
                            module.get(TypeHelper.class).typeRef(module.get(CompilationHelper.class)
                                    .getFullyQualifiedName(inputSafe).toString("."))
                    )).orElseGet(() -> module.get(TypeHelper.class).SERIALIZABLE)
            );
            private final LazyValue<List<Util.Tuple2<String, IJadescriptType>>> lazyParameters = new LazyValue<>(() -> {
                if (input.isInstanceOf(FeatureWithSlots.class)) {
                    List<Util.Tuple2<String, IJadescriptType>> list = new ArrayList<>();
                    for (Maybe<SlotDeclaration> slot : Maybe.toListOfMaybes(input.__(i -> (FeatureWithSlots) i)
                            .__(FeatureWithSlots::getSlots))) {
                        String name = slot.__(SlotDeclaration::getName).extract(nullAsEmptyString);
                        final Maybe<TypeExpression> type = slot.__(SlotDeclaration::getType);
                        if (type.isPresent()) {
                            list.add(new Util.Tuple2<>(
                                    name,
                                    module.get(TypeExpressionSemantics.class).toJadescriptType(type)
                            ));
                        }
                    }
                    return list;
                } else {
                    return Collections.emptyList();
                }
            });

            @Override
            public SearchLocation sourceLocation() {
                return OntologyDeclarationSupportContext.this.currentLocation();
            }

            @Override
            public String name() {
                return lazyName.get();
            }

            @Override
            public IJadescriptType returnType() {
                return lazyReturnType.get();
            }

            @Override
            public Map<String, IJadescriptType> parameterTypesByName() {
                return lazyParameters.get().stream()
                        .collect(Collectors.toMap(Util.Tuple2::get_1, Util.Tuple2::get_2));
            }

            @Override
            public List<String> parameterNames() {
                return lazyParameters.get().stream()
                        .map(Util.Tuple2::get_1)
                        .collect(Collectors.toList());
            }

            @Override
            public List<IJadescriptType> parameterTypes() {
                return lazyParameters.get().stream()
                        .map(Util.Tuple2::get_2)
                        .collect(Collectors.toList());
            }

            @Override
            public String compileInvokeByName(String dereferencePrefix, Map<String, String> compiledRexprs) {
                return dereferencePrefix +
                        lazyName.get() + "(" +
                        String.join(
                                ", ",
                                MethodInvocationSemantics.sortToMatchParamNames(compiledRexprs, parameterNames())
                        ) + ")";
            }

            @Override
            public String compileInvokeByArity(String dereferencePrefix, List<String> compiledRexprs) {
                return dereferencePrefix +
                        lazyName.get() + "(" +
                        String.join(
                                ", ",
                                compiledRexprs
                        ) + ")";

            }
        };
    }

    @Override
    public Stream<? extends CallableSymbol> searchCallable(Predicate<String> name, Predicate<IJadescriptType> returnType, BiPredicate<Integer, Function<Integer, String>> parameterNames, BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes) {
        Stream<? extends CallableSymbol> stream = Maybe.toListOfMaybes(input.__(Ontology::getFeatures)).stream()
                .filter(f -> f.__(ff -> ff instanceof ExtendingFeature).extract(nullAsFalse))
                .map(f -> f.__(ff -> (ExtendingFeature) ff))
                .map(this::fromExtendingFeature);
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
