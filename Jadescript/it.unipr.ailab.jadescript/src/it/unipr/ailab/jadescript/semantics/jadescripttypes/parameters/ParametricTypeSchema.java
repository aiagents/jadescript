package it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts.VALID;

public class ParametricTypeSchema<T extends IJadescriptType> {

    private final static int INIT = 0;
    private final static int DEFAULTS = 1;
    private final static int VARIADIC = 2;
    private final static int SEALED = 3;
    /*package-private*/ final SemanticsModule module;
    /*package-private*/ final List<FormalTypeParameter> formalTypeParameters
        = new LinkedList<>();
    /*package-private*/ final List<IJadescriptType> upperBounds
        = new LinkedList<>();
    /*package-private*/ int defaultParametersCount = 0;
    /*package-private*/ Maybe<VariadicTypeParameter> varadicTypeParameter =
        Maybe.nothing();
    /*package-private*/ Maybe<IJadescriptType> variadicUpperBound =
        Maybe.nothing();
    private ParametricTypeBuilder<? extends T> builder = null;
    private int state = INIT;


    /*package-private*/ ParametricTypeSchema(
        SemanticsModule module
    ) {
        this.module = module;
    }


    private void stateCheckAndUpdate(int newState) {



        if (newState < this.state) {
            throw new IllegalStateException(
                "Invalid order of specification in building a Jadescript " +
                    "parametric type skeleton. Non-default parameters first, " +
                    "then eventual default parameters " +
                    "OR the eventual variadic parameter, " +
                    "then finalize with builder."
            );
        }

        if (this.state == VARIADIC && newState == VARIADIC) {
            throw new IllegalStateException(
                "Invalid state in building a Jadescript " +
                    "parametric type skeleton: cannot add two variadic " +
                    "parameters."
            );
        }

        if (this.state == DEFAULTS && newState == VARIADIC) {
            throw new IllegalStateException(
                "Invalid state in building a Jadescript " +
                    "parametric type skeleton: cannot define a variadic " +
                    "parameter when default parameters were defined."
            );
        }

        this.state = newState;
    }


    @Contract("_ -> this")
    public ParametricTypeSchema<T> add(
        @NotNull FormalTypeParameter parameter
    ) {
        if (parameter.hasDefaultArgument()) {
            stateCheckAndUpdate(DEFAULTS);
            this.defaultParametersCount++;
        } else {
            stateCheckAndUpdate(INIT);
        }

        int index = formalTypeParameters.size();

        parameter.registerParameter(index);

        this.formalTypeParameters.add(parameter);
        this.upperBounds.add(parameter.getUpperBound());


        return this;
    }


    @Contract("_ -> this")
    public ParametricTypeSchema<T> add(
        @NotNull VariadicTypeParameter parameter
    ) {
        stateCheckAndUpdate(VARIADIC);
        int index = formalTypeParameters.size();

        parameter.registerParameter(index);

        this.varadicTypeParameter = Maybe.some(parameter);
        this.variadicUpperBound = Maybe.some(parameter.getUpperBound());

        return this;
    }


    public ParametricTypeSchema<T> seal(
        ParametricTypeBuilder<? extends T> builder
    ) {
        stateCheckAndUpdate(SEALED);

        this.builder = builder;

        this.builder.register(this);

        return this;
    }


    public List<TypeArgument> limitToUpperBounds(List<TypeArgument> arguments) {
        List<TypeArgument> result = new ArrayList<>(arguments.size());

        final TypeComparator typeComparator =
            module.get(TypeComparator.class);
        final TypeHelper typeHelper =
            module.get(TypeHelper.class);
        int i;
        for (i = 0; i < Math.min(upperBounds.size(), arguments.size()); i++) {
            final IJadescriptType ub = upperBounds.get(i);
            final IJadescriptType arg = arguments.get(i).ignoreBound();
            final IJadescriptType min = typeComparator.min(ub, arg).orElse(ub);
            if(TypeRelationshipQuery.equal().matches(
                typeComparator.compare(min, ub)
            )){
                result.add(typeHelper.covariant(min));
            }else {
                result.add(min);
            }
        }
        if(i < upperBounds.size()){
            for(; i < upperBounds.size(); i++){
                result.add(typeHelper.covariant(upperBounds.get(i)));
            }
        }
        return result;
    }


