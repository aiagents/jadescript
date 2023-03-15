package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import org.eclipse.xtext.common.types.JvmTypeReference;

public abstract class UserDefinedType<T extends JadescriptType>
        extends JadescriptType {


    private final JvmTypeReference jvmType;
    private final T rootCategoryType;

    public UserDefinedType(
            SemanticsModule module,
            JvmTypeReference jvmType,
            T rootCategoryType
    ) {
        super(
                module,
                jvmType.getQualifiedName('.'),
                jvmType.getQualifiedName('.'),
                rootCategoryType.getCategoryName()
        );
        this.jvmType = jvmType;
        this.rootCategoryType = rootCategoryType;
    }

    @Override
    public JvmTypeReference asJvmTypeReference() {
        return jvmType;
    }


    public T getRootCategoryType() {
        return rootCategoryType;
    }

    @Override
    public void addProperty(Property prop) {
        // Do nothing
    }


    @Override
    public boolean isBasicType() {
        return false;
    }

    @Override
    public boolean isSlottable() {
        return getRootCategoryType().isSlottable();
    }

    @Override
    public boolean isSendable() {
        return false;
    }

    @Override
    public boolean isReferrable() {
        return true;
    }

    @Override
    public boolean hasProperties() {
        return true;
    }

    @Override
    public boolean isErroneous() {
        return false;
    }

    @Override
    public boolean isCollection() {
        return false;
    }
}
