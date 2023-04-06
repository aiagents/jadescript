package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.JadescriptTypeLocation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.nothing;

public abstract class JadescriptType
    implements SemanticsConsts, IJadescriptType {


    protected final SemanticsModule module;

    // Unique id of the type (excluding type arguments)
    protected final String typeRawID;
    // Simple name showed to the programmer (excluding type arguments)
    protected final String simpleName;
    // Category name used for the runtime converter
    protected final String categoryName;


    public JadescriptType(
        SemanticsModule module,
        String typeID,
        String simpleName,
        String categoryName
    ) {
        this.module = module;
        this.typeRawID = typeID;
        this.simpleName = simpleName;
        this.categoryName = categoryName;
    }





    @Override
    public String getCategoryName() {
        return categoryName;
    }


    @Override
    public SearchLocation getLocation() {
        return new JadescriptTypeLocation(this);
    }


    public abstract void addBultinProperty(Property prop);


    @Override
    public String getID() {
        return this.getRawID() +
            typeArguments().stream()
                .map(TypeArgument::getID)
                .collect(Collectors.joining(", ", "[", "]"));
    }


    @Override
    public String getRawID() {
        return this.typeRawID;
    }


    @Override
    public boolean validateType(
        Maybe<? extends EObject> input,
        ValidationMessageAcceptor acceptor
    ) {
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);
        boolean erroneousCheck = validationHelper.asserting(
            !isErroneous(),
            "InvalidType",
            "Invalid type: '" + this.getFullJadescriptName() + "'.",
            input,
            acceptor
        );

        if (erroneousCheck) {
            return INVALID;
        }

        boolean argsNumberCheck = VALID;
        boolean argsBoundCheck = VALID;
        boolean argsValidCheck = VALID;

        final @Nullable List<IJadescriptType> upperBounds =
            typeParametersUpperBounds();

        if (upperBounds != null) {
            final List<TypeArgument> typeArguments = typeArguments();
            argsNumberCheck = validationHelper.asserting(
                upperBounds.size() == typeArguments.size(),
                "InvalidTypeInstantiation",
                "Invalid number of type arguments; expected: "
                    + upperBounds.size() + ", provided: " +
                    typeArguments.size() + ".",
                input,
                acceptor
            );

            int assumedSize = Math.min(
                upperBounds.size(),
                typeArguments.size()
            );

            for (int i = 0; i < assumedSize; i++) {
                IJadescriptType upperBound = upperBounds.get(i);
                IJadescriptType typeArgument =
                    typeArguments.get(i).ignoreBound();

                final boolean vtemp = validationHelper.assertExpectedType(
                    upperBound,
                    typeArgument,
                    "InvalidTypeArgument",
                    input,
                    acceptor
                );

                argsBoundCheck = argsBoundCheck && vtemp;
            }

            for (TypeArgument typeArgument : typeArguments) {
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


        }

        return argsNumberCheck && argsBoundCheck && argsValidCheck;
    }


    @Override
    public String toString() {
        return this.getFullJadescriptName();
    }


    @Override
    public String getRawJadescriptName() {
        return this.simpleName;
    }


    @Override
    public String getFullJadescriptName() {
        String result = this.getRawJadescriptName();
        String opener = getParametricIntroductor().isBlank()
            ? getParametricListDelimiterOpen()
            : " " + getParametricIntroductor().trim() +
            " " + getParametricListDelimiterOpen();

        return result + typeArguments().stream()
            .map(TypeArgument::getFullJadescriptName)
            .collect(Collectors.joining(
                getParametricListSeparator(),
                opener,
                getParametricListDelimiterClose()
            ));
    }


    @Override
    public Maybe<IJadescriptType> getElementTypeIfCollection() {
        return nothing();
    }


    @Override
    public String compileConversionType() {
        final List<TypeArgument> typeArguments = typeArguments();
        return "new jadescript.util.types.JadescriptTypeReference(" +
            "jadescript.util.types.JadescriptBuiltinTypeAtom." +
            getCategoryName() +
            (typeArguments.isEmpty() ? "" :
                ", " + typeArguments.stream()
                    .map(TypeArgument::ignoreBound)
                    .map(IJadescriptType::compileConversionType)
                    .collect(Collectors.joining(", "))) +
            ")";
    }


    /**
     * The JADE Ontology schema name of this type, when used as type of slot.
     */
    @Override
    public String getSlotSchemaName() {
        return this.asJvmTypeReference().getSimpleName();
    }


    @Override
    public IJadescriptType ignoreBound() {
        return this;
    }


    @Override
    public String compileAsJavaCast() {
        return "(" + compileToJavaTypeReference() + ")";
    }


    @Override
    @Nullable
    public JvmTypeNamespace jvmNamespace() {
        return JvmTypeNamespace.resolve(
            module,
            asJvmTypeReference()
        );
    }


    @Override
    public String getDebugPrint() {
        return this.getFullJadescriptName() + "{Class=(" +
            getClass().getSimpleName() + "); JvmTypeReference=(" +
            compileToJavaTypeReference() + ")" +
            typeArguments().stream()
                .map(TypeArgument::getDebugPrint)
                .collect(Collectors.joining(", ", "; typeargs: [", "]")) +
            "}";
    }

}
