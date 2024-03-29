package it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.maybe.Maybe;

public class FormalTypeParameter
    implements TypeParameter {

    /*package-private*/ final SemanticsModule module;
    /*package-private*/ final Maybe<IJadescriptType> upperBound;
    /*package-private*/ final Maybe<TypeArgument> defaultArgument;
    /*package-private*/ int index = -1;


    /*package-private*/ FormalTypeParameter(
        SemanticsModule module,
        Maybe<IJadescriptType> upperBound,
        Maybe<TypeArgument> defaultArgument
    ) {
        this.module = module;
        this.upperBound = upperBound;
        this.defaultArgument = defaultArgument;
    }


    @Override
    public void registerParameter(int index) {
        this.index = index;
    }


    public Maybe<TypeArgument> getDefaultArgument() {
        return defaultArgument;
    }


    public boolean hasDefaultArgument() {
        return defaultArgument.isPresent();
    }


    @Override
    public IJadescriptType getUpperBound() {
        if (this.upperBound.isNothing()) {
            return module.get(BuiltinTypeProvider.class)
                .any("No upper bound specified for type " +
                    "parameter at position " + index);

        }

        return this.upperBound.toNullable();
    }


}
