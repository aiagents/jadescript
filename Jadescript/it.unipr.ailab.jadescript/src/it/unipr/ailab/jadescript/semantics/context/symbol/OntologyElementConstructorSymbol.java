package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.jadescript.ExtendingFeature;
import it.unipr.ailab.jadescript.jadescript.FeatureWithSlots;
import it.unipr.ailab.jadescript.jadescript.SlotDeclaration;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.MethodCallSemantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;

import java.util.*;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.nullAsEmptyString;

public class OntologyElementConstructorSymbol implements CallableSymbol {
    private final LazyValue<String> lazyName;
    private final LazyValue<IJadescriptType> lazyReturnType;
    private final LazyValue<List<Util.Tuple2<String, IJadescriptType>>> lazyParameters;
    private final SearchLocation location;

    public OntologyElementConstructorSymbol(
            SemanticsModule module,
            Maybe<ExtendingFeature> input,
            SearchLocation location
    ) {
        lazyName = new LazyValue<>(() ->
                input.__(ExtendingFeature::getName).extract(nullAsEmptyString)
        );
        lazyReturnType = new LazyValue<>(() ->
                input.__(inputSafe -> module.get(TypeHelper.class).jtFromJvmTypeRef(
                        module.get(TypeHelper.class).typeRef(module.get(CompilationHelper.class)
                                .getFullyQualifiedName(inputSafe).toString("."))
                )).orElseGet(() -> module.get(TypeHelper.class).SERIALIZABLE)
        );
        lazyParameters = new LazyValue<>(() -> {
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
        this.location = location;
    }

    public OntologyElementConstructorSymbol(
            String name,
            IJadescriptType returnType,
            List<Util.Tuple2<String, IJadescriptType>> parameters,
            SearchLocation location
    ) {
        this.lazyName = new LazyValue<>(name);
        this.lazyReturnType = new LazyValue<>(returnType);
        this.lazyParameters = new LazyValue<>(parameters);
        this.location = location;
    }

    @Override
    public SearchLocation sourceLocation() {
        return location;
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
                        MethodCallSemantics.sortToMatchParamNames(compiledRexprs, parameterNames())
                ) + ")";
    }

    @Override
    public boolean isPure() {
        return true;
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
}
