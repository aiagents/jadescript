package it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.maybe.Maybe;

public class VariadicTypeParameter implements TypeParameter {

    /*package-private*/ final SemanticsModule module;
    /*package-private*/ final Maybe<IJadescriptType> upperBound;
    /*package-private*/ int startIndex = -1;


    /*package-private*/ VariadicTypeParameter(
        SemanticsModule module,
        Maybe<IJadescriptType> upperBound
    ) {
        this.module = module;
        this.upperBound = upperBound;
    }


    @Override
    public void registerParameter(int index) {
        this.startIndex = index;
    }


    @Override
    public IJadescriptType getUpperBound() {
        if (this.upperBound.isNothing()) {
            return module.get(BuiltinTypeProvider.class)
                .any("No upper bound specified for variadic" +
                    " type parameter starting at position " + startIndex);
        }

        return this.upperBound.toNullable();
    }

}
