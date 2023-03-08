package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.EmptyTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import jadescript.java.AgentEnv;
import jadescript.java.SideEffectsFlag;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypeReferenceBuilder;

import java.util.List;

public class AgentEnvType extends ParametricType {

    private final TypeArgument forAgent;
    private final SEMode seMode;


    public AgentEnvType(
        SemanticsModule module,
        TypeArgument forAgent,
        TypeArgument seMode,
        BaseAgentType baseAgentType
    ) {
        super(
            module,
            TypeHelper.builtinPrefix + "AgentEnv",
            "(internal)AgentEnv",
            "OTHER",
            "for",
            "(",
            ")",
            "",
            List.of(forAgent),
            List.of(baseAgentType)
        );
        this.forAgent = forAgent;
        this.seMode = getFromTypeArgument(seMode);
    }


    public static SEMode getFromTypeArgument(TypeArgument seMode) {
        if (SideEffectsFlag.AnySideEffectFlag.class.getName()
            .equals(seMode.compileToJavaTypeReference())) {
            return SEMode.ANY;
        } else if (SideEffectsFlag.WithSideEffects.class.getName()
            .equals(seMode.compileToJavaTypeReference())) {
            return SEMode.WITH_SE;
        } else if (SideEffectsFlag.NoSideEffects.class.getName()
            .equals(seMode.compileToJavaTypeReference())) {
            return SEMode.NO_SE;
        } else {
            return SEMode.ANY;
        }
    }

    public static Class<?> toSEModeClass(SEMode mode){
        switch (mode) {
            case WITH_SE:
                return SideEffectsFlag.WithSideEffects.class;
            case NO_SE:
                return SideEffectsFlag.NoSideEffects.class;
            default:
            case ANY:
                return SideEffectsFlag.AnySideEffectFlag.class;
        }
    }


    @Override
    public JvmTypeReference asJvmTypeReference() {
        final JvmTypeReferenceBuilder jtrb =
            module.get(JvmTypeReferenceBuilder.class);
        Class<?> seModeClass;
        switch (this.seMode) {
            case ANY:
                seModeClass = SideEffectsFlag.AnySideEffectFlag.class;
                break;
            case WITH_SE:
                seModeClass = SideEffectsFlag.WithSideEffects.class;
                break;
            default:
            case NO_SE:
                seModeClass = SideEffectsFlag.NoSideEffects.class;
                break;
        }
        return jtrb.typeRef(
            AgentEnv.class,
            forAgent.asJvmTypeReference(),
            jtrb.typeRef(seModeClass)
        );
    }


    public TypeArgument getAgentType() {
        return forAgent;
    }



    public boolean isWithoutSideEffects(){
        return seMode == SEMode.NO_SE;
    }


    @Override
    public boolean isSupEqualTo(IJadescriptType other) {
        if (other.compileToJavaTypeReference()
            .startsWith("jadescript.java.AgentEnv")) {
            return true;
        }
        return super.isSupEqualTo(other);
    }


    @Override
    public boolean isSlottable() {
        return false;
    }


    @Override
    public boolean isSendable() {
        return false;
    }


    @Override
    public boolean isReferrable() {
        return false;
    }


    @Override
    public boolean hasProperties() {
        return false;
    }


    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        return Maybe.nothing();
    }


    @Override
    public boolean isCollection() {
        return false;
    }


    @Override
    public TypeNamespace namespace() {
        return module.get(EmptyTypeNamespace.class);
    }


    @Override
    public void addProperty(Property prop) {
        //do nothing
    }


    public enum SEMode {
        ANY, NO_SE, WITH_SE
    }

}
