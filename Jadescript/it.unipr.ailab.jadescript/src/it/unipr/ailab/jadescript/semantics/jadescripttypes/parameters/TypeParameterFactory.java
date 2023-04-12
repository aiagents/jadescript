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


    public <A extends TypeArgument> FormalTypeParameter<A>
    typeParameter() {
        return new FormalTypeParameter<>(
            factoryModule,
            Maybe.nothing(),
            Maybe.nothing()
        );
    }


    public <A extends TypeArgument> FormalTypeParameter<A>
    boundedTypeParameter(IJadescriptType upperBound) {
        return new FormalTypeParameter<>(
            factoryModule,
            Maybe.some(upperBound),
            Maybe.nothing()
        );
    }


    public <A extends TypeArgument> FormalTypeParameter<A>
    typeParameterWithDefault(A defaultArgument) {
        return new FormalTypeParameter<>(
            factoryModule,
            Maybe.nothing(),
            Maybe.some(defaultArgument)
        );
    }


    public <A extends TypeArgument> FormalTypeParameter<A>
    boundedTypeParameterWithDefault(
        IJadescriptType upperBound,
        A defaultArg
    ) {
        return new FormalTypeParameter<>(
            factoryModule,
            Maybe.some(upperBound),
            Maybe.some(defaultArg)
        );
    }

    public <A extends TypeArgument> VariadicTypeParameter<A>
    varadicTypeParameter(){
        return new VariadicTypeParameter<>(
            factoryModule,
            Maybe.nothing()
        );
    }

    public <A extends TypeArgument> MessageTypeParameter<A>
    messageTypeParameter(
        IJadescriptType upperBound,
        A defaultArg,
        BiFunction<TypeArgument, String, String> contentDefaultPromoter
    ) {
        return new MessageTypeParameter<>(
            factoryModule,
            upperBound,
            defaultArg,
            contentDefaultPromoter
        );
    }

}
