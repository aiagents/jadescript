package it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;

import java.util.function.BiFunction;

public class TypeParameterFactory {

    private final SemanticsModule factoryModule;


    public TypeParameterFactory(SemanticsModule factoryModule) {
        this.factoryModule = factoryModule;
    }


    public FormalTypeParameter
    typeParameter() {
        return new FormalTypeParameter(
            factoryModule,
            Maybe.nothing(),
            Maybe.nothing()
        );
    }


    public FormalTypeParameter boundedTypeParameter(
        IJadescriptType upperBound
    ) {
        return new FormalTypeParameter(
            factoryModule,
            Maybe.some(upperBound),
            Maybe.nothing()
        );
    }


    public FormalTypeParameter typeParameterWithDefault(
        TypeArgument defaultArgument
    ) {
        return new FormalTypeParameter(
            factoryModule,
            Maybe.nothing(),
            Maybe.some(defaultArgument)
        );
    }


    public FormalTypeParameter boundedTypeParameterWithDefault(
        IJadescriptType upperBound,
        TypeArgument defaultArg
    ) {
        return new FormalTypeParameter(
            factoryModule,
            Maybe.some(upperBound),
            Maybe.some(defaultArg)
        );
    }


    public VariadicTypeParameter varadicTypeParameter() {
        return new VariadicTypeParameter(
            factoryModule,
            Maybe.nothing()
        );
    }


    public MessageTypeParameter messageTypeParameter(
        IJadescriptType upperBound,
        TypeArgument defaultArg,
        BiFunction<TypeArgument, String, String> contentDefaultPromoter
    ) {
        return new MessageTypeParameter(
            factoryModule,
            upperBound,
            defaultArg,
            contentDefaultPromoter
        );
    }

}
