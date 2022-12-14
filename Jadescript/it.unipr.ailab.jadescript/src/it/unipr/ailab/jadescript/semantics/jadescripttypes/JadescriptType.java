package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.JadescriptTypeLocation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.namespace.JvmModelBasedNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.jetbrains.annotations.Nullable;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.of;

public abstract class JadescriptType implements SemanticsConsts, IJadescriptType {
    protected final SemanticsModule module;
    protected final String typeID;
    protected final String simpleName;
    protected final String categoryName;

    public JadescriptType(
            SemanticsModule module,
            String typeID,
            String simpleName,
            String categoryName
    ) {
        this.module = module;
        this.typeID = typeID;
        this.simpleName = simpleName;
        this.categoryName = categoryName;
    }

    @Override
    public boolean typeEquals(IJadescriptType other) {
        if (other == null) return false;
        return this == other || this.getID().equals(other.getID());
    }

    @Override
    public String getCategoryName() {
        return categoryName;
    }

    @Override
    public SearchLocation getLocation() {
        return new JadescriptTypeLocation(this);
    }

    @Override
    public boolean isAssignableFrom(IJadescriptType other) {
        other = other.postResolve();
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (other.typeEquals(typeHelper.NOTHING)) {
            return true;
        }

        if (this.typeEquals(other)) {
            return true;
        }

        if (typeHelper.implicitConversionCanOccur(other, this)) {
            return true;
        }

        return typeHelper.isAssignable(
                this.asJvmTypeReference(),
                other.asJvmTypeReference()
        );
    }

    public abstract void addProperty(Property prop);


    @Override
    public String getID() {
        return typeID;
    }


    @Override
    public void validateType(Maybe<? extends EObject> input, ValidationMessageAcceptor acceptor) {
        module.get(ValidationHelper.class).assertion(
                !isErroneous(),
                "InvalidType",
                "Invalid type: '" + getJadescriptName() + "'.",
                input,
                acceptor
        );
    }

    @Override
    public String toString() {
        return getJadescriptName();
    }


    @Override
    public String getJadescriptName() {
        return simpleName;
    }


    @Override
    public Maybe<IJadescriptType> getElementTypeIfCollection() {
        if (this instanceof MapType) {
            return of(((MapType) this).getValueType());
        } else if (this instanceof ListType) {
            return of(((ListType) this).getElementType());
        } else if (this instanceof SetType) {
            return of(((SetType) this).getElementType());
        } else {
            return nothing();
        }
    }


    @Override
    public String compileConversionType() {

        return "new jadescript.util.types.JadescriptTypeReference(" +
                "jadescript.util.types.JadescriptBaseType." + getCategoryName() + ")";
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
    public JvmModelBasedNamespace jvmNamespace() {
        return JvmModelBasedNamespace.fromTypeReference(module, asJvmTypeReference());
    }

    @Override
    public String getDebugPrint() {
        return getJadescriptName()
                + "{Class=("
                + getClass().getSimpleName()
                + "); JvmTypeReference=("
                + compileToJavaTypeReference()
                + ")}";
    }
}
