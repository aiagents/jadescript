package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import org.eclipse.xtext.common.types.JvmTypeReference;

public abstract class UtilityType extends JadescriptType{

    private final JvmTypeReference jvmType;

    public UtilityType(
            SemanticsModule module,
            String typeID,
            String simpleName,
            JvmTypeReference jvmType
    ) {
        super(module, typeID, simpleName, "OTHER");
        this.jvmType = jvmType;
    }


    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public void addProperty(Property prop) {

    }


    @Override
    public boolean isBasicType() {
        return false;
    }

    @Override
    public boolean isSlottable() {
        return false;
    }

    @Override
    public boolean isReferrable() {
        return false;
    }

    @Override
    public boolean isManipulable() {
        return false;
    }

    @Override
    public JvmTypeReference asJvmTypeReference(){
        return jvmType;
    }
}
