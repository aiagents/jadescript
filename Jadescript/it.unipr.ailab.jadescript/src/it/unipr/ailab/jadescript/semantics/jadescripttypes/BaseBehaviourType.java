package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.BehaviourTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import jadescript.core.behaviours.Behaviour;
import jadescript.core.behaviours.CyclicBehaviour;
import jadescript.core.behaviours.OneShotBehaviour;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.*;

public class BaseBehaviourType
    extends ParametricType
    implements EmptyCreatable, BehaviourType {

    private final Kind kind;
    private final TypeArgument forAgentType;
    private final List<Property> properties = new ArrayList<>();
    private boolean initializedProperties = false;


    public BaseBehaviourType(
        SemanticsModule module,
        Kind kind,
        TypeArgument forAgentType,
        IJadescriptType baseAgentType
    ) {
        super(

            module,
            TypeHelper.builtinPrefix + getTypeName(kind),
            getTypeName(kind),
            "BEHAVIOUR",
            "for",
            "",
            "",
            "",
            Arrays.asList(forAgentType),
            Arrays.asList(baseAgentType)
        );

        this.kind = kind;
        this.forAgentType = forAgentType;
    }


    private static String getTypeName(Kind kind) {
        switch (kind) {
            case Cyclic:
                return "CyclicBehaviour";
            case OneShot:
                return "OneShotBehaviour";
            case Base:
            default:
                return "Behaviour";
        }
    }


    @Override
    public String compileNewEmptyInstance() {
        return "((" + compileToJavaTypeReference() + ") " +
            "jadescript.core.behaviours." + getTypeName(getBehaviourKind()) +
            ".__createEmpty())";
    }


    @Override
    public boolean requiresAgentEnvParameter() {
        return true;
    }


    public IJadescriptType getForAgentType() {
        return forAgentType.ignoreBound();
    }


    private void initBuiltinProperties() {
        if (!initializedProperties) {
            this.addProperty(
                Property.readonlyProperty(
                    "behaviourName",
                    module.get(TypeHelper.class).TEXT,
                    getLocation(),
                    Property.compileWithJVMGetter("behaviourName")
                )
            );
            this.addProperty(
                Property.readonlyProperty(
                    "state",
                    module.get(TypeHelper.class).TEXT,
                    getLocation(),
                    Property.compileGetWithCustomMethod("getExecutionState")
                )
            );

            this.addProperty(
                Property.readonlyProperty(
                    "agent",
                    getForAgentType(),
                    getLocation(),
                    Property.compileGetWithCustomMethod("getJadescriptAgent")
                )
            );

            this.addProperty(
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
    public void addProperty(Property prop) {
        properties.add(prop);
    }


    private List<Property> getBuiltinProperties() {
        initBuiltinProperties();
        return properties;
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
    public boolean isCollection() {
        return false;
    }


    @Override
    public boolean isSupEqualTo(IJadescriptType other) {
        other = other.postResolve();
        if (other instanceof UserDefinedBehaviourType) {
            return (
                this.getBehaviourKind().equals(Kind.Base)
                    || this.getBehaviourKind().equals(
                    ((UserDefinedBehaviourType) other).getBehaviourKind()
                )
            ) && this.getForAgentType().isSupEqualTo(
                ((UserDefinedBehaviourType) other).getForAgentType()
            );
        }
        return super.isSupEqualTo(other);
    }


    @Override
    public BehaviourTypeNamespace namespace() {
        return new BehaviourTypeNamespace(module, this, getBuiltinProperties());
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


    public enum Kind {
        Cyclic, OneShot, Base
    }

}
