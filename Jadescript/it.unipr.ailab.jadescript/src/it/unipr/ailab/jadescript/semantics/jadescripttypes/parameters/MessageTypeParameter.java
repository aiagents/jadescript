package it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;

import java.util.function.BiFunction;

import static it.unipr.ailab.maybe.Maybe.some;

public class MessageTypeParameter extends FormalTypeParameter {

    /*package-private*/ final IJadescriptType upperBound;
    /*package-private*/ final TypeArgument defaultArgumentType;
    /*package-private*/ final BiFunction<TypeArgument, String, String> promoter;


    /*package-private*/ MessageTypeParameter(
        SemanticsModule module,
        IJadescriptType upperBound,
        TypeArgument defaultArgumentType,
        BiFunction<TypeArgument, String, String> promoter
    ) {
        super(module, some(upperBound), some(defaultArgumentType));
        this.upperBound = upperBound;
        this.defaultArgumentType = defaultArgumentType;
        this.promoter = promoter;
    }


    @Override
    public IJadescriptType getUpperBound() {
        return upperBound;
    }


    @Override
    public boolean hasDefaultArgument() {
        return true;
    }


    @Override
    public Maybe<TypeArgument> getDefaultArgument() {
        return some(defaultArgumentType);
    }


    public BiFunction<TypeArgument, String, String> getPromoter() {
        return promoter;
    }

}
