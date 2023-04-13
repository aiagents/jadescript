package it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.EmptyCreatable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.JadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agent.AgentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.BehaviourTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.utils.LazyInit;
import jadescript.core.behaviours.Behaviour;
import jadescript.core.behaviours.CyclicBehaviour;
import jadescript.core.behaviours.OneShotBehaviour;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.List;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.utils.LazyInit.lazyInit;

public class BaseBehaviourType
    extends JadescriptType
    implements EmptyCreatable, BehaviourType {

    private final Kind kind;
    private final TypeArgument forAgentType;
    private final LazyInit<IJadescriptType> baseAgentType
        = lazyInit(() -> module.get(BuiltinTypeProvider.class).agent());


    public BaseBehaviourType(
        SemanticsModule module,
        Kind kind,
        TypeArgument forAgentType
    ) {
        super(

            module,
            TypeHelper.builtinPrefix + BehaviourType.getTypeName(kind),
            BehaviourType.getTypeName(kind),
            "BEHAVIOUR"
        );

        this.kind = kind;
        this.forAgentType = forAgentType;
    }


    @Override
    public String compileNewEmptyInstance() {
        return "((" + compileToJavaTypeReference() + ") " +
            "jadescript.core.behaviours." +
            BehaviourType.getTypeName(getBehaviourKind()) +
            ".__createEmpty())";
    }


    @Override
    public String getParametricIntroductor() {
        return "for";
    }


    @Override
    public boolean requiresAgentEnvParameter() {
        return true;
    }


    @Override
    public IJadescriptType getForAgentType() {
        return forAgentType.ignoreBound();
    }


    public Kind getBehaviourKind() {
        return kind;
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
    public Maybe<OntologyType> getDeclaringOntology() {
        return Maybe.nothing();
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


    private final LazyInit<BehaviourTypeNamespace> namespace
        = lazyInit(() -> {

        final BuiltinTypeProvider builtins = module.get(
            BuiltinTypeProvider.class);
        return new BehaviourTypeNamespace(
            BaseBehaviourType.this.module,
            BaseBehaviourType.this,
            List.of(
                Property.readonlyProperty(
                    "behaviourName",
                    builtins.text(),
                    getLocation(),
                    Property.compileWithJVMGetter("behaviourName")
                ),
                Property.readonlyProperty(
                    "state",
                    builtins.text(),
                    getLocation(),
                    Property.compileGetWithCustomMethod("getExecutionState")
                ),
                Property.readonlyProperty(
                    "agent",
                    BaseBehaviourType.this.getForAgentType(),
                    getLocation(),
                    Property.compileGetWithCustomMethod("getJadescriptAgent")
                ),
                Property.readonlyProperty(
                    "isActive",
                    builtins.boolean_(),
                    getLocation(),
                    Property.compileGetWithCustomMethod("isActive")
                )
            )
        );
    });


    @Override
    public BehaviourTypeNamespace namespace() {
        return namespace.get();
    }


    @Override
    public Stream<IJadescriptType> declaredSupertypes() {
        return Stream.empty();
    }


    @Override
    public List<TypeArgument> typeArguments() {
        return List.of();
    }


    @Override
    public JvmTypeReference asJvmTypeReference() {
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        switch (kind) {
            case Cyclic:
                return jvm.typeRef(
                    CyclicBehaviour.class,
                    getForAgentType().asJvmTypeReference()
                );
            case OneShot:
                return jvm.typeRef(
                    OneShotBehaviour.class,
                    getForAgentType().asJvmTypeReference()
                );
            case Base:
            default:
                return jvm.typeRef(
                    Behaviour.class,
                    getForAgentType().asJvmTypeReference()
                );
        }
    }


}
