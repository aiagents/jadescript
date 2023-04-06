package it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.EmptyCreatable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.JadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agent.AgentType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.BehaviourTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import jadescript.core.behaviours.Behaviour;
import jadescript.core.behaviours.CyclicBehaviour;
import jadescript.core.behaviours.OneShotBehaviour;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class BaseBehaviourType
    extends JadescriptType
    implements EmptyCreatable, BehaviourType {

    private final Kind kind;
    private final TypeArgument forAgentType;
    private final IJadescriptType baseAgentType;
    private final List<Property> properties = new ArrayList<>();
    private boolean initializedProperties = false;


    public BaseBehaviourType(
        SemanticsModule module,
        Kind kind,
        AgentType forAgentType,
        IJadescriptType baseAgentType //TODO preferred removal
    ) {
        super(

            module,
            TypeHelper.builtinPrefix + BehaviourType.getTypeName(kind),
            BehaviourType.getTypeName(kind),
            "BEHAVIOUR"
        );

        this.kind = kind;
        this.forAgentType = forAgentType;
        this.baseAgentType = baseAgentType;
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


    private void initBuiltinProperties() {
        if (!initializedProperties) {
            this.addBultinProperty(
                Property.readonlyProperty(
                    "behaviourName",
                    module.get(TypeHelper.class).TEXT,
                    getLocation(),
                    Property.compileWithJVMGetter("behaviourName")
                )
            );
            this.addBultinProperty(
                Property.readonlyProperty(
                    "state",
                    module.get(TypeHelper.class).TEXT,
                    getLocation(),
                    Property.compileGetWithCustomMethod("getExecutionState")
                )
            );

            this.addBultinProperty(
                Property.readonlyProperty(
                    "agent",
                    getForAgentType(),
                    getLocation(),
                    Property.compileGetWithCustomMethod("getJadescriptAgent")
                )
            );

            this.addBultinProperty(
                Property.readonlyProperty(
                    "isActive",
                    module.get(TypeHelper.class).BOOLEAN,
                    getLocation(),
                    Property.compileGetWithCustomMethod("isActive")
                )
            );
        }
        this.initializedProperties = true;
    }


    public Kind getBehaviourKind() {
        return kind;
    }


    @Override
    public void addBultinProperty(Property prop) {
        properties.add(prop);
    }


    private List<Property> getBuiltinProperties() {
        initBuiltinProperties();
        return properties;
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


    @Override
    public BehaviourTypeNamespace namespace() {
        return new BehaviourTypeNamespace(module, this, getBuiltinProperties());
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
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        switch (kind) {
            case Cyclic:
                return typeHelper.typeRef(
                    CyclicBehaviour.class,
                    getForAgentType().asJvmTypeReference()
                );
            case OneShot:
                return typeHelper.typeRef(
                    OneShotBehaviour.class,
                    getForAgentType().asJvmTypeReference()
                );
            case Base:
            default:
                return typeHelper.typeRef(
                    Behaviour.class,
                    getForAgentType().asJvmTypeReference()
                );
        }
    }


}
