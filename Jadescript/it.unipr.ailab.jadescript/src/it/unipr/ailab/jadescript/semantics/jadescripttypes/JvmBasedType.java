package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.*;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;

public abstract class JvmBasedType extends JadescriptType {


    protected final @Nullable JvmTypeReference jvmTypeReference;


    public JvmBasedType(
        SemanticsModule module,
        String typeID,
        String simpleName,
        String categoryName,
        @Nullable JvmTypeReference jvmTypeReference
    ) {
        super(module, typeID, simpleName, categoryName);
        this.jvmTypeReference = jvmTypeReference;
    }

    private Map<String, JvmTypeReference> applyArguments(
        JvmDeclaredType type,
        JvmTypeReference referenceToType
    ) {
        if (referenceToType instanceof JvmParameterizedTypeReference
            && type instanceof JvmGenericType) {
            return applyArguments(
                ((JvmGenericType) type),
                ((JvmParameterizedTypeReference) referenceToType)
            );
        }

        return Map.of();
    }


    private Map<String, JvmTypeReference> applyArguments(
        JvmGenericType genericType,
        JvmParameterizedTypeReference parameterizedRef
    ) {


        final MaybeList<JvmTypeParameter> parameters =
            MaybeList.someListNullsRemoved(
                genericType.getTypeParameters()
            );

        final MaybeList<JvmTypeReference> arguments =
            MaybeList.someListNullsRemoved(
                parameterizedRef.getArguments()
            );

        if (parameters.size() != arguments.size()) {
            return Map.of();
        }

        int size = parameters.size();

        Map<String, JvmTypeReference> result = new HashMap<>();

        for (int i = 0; i < size; i++) {
            final Maybe<JvmTypeParameter> parameter = parameters.get(i);
            final Maybe<JvmTypeReference> argument = arguments.get(i);

            if (parameter.isNothing() || argument.isNothing()) {
                continue;
            }

            if (parameter.__(JvmTypeParameter::getName)
                .nullIf(String::isBlank)
                .isNothing()) {
                continue;
            }

            final String paramName =
                parameter.__(JvmTypeParameter::getName).toNullable();

            final JvmTypeReference argumentSafe = argument.toNullable();

            result.put(paramName, argumentSafe);
        }

        return result;
    }


    private Maybe<JvmTypeReference> replaceTypeArguments(
        Maybe<JvmTypeReference> input,
        Map<String, JvmTypeReference> appliedArguments
    ) {
        if (input.isNothing()) {
            return nothing();
        }

        final JvmTypeReference inputSafe = input.toNullable();

        if (inputSafe.getType() == null) {
            return nothing();
        }

        List<JvmTypeReference> typeArgs;
        if (inputSafe instanceof JvmParameterizedTypeReference) {
            JvmParameterizedTypeReference paramerizedInput =
                (JvmParameterizedTypeReference) inputSafe;

            typeArgs = new LinkedList<>();

            MaybeList<JvmTypeReference> arguments = MaybeList
                .someListNullsRemoved(paramerizedInput.getArguments());

            for (Maybe<JvmTypeReference> argument : arguments) {
                JvmTypeReference resultArg;

                final Maybe<String> argID = argument
                    .__(JvmTypeReference::getIdentifier);

                if (argID.isPresent()
                    && appliedArguments.containsKey(argID.toNullable())) {
                    resultArg = appliedArguments.get(argID.toNullable());
                } else {
                    resultArg = argument.toNullable();
                }

                typeArgs.add(resultArg);
            }

        } else {
            typeArgs = List.of();
        }

        return some(module.get(JvmTypeHelper.class)
            .typeRef(inputSafe.getType(), typeArgs));
    }


    @Override
    public Stream<IJadescriptType> declaredSupertypes() {
        if (this.jvmTypeReference != null
            && this.jvmTypeReference.getType() instanceof JvmDeclaredType) {
            JvmDeclaredType type =
                (JvmDeclaredType) this.jvmTypeReference.getType();


            Map<String, JvmTypeReference> appliedArguments =
                applyArguments(type, this.jvmTypeReference);


            final TypeSolver typeSolver = module.get(TypeSolver.class);
            return Maybe.someStream(type.getSuperTypes())
                .map(superType ->
                    replaceTypeArguments(superType, appliedArguments)
                ).map(mt -> mt.__(typeSolver::fromJvmTypeReference))
                .map(mt -> mt.__(TypeArgument::ignoreBound))
                .filter(Maybe::isPresent)
                .map(Maybe::toNullable);
        }

        return Stream.empty();
    }


    @Override
    public List<TypeArgument> typeArguments() {
        if (this.jvmTypeReference == null
            || !(this.jvmTypeReference
            instanceof JvmParameterizedTypeReference)) {
            return List.of();
        }
        JvmParameterizedTypeReference parameterizedRef =
            (JvmParameterizedTypeReference) this.jvmTypeReference;

        final EList<JvmTypeReference> arguments =
            parameterizedRef.getArguments();

        if (arguments == null) {
            return List.of();
        }

        final TypeSolver typeSolver = module.get(TypeSolver.class);

        List<TypeArgument> result = new LinkedList<>();


        for (JvmTypeReference argument : arguments) {
            result.add(typeSolver.fromJvmTypeReference(argument));
        }

        return result;
    }


    @Override
    public JvmTypeReference asJvmTypeReference() {
        return jvmTypeReference;
    }


    @Override
    public IJadescriptType postResolve() {
        final JvmTypeReference attemptedResolution =
            module.get(JvmTypeHelper.class)
                .attemptResolveTypeRef(asJvmTypeReference());

        if (attemptedResolution == jvmTypeReference) {
            return this;
        }
        return module.get(TypeSolver.class)
            .fromJvmTypeReference(attemptedResolution)
            .ignoreBound();
    }

}
