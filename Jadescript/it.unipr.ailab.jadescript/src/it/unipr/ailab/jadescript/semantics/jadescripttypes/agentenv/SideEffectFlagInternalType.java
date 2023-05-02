package it.unipr.ailab.jadescript.semantics.jadescripttypes.agentenv;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategory;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategoryAdapter;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.util.UtilityType;
import it.unipr.ailab.jadescript.semantics.namespace.EmptyTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;

import java.util.stream.Stream;

public class SideEffectFlagInternalType extends UtilityType {

    private final SEMode seMode;


    public SideEffectFlagInternalType(
        SemanticsModule module,
        SEMode seMode
    ) {
        super(
            module,
            typeIDFromMode(seMode),
            typeIDFromMode(seMode),
            module.get(JvmTypeHelper.class)
                .typeRef(AgentEnvType.toSEModeClass(seMode))
        );
        this.seMode = seMode;
    }


    public static String typeIDFromMode(SEMode mode) {
        switch (mode) {
            case BOTTOM:
                return TypeHelper.builtinPrefix + "ANY_SE_MODE_CLASS";
            case NO_SE:
                return TypeHelper.builtinPrefix + "NO_SE_MODE_CLASS";
            case WITH_SE:
                return TypeHelper.builtinPrefix + "WITH_SE_MODE_CLASS";
            case TOP:
            default:
                return TypeHelper.builtinPrefix + "TOP_SE_MODE_CLASS";
        }
    }


    @Override
    public TypeCategory category() {
        return new TypeCategoryAdapter() {
            @Override
            public boolean isSideEffectFlag() {
                return true;
            }
        };
    }


    @Override
    public boolean isSendable() {
        return false;
    }


    @Override
    public boolean isErroneous() {
        return false;
    }


    @Override
    public TypeNamespace namespace() {
        return module.get(EmptyTypeNamespace.class);
    }


    public SEMode getMode() {
        return seMode;
    }


    @Override
    public Stream<IJadescriptType> declaredSupertypes() {
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        switch (seMode){
            case BOTTOM:
                return Stream.of(
                    builtins.seModeWithSE(),
                    builtins.seModeNoSE()
                );
            case NO_SE:
            case WITH_SE:
                return Stream.of(
                    builtins.seModeTop()
                );
            case TOP:
            default:
                return Stream.empty();
        }
    }

}
