package it.unipr.ailab.jadescript.semantics.jadescripttypes.agentenv;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.JadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategory;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategoryAdapter;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.EmptyTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import jadescript.java.AgentEnv;
import jadescript.java.SideEffectsFlag;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypeReferenceBuilder;

import java.util.List;
import java.util.stream.Stream;

public class AgentEnvType extends JadescriptType {

    public static final TypeCategoryAdapter CATEGORY
        = new TypeCategoryAdapter() {
        @Override
        public boolean isAgentEnv() {
            return true;
        }
    };

    private final TypeArgument forAgent;
    private final SEMode seMode;


    public AgentEnvType(
        SemanticsModule module,
        TypeArgument forAgent,
        TypeArgument seMode
    ) {
        super(
            module,
            TypeHelper.builtinPrefix + "AgentEnv",
            "(internal)AgentEnv",
            "OTHER"
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


    public static Class<?> toSEModeClass(SEMode mode) {
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
    public String getParametricIntroductor() {
        return "#";
    }


    @Override
    public String getParametricListSeparator() {
        return "|";
    }


    @Override
    public Stream<IJadescriptType> declaredSupertypes() {
        return Stream.of();
    }


    @Override
    public List<TypeArgument> typeArguments() {
        return List.of(
            this.forAgent,
            module.get(TypeSolver.class)
                .fromClass(toSEModeClass(this.seMode))
        );
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


    @Override
    public boolean isErroneous() {
        return false;
    }


    public TypeArgument getAgentType() {
        return forAgent;
    }


    public boolean isWithoutSideEffects() {
        return seMode == SEMode.NO_SE;
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
    public TypeCategory category() {
        return CATEGORY;
    }


    @Override
    public TypeNamespace namespace() {
        return module.get(EmptyTypeNamespace.class);
    }


    public enum SEMode {
        ANY, NO_SE, WITH_SE
    }

}
