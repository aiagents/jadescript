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


    private static Boolean typeArgumentIsSuperOrEquals(
        TypeArgument ta1,
        TypeArgument ta2
    ) {
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
        } else if (t1.isSupertypeOrEqualTo(t2)) {
            return v1 == BoundedTypeArgument.Variance.EXTENDS
                && (v2 == BoundedTypeArgument.Variance.INVARIANT
                || v2 == BoundedTypeArgument.Variance.EXTENDS);
        } else if (t2.isSupertypeOrEqualTo(t1)) {
            return v1 == BoundedTypeArgument.Variance.SUPER
                && (v2 == BoundedTypeArgument.Variance.INVARIANT
                || v2 == BoundedTypeArgument.Variance.SUPER);
        } else {
            return false;
        }
    }


    private static Boolean typeArgumentEquals(
        TypeArgument ta1,
        TypeArgument ta2
    ) {
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


    @Override
    public String getID() {
        return typeID + "<" + typeArguments.stream()
            .map(TypeArgument::getID)
            .reduce((s1, s2) -> s1 + ", " + s2).orElse("")
            + ">";
    }


    public String getJadescriptName() {
        String result = simpleName;
        result += " " + parametricIntroductor + " " +
            parametricListDelimiterOpen + typeArguments.stream()
            .map(TypeArgument::getJadescriptName)
            .map(s -> "(" + s + ")")
            .reduce((s1, s2) -> s1 + parametricListSeparator + s2).orElse(
                "(###notypeargs###)")
            + parametricListDelimiterClose;
        return result;
    }


    public boolean parametricTypeEquals(ParametricType other) {
        if (!super.typeEquals(other)
            || this.getTypeArguments().size()
            != other.getTypeArguments().size()) {
            return false;
        }

        int size = this.typeArguments.size();
        for (int i = 0; i < size; i++) {
            final TypeArgument arg1 = this.getTypeArguments().get(i);
            final TypeArgument arg2 = other.getTypeArguments().get(i);

            if (!typeArgumentEquals(arg1, arg2)) {
                return false;
            }
        }

        return true;
    }


    @Override
    public boolean typeEquals(IJadescriptType other) {
        if (other instanceof ParametricType) {
            return parametricTypeEquals(((ParametricType) other));
        }

        return super.typeEquals(other);
    }


    public boolean containerTypeEquals(IJadescriptType other) {
        return super.typeEquals(other);
    }


    public boolean isParametricSupertypeOrEqualTo(ParametricType other) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);

        if (other.typeEquals(typeHelper.NOTHING)) {
            return true;
        }

        if (!containerTypeEquals(other)) {
            return false;
        }


        if (this.getTypeArguments().size() != other.getTypeArguments().size()) {
            return false;
        }

        int size = this.getTypeArguments().size();

        for (int i = 0; i < size; i++) {
            final TypeArgument arg1 = this.getTypeArguments().get(i);
            final TypeArgument arg2 = other.getTypeArguments().get(i);

            if(!typeArgumentIsSuperOrEquals(arg1, arg2)){
                return false;
            }
        }

        return true;
    }


    @Override
    public boolean isSupertypeOrEqualTo(IJadescriptType other) {
        other = other.postResolve();
        final TypeHelper typeHelper = module.get(TypeHelper.class);

        if (other instanceof ParametricType) {
            return isParametricSupertypeOrEqualTo(((ParametricType) other));
        }

        if (other.typeEquals(typeHelper.NOTHING)) {
            return true;
        }


        if (this.typeEquals(other)) {
            return true;
        }


        return super.isSupertypeOrEqualTo(other);
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
            "jadescript.util.types.JadescriptBuiltinTypeAtom." +
            getCategoryName() +
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
