package it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;

import java.util.List;

abstract class ParametricTypeBuilder<T extends IJadescriptType> {

    abstract void register(ParametricTypeSchema<? super T> skeleton);

    abstract T instantiateType(List<? extends TypeArgument> arguments)
        throws InvalidTypeInstantiatonException;

    abstract T instantiateErroneous(
        List<? extends TypeArgument> arguments,
        InvalidTypeInstantiatonException error
    );
}
