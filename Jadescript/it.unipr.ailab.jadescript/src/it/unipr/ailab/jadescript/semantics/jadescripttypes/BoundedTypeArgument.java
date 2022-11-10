package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypeReferenceBuilder;

public class BoundedTypeArgument implements TypeArgument{

    private final SemanticsModule module;
    private final IJadescriptType type;
    private final Variance variance;

    public BoundedTypeArgument(SemanticsModule module, IJadescriptType type, Variance variance) {
        this.module = module;
        this.type = type;
        this.variance = variance;
    }

    public enum Variance{
        INVARIANT,
        SUPER,
        EXTENDS
    }




    @Override
    public String getID() {
        switch (variance){
            case EXTENDS:
                return "«subtype of " + type.getID()+"»";
            case SUPER:
                return "«supertype of " + type.getID()+"»";
            default:
                return type.getID();
        }
    }

    @Override
    public String getJadescriptName() {
        switch (variance){
            case EXTENDS:
                return "«subtype of " + type.getJadescriptName()+"»";
            case SUPER:
                return "«supertype of " + type.getJadescriptName()+"»";
            default:
                return type.getJadescriptName();
        }
    }

    @Override
    public JvmTypeReference asJvmTypeReference() {
        switch (variance){
            case EXTENDS:
                return module.get(JvmTypeReferenceBuilder.class).wildcardExtends(type.asJvmTypeReference());
            case SUPER:
                return module.get(JvmTypeReferenceBuilder.class).wildcardSuper(type.asJvmTypeReference());
            default:
                return type.asJvmTypeReference();
        }
    }

    @Override
    public IJadescriptType ignoreBound() {
        return type;
    }

    @Override
    public String getDebugPrint() {
        switch (variance){
            case SUPER:
                return "«? super " + type.getDebugPrint() + "»";
            case EXTENDS:
                return "«? extends " + type.getDebugPrint() + "»";
            default:
                return type.getDebugPrint();
        }
    }


    public IJadescriptType getType() {
        return type;
    }

    public Variance getVariance() {
        return variance;
    }
}
