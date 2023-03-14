package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;
import java.util.stream.Collectors;

public abstract class ParametricType extends JadescriptType {

    private final String parametricIntroductor;
    private final String parametricListDelimiterOpen;
    private final String parametricListDelimiterClose;
    private final String parametricListSeparator;
    private final List<TypeArgument> typeArguments;
    private final List<IJadescriptType> upperBounds;


    public ParametricType(
        SemanticsModule module,
        String baseTypeID,
        String simpleName,
        String categoryName,
        String parametricIntroductor,
        String parametricListDelimiterOpen,
        String parametricListDelimiterClose,
        String parametricListSeparator,
        List<TypeArgument> typeArguments,
        /*Nullable*/ List<IJadescriptType> upperBounds

    ) {
        super(module, baseTypeID, simpleName, categoryName);
        this.parametricIntroductor = parametricIntroductor;
        this.parametricListDelimiterOpen = parametricListDelimiterOpen;
        this.parametricListDelimiterClose = parametricListDelimiterClose;
        this.parametricListSeparator = parametricListSeparator;
        this.typeArguments = typeArguments;
        this.upperBounds = upperBounds;
    }


    @Override
    public String getID() {
        return typeID + "<" + typeArguments.stream()
            .map(TypeArgument::getID)
            .reduce((s1, s2) -> s1 + ", " + s2).orElse("")
            + ">";
    }


    public String getJadescriptName() {
        String result = simpleName;
        result += " " + parametricIntroductor + " " + parametricListDelimiterOpen + typeArguments.stream()
            .map(TypeArgument::getJadescriptName)
            .map(s -> "(" + s + ")")
            .reduce((s1, s2) -> s1 + parametricListSeparator + s2).orElse(
                "(###notypeargs###)")
            + parametricListDelimiterClose;
        return result;
    }


    @Override
    public boolean typeEquals(IJadescriptType other) {
        if (other instanceof ParametricType) {
            final ParametricType parOther = (ParametricType) other;
            return this.typeID.equals(parOther.typeID)
                && this.typeArguments.size() == parOther.typeArguments.size()
                && Streams.zip(
                this.typeArguments.stream(),
                parOther.typeArguments.stream(),
                (ta1, ta2) -> {
                    BoundedTypeArgument.Variance v1;
                    IJadescriptType t1;
                    if (ta1 instanceof BoundedTypeArgument) {
                        v1 = ((BoundedTypeArgument) ta1).getVariance();
                        t1 = ((BoundedTypeArgument) ta1).getType();
                    } else {
                        v1 = BoundedTypeArgument.Variance.INVARIANT;
                        t1 = ((IJadescriptType) ta1);
                    }
                    BoundedTypeArgument.Variance v2;
                    IJadescriptType t2;
                    if (ta2 instanceof BoundedTypeArgument) {
                        v2 = ((BoundedTypeArgument) ta2).getVariance();
                        t2 = ((BoundedTypeArgument) ta2).getType();
                    } else {
                        v2 = BoundedTypeArgument.Variance.INVARIANT;
                        t2 = ((IJadescriptType) ta2);
                    }
                    return t1.typeEquals(t2) && v1 == v2;
                }
            ).allMatch(b -> b);
        }
        return super.typeEquals(other);
    }