    public boolean isApplicable(List<TypeArgument> arguments) {
        final int paramSize = formalTypeParameters.size();
        final int argSize = arguments.size();
        final TypeComparator typeComparator = module.get(TypeComparator.class);

        if (varadicTypeParameter.isNothing()) {
            if (argSize > paramSize) {
                return false;
            }

            for (final FormalTypeParameter formalTypeParameter :
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


        final Maybe<IJadescriptType> variadicUpperBound =
            this.variadicUpperBound;


        int i = -1;
        for (FormalTypeParameter formalTypeParameter
            : formalTypeParameters) {

            if (formalTypeParameter.index >= argSize) {
                return false;
            }

            if (formalTypeParameter.upperBound.isPresent()) {
                if (!typeComparator.isAssignable(
                    formalTypeParameter.upperBound.toNullable(),
                    arguments.get(formalTypeParameter.index).ignoreBound()
                )) {
                    return false;
                }
            }

            if (i < formalTypeParameter.index) {
                i = formalTypeParameter.index;
            }
        }

        i++;

        for (; i < argSize; i++) {
            if (variadicUpperBound.isPresent()) {
                if (!typeComparator.isAssignable(
                    variadicUpperBound.toNullable(),
                    arguments.get(i).ignoreBound()
                )) {
                    return false;
                }
            }
        }

        return true;

    }

    public boolean validateApplication(
        List<TypeArgument> arguments,
        Maybe<? extends EObject> input,
        ValidationMessageAcceptor acceptor
    ){
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);
        boolean argsNumberCheck = VALID;
        boolean argsBoundCheck = VALID;
        boolean argsValidCheck = VALID;

        final List<IJadescriptType> upperBounds =
            getUpperBounds();

        argsNumberCheck = validationHelper.asserting(
            upperBounds.size() == arguments.size(),
            "InvalidTypeInstantiation",
            "Invalid number of type arguments; expected: "
                + upperBounds.size() + ", provided: " +
                arguments.size() + ".",
            input,
            acceptor
        );

        int assumedSize = Math.min(
            upperBounds.size(),
            arguments.size()
        );

        for (int i = 0; i < assumedSize; i++) {
            IJadescriptType upperBound = upperBounds.get(i);
            IJadescriptType typeArgument =
                arguments.get(i).ignoreBound();

            final boolean vtemp = validationHelper.assertExpectedType(
                upperBound,
                typeArgument,
                "InvalidTypeArgument",
                input,
                acceptor
            );

            argsBoundCheck = argsBoundCheck && vtemp;
        }

        for (TypeArgument typeArgument : arguments) {
            boolean vtemp = validationHelper.asserting(
                !typeArgument.ignoreBound().isErroneous(),
                "InvalidTypeArgument",
                "Invalid type argument. Type: '"
                    + typeArgument.getFullJadescriptName() + "'.",
                input,
                acceptor
            );

            argsValidCheck = argsValidCheck && vtemp;
        }


        return argsNumberCheck && argsBoundCheck && argsValidCheck;

    }


    public T create(List<? extends TypeArgument> arguments)
        throws InvalidTypeInstantiatonException {
        stateCheckAndUpdate(SEALED);

        if (this.builder == null) {
            throw new InvalidTypeInstantiatonException(
                "Missing parametric type builder in the skeleton."
            );
        }

        return this.builder.instantiateType(arguments);
    }


    public T create(TypeArgument... arguments)
        throws InvalidTypeInstantiatonException {
        return create(Arrays.asList(arguments));
    }


    public List<IJadescriptType> getUpperBounds() {
        return this.upperBounds;
    }


    public int getParameterCount() {
        return formalTypeParameters.size();
    }

}
