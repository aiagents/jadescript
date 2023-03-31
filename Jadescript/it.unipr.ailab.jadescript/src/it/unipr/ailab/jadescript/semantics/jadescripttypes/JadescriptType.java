package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.JadescriptTypeLocation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.jetbrains.annotations.Nullable;

import static it.unipr.ailab.maybe.Maybe.nothing;

public abstract class JadescriptType
    implements SemanticsConsts, IJadescriptType {

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
        return typeID;
    }


    @Override
    public boolean validateType(
        Maybe<? extends EObject> input,
        ValidationMessageAcceptor acceptor
    ) {
        return module.get(ValidationHelper.class).asserting(
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
        return nothing();
    }


    @Override
    public String compileConversionType() {
        return "new jadescript.util.types.JadescriptTypeReference(" +
            "jadescript.util.types.JadescriptBuiltinTypeAtom." +
            getCategoryName() + ")";
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
        return getJadescriptName()
            + "{Class=("
            + getClass().getSimpleName()
            + "); JvmTypeReference=("
            + compileToJavaTypeReference()
            + ")}";
    }

}