    @Override
    public boolean isSupEqualTo(IJadescriptType other) {
        other = other.postResolve();
        final TypeHelper typeHelper = module.get(TypeHelper.class);

        if (other.typeEquals(typeHelper.NOTHING)) {
            return true;
        }

        if (other instanceof ParametricType) {
            final ParametricType parOther = (ParametricType) other;
            final boolean equals = this.typeID.equals(parOther.typeID);
            final boolean equalsSize =
                this.typeArguments.size() == parOther.typeArguments.size();
            return equals
                && equalsSize
                && Streams.zip(
                this.typeArguments.stream(),
                parOther.typeArguments.stream(),
                (ta1, ta2) -> {
                    BoundedTypeArgument.Variance v1;
                    IJadescriptType t1;
                    if (ta1 instanceof BoundedTypeArgument) {
                        v1 = ((BoundedTypeArgument) ta1).getVariance();
                        t1 = ((BoundedTypeArgument) ta1).getType();
                    } else {
                        v1 = BoundedTypeArgument.Variance.INVARIANT;
                        t1 = ((IJadescriptType) ta1);
                    }
                    BoundedTypeArgument.Variance v2;
                    IJadescriptType t2;
                    if (ta2 instanceof BoundedTypeArgument) {
                        v2 = ((BoundedTypeArgument) ta2).getVariance();
                        t2 = ((BoundedTypeArgument) ta2).getType();
                    } else {
                        v2 = BoundedTypeArgument.Variance.INVARIANT;
                        t2 = ((IJadescriptType) ta2);
                    }

                    if (t1.typeEquals(t2)) {
                        return v2 == BoundedTypeArgument.Variance.INVARIANT
                            || v1 == v2;
                    } else if (t1.isSupEqualTo(t2)) {
                        return v1 == BoundedTypeArgument.Variance.EXTENDS
                            && (v2 == BoundedTypeArgument.Variance.INVARIANT
                            || v2 == BoundedTypeArgument.Variance.EXTENDS);
                    } else if (t2.isSupEqualTo(t1)) {
                        return v1 == BoundedTypeArgument.Variance.SUPER
                            && (v2 == BoundedTypeArgument.Variance.INVARIANT
                            || v2 == BoundedTypeArgument.Variance.SUPER);
                    } else {
                        return false;
                    }
                }
            ).allMatch(b -> b);

        }

        if (this.typeEquals(other)) {
            return true;
        }


        return super.isSupEqualTo(other);
    }


    @Override
    public boolean isBasicType() {
        return false;
    }


    @Override
    public boolean validateType(
        Maybe<? extends EObject> input,
        ValidationMessageAcceptor acceptor
    ) {
        boolean v1 = VALID;
        boolean v2 = VALID;
        boolean v3 = VALID;
        if (upperBounds != null) {
            v1 = module.get(ValidationHelper.class).asserting(
                upperBounds.size() == typeArguments.size(),
                "InvalidParametricType",
                "Invalid number of type arguments; expected: " +
                    upperBounds.size() + ", provided: " +
                    typeArguments.size() + ".",
                input,
                acceptor
            );

            for (int i = 0; i < Math.min(
                upperBounds.size(),
                typeArguments.size()
            ); i++) {
                IJadescriptType upperBound = upperBounds.get(i);
                IJadescriptType typeArgument =
                    typeArguments.get(i).ignoreBound();
                final boolean vtemp = module.get(ValidationHelper.class)
                    .assertExpectedType(
                        upperBound,
                        typeArgument,
                        "InvalidParametricType",
                        input,
                        acceptor
                    );
                v2 = v2 && vtemp;
            }
        }


        for (TypeArgument typeArgument : typeArguments) {
            v3 = module.get(ValidationHelper.class).asserting(
                !typeArgument.ignoreBound().isErroneous(),
                "InvalidParametricType",
                "Invalid parametric type type: '" +
                    typeArgument.getJadescriptName() + "'.",
                input,
                acceptor
            );
        }

        return v1 && v2 && v3;
    }


    @Override
    public boolean isErroneous() {
        return getTypeArguments().stream()
            .map(TypeArgument::ignoreBound)
            .anyMatch(IJadescriptType::isErroneous);
    }


    public List<TypeArgument> getTypeArguments() {
        return typeArguments;
    }


    @Override
    public String compileConversionType() {

        return "new jadescript.util.types.JadescriptTypeReference(" +
            "jadescript.util.types.JadescriptBaseType." + getCategoryName() +
            (getTypeArguments().isEmpty() ? "" :
                ", " + getTypeArguments().stream()
                    .map(TypeArgument::ignoreBound)
                    .map(IJadescriptType::compileConversionType)
                    .reduce((s1, s2) -> s1 + ", " + s2).orElse("null")) +
            ")";
    }


    @Override
    public String getDebugPrint() {
        String sup = super.getDebugPrint();
        sup = sup.substring(0, sup.length() - 1); //removing the last '}'
        sup += "; typeArguments: [" + typeArguments.stream()
            .map(TypeArgument::getDebugPrint).collect(
                Collectors.joining(", ")
            ) + "]";
        sup += "}";
        return sup;
    }

}
