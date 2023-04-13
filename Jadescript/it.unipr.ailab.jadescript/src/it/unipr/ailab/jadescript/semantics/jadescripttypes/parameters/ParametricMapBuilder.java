package it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.maybe.Maybe;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class ParametricMapBuilder<T extends IJadescriptType>
    extends ParametricTypeBuilder<T> {


    private final Map<FormalTypeParameter, TypeArgument>
        parameterArgumentMap = new HashMap<>();
    private int variadicParameterHash = new Object().hashCode();
    private Maybe<List<TypeArgument>> variadicArgument =
        Maybe.nothing();

    private ParametricTypeSchema<? super T> skeleton = null;


    @Override
    void register(ParametricTypeSchema<? super T> skeleton) {
        this.skeleton = skeleton;
    }


    @Override
    final T instantiateType(List<? extends TypeArgument> arguments)
        throws InvalidTypeInstantiatonException {
        if (this.skeleton == null) {
            throw new IllegalStateException("Missing skeleton.");
        }

        List<FormalTypeParameter> typeParameters =
            this.skeleton.formalTypeParameters;

        final Maybe<VariadicTypeParameter> variadicTypeParameter =
            this.skeleton.varadicTypeParameter;


        int minRequired = typeParameters.size()
            - this.skeleton.defaultParametersCount;


        Maybe<Integer> maxRequired;
        if (variadicTypeParameter.isPresent()) {
            maxRequired = Maybe.nothing();
        } else {
            maxRequired = Maybe.some(typeParameters.size());
        }

        int argSize = arguments.size();

        if (argSize < minRequired ||
            (maxRequired.isPresent() && argSize > maxRequired.toNullable())) {
            String maxRequiredText;
            if (maxRequired.isPresent()) {
                maxRequiredText = ". Maximum supported: " +
                    maxRequired.toNullable();
            } else {
                maxRequiredText = "";
            }
            throw new InvalidTypeInstantiatonException(
                "Wrong number of arguments provided. Required minimum: " +
                    minRequired + maxRequiredText +
                    ". Provided: " + argSize + "."
            );
        }


        int i;
        for (i = 0; i < typeParameters.size(); i++) {
            FormalTypeParameter formalTypeParameter =
                typeParameters.get(i);

            TypeArgument arg;
            if (i < argSize) {
                arg = arguments.get(i);
            } else {
                final Maybe<? extends TypeArgument> defaultArgument =
                    formalTypeParameter.defaultArgument;

                if (defaultArgument.isNothing()) {
                    throw new InvalidTypeInstantiatonException(
                        "No argument provided for mandatory type " +
                            "parameter at position " +
                            formalTypeParameter.index
                    );
                }

                arg = defaultArgument.toNullable();
            }

            parameterArgumentMap.put(formalTypeParameter, arg);
        }

        if (variadicTypeParameter.isPresent()) {
            VariadicTypeParameter vtparam =
                variadicTypeParameter.toNullable();

            List<TypeArgument> vtarg = new LinkedList<>();

            for (; i < arguments.size(); i++) {
                vtarg.add(arguments.get(i));
            }

            this.variadicArgument = Maybe.some(vtarg);
            this.variadicParameterHash = vtparam.hashCode();
        }


        return build();
    }


    protected TypeArgument getArgument(FormalTypeParameter parameter)
        throws InvalidTypeInstantiatonException {
        if (!parameterArgumentMap.containsKey(parameter)) {
            throw new InvalidTypeInstantiatonException(
                "Missing argument for parameter in position " +
                    parameter.index
            );
        }

        return parameterArgumentMap.get(parameter);
    }


    protected List<TypeArgument> getVariadic(
        VariadicTypeParameter parameter
    ) throws InvalidTypeInstantiatonException {
        if (variadicArgument.isNothing()
            || variadicParameterHash != parameter.hashCode()) {
            throw new InvalidTypeInstantiatonException(
                "Missing argument for variadic parameter in position " +
                    parameter.startIndex
            );
        }

        return variadicArgument.toNullable();
    }


    protected List<IJadescriptType> getNonVariadicUpperBounds() {
        return this.skeleton.upperBounds;
    }


    protected IJadescriptType getVariadicUpperBound() {
        final Supplier<IJadescriptType> anyTypeSupplier =
            () -> this.skeleton.module.get(BuiltinTypeProvider.class)
                .any("");

        if (this.skeleton.varadicTypeParameter.isNothing()) {
            return anyTypeSupplier.get();
        } else {
            return this.skeleton.variadicUpperBound.orElseGet(anyTypeSupplier);
        }
    }


    public abstract T build() throws InvalidTypeInstantiatonException;

}
