package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.maybe.Maybe;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class ParametricSkeleton<T extends IJadescriptType> {

    private final static int INIT = 0;
    private final static int DEFAULTS = 1;
    private final static int SEALED = 2;
    private final SemanticsModule module;
    private final List<FormalTypeParameter<?>> formalTypeParameters =
        new LinkedList<>();
    private int defaultParametersCount = 0;
    private ParametricTypeBuilder<T> builder = null;
    private int state = INIT;

    private ParametricSkeleton(
        SemanticsModule module
    ) {
        this.module = module;
    }


    private void stateCheckAndUpdate(int state) {
        if (state < this.state) {
            throw new IllegalStateException(
                "Invalid order of specification in building a Jadescript " +
                    "parametric type skeleton. Non-default parameters first, " +
                    "then default parameters, then finalize with builder."
            );
        }

        this.state = state;
    }


    public ParametricSkeleton<T> add() {
        stateCheckAndUpdate(INIT);
        int index = formalTypeParameters.size();

        this.formalTypeParameters.add(new FormalTypeParameter<>(
            index,
            Maybe.nothing(),
            Maybe.nothing()
        ));

        return this;
    }


    public ParametricSkeleton<T> addWithDefault(IJadescriptType defaultType) {
        stateCheckAndUpdate(DEFAULTS);

        int index = formalTypeParameters.size();

        this.formalTypeParameters.add(new FormalTypeParameter<>(
            index,
            Maybe.nothing(),
            Maybe.some(defaultType)
        ));

        this.defaultParametersCount++;

        return this;
    }


    public ParametricSkeleton<T> seal(ParametricTypeBuilder<T> builder) {
        stateCheckAndUpdate(SEALED);

        this.builder = builder;

        this.builder.register(this);

        return this;
    }


    public boolean isApplicable(List<TypeArgument> arguments) {
        final int paramSize = formalTypeParameters.size();
        final int argSize = arguments.size();
        final TypeComparator typeComparator = module.get(TypeComparator.class);


        for (final FormalTypeParameter<?> formalTypeParameter :
            formalTypeParameters) {
            if (formalTypeParameter.index >= argSize
                && !formalTypeParameter.hasDefaultArgument()) {
                return false;
            }

            if (formalTypeParameter.index < argSize
                && formalTypeParameter.upperBound.isPresent()) {
                if (!typeComparator.isAssignable(
                    formalTypeParameter.upperBound.toNullable(),
                    arguments.get(formalTypeParameter.index).ignoreBound()
                )) {
                    return false;
                }
            }
        }

        return true;
    }


    public T applyArguments(List<TypeArgument> arguments)
        throws InvalidTypeInstantiatonException {
        stateCheckAndUpdate(SEALED);

        if (this.builder == null) {
            throw new InvalidTypeInstantiatonException(
                "Missing parametric type builder in the skeleton."
            );
        }

        return this.builder.apply(arguments);
    }

    public static class Factory {

        private final SemanticsModule factoryModule;


        public Factory(SemanticsModule module) {
            this.factoryModule = module;
        }


        public <S extends IJadescriptType> ParametricSkeleton<S>
        parametricType() {
            return new ParametricSkeleton<>(this.factoryModule);
        }

    }

    public static class InvalidTypeInstantiatonException extends Exception {

        public InvalidTypeInstantiatonException(String message) {
            super(message);
        }


        public InvalidTypeInstantiatonException(
            String message,
            Throwable cause
        ) {
            super(message, cause);
        }

    }


    static abstract class ParametricTypeBuilder<T extends IJadescriptType> {

        abstract void register(ParametricSkeleton<T> skeleton);

        abstract T apply(List<TypeArgument> arguments)
            throws InvalidTypeInstantiatonException;

    }

    public static abstract class ParametricMapBuilder<T extends IJadescriptType>
        extends ParametricTypeBuilder<T> {


        private final Map<FormalTypeParameter<?>, TypeArgument>
            parameterArgumentMap = new HashMap<>();

        private ParametricSkeleton<T> skeleton = null;


        @Override
        void register(ParametricSkeleton<T> skeleton) {
            this.skeleton = skeleton;
        }


        @Override
        final T apply(List<TypeArgument> arguments)
            throws InvalidTypeInstantiatonException {
            if (this.skeleton == null) {
                throw new IllegalStateException("Missing skeleton.");
            }

            List<FormalTypeParameter<?>> typeParameters =
                this.skeleton.formalTypeParameters;

            int minRequired =
                typeParameters.size() - this.skeleton.defaultParametersCount;

            int maxRequired =
                typeParameters.size();

            int argSize = arguments.size();

            if (argSize < minRequired || argSize > maxRequired) {
                throw new InvalidTypeInstantiatonException(
                    "Wrong number of arguments provided. Required minimum: " +
                        minRequired + ". Maximum supported: " + maxRequired +
                        ". Provided: " + argSize + "."
                );
            }


            for (int i = 0; i < typeParameters.size(); i++) {
                FormalTypeParameter<?> formalTypeParameter =
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


            return build();
        }


        @SuppressWarnings("unchecked")
        protected <A extends TypeArgument> A getArgument(
            FormalTypeParameter<A> parameter
        ) throws InvalidTypeInstantiatonException {
            if (!parameterArgumentMap.containsKey(parameter)) {
                throw new InvalidTypeInstantiatonException(
                    "Missing argument for parameter in position " +
                        parameter.index
                );
            }

            return (A) parameterArgumentMap.get(parameter);
        }


        public abstract T build() throws InvalidTypeInstantiatonException;

    }

    public static class FormalTypeParameter<A extends TypeArgument> {

        private final int index;
        private final Maybe<IJadescriptType> upperBound;
        private final Maybe<A> defaultArgument;


        FormalTypeParameter(
            int index,
            Maybe<IJadescriptType> upperBound,
            Maybe<A> defaultArgument
        ) {
            this.index = index;
            this.upperBound = upperBound;
            this.defaultArgument = defaultArgument;
        }


        public Maybe<A> getDefaultArgument() {
            return defaultArgument;
        }


        public boolean hasDefaultArgument() {
            return defaultArgument.isPresent();
        }

    }


}
